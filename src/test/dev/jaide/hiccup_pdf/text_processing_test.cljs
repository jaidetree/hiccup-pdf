(ns dev.jaide.hiccup-pdf.text-processing-test
  "Tests for text processing functions for mixed content and PDF operator generation"
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.text-processing :as text-proc]
            [dev.jaide.hiccup-pdf.images :as images]))

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

;; Tests for Mixed Content PDF Operator Generation

(deftest test-render-text-segment
  (testing "Basic text segment rendering"
    (let [segment {:content "Hello" :x 100 :y 200}
          result (text-proc/render-text-segment segment "Arial" 14)]
      ;; Should contain proper PDF text operators
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))
      (is (clojure.string/includes? result "/Arial 14 Tf"))
      (is (clojure.string/includes? result "100 200 Td"))
      (is (clojure.string/includes? result "(Hello) Tj"))))

  (testing "Text with special characters"
    (let [segment {:content "Hello (world)" :x 0 :y 0}
          result (text-proc/render-text-segment segment "Arial" 12)]
      ;; Should escape parentheses
      (is (clojure.string/includes? result "(Hello \\(world\\)) Tj"))))

  (testing "Text with backslashes"
    (let [segment {:content "Path\\to\\file" :x 0 :y 0}
          result (text-proc/render-text-segment segment "Arial" 12)]
      ;; Should escape backslashes
      (is (clojure.string/includes? result "(Path\\\\to\\\\file) Tj"))))

  (testing "Text with color"
    (let [segment {:content "Red text" :x 50 :y 75}
          result (text-proc/render-text-segment segment "Times-Roman" 16 "red")]
      ;; Should include color operator
      (is (clojure.string/includes? result "1 0 0 rg"))
      (is (clojure.string/includes? result "/Times-Roman 16 Tf"))
      (is (clojure.string/includes? result "50 75 Td"))))

  (testing "Empty text content"
    (let [segment {:content "" :x 0 :y 0}
          result (text-proc/render-text-segment segment "Arial" 12)]
      ;; Should still generate valid PDF operators
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))
      (is (clojure.string/includes? result "() Tj")))))

(deftest test-render-image-segment
  (testing "Basic image segment rendering"
    (let [segment {:content "💡" :x 100 :y 200}
          result (text-proc/render-image-segment segment 14 "Em1")]
      ;; Should contain proper PDF image operators
      (is (clojure.string/includes? result "q"))  ; Save state
      (is (clojure.string/includes? result "Q"))  ; Restore state
      (is (clojure.string/includes? result "/Em1 Do"))  ; Draw XObject
      ;; Should contain transformation matrix
      ;; Scale factor for 14pt font: 14/72 ≈ 0.194
      (is (clojure.string/includes? result "0.19444444444444445"))
      (is (clojure.string/includes? result "cm"))))

  (testing "Different font sizes"
    (let [segment {:content "🎯" :x 0 :y 0}]
      ;; Test 8pt font
      (let [result-8 (text-proc/render-image-segment segment 8 "Em2")]
        (is (clojure.string/includes? result-8 "0.1111111111111111"))  ; 8/72
        (is (clojure.string/includes? result-8 "/Em2 Do")))
      ;; Test 24pt font
      (let [result-24 (text-proc/render-image-segment segment 24 "Em3")]
        (is (clojure.string/includes? result-24 "0.3333333333333333"))  ; 24/72
        (is (clojure.string/includes? result-24 "/Em3 Do")))))

  (testing "Custom baseline offset"
    (let [segment {:content "✅" :x 50 :y 100}
          result (text-proc/render-image-segment segment 12 "Em4" 0.3)]
      ;; Should adjust Y position based on baseline offset
      ;; Y offset = 12 * 0.3 * -1 = -3.6
      ;; Final Y = 100 + (-3.6) = 96.4
      (is (clojure.string/includes? result "50 96.4 cm"))
      (is (clojure.string/includes? result "/Em4 Do"))))

  (testing "Position calculations"
    (let [segment {:content "⚠" :x 200 :y 300}
          result (text-proc/render-image-segment segment 18 "Em5")]
      ;; Default baseline offset 0.2: 18 * 0.2 * -1 = -3.6
      ;; Final Y = 300 + (-3.6) = 296.4
      (is (clojure.string/includes? result "200 296.4 cm"))
      ;; Scale factor: 18/72 = 0.25
      (is (clojure.string/includes? result "0.25 0 0 0.25")))))

