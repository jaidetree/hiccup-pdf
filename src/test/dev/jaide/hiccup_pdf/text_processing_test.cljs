(ns dev.jaide.hiccup-pdf.text-processing-test
  "Tests for text processing functions for mixed content"
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.text-processing :as text-proc]))

(deftest test-segment-text
  (testing "Text with single emoji"
    (let [result (text-proc/segment-text "Hello 💡 world")]
      (is (= 3 (count result)))
      (is (= {:type :text :content "Hello " :start-idx 0 :end-idx 6} (first result)))
      (is (= {:type :emoji :content "💡" :start-idx 6 :end-idx 8} (second result)))
      (is (= {:type :text :content " world" :start-idx 8 :end-idx 14} (nth result 2)))))
  
  (testing "Text with multiple emoji"
    (let [result (text-proc/segment-text "Status: ✅ Target: 🎯")]
      (is (= 4 (count result)))
      (is (= {:type :text :content "Status: " :start-idx 0 :end-idx 8} (first result)))
      (is (= {:type :emoji :content "✅" :start-idx 8 :end-idx 9} (second result)))
      (is (= {:type :text :content " Target: " :start-idx 9 :end-idx 18} (nth result 2)))
      (is (= {:type :emoji :content "🎯" :start-idx 18 :end-idx 20} (nth result 3)))))
  
  (testing "Adjacent emoji"
    (let [result (text-proc/segment-text "💡🎯")]
      (is (= 2 (count result)))
      (is (= {:type :emoji :content "💡" :start-idx 0 :end-idx 2} (first result)))
      (is (= {:type :emoji :content "🎯" :start-idx 2 :end-idx 4} (second result)))))
  
  (testing "Text with no emoji"
    (let [result (text-proc/segment-text "Hello world")]
      (is (= 1 (count result)))
      (is (= {:type :text :content "Hello world" :start-idx 0 :end-idx 11} (first result)))))
  
  (testing "Empty string"
    (is (= [] (text-proc/segment-text ""))))
  
  (testing "Only emoji"
    (let [result (text-proc/segment-text "💡")]
      (is (= 1 (count result)))
      (is (= {:type :emoji :content "💡" :start-idx 0 :end-idx 2} (first result)))))
  
  (testing "Emoji at boundaries"
    ;; Emoji at start
    (let [result (text-proc/segment-text "💡 Hello")]
      (is (= 2 (count result)))
      (is (= {:type :emoji :content "💡" :start-idx 0 :end-idx 2} (first result)))
      (is (= {:type :text :content " Hello" :start-idx 2 :end-idx 8} (second result))))
    ;; Emoji at end
    (let [result (text-proc/segment-text "Hello 💡")]
      (is (= 2 (count result)))
      (is (= {:type :text :content "Hello " :start-idx 0 :end-idx 6} (first result)))
      (is (= {:type :emoji :content "💡" :start-idx 6 :end-idx 8} (second result)))))
  
  (testing "Complex mixed content"
    (let [result (text-proc/segment-text "Task 💡 Done ✅ Next 🎯")]
      (is (= 6 (count result)))
      (is (= {:type :text :content "Task " :start-idx 0 :end-idx 5} (nth result 0)))
      (is (= {:type :emoji :content "💡" :start-idx 5 :end-idx 7} (nth result 1)))
      (is (= {:type :text :content " Done " :start-idx 7 :end-idx 13} (nth result 2)))
      (is (= {:type :emoji :content "✅" :start-idx 13 :end-idx 14} (nth result 3)))
      (is (= {:type :text :content " Next " :start-idx 14 :end-idx 20} (nth result 4)))
      (is (= {:type :emoji :content "🎯" :start-idx 20 :end-idx 22} (nth result 5))))))

