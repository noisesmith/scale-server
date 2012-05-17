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

(def diagram-start
;"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>
"<svg id=\"fingering\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 744.09 1052.4\" version=\"1.1\" xmlns:cc=\"http://creativecommons.org/ns#\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">
 <defs id=\"defs4\"></defs>
 <metadata id=\"metadata7\">
  <rdf:RDF>
   <cc:Work rdf:about=\"\">
    <dc:format>image/svg+xml</dc:format>
    <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/StillImage\"/>
    <dc:title/>
   </cc:Work>
  </rdf:RDF>
 </metadata>
 <g id=\"layer1\">
  <rect id=\"string0\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" stroke-dashoffset=\"0\" height=\"922.86\" width=\"39.653\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" y=\"22.076\" x=\"174\" stroke-width=\"2.1\" fill=\"#000\"/>
  <rect id=\"string1\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" stroke-dashoffset=\"0\" height=\"922.86\" width=\"32.218\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" y=\"22.076\" x=\"344.38\" stroke-width=\"2.1\" fill=\"#000\"/>
  <rect id=\"string3\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" stroke-dashoffset=\"0\" height=\"922.86\" width=\"9.9132\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" y=\"22.076\" x=\"685.15\" stroke-width=\"2.1\" fill=\"#000\"/>
  <rect id=\"string2\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" stroke-dashoffset=\"0\" height=\"922.86\" width=\"18.587\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" y=\"22.076\" x=\"514.77\" stroke-width=\"2.1\" fill=\"#000\"/>
  <path id=\"oct0\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" d=\"m107.14,463.79a19.286,20,0,1,1,-38.571,0,19.286,20,0,1,1,38.571,0z\" transform=\"matrix(0.86740331,0,0,0.88980716,77.32597,70.82072)\" stroke-dashoffset=\"0\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" stroke-width=\"2.39034605\" fill=\"#5c5c5c\"/>
  <path id=\"oct3\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" d=\"m107.14,463.79a19.286,20,0,1,1,-38.571,0,19.286,20,0,1,1,38.571,0z\" transform=\"matrix(0.86740331,0,0,0.88980716,591.77822,70.82072)\" stroke-dashoffset=\"0\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" stroke-width=\"2.39034605\" fill=\"#5c5c5c\"/>
  <path id=\"oct2\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" d=\"m107.14,463.79a19.286,20,0,1,1,-38.571,0,19.286,20,0,1,1,38.571,0z\" transform=\"matrix(0.86740331,0,0,0.88980716,416.21074,70.82072)\" stroke-dashoffset=\"0\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" stroke-width=\"2.39034605\" fill=\"#5c5c5c\"/>
  <path id=\"oct1\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" d=\"m107.14,463.79a19.286,20,0,1,1,-38.571,0,19.286,20,0,1,1,38.571,0z\" transform=\"matrix(0.86740331,0,0,0.88980716,248.55643,70.82072)\" stroke-dashoffset=\"0\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" stroke-width=\"2.39034605\" fill=\"#5c5c5c\"/>
  <text id=\"octavelable\" font-weight=\"normal\" xml:space=\"preserve\" font-size=\"40px\" font-style=\"normal\" font-stretch=\"normal\" font-variant=\"normal\" y=\"494.80505\" x=\"7.7142868\" font-family=\"Bitstream Charter\" fill=\"#000000\"><tspan id=\"tspan2902\" x=\"7.7142868\" y=\"494.80505\">octave</tspan></text>
<text id=\"dominantlabel\" font-weight=\"normal\" xml:space=\"preserve\" font-size=\"40px\" font-style=\"normal\" font-stretch=\"normal\" font-variant=\"normal\" y=\"778\" x=\"7.7142868\" font-family=\"Bitstream Charter\" fill=\"#000000\"><tspan id=\"tspan2902\" x=\"7.7142868\" y=\"340\">fifth</tspan></text>
<text id=\"octave2label\" font-weight=\"normal\" xml:space=\"preserve\" font-size=\"40px\" font-style=\"normal\" font-stretch=\"normal\" font-variant=\"normal\" y=\"778\" x=\"7.7142868\" font-family=\"Bitstream Charter\" fill=\"#000000\"><tspan id=\"tspan2902\" x=\"7.7142868\" y=\"728\">octv. 2</tspan></text>")

(def diagram-end
 "</g>
</svg>")


(defn get-pos [coords]
  (str
   "\n<path id=\"position\" stroke-linejoin=\"miter\" style=\"stroke-dasharray:none;\" fill=\"#982020\" stroke-dashoffset=\"0\" transform=\"translate("
   (+ (nth coords 0) 342.85714)
   ","
   (- (nth coords 1) 303.71429)
   ")\" stroke=\"#FFF\" stroke-linecap=\"round\" stroke-miterlimit=\"4\" stroke-width=\"2.0999999\" d=\"m-91.429,350.93-37.843-21.008,37.115-22.269,0.72803,43.277z\"/>\n"))

; diagram working in chrome but not firefox
(defn diagram [positions]
  (str diagram-start
       (apply str (map get-pos positions))
       diagram-end))

(def fundamental 130.813)

(def cents-base (Math/pow 2 (/ 1 1200)))

(defn all-spots [freqs]
  (let [string-data [[fundamental 1]
		     [(* fundamental (Math/pow cents-base 700)) 163]
		     [(* fundamental (Math/pow cents-base 1400)) 320]
		     [(* fundamental (Math/pow cents-base 2100)) 482]]
	string-scale 918
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
   (and (not comment) (re-find #"[0-9]+\.?[0-9]*" line))
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
       header (str "<html><head><title>" name "</title><link href=\"scales.css\" rel=\"stylesheet\"type=\"text/css\"></head><body>")
       footer (str "</div></body></html>")
       scale-text (try (slurp (str "scl/" name ".scl"))
		       (catch Exception e
			      (str "failed in opening file: scl/" name "<br>"
				   (.getMessage e))))
       lines (str/split-lines scale-text)
       base 130.813
       mults (sort (lines-rec lines))]
    (str header "<h1>" name "</h1><br><div class=\"multicolumn2\"><pre>"
	 scale-text
	 "</pre>"
	 "<table border=\"1\"><tr>"
	 "<th>mult</th><th>cents</th><th>note</th><th>hz</th></tr>"
	 (apply str (show-info mults))
	 "</table>"
	 (diagram (all-spots (freqs-hz base (sort > mults))))
	 footer)))

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
