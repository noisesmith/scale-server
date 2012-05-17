(defproject demo "0.1.0-SNAPSHOT"
  :description "simple clojure/compojure demo"
  :dependencies
  [[org.clojure/clojure "1.3.0"]
   [compojure "1.0.4"]
   [fs "1.0.0"]]
  :url "http://github.com/noisesmith"
  :main demo.core
  :plugins [[lein-ring "0.7.0"]]
  :ring {:handler demo.core/app})

; run with 'lein run server'