(deftest test-prepare-mixed-content
  (testing "Basic text segment preparation"
    (let [segments [{:type :text :content "Hello" :start-idx 0 :end-idx 5}]
          result (text-proc/prepare-mixed-content segments)]
      (is (= 1 (count result)))
      (let [segment (first result)]
        (is (= :text (:type segment)))
        (is (= "Hello" (:content segment)))
        (is (= 0 (:x segment)))
        (is (= 0 (:y segment)))
        (is (< (Math/abs (- 36.0 (:width segment))) 0.01))  ; 5 chars * 12 font-size * 0.6, with tolerance
        (is (= 12 (:height segment))))))
  
  (testing "Basic emoji segment preparation"
    (let [segments [{:type :emoji :content "💡" :start-idx 0 :end-idx 2}]
          result (text-proc/prepare-mixed-content segments)]
      (is (= 1 (count result)))
      (let [segment (first result)]
        (is (= :emoji (:type segment)))
        (is (= "💡" (:content segment)))
        (is (= 0 (:x segment)))
        (is (= 0 (:y segment)))
        (is (= 12 (:width segment)))   ; Font size
        (is (= 12 (:height segment))))))
  
  (testing "Mixed content with positioning"
    (let [segments [{:type :text :content "Hi " :start-idx 0 :end-idx 3}
                    {:type :emoji :content "💡" :start-idx 3 :end-idx 5}
                    {:type :text :content " ok" :start-idx 5 :end-idx 8}]
          result (text-proc/prepare-mixed-content segments)]
      (is (= 3 (count result)))
      ;; First segment (text) - "Hi " = 3 chars
      (let [seg1 (nth result 0)]
        (is (= 0 (:x seg1)))
        (is (< (Math/abs (- 21.6 (:width seg1))) 0.01)))  ; 3 chars * 12 * 0.6, with tolerance
      ;; Second segment (emoji)
      (let [seg2 (nth result 1)]
        (is (< (Math/abs (- 21.6 (:x seg2))) 0.01))       ; After first segment, with tolerance
        (is (= 12 (:width seg2))))
      ;; Third segment (text) - " ok" = 3 chars
      (let [seg3 (nth result 2)]
        (is (< (Math/abs (- 33.6 (:x seg3))) 0.01))       ; After first two segments, with tolerance
        (is (< (Math/abs (- 21.6 (:width seg3))) 0.01)))))  ; 3 chars * 12 * 0.6, with tolerance
  
  (testing "Custom options"
    (let [segments [{:type :text :content "Hi" :start-idx 0 :end-idx 2}]
          options {:font-size 16 :base-x 100 :base-y 200}
          result (text-proc/prepare-mixed-content segments options)]
      (is (= 1 (count result)))
      (let [segment (first result)]
        (is (= 100 (:x segment)))     ; Custom base-x
        (is (= 200 (:y segment)))     ; Custom base-y
        (is (< (Math/abs (- 19.2 (:width segment))) 0.01)) ; 2 chars * 16 * 0.6, with tolerance
        (is (= 16 (:height segment))))))  ; Custom font-size
  
  (testing "Empty segments"
    (is (= [] (text-proc/prepare-mixed-content []))))
  
  (testing "Performance with many segments"
    (let [segments (vec (for [i (range 100)]
                          {:type (if (even? i) :text :emoji)
                           :content (if (even? i) "a" "💡")
                           :start-idx i
                           :end-idx (+ i 1)}))
          result (text-proc/prepare-mixed-content segments)]
      (is (= 100 (count result)))
      ;; Check that positioning is cumulative
      (is (= 0 (:x (first result))))
      (is (> (:x (last result)) 500)))))  ; Should have accumulated substantial x position

