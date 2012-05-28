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

(defn debug [& args]
  (spit "debug.log" (apply str args)))

(defn name-to-url [fname]
  (let [name (nth (re-find #"(.*)(.scl)" fname) 1)]
    (str "<a href=\"showscale?scale=" name "\">" name "</a> \n")))

(defn list-scales []
  (let [scale-files (fs/list-dir "scl")
        header (str "<html><head><title>Scale List</title>\n"
		    "<link href=\"scales.css\" rel=\"stylesheet\" "
		    "type=\"text/css\"></head>")
	intro (str "<body><h1>Scales available for display:</h1>"
		   "<div class=\"multicolumn8\">")
	footer "</div></body></html>"]
    { :status 200
     :headers {"Content-Type" "text/html"}
     :body (str header intro
                (apply str (map name-to-url (sort scale-files)))
                footer)
                                        ;      :session {:fundamental 100 :strings [100 440 666]}
     }))

(defn instr-link [name place strings]
  (str "<a href=\"" place "&amp;strings="
       (apply str (map (fn [x] (str x ":")) strings))
       "\">show " name " </a><br/>\n"))

(defn instr-links [place]
  (str (instr-link "violin strings" place [195.998 293.665 440.0 659.255])
       (instr-link "viola strings" place [130.813 195.998 293.665 440.0])
       (instr-link "cello strings" place [65.406 97.999 146.832 220.0])
       (instr-link "guitar strings" place
		   [82.41 110 146.83 196.0 246.94 329.63])
       (instr-link "bass strings" place [41.203 55.0 73.416 98.0])))

(defn diagram [positions scale-count strings]
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
	nstrings (reduce (fn [x y] (+ 1 x)) 0 strings)
	string-x0s (map (fn [x] (+ 35 (* x (/ 100.0 nstrings))))
			(range 0 nstrings))
	thickest-string (first (sort < strings))
	string-x1s (map (fn [x h] (+ x 0.1 (* 4.0 (/ thickest-string h))))
			string-x0s
			strings)
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
    (str "<div float=\"right\" margin=\"0\" padding=\"0\">"
	 "<svg xmlns=\"http://www.w3.org/2000/svg\" "
	 "version=\"1.1\" viewBox=\"0 0 100 600\">\n"
	 ;; display instrument strings
	 (apply str (map (fn [x0 x1]
                           (svg-rect x0
                                     string-start
                                     x1
                                     string-end "black"))
			 (take nstrings string-x0s)
			 string-x1s))
	 ;; display fingering markings
	 (apply str (map (fn [x]
                           (svg-arrow (+ (nth string-x1s (nth x 0)) 1)
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

(def cents-base (Math/pow 2 (/ 1 1200)))

(defn all-spots [freqs strings]
  (let [string-data (map-indexed (fn [i x] [x i]) strings)
	two-oct-plus 4.1
	coord (fn [hz hz-pos degree]
                (let [base-hz (nth hz-pos 0)
                      pos (nth hz-pos 1)]
					; stop display at just over two octaves
                  (if (or (< hz base-hz) (> hz (* base-hz two-oct-plus)))
                    false
                    [pos (- 1 (/ base-hz hz)) degree])))
	spots (filter identity
		      (mapcat (fn [hz-deg] 
                                (map (fn [hz-pos]
                                       (coord (nth hz-deg 0)
                                              hz-pos (nth hz-deg 1)))
                                     string-data))
			      freqs))]
    spots))

(defn string->number [str default]
  (try
    (let [n (read-string str)]
      (if (number? n)
        n default))
    (catch Exception e
      default)))

(defn line-mult [line comment]
  (cond
   (re-find #"!.*scl" line)
   :do-nothing
   (= line "!")
   :toggle-comment
   (and (not comment) (re-find #"^[^!][0-9]+/[0-9]+" line))
   (let [match (re-find #"^[^!]([0-9]+)/([0-9]+)" line)]
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

(defn freqs-hz [base mults nmults fund]
  (let [tonic-mult (first (sort > mults))
	adj-base (loop [base base] (if (< base fund)
                                     base
				     (recur (/ base tonic-mult))))]
    (loop [base adj-base mults mults degrees (cycle (range 0 nmults)) acc []]
      (if (> base (* 440 4)) ; maximum frequency we display
        acc
        (let [next (sort (fn [x y]
                           (> (first x)
                              (first y)))
                         (reduce conj acc (map (fn [d x] [(* x base) d])
                                               degrees
                                               mults)))]
          (if (= next '()) '()
              (recur (first (first next)) mults degrees next)))))))

(defn show-info [mults fundamental]
  (let [log (fn [x base] (/ (Math/log x) (Math/log base)))
	degrees ["c" "c#" "d" "d#" "e" "f" "f#" "g" "g#" "a" "a#" "b"]]
    (map (fn [x]
           (let [middle-c 261.6259
                 cents-rel (log x cents-base)
                 cents-abs (mod (log
                                 (/ (* fundamental x) middle-c)
                                 cents-base) 1200)
                 degree (mod (int (Math/round (/ cents-abs 100))) 12)]
             (str "<tr><td>"
                  (if (= (class x) clojure.lang.Ratio)
                    x
                    (format "%.4f" (* x 1.0))) "</td><td>"
		    (format "%.3f" cents-rel) "</td><td>"
		    (nth degrees degree)
		    (if (< degree (/ cents-abs 100)) "+" "-")
		    (int (Math/abs (- (mod cents-abs 1200) (* degree 100))))
		    "</td><td>"
		    ;; need the 1.0 for float contagion, naturally
		    (format "%.4f" (* x fundamental 1.)) "</td></tr>")))
	 (sort mults))))

(defn process-scale-request [request]
  (let [params (get request :query-params)
        scl (get params "scale" false)
	strs (get params "strings" false)
	fund (get params "fund" false)
	scale-request (str/split scl #":")
	name (nth scale-request 0)
	header (str "<html xmlns='http://www.w3.org/1999/xhtml'><head>"
		    "<title>" name "</title><link href=\"scales.css\" "
		    "rel=\"stylesheet\" type=\"text/css\"></link></head>"
		    "<body><a href=\"/\">scale list</a><br/>")
	footer (str "</div></body></html>")
	scale-text (try (slurp (str "scl/" name ".scl"))
			(catch Exception e
                          (str "failed in opening file: scl/" name "<br/>"
                               (.getMessage e))))
	lines (str/split-lines scale-text)
	mults+len (lines-rec lines)
	mults-len (nth mults+len 1)
	mults (nth mults+len 0)
	default-fund (if-let [default (-> request :session :fundamental)]
                       default 130.813)
	fundamental (string->number fund default-fund)
	default-strings (if-let [default (-> request :session :strings)]
                          default
                          (take 4 (iterate (fn [x] (* x 1.5))
                                           default-fund)))
	strings (if (not strs)
                  default-strings
		  (map string->number (str/split strs #":")
		       (cycle default-strings)))
	body (str header "<h1>" name "</h1><br/>"
		  (instr-link "edit strings in url"
			      (str "showscale?&amp;scale=" scl)
			      strings)
		  (instr-links (str "showscale?&amp;scale=" scl))
		  "<div class=\"multicolumn2\">"
		  "<div float=\"left\" margin=\"0\" padding=\"0\">"
		  "<table border=\"1\"><tr>"
		  "<th>mult</th><th>cents</th><th>note</th><th>hz</th></tr>"
		  (apply str (show-info mults fundamental))
		  "</table>"
		  "<pre>"
		  (str/escape scale-text {\< "&lt;", \> "&gt;", \& "&amp;"})
		  "</pre>"
		  "</div>"
     		  (diagram (all-spots (freqs-hz fundamental (sort > mults)
						mults-len
						(first (sort < strings)))
				      strings) mults-len strings)
		  footer)]
    { :status 200
     :headers {"Content-Type" "application/xhtml+xml"}
     :body body
     :session {:fundamental fundamental :strings strings}}))

(def scl-memo (memoize list-scales))

(defroutes main-routes
  (GET "/" [] (scl-memo))
  (GET "/list" [] (scl-memo))
  (GET "/scales.css" [] stylesheet)
  (route/resources "/")
  (GET "/showscale*" request ;{params :query-params}
       (process-scale-request request)) ;(get params "scale")
  ;;(get params "strings")
  ;;(get params "fund")))
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (handler/site main-routes))
