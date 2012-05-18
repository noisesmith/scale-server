(ns demo.core
    (:use compojure.core)
    (:require [compojure.route :as route]
	      [compojure.handler :as handler]
	      [clojure.string :as str]
	      [fs.core :as fs]))

(def stylesheet
"div.multicolumn2 {
	[-moz- | -webkit-]column-width: 400px;
	[-moz- | -webkit-]column-gap: 20px;
        -moz-column-count: 2;
        -webkit-column-count: 2;
div.multicolumn8 {
	[-moz- | -webkit-]column-width: 150px;
	[-moz- | -webkit-]column-gap: 20px;
        -moz-column-count: 8;
        -webkit-column-count: 8;

}
")


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

(defn diagram [positions]
  (let [svg-rect (fn [x0 y0 x1 y1 color]
		     (str "<path d=\"M" x0 " " y0 " L" x1 " " y0 " L" x1 " "
			  y1 " L" x0 " " y1 "Z\" fill=\"" color "\"/>"))
	svg-dot (fn [x y r c]
		    (str "<circle cx=\"" x "\" cy=\"" y "\" r=\"" r
			 "\" stroke=\"black\""
			 " stroke-width=\"2\" fill=\"" c "\"/>"))
	svg-arrow (fn [x y c]
		      (str "<path d=\"M0 0 L10 -5 L10 5 L0 0 Z\" fill=\"" c
			   "\" stroke=\"black\""
			   " transform=\"" "translate(" x "," y ")\"/>"))]
     (str "<div float=\"right\" margin=\"0\" padding=\"0\"><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" viewBox=\"0 0 600 800\">"
	  ; strings
	  (svg-rect 35 10 53 600 "black")
	  (svg-rect 75 10 89 600 "black")
	  (svg-rect 115 10 126 600 "black")
	  (svg-rect 155 10 163 600 "black")
	  (apply str (map (fn [x] (svg-arrow (nth x 0) (+ 10 (nth x 1)) "red"))
			  positions))
	  (svg-dot 25 308 8 "gray")
	  (svg-dot 25 456 6 "gray")
	  (svg-dot 25 206 6 "gray") ;fifth
	  "</svg></div>\n")))

(def fundamental 130.813)

(def cents-base (Math/pow 2 (/ 1 1200)))

(defn all-spots [freqs]
  (let [string-data [[fundamental 55]
		     [(* fundamental (Math/pow cents-base 700)) 91]
		     [(* fundamental (Math/pow cents-base 1400)) 127]
		     [(* fundamental (Math/pow cents-base 2100)) 165]]
	string-scale 600
	coord (fn [hz hz-pos]
		  (let [base-hz (nth hz-pos 0)
			pos (nth hz-pos 1)]
		    (if (or (< hz base-hz) (> hz (* base-hz 4)))
			false
			[pos (- string-scale (* string-scale
						      (/ base-hz hz)))])))
	output '()]
    (filter identity
	    (mapcat (fn [hz] 
			(map (fn [hz-pos]
				 (coord hz hz-pos))
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

(defn mults-rec [mults lines comment]
  (if (= lines '())
      mults
  (let [line (first lines)
        lines (rest lines)
	mult (line-mult line comment)]
    (cond
     (= mult :do-nothing)
       (mults-rec mults lines comment)
     (= mult :toggle-comment)
       (mults-rec mults lines (not comment))
     true
       (mults-rec (cons mult mults) lines comment)))))

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
	(mults-rec '(1) lines true)))))

(defn freqs-hz [base mults]
  (loop [base base mults mults acc []]
	(if (> base (* 440 4)) ; maximum frequency we display
	    acc
	  (let [next (sort > (reduce conj acc (map (fn [x] (* x base)) mults)))]
	    (if (= next '()) '()
	      (recur (nth next 0) mults next))))))

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
		    (format "%.4f" (* x fundamental)) "</td></tr>"))) mults)))
     
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
       mults (sort (lines-rec lines))]
    {:status 200
     :headers {"Content-Type" "application/xhtml+xml"}
     :body
     (str header "<h1>" name "</h1><br/><div class=\"multicolumn2\">"
	  "<div float=\"left\" margin=\"0\" padding=\"0\"><pre>"
	  scale-text
	  "</pre>"
	  "<table border=\"1\"><tr>"
	  "<th>mult</th><th>cents</th><th>note</th><th>hz</th></tr>"
	  (apply str (show-info mults))
	  "</table></div>"
	  (diagram (all-spots (freqs-hz base (sort > mults))))
	  footer)}))

(def scl-memo (memoize list-scales))

(defroutes main-routes
  (GET "/" [] (scl-memo))
  (GET "/list" [] (scl-memo))
  (GET "/scales.css" [] stylesheet)
  (route/resources "/")
  (GET "/showscale*" {params :query-params}
		  (process-scale-request (get params "scale")))
;  (GET "/:req" [req]
;       (process-scale-request req))
  (route/not-found "<h1>Page not found</h1>"))

(def app
     (handler/site main-routes))