(deftest test-validate-segments
  (testing "Valid segmentation"
    (let [text "Hello 💡 world"
          segments [{:type :text :content "Hello " :start-idx 0 :end-idx 6}
                    {:type :emoji :content "💡" :start-idx 6 :end-idx 8}
                    {:type :text :content " world" :start-idx 8 :end-idx 14}]
          result (text-proc/validate-segments segments text)]
      (is (:valid? result))
      (is (empty? (:errors result)))
      (is (= 14 (:expected (:coverage result))))
      (is (= 14 (:actual (:coverage result))))))
  
  (testing "Gap in segmentation"
    (let [text "Hello world"
          segments [{:type :text :content "Hello" :start-idx 0 :end-idx 5}
                    {:type :text :content "world" :start-idx 6 :end-idx 11}]  ; Gap at position 5
          result (text-proc/validate-segments segments text)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Gap from position 5 to 6") (:errors result)))))
  
  (testing "Overlap in segmentation"
    (let [text "Hello"
          segments [{:type :text :content "He" :start-idx 0 :end-idx 2}
                    {:type :text :content "lo" :start-idx 1 :end-idx 3}]  ; Overlap at position 1-2
          result (text-proc/validate-segments segments text)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Overlap") (:errors result)))))
  
  (testing "Missing coverage at end"
    (let [text "Hello world"
          segments [{:type :text :content "Hello " :start-idx 0 :end-idx 6}]  ; Missing "world"
          result (text-proc/validate-segments segments text)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Missing coverage from position 6 to 11") (:errors result)))))
  
  (testing "Content mismatch"
    (let [text "Hello"
          segments [{:type :text :content "Hi" :start-idx 0 :end-idx 5}]  ; Wrong content
          result (text-proc/validate-segments segments text)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Content mismatch") (:errors result)))))
  
  (testing "Empty segments for empty text"
    (let [result (text-proc/validate-segments [] "")]
      (is (:valid? result))
      (is (empty? (:errors result)))))
  
  (testing "Empty segments for non-empty text"
    (let [result (text-proc/validate-segments [] "Hello")]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Missing coverage from position 0 to 5") (:errors result)))))
  
  (testing "Complex validation with emoji"
    (let [text "Hi 💡 ok"
          segments [{:type :text :content "Hi " :start-idx 0 :end-idx 3}
                    {:type :emoji :content "💡" :start-idx 3 :end-idx 5}
                    {:type :text :content " ok" :start-idx 5 :end-idx 8}]
          result (text-proc/validate-segments segments text)]
      (is (:valid? result))
      (is (empty? (:errors result)))
      (is (= 8 (:expected (:coverage result))))
      (is (= 8 (:actual (:coverage result)))))))

(deftest test-integration-with-emoji-functions
  (testing "Integration with actual emoji detection"
    (let [text "Status: ✅ Progress: 💡 Target: 🎯"
          segments (text-proc/segment-text text)
          validation (text-proc/validate-segments segments text)
          prepared (text-proc/prepare-mixed-content segments)]
      ;; Validate segmentation
      (is (:valid? validation))
      (is (empty? (:errors validation)))
      ;; Check prepared content
      (is (= (count segments) (count prepared)))
      ;; Verify all segments have positioning
      (is (every? #(contains? % :x) prepared))
      (is (every? #(contains? % :y) prepared))
      (is (every? #(contains? % :width) prepared))
      (is (every? #(contains? % :height) prepared))))
  
  (testing "End-to-end processing pipeline"
    (let [text "Task 💡 complete ✅"
          segments (text-proc/segment-text text)
          prepared (text-proc/prepare-mixed-content segments {:font-size 14 :base-x 50 :base-y 100})]
      ;; Should have 4 segments: "Task ", "💡", " complete ", "✅"
      (is (= 4 (count prepared)))
      ;; Check types
      (is (= [:text :emoji :text :emoji] (map :type prepared)))
      ;; Check positioning starts at base
      (is (= 50 (:x (first prepared))))
      (is (= 100 (:y (first prepared))))
      ;; Check cumulative positioning
      (is (< (:x (first prepared)) (:x (second prepared))))
      (is (< (:x (second prepared)) (:x (nth prepared 2))))
      (is (< (:x (nth prepared 2)) (:x (last prepared))))))
  
  (testing "Performance with complex text"
    (let [text (str "Performance test with multiple emoji: "
                    "💡 🎯 ✅ " 
                    "and more text content " 
                    "💡 🎯 ✅ "
                    "end of test")
          segments (text-proc/segment-text text)
          validation (text-proc/validate-segments segments text)
          prepared (text-proc/prepare-mixed-content segments)]
      ;; Should process successfully
      (is (:valid? validation))
      (is (pos? (count segments)))
      (is (= (count segments) (count prepared)))
      ;; Should contain both text and emoji segments
      (is (some #(= :text (:type %)) segments))
      (is (some #(= :emoji (:type %)) segments)))))