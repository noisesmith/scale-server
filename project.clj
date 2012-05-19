(defproject scale-server "0.1.0-SNAPSHOT"
  :description "server to display musical scales as fingerings for a true pitch instrument"
  :dependencies
  [[org.clojure/clojure "1.3.0"]
   [compojure "1.0.4"]
   [fs "1.0.0"]]
  :url "http://github.com/noisesmith"
  :main scale-server.core
  :plugins [[lein-ring "0.7.0"]]
  :ring {:handler scale-server.core/app})

; run with 'lein run server'
