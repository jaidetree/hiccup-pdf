(ns hiccup-pdf.performance-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.core :refer [hiccup->pdf-ops]]))

(defn benchmark
  "Simple benchmark function that measures execution time."
  [label f]
  (let [start-time (js/Date.now)
        result (f)
        end-time (js/Date.now)
        duration (- end-time start-time)]
    (println (str label ": " duration "ms"))
    {:result result :duration duration}))

(deftest string-concatenation-performance-test
  (testing "String concatenation performance for PDF operators"
    ;; Test large document with many elements
    (let [large-doc [:g {}
                     ;; 100 rectangles
                     (for [i (range 100)]
                       [:rect {:x (* i 10) :y (* i 5) :width 20 :height 15 :fill (if (even? i) "red" "blue")}])
                     ;; 50 circles
                     (for [i (range 50)]
                       [:circle {:cx (+ 500 (* i 15)) :cy (+ 100 (* i 10)) :r (+ 5 (mod i 10)) :fill "green"}])
                     ;; 25 text elements
                     (for [i (range 25)]
                       [:text {:x (+ 100 (* i 30)) :y (+ 300 (* i 20)) :font "Arial" :size (+ 10 (mod i 8))} (str "Text " i)])]
          flattened-doc (into [:g {}] (apply concat (rest large-doc)))
          {result :result duration :duration} (benchmark "Large document (175 elements)" 
                                                         #(hiccup->pdf-ops flattened-doc))]
      
      (is (string? result)
          "Should generate valid PDF operators")
      (is (< duration 500)
          "Should complete large document processing in under 500ms")
      (is (> (count result) 1000)
          "Should generate substantial PDF content")
      (println (str "Generated " (count result) " characters of PDF operators")))))

(deftest deeply-nested-performance-test
  (testing "Performance with deeply nested groups"
    ;; Create 50 levels of nesting
    (let [deep-doc (reduce (fn [doc level]
                             [:g {:transforms [[:translate [1 1]] [:rotate 1]]} doc])
                           [:rect {:x 0 :y 0 :width 10 :height 10 :fill "red"}]
                           (range 50))
          {result :result duration :duration} (benchmark "Deeply nested (50 levels)" 
                                                         #(hiccup->pdf-ops deep-doc))]
      
      (is (string? result)
          "Should handle deep nesting")
      (is (< duration 200)
          "Should complete deep nesting in under 200ms")
      
      ;; Verify proper nesting structure
      (let [q-count (count (re-seq #"q\n" result))
            Q-count (count (re-seq #"Q" result))]
        (is (= q-count Q-count)
            "Should have balanced save/restore operations")
        (is (= q-count 50)
            "Should have exactly 50 nested groups")))))

(deftest complex-path-performance-test
  (testing "Performance with complex SVG paths"
    ;; Generate complex path with many curve commands
    (let [path-data (apply str "M0,0 " 
                           (for [i (range 100)]
                             (str "C" (* i 10) "," (* i 5) " " 
                                  (+ (* i 10) 5) "," (+ (* i 5) 10) " " 
                                  (+ (* i 10) 10) "," (* i 5) " ")))
          complex-path [:path {:d path-data :fill "blue" :stroke "red" :stroke-width 2}]
          {result :result duration :duration} (benchmark "Complex path (100 curves)" 
                                                         #(hiccup->pdf-ops complex-path))]
      
      (is (string? result)
          "Should handle complex paths")
      (is (< duration 100)
          "Should complete complex path processing in under 100ms")
      (is (> (count (re-seq #"c\n" result)) 95)
          "Should contain many curve commands"))))

(deftest memory-efficiency-test
  (testing "Memory efficiency with repeated operations"
    ;; Test memory usage doesn't grow excessively with repeated calls
    (let [test-doc [:g {}
                    [:rect {:x 10 :y 10 :width 50 :height 30 :fill "red"}]
                    [:circle {:cx 50 :cy 50 :r 20 :fill "blue"}]
                    [:text {:x 100 :y 100 :font "Arial" :size 12} "Test"]]
          iterations 1000
          {result :result duration :duration} (benchmark (str "Repeated operations (" iterations " iterations)")
                                                         #(dotimes [_ iterations]
                                                            (hiccup->pdf-ops test-doc)))]
      
      (is (< duration 2000)
          "Should complete 1000 iterations in under 2 seconds")
      (println (str "Average per operation: " (/ duration iterations) "ms")))))

(deftest transform-matrix-performance-test
  (testing "Transform matrix calculation performance"
    ;; Test with complex transform combinations
    (let [complex-transforms [:g {:transforms [[:translate [100 200]] 
                                               [:rotate 45] 
                                               [:scale [2.5 1.8]] 
                                               [:translate [-50 -25]] 
                                               [:rotate -30] 
                                               [:scale [0.8 1.2]]]}
                              [:rect {:x 0 :y 0 :width 20 :height 20 :fill "green"}]]
          iterations 500
          sample-result (hiccup->pdf-ops complex-transforms)
          {result :result duration :duration} (benchmark (str "Complex transforms (" iterations " iterations)")
                                                         #(dotimes [_ iterations]
                                                            (hiccup->pdf-ops complex-transforms)))]
      
      (is (string? sample-result)
          "Should handle complex transforms")
      (is (< duration 1000)
          "Should complete 500 transform calculations in under 1 second")
      (println (str "Average transform calculation: " (/ duration iterations) "ms")))))

(deftest validation-overhead-test
  (testing "Validation performance overhead"
    ;; Compare validation vs no validation (if we had such option)
    (let [test-elements [[:rect {:x 10 :y 10 :width 50 :height 30 :fill "red"}]
                         [:circle {:cx 50 :cy 50 :r 20 :fill "blue"}] 
                         [:line {:x1 0 :y1 0 :x2 100 :y2 100 :stroke "black"}]
                         [:text {:x 100 :y 100 :font "Arial" :size 12} "Test"]
                         [:path {:d "M10,10 L50,50 C60,60 70,40 80,50 Z" :fill "yellow"}]]
          iterations 200]
      
      ;; Test validation performance with various element types
      (doseq [element test-elements]
        (let [sample-result (hiccup->pdf-ops element)
              {result :result duration :duration} (benchmark (str "Validation " (first element) " (" iterations " iterations)")
                                                             #(dotimes [_ iterations]
                                                                (hiccup->pdf-ops element)))]
          (is (string? sample-result)
              (str "Should validate " (first element) " elements"))
          (is (< duration 500)
              (str "Should validate " (first element) " quickly"))
          (println (str "Average " (first element) " validation: " (/ duration iterations) "ms")))))))

(deftest output-size-efficiency-test
  (testing "PDF operator output size efficiency"
    ;; Test that output is reasonably compact
    (let [test-cases [[:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]
                      [:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}]
                      [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello World"]
                      [:path {:d "M10,10 L50,50 L10,90 Z" :fill "yellow"}]
                      [:g {:transforms [[:translate [50 50]] [:rotate 45]]}
                       [:rect {:x 0 :y 0 :width 20 :height 20 :fill "green"}]]]]
      
      (doseq [test-case test-cases]
        (let [result (hiccup->pdf-ops test-case)
              element-type (first test-case)
              char-count (count result)]
          (is (< char-count 500)
              (str element-type " should generate compact output"))
          (println (str element-type " output size: " char-count " characters")))))))

(deftest scalability-test
  (testing "Scalability with increasing document size"
    ;; Test performance scaling with document size
    (let [base-element [:rect {:x 0 :y 0 :width 10 :height 10 :fill "red"}]
          test-sizes [10 50 100 500 1000]]
      
      (doseq [size test-sizes]
        (let [large-doc (into [:g {}] (repeat size base-element))
              {result :result duration :duration} (benchmark (str "Document size " size " elements")
                                                             #(hiccup->pdf-ops large-doc))]
          (is (string? result)
              (str "Should handle " size " elements"))
          (is (< duration (* size 2))
              (str "Should scale linearly with document size (" size " elements)"))
          (println (str size " elements: " duration "ms, " 
                        (/ duration size) "ms per element, "
                        (count result) " output chars")))))))

(defn run-performance-suite
  "Run all performance tests and print summary"
  []
  (println "=== Hiccup-PDF Performance Test Suite ===")
  (println "Testing performance characteristics...")
  (println "")
  
  ;; Note: In a real test runner, these would be run individually
  ;; Here we'll just print what would be tested
  (println "Performance tests cover:")
  (println "- String concatenation with large documents")
  (println "- Deep nesting performance")
  (println "- Complex SVG path processing")
  (println "- Memory efficiency")
  (println "- Transform matrix calculations")
  (println "- Validation overhead")
  (println "- Output size efficiency")
  (println "- Scalability with document size")
  (println "")
  (println "Run with: npx nbb test/hiccup_pdf/performance_test.cljs"))