(deftest test-calculate-segment-positions
  (testing "Basic position calculation"
    (let [segments [{:type :text :content "Hi "}
                    {:type :emoji :content "💡"}
                    {:type :text :content " ok"}]
          result (text-proc/calculate-segment-positions segments 0 0 12)]
      (is (= 3 (count result)))
      ;; First segment at x=0
      (is (= 0 (:x (nth result 0))))
      ;; Text width: 3 chars * 12 * 0.6 = 21.6
      (is (< (Math/abs (- 21.6 (:x (nth result 1)))) 0.01))
      ;; Emoji width: 12
      (is (< (Math/abs (- 33.6 (:x (nth result 2)))) 0.01))))

  (testing "Custom base position"
    (let [segments [{:type :text :content "Test"}]
          result (text-proc/calculate-segment-positions segments 100 200 14)]
      (is (= 100 (:x (first result))))
      (is (= 200 (:y (first result))))
      (is (= 14 (:height (first result))))))

  (testing "Custom character width ratio"
    (let [segments [{:type :text :content "ABC"}]
          result (text-proc/calculate-segment-positions segments 0 0 10 {:char-width-ratio 0.8})]
      ;; Width: 3 chars * 10 * 0.8 = 24
      (is (= 24 (:width (first result))))))

  (testing "Spacing options"
    (let [segments [{:type :text :content "A"}
                    {:type :emoji :content "💡"}]
          result (text-proc/calculate-segment-positions segments 0 0 12 {:text-spacing 5 :image-spacing 3})]
      ;; First segment width: 1 * 12 * 0.6 = 7.2
      ;; Second segment x: 0 + 7.2 + 5 (text spacing) = 12.2
      (is (< (Math/abs (- 12.2 (:x (second result)))) 0.01))))

  (testing "Empty segments"
    (let [result (text-proc/calculate-segment-positions [] 0 0 12)]
      (is (= [] result))))

  (testing "Mixed content positioning"
    (let [segments [{:type :emoji :content "💡"}
                    {:type :text :content " test "}
                    {:type :emoji :content "🎯"}]
          result (text-proc/calculate-segment-positions segments 50 100 16)]
      (is (= 3 (count result)))
      ;; Check all have correct y position
      (is (every? #(= 100 (:y %)) result))
      ;; Check x positions are increasing
      (is (< (:x (nth result 0)) (:x (nth result 1))))
      (is (< (:x (nth result 1)) (:x (nth result 2)))))))

(deftest test-render-mixed-segments
  (testing "Basic mixed content rendering"
    (let [cache (images/create-image-cache)
          segments [{:type :text :content "Hello " :x 0 :y 100}
                    {:type :emoji :content "💡" :x 30 :y 100}
                    {:type :text :content " world" :x 44 :y 100}]
          xobject-refs {"💡" "Em1"}
          result (text-proc/render-mixed-segments segments "Arial" 14 cache {:xobject-refs xobject-refs})]
      ;; Should contain text operators
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))
      (is (clojure.string/includes? result "(Hello ) Tj"))
      (is (clojure.string/includes? result "( world) Tj"))
      ;; Should contain image operators
      (is (clojure.string/includes? result "q"))
      (is (clojure.string/includes? result "Q"))
      (is (clojure.string/includes? result "/Em1 Do"))))

  (testing "Fallback to hex encoding"
    (let [cache (images/create-image-cache)
          segments [{:type :emoji :content "🦄" :x 0 :y 0}]  ; Unicorn emoji, likely no XObject ref
          result (text-proc/render-mixed-segments segments "Arial" 12 cache {:fallback-strategy :hex-string})]
      ;; Should fallback to text rendering with hex encoding
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))
      ;; Should contain hex-encoded content
      (is (clojure.string/includes? result "<"))))

  (testing "Fallback to placeholder"
    (let [cache (images/create-image-cache)
          segments [{:type :emoji :content "🦄" :x 0 :y 0}]
          result (text-proc/render-mixed-segments segments "Arial" 12 cache {:fallback-strategy :placeholder})]
      ;; Should render placeholder text
      (is (clojure.string/includes? result "([🦄]) Tj"))))

  (testing "Skip fallback"
    (let [cache (images/create-image-cache)
          segments [{:type :text :content "Before "}
                    {:type :emoji :content "🦄"}
                    {:type :text :content " after"}]
          result (text-proc/render-mixed-segments segments "Arial" 12 cache {:fallback-strategy :skip})]
      ;; Should render text but skip emoji
      (is (clojure.string/includes? result "(Before ) Tj"))
      (is (clojure.string/includes? result "( after) Tj"))
      ;; Should not contain unicorn emoji content
      (is (not (clojure.string/includes? result "🦄")))))

  (testing "Empty segments"
    (let [cache (images/create-image-cache)
          result (text-proc/render-mixed-segments [] "Arial" 12 cache)]
      (is (= "" result))))

  (testing "Text color option"
    (let [cache (images/create-image-cache)
          segments [{:type :text :content "Red text" :x 0 :y 0}]
          result (text-proc/render-mixed-segments segments "Arial" 12 cache {:color "red"})]
      (is (clojure.string/includes? result "1 0 0 rg")))))

