(ns scale-server.core
    (:use compojure.core)
    (:require [compojure.route :as route]
	      [compojure.handler :as handler]
	      [clojure.string :as str]
	      [fs.core :as fs]))

(def stylesheet
     {:status 200
     :headers {"Content-Type" "text/css"}
     :body
     "
div.multicolumn2 {
	[-moz- | -webkit-]column-width: 512px;
	[-moz- | -webkit-]column-gap: 10px;
        -moz-column-count: 2;
        -webkit-column-count: 2;
}

div.multicolumn3 {
	[-moz- | -webkit-]column-width: 400px;
	[-moz- | -webkit-]column-gap: 10px;
        -moz-column-count: 3;
        -webkit-column-count: 3;
}

div.multicolumn8 {
	[-moz- | -webkit-]column-width: 150px;
	[-moz- | -webkit-]column-gap: 10px;
        -moz-column-count: 8;
        -webkit-column-count: 8;

}
"})


(defn name-to-url [fname]
  (let [name (nth (re-find #"(.*)(.scl)" fname) 1)]
       (str "<a href=\"showscale?scale=" name "\">" name "</a> \n")))

(defn list-scales []
  (let [scale-files (fs/list-dir "scl")
        header "<html><head><title>Scale List</title>
<link href=\"scales.css\" rel=\"stylesheet\" type=\"text/css\"></head>"
	intro "<body><h1>Scales available for display:</h1><div class=\"multicolumn8\">"
	footer "</div></body></html>"]
       (str header intro
	    (apply str (map name-to-url (sort scale-files)))
	    footer)))

(defn diagram [positions scale-count]
  (let [svg-rect (fn [x0 y0 x1 y1 color]
		     (str "<path d=\"M" x0 " " y0 " L" x1 " " y0 " L" x1 " "
			  y1 " L" x0 " " y1 "Z\" fill=\"" color "\"/>\n"))
	svg-dot (fn [x y r c]
		    (str "<circle cx=\"" x "\" cy=\"" y "\" r=\"" r
			 "\" stroke=\"black\""
			 " stroke-width=\"2\" fill=\"" c "\"/>\n"))
	svg-arrow (fn [x y c]
		      (str "<path d=\"M0 0 L7 -5 L7 5 L0 0 Z\" fill=\"" c
			   "\" stroke=\"black\""
			   " transform=\"" "translate("
			   x "," y ")\"/>\n"))
	string-start 10.
	string-end 600.
	string-len (- string-end string-start)
	string-x0s [35 75 115 155]
	string-x1s [53 89 126 163]
	color-cycle
          (fn [n reps]
	      (let [from-0 (< reps 0)
			   reps (Math/abs reps)
			   count (int (Math/ceil (/ n reps)))]
		(cycle
		 (map (fn [x]
			  (mod (int (* (if from-0 (- count x) x)
				       (/ 255.0 count)))
			       256))
		      (range 0 count)))))
        ;; get unique colors for each degree of the scale
        ;; for scale-count degrees of the scale, this gets us scale-count colors
	colors (map (fn [r g b]
			(format "#%02X%02X%02X" r g b))
		    (color-cycle scale-count -1)
		    (color-cycle scale-count 2)
		    (color-cycle scale-count 3))]
     (str "<div float=\"right\" margin=\"0\" padding=\"0\"><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 200 600\">\n"
	  ; display instrument strings
	  (apply str (map (fn [x]
			      (svg-rect (nth string-x0s x)
					string-start
					(nth string-x1s x)
					string-end "black"))
			  (range 0 4)))
	  ; display fingering markings
	  (apply str (map (fn [x]
			      (svg-arrow (+ (nth string-x1s (nth x 0)) 2)
					 (+ string-start
					    (* string-len (nth x 1)))
					 (nth colors (nth x 2))))
			  positions))
	  ; display reference marks
	  (svg-dot 25 (+ string-start (/ string-len 2)) 8 "gray") ;octave
	  (svg-dot 25 (+ string-start (* string-len (/ 3 4))) 6 "gray") ;two oct
	  (svg-dot 25 (+ string-start (/ string-len 3)) 6 "gray") ;fifth
	  (svg-dot 25 (+ string-start (/ string-len 4)) 6 "gray") ;fourth
	  (svg-dot 25 (+ string-start (/ string-len 5)) 6 "gray") ;third
	  "</svg>"
	  "</div>\n")))

(def fundamental 130.813)

(def cents-base (Math/pow 2 (/ 1 1200)))

(defn all-spots [freqs]
  (let [string-data [[fundamental 0]
		     [(* fundamental (Math/pow cents-base 700)) 1]
		     [(* fundamental (Math/pow cents-base 1400)) 2]
		     [(* fundamental (Math/pow cents-base 2100)) 3]]
	two-oct-plus 4.1
	coord (fn [hz hz-pos degree]
		  (let [base-hz (nth hz-pos 0)
			pos (nth hz-pos 1)]
					; stop display at just over two octaves
		    (if (or (< hz base-hz) (> hz (* base-hz two-oct-plus)))
			false
			[pos (- 1 (/ base-hz hz)) degree])))]
    (filter identity
	    (mapcat (fn [hz-deg] 
			(map (fn [hz-pos]
				 (coord (nth hz-deg 0) hz-pos (nth hz-deg 1)))
			     string-data))
		    freqs))))

(defn line-mult [line comment]
  (cond
   (re-find #"!.*scl" line)
     :do-nothing
   (= line "!")
     :toggle-comment
   (and (not comment) (re-find #"[0-9]+/[0-9]+" line))
     (let [match (re-find #"([0-9]+)/([0-9]+)" line)]
       (/ (read-string (nth match 1))
	  (read-string (nth match 2))))
   (and (not comment) (re-find #"^ *[0-9]+\.?[0-9]*" line))
     (let [match (re-find #"([0-9]+\.?[0-9]*)" line)]
     	 (Math/pow cents-base (read-string (nth match 1))))
   true
    :do-nothing))

(defn mults-rec [mults lines comment count]
  (if (= lines '())
      [mults count]
  (let [line (first lines)
        lines (rest lines)
	mult (line-mult line comment)]
    (cond
     (= mult :do-nothing)
       (mults-rec mults lines comment count)
     (= mult :toggle-comment)
       (mults-rec mults lines (not comment) count)
     true
       (mults-rec (cons mult mults) lines comment (+ count 1))))))

(defn lines-rec [lines]
  "scoop up the header"
  (if (= lines '())
      []
    (let [line (first lines)
	  lines (rest lines)
	  read (line-mult line false)]
      (if (or (= read :do-nothing) (= read :toggle-comment))
	  (lines-rec lines)
	;; after our first numeric result, we can start looking for a multiplier
	(mults-rec '(1) lines true 0)))))

(defn freqs-hz [base mults nmults]
  (loop [base base mults mults degrees (cycle (range 0 nmults)) acc []]
	(if (> base (* 440 4)) ; maximum frequency we display
	    acc
	  (let [next (sort (fn [x y]
			       (> (first x)
				  (first y)))
			   (reduce conj acc (map (fn [d x] [(* x base) d])
						   degrees
						   mults)))]
	    (if (= next '()) '()
	      (recur (first (first next)) mults degrees next))))))

(defn show-info [mults]
  (let [log (fn [x base] (/ (Math/log x) (Math/log base)))
	degrees ["c" "c#" "d" "d#" "e" "f" "f#" "g" "g#" "a" "a#" "b"]]
    (map (fn [x]
	     (let [cents (log x cents-base)
		   degree (mod (int (/ cents 100)) 12)]
	       (str "<tr><td>"
		    (if (= (class x) clojure.lang.Ratio)
			x
		      (format "%.4f" (* x 1.0))) "</td><td>"
		    (format "%.3f" cents) "</td><td>"
		    (nth degrees degree)
		    "+" (int (- (mod cents 1200) (* degree 100))) "</td><td>"
		    (format "%.4f" (* x fundamental)) "</td></tr>")))
	 (sort mults))))
     
(defn process-scale-request [req]
  (let [scale-request (str/split req #":")
       name (nth scale-request 0)
       header (str "<html xmlns='http://www.w3.org/1999/xhtml'><head>
<title>" name "</title><link href=\"scales.css\" rel=\"stylesheet\" type=\"text/css\"></link></head><body>")
       footer (str "</div></body></html>")
       scale-text (try (slurp (str "scl/" name ".scl"))
		       (catch Exception e
			      (str "failed in opening file: scl/" name "<br/>"
				   (.getMessage e))))
       lines (str/split-lines scale-text)
       base 130.813
       mults+len (lines-rec lines)
       mults-len (nth mults+len 1)
       mults (nth mults+len 0)]
    {:status 200
     :headers {"Content-Type" "application/xhtml+xml"}
     :body
     (str header "<h1>" name "</h1><br/><div class=\"multicolumn2\">"
	  "<div float=\"left\" margin=\"0\" padding=\"0\">"
	  "<table border=\"1\"><tr>"
	  "<th>mult</th><th>cents</th><th>note</th><th>hz</th></tr>"
	  (apply str (show-info mults))
	  "</table>"
	  "<pre>"
	  (str/escape scale-text {\< "&lt;", \> "&gt;", \& "&amp;"})
	  "</pre>"
	  "</div>"
	  (diagram (all-spots (freqs-hz base (sort > mults) mults-len))
		   mults-len)
	  footer)}))

(def scl-memo (memoize list-scales))

(defroutes main-routes
  (GET "/" [] (scl-memo))
  (GET "/list" [] (scl-memo))
  (GET "/scales.css" [] stylesheet)
  (route/resources "/")
  (GET "/showscale*" {params :query-params}
		  (process-scale-request (get params "scale")))
  (route/not-found "<h1>Page not found</h1>"))

(def app
     (handler/site main-routes))
