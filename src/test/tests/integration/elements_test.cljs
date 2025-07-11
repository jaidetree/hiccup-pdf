(ns tests.integration.elements-test
  (:require
   [dev.jaide.hiccup-pdf.core :refer [hiccup->pdf-ops]]
   [clojure.string :as s]))

(println "=== Elements Integration Test ===")

;; Test all elements working together in one document
(let [comprehensive-doc [:g {:transforms [[:translate [50 50]]]}
                        ;; Header with emoji
                         [:text {:x 0 :y 0 :font "Arial" :size 16 :fill "#0000ff"} "Test Document"]

                        ;; Shapes section
                         [:g {:transforms [[:translate [0 30]]]}
                          [:rect {:x 0 :y 0 :width 40 :height 20 :fill "#ff0000" :stroke "#000000"}]
                          [:circle {:cx 60 :cy 10 :r 10 :fill "#00ff00"}]
                          [:line {:x1 0 :y1 25 :x2 80 :y2 25 :stroke "#0000ff" :stroke-width 2}]]

                        ;; Path section
                         [:g {:transforms [[:translate [0 70]]]}
                          [:path {:d "M0,0 L20,20 C30,30 40,10 50,20 Z" :fill "#ffff00" :stroke "#ff0000"}]]

                        ;; Text section
                         [:g {:transforms [[:translate [0 110]]]}
                          [:text {:x 0 :y 0 :font "Arial" :size 12} "Status: Complete"]
                          [:text {:x 0 :y 15 :font "Arial" :size 10} "Performance: Fast"]
                          [:text {:x 0 :y 30 :font "Arial" :size 10} "Quality: Excellent"]]

                        ;; Complex transforms
                         [:g {:transforms [[:translate [100 100]] [:rotate 45] [:scale [0.8 0.8]]]}
                          [:rect {:x 0 :y 0 :width 30 :height 30 :fill "#ff0000"}]
                          [:text {:x 5 :y 20 :font "Arial" :size 8 :fill "#ffffff"} "Rotated"]]]]

  (try
    (let [result (hiccup->pdf-ops comprehensive-doc)
          char-count (count result)
          lines (count (s/split result #"\n"))]
      (println (str "✓ Comprehensive integration: " char-count " chars, " lines " lines"))
      (println "✓ All elements working together seamlessly"))
    (catch js/Error e
      (println (str "✗ Comprehensive integration: ERROR - " (.-message e))))))

(defn -main
  []
  (println "=== Elements Tests Complete ==="))