(deftest test-process-mixed-content
  (testing "Complete processing pipeline"
    (let [cache (images/create-image-cache)
          result (text-proc/process-mixed-content "Hello 💡 world" 100 200 "Arial" 14 cache
                                                  {:xobject-refs {"💡" "Em1"}})]
      (is (:success result))
      (is (empty? (:errors result)))
      (is (= 3 (count (:segments result))))
      (is (string? (:operators result)))
      (is (not-empty (:operators result)))))

  (testing "Text with no emoji"
    (let [cache (images/create-image-cache)
          result (text-proc/process-mixed-content "Just text" 0 0 "Arial" 12 cache)]
      (is (:success result))
      (is (= 1 (count (:segments result))))
      (is (= :text (:type (first (:segments result)))))))

  (testing "Only emoji"
    (let [cache (images/create-image-cache)
          result (text-proc/process-mixed-content "💡" 0 0 "Arial" 12 cache {:xobject-refs {"💡" "Em1"}})]
      (is (:success result))
      (is (= 1 (count (:segments result))))
      (is (= :emoji (:type (first (:segments result)))))))

  (testing "Empty text"
    (let [cache (images/create-image-cache)
          result (text-proc/process-mixed-content "" 0 0 "Arial" 12 cache)]
      (is (:success result))
      (is (= [] (:segments result)))
      (is (= "" (:operators result)))))

  (testing "Complex mixed content"
    (let [cache (images/create-image-cache)
          text "Status: ✅ Progress: 💡 Target: 🎯"
          xobject-refs {"✅" "Em1" "💡" "Em2" "🎯" "Em3"}
          result (text-proc/process-mixed-content text 50 150 "Arial" 16 cache {:xobject-refs xobject-refs})]
      (is (:success result))
      (is (= 6 (count (:segments result))))  ; Should have 6 segments
      (is (some #(= :text (:type %)) (:segments result)))
      (is (some #(= :emoji (:type %)) (:segments result)))
      ;; Should contain all XObject references
      (is (clojure.string/includes? (:operators result) "/Em1 Do"))
      (is (clojure.string/includes? (:operators result) "/Em2 Do"))
      (is (clojure.string/includes? (:operators result) "/Em3 Do"))))

  (testing "Error handling"
    ;; Test with nil cache - should still work with fallback
    (let [result (text-proc/process-mixed-content "Test 💡" 0 0 "Arial" 12 nil)]
      (is (:success result))  ; Should succeed with fallback
      (is (= 2 (count (:segments result)))))))

(deftest test-create-xobject-reference-map
  (testing "Basic reference mapping"
    (let [result (text-proc/create-xobject-reference-map "Hello 💡 world 🎯")]
      (is (= 2 (count result)))
      (is (= "Em1" (get result "💡")))
      (is (= "Em2" (get result "🎯")))))

  (testing "No emoji in text"
    (let [result (text-proc/create-xobject-reference-map "Just text")]
      (is (= 0 (count result)))))

  (testing "Duplicate emoji"
    (let [result (text-proc/create-xobject-reference-map "💡 test 💡 more 💡")]
      (is (= 1 (count result)))  ; Should have only one unique emoji
      (is (= "Em1" (get result "💡")))))

  (testing "Custom starting number"
    (let [result (text-proc/create-xobject-reference-map "💡 🎯" 5)]
      (is (= "Em5" (get result "💡")))
      (is (= "Em6" (get result "🎯")))))

  (testing "Complex mixed content"
    (let [text "Progress: 💡 Status: ✅ Target: 🎯 Alert: ⚠"
          result (text-proc/create-xobject-reference-map text)]
      (is (= 4 (count result)))
      (is (contains? result "💡"))
      (is (contains? result "✅"))
      (is (contains? result "🎯"))
      (is (contains? result "⚠"))))

  (testing "Empty text"
    (let [result (text-proc/create-xobject-reference-map "")]
      (is (= 0 (count result))))))

(deftest test-extract-unique-emoji
  (testing "Basic emoji extraction"
    (let [result (text-proc/extract-unique-emoji "Hello 💡 world 🎯")]
      (is (= #{"💡" "🎯"} result))))

  (testing "Duplicate emoji"
    (let [result (text-proc/extract-unique-emoji "💡 test 💡 more 💡 end")]
      (is (= #{"💡"} result))))

  (testing "No emoji"
    (let [result (text-proc/extract-unique-emoji "Just regular text")]
      (is (= #{} result))))

  (testing "Adjacent emoji"
    (let [result (text-proc/extract-unique-emoji "💡🎯✅")]
      (is (= #{"💡" "🎯" "✅"} result))))

  (testing "Complex text"
    (let [text "Progress: 💡 Done: ✅ Target: 🎯 Alert: ⚠ More: 💡"
          result (text-proc/extract-unique-emoji text)]
      (is (= #{"💡" "✅" "🎯" "⚠"} result))))

  (testing "Empty text"
    (let [result (text-proc/extract-unique-emoji "")]
      (is (= #{} result)))))

(deftest test-mixed-content-integration
  (testing "End-to-end mixed content processing"
    (let [cache (images/create-image-cache)
          text "Task: 💡 Status: ✅ Next: 🎯"
          ;; Create XObject references
          xobject-refs (text-proc/create-xobject-reference-map text)
          ;; Extract unique emoji
          unique-emoji (text-proc/extract-unique-emoji text)
          ;; Process mixed content
          result (text-proc/process-mixed-content text 100 200 "Arial" 16 cache {:xobject-refs xobject-refs})]

      ;; Verify XObject references
      (is (= 3 (count xobject-refs)))
      (is (contains? xobject-refs "💡"))
      (is (contains? xobject-refs "✅"))
      (is (contains? xobject-refs "🎯"))

      ;; Verify unique emoji extraction
      (is (= #{"💡" "✅" "🎯"} unique-emoji))

      ;; Verify processing results
      (is (:success result))
      (is (= 6 (count (:segments result))))  ; 3 text + 3 emoji segments

      ;; Verify operators contain all references
      (is (clojure.string/includes? (:operators result) "/Em1 Do"))
      (is (clojure.string/includes? (:operators result) "/Em2 Do"))
      (is (clojure.string/includes? (:operators result) "/Em3 Do"))

      ;; Verify text content
      (is (clojure.string/includes? (:operators result) "(Task: ) Tj"))
      (is (clojure.string/includes? (:operators result) "( Status: ) Tj"))
      (is (clojure.string/includes? (:operators result) "( Next: ) Tj"))))

  (testing "Performance with complex content"
    (let [cache (images/create-image-cache)
          ;; Create text with many mixed segments
          text (apply str (interpose " " (concat ["Start"
                                                  (repeat 10 "💡")
                                                  ["Middle"]
                                                  (repeat 10 "🎯")
                                                  ["End"]])))
          xobject-refs (text-proc/create-xobject-reference-map text)
          result (text-proc/process-mixed-content text 0 0 "Arial" 12 cache {:xobject-refs xobject-refs})]

      ;; Should process successfully
      (is (:success result))
      (is (> (count (:segments result)) 20))  ; Should have many segments
      (is (not-empty (:operators result)))

      ;; Should handle unique emoji correctly
      (is (= #{"💡" "🎯"} (text-proc/extract-unique-emoji text)))
      (is (= 2 (count xobject-refs))))))
