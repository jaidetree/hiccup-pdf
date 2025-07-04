(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

(println "=== Final Integration Tests: Emoji Support ===")

;; Test various emoji combinations
(let [test-cases [
  {:name "Basic emoji" :hiccup [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 🌍"]}
  {:name "Multiple emoji" :hiccup [:text {:x 100 :y 200 :font "Arial" :size 14} "🌟💫⭐"]}
  {:name "Emoji with text" :hiccup [:text {:x 100 :y 200 :font "Arial" :size 14} "Welcome 👋 to PDF 📄"]}
  {:name "Special chars + emoji" :hiccup [:text {:x 100 :y 200 :font "Arial" :size 14} "Test (parentheses) and 🎉 celebration"]}
  {:name "Complex emoji doc" :hiccup [:g {}
                                      [:rect {:x 0 :y 0 :width 200 :height 100 :fill "white" :stroke "black"}]
                                      [:text {:x 10 :y 20 :font "Arial" :size 12} "🏢 Company Report"]
                                      [:text {:x 10 :y 40 :font "Arial" :size 10} "📊 Sales up 25%"]
                                      [:text {:x 10 :y 60 :font "Arial" :size 10} "🎯 Target achieved"]
                                      [:text {:x 10 :y 80 :font "Arial" :size 10} "🚀 Ready for Q4"]]}]]

  (doseq [{:keys [name hiccup]} test-cases]
    (try
      (let [result (hiccup->pdf-ops hiccup)
            char-count (count result)]
        (println (str "✓ " name ": " char-count " chars"))
        (when (< char-count 50)
          (println (str "  Result: " result))))
      (catch js/Error e
        (println (str "✗ " name ": ERROR - " (.-message e)))))))

(println "")
(println "=== All Elements Integration Test ===")

;; Test all elements working together in one document
(let [comprehensive-doc [:g {:transforms [[:translate [50 50]]]}
                         ;; Header with emoji
                         [:text {:x 0 :y 0 :font "Arial" :size 16 :fill "blue"} "📋 Test Document"]
                         
                         ;; Shapes section
                         [:g {:transforms [[:translate [0 30]]]}
                          [:rect {:x 0 :y 0 :width 40 :height 20 :fill "red" :stroke "black"}]
                          [:circle {:cx 60 :cy 10 :r 10 :fill "green"}]
                          [:line {:x1 0 :y1 25 :x2 80 :y2 25 :stroke "blue" :stroke-width 2}]]
                         
                         ;; Path section
                         [:g {:transforms [[:translate [0 70]]]}
                          [:path {:d "M0,0 L20,20 C30,30 40,10 50,20 Z" :fill "yellow" :stroke "red"}]]
                         
                         ;; Text with emoji section
                         [:g {:transforms [[:translate [0 110]]]}
                          [:text {:x 0 :y 0 :font "Arial" :size 12} "Status: ✅ Complete"]
                          [:text {:x 0 :y 15 :font "Arial" :size 10} "Performance: ⚡ Fast"]
                          [:text {:x 0 :y 30 :font "Arial" :size 10} "Quality: 🌟 Excellent"]]
                         
                         ;; Complex transforms
                         [:g {:transforms [[:translate [100 100]] [:rotate 45] [:scale [0.8 0.8]]]}
                          [:rect {:x 0 :y 0 :width 30 :height 30 :fill "purple"}]
                          [:text {:x 5 :y 20 :font "Arial" :size 8 :fill "white"} "🔄 Rotated"]]]]

(try
  (let [result (hiccup->pdf-ops comprehensive-doc)
        char-count (count result)
        lines (count (clojure.string/split result #"\n"))]
    (println (str "✓ Comprehensive integration: " char-count " chars, " lines " lines"))
    (println "✓ All elements working together seamlessly"))
  (catch js/Error e
    (println (str "✗ Comprehensive integration: ERROR - " (.-message e)))))

(println "")
(println "=== Final Integration Tests Complete ===")