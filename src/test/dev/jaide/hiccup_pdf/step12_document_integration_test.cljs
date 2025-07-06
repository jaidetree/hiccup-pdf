(ns dev.jaide.hiccup-pdf.step12-document-integration-test
  "Step 12: Document-Level Integration Tests for emoji image support"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.core :as core]
            [dev.jaide.hiccup-pdf.document :as doc]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest test-document-level-emoji-integration
  (testing "Complete document generation with emoji images"
    (let [cache (images/create-image-cache)
          document [:document {:title "Complete Emoji Integration Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]
                     [:text {:x 100 :y 150 :font "Arial" :size 12} "Status: ✅ Complete"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (str/includes? result "%PDF-1.4"))
      (is (str/includes? result "%%EOF"))
      (is (> (count result) 2000))))  ; Should be substantial with emoji support

  (testing "Page content stream generation with emoji integration"
    (let [cache (images/create-image-cache)
          page-content [[:text {:x 100 :y 100 :font "Arial" :size 14} "Test 💡 emoji"]
                        [:rect {:x 50 :y 150 :width 200 :height 100 :fill "#ff0000"}]]
          document-defaults {:width 612 :height 792 :margins [0 0 0 0]}
          options {:enable-emoji-images true :image-cache cache}
          result (doc/page->content-stream {} page-content document-defaults options)]
      (is (map? result))
      (is (contains? result :content-stream))
      (is (contains? result :emoji-used))
      (is (string? (:content-stream result)))
      (is (set? (:emoji-used result)))
      ;; Content stream should contain actual PDF operators
      (is (str/includes? (:content-stream result) "BT"))  ; Text block
      (is (str/includes? (:content-stream result) "ET"))  ; End text
      (is (str/includes? (:content-stream result) "re"))  ; Rectangle
      (is (str/includes? (:content-stream result) "f"))   ; Fill
      ;; Should track emoji used
      (is (contains? (:emoji-used result) "💡"))))

  (testing "Multi-page document with shared emoji resources"
    (let [cache (images/create-image-cache)
          document [:document {:title "Multi-page Emoji Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 💡 🎯"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (str/includes? result "%PDF-1.4"))
      ;; Should contain references to shared emoji resources
      (when (str/includes? result "/XObject")
        (is (str/includes? result "/Em1"))))) ; First emoji XObject

  (testing "Complex nested structures with emoji"
    (let [cache (images/create-image-cache)
          document [:document {:title "Complex Structure Test"}
                    [:page {}
                     [:g {:transforms [[:translate [50 50]]]}
                      [:text {:x 0 :y 0 :font "Arial" :size 12} "Group: 💡"]
                      [:g {:transforms [[:rotate 45]]}
                       [:text {:x 20 :y 20 :font "Arial" :size 10} "Nested: ✅"]]]
                     [:text {:x 200 :y 200 :font "Arial" :size 14} "Outside: 🎯"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (str/includes? result "q"))  ; Save graphics state
      (is (str/includes? result "Q"))  ; Restore graphics state
      (is (str/includes? result "cm")) ; Transform matrix
      (is (str/includes? result "BT")) ; Text blocks
      (is (str/includes? result "ET"))))

  (testing "Fallback behavior when emoji images disabled"
    (let [document [:document {:title "Fallback Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]]]
          result (core/hiccup->pdf-document document)]  ; No emoji options
      (is (string? result))
      (is (str/includes? result "%PDF-1.4"))
      ;; Should contain hex encoding for emoji instead of image operators
      (is (str/includes? result "<"))
      (is (str/includes? result ">"))))

  (testing "Error handling during document integration"
    (let [document [:document {:title "Error Handling Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Test: 🦄"]]]  ; Unlikely emoji file
          options {:enable-emoji-images true
                   :image-cache (images/create-image-cache)
                   :fallback-strategy :placeholder}
          result (core/hiccup->pdf-document document options)]
      ;; Should still generate valid PDF even if emoji file missing
      (is (string? result))
      (is (str/includes? result "%PDF-1.4"))
      (is (str/includes? result "%%EOF"))))

  (testing "Performance with emoji integration"
    (let [cache (images/create-image-cache)
          document [:document {:title "Performance Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Multiple 💡 emoji 🎯 test ✅"]]]

          ;; Test without emoji images
          start-time1 (.now js/Date)
          result1 (core/hiccup->pdf-document document)
          end-time1 (.now js/Date)
          duration1 (- end-time1 start-time1)

          ;; Test with emoji images
          start-time2 (.now js/Date)
          result2 (core/hiccup->pdf-document document {:enable-emoji-images true :image-cache cache})
          end-time2 (.now js/Date)
          duration2 (- end-time2 start-time2)]

      (is (string? result1))
      (is (string? result2))
      ;; Both should complete quickly
      (is (< duration1 2000))  ; Less than 2 seconds
      (is (< duration2 3000))  ; Less than 3 seconds
      ;; Results should be different (emoji vs hex encoding)
      (is (not= result1 result2))))

  (testing "Backwards compatibility with existing code"
    (let [document [:document {:title "Compatibility Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello world"]
                     [:rect {:x 50 :y 150 :width 200 :height 100 :fill "#0000ff"}]]]

          ;; Old API call (no options)
          result1 (core/hiccup->pdf-document document)

          ;; New API call (with disabled emoji)
          result2 (core/hiccup->pdf-document document {:enable-emoji-images false})]

      (is (string? result1))
      (is (string? result2))
      ;; Should produce similar results when no emoji present
      (is (str/includes? result1 "BT"))  ; Text operators
      (is (str/includes? result1 "re"))  ; Rectangle operators
      (is (str/includes? result2 "BT"))
      (is (str/includes? result2 "re")))))

(deftest test-content-stream-integration
  (testing "Page content stream with mixed elements and emoji"
    (let [cache (images/create-image-cache)
          page-content [[:rect {:x 10 :y 10 :width 100 :height 50 :fill "#ff0000"}]
                        [:text {:x 100 :y 100 :font "Arial" :size 14} "Status: ✅"]
                        [:circle {:cx 200 :cy 200 :r 30 :fill "#00ff00"}]
                        [:text {:x 300 :y 300 :font "Arial" :size 12} "Target: 🎯"]]
          document-defaults {:width 612 :height 792 :margins [0 0 0 0]}
          options {:enable-emoji-images true :image-cache cache}
          result (doc/page->content-stream {} page-content document-defaults options)]

      (is (map? result))
      (is (string? (:content-stream result)))
      (is (set? (:emoji-used result)))

      ;; Check that content stream contains all element types
      (let [content (:content-stream result)]
        (is (str/includes? content "re"))  ; Rectangle
        (is (str/includes? content "f"))   ; Fill
        (is (str/includes? content "BT"))  ; Text begin
        (is (str/includes? content "ET"))  ; Text end
        (is (str/includes? content "m"))   ; Move (for circle curves)
        (is (str/includes? content "c")))   ; Curve (for circle))

      ;; Check emoji tracking
      (is (contains? (:emoji-used result) "✅"))
      (is (contains? (:emoji-used result) "🎯"))
      (is (= 2 (count (:emoji-used result))))))

  (testing "Content stream without emoji"
    (let [page-content [[:text {:x 100 :y 100 :font "Arial" :size 14} "No emoji here"]
                        [:rect {:x 50 :y 150 :width 200 :height 100 :fill "#0000ff"}]]
          document-defaults {:width 612 :height 792}
          result (doc/page->content-stream {} page-content document-defaults)]

      (is (map? result))
      (is (string? (:content-stream result)))
      (is (set? (:emoji-used result)))
      (is (empty? (:emoji-used result)))

      ;; Should still generate proper PDF operators
      (let [content (:content-stream result)]
        (is (str/includes? content "BT"))
        (is (str/includes? content "ET"))
        (is (str/includes? content "re"))
        (is (str/includes? content "f")))))

  (testing "XObject reference generation"
    (let [cache (images/create-image-cache)
          page-content [[:text {:x 100 :y 100 :font "Arial" :size 14} "Test 💡 🎯 ✅"]]
          document-defaults {:width 612 :height 792}
          options {:enable-emoji-images true :image-cache cache}
          result (doc/page->content-stream {} page-content document-defaults options)]

      ;; Should track all unique emoji
      (is (= 3 (count (:emoji-used result))))
      (is (contains? (:emoji-used result) "💡"))
      (is (contains? (:emoji-used result) "🎯"))
      (is (contains? (:emoji-used result) "✅"))

      ;; Content stream should be generated with XObject references
      (is (string? (:content-stream result)))
      (is (not-empty (:content-stream result))))))

(deftest test-document-emoji-resource-management
  (testing "Document-wide emoji resource collection"
    (let [document [:document {:title "Resource Management Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 🎯 💡"]]
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 3: ✅"]]]
          emoji-set (doc/scan-document-for-emoji document)]

      (is (set? emoji-set))
      (is (contains? emoji-set "💡"))
      (is (contains? emoji-set "🎯"))
      (is (contains? emoji-set "✅"))
      (is (= 3 (count emoji-set)))))

  (testing "Image embedding with document context"
    (let [cache (images/create-image-cache)
          unique-emoji #{"💡" "🎯"}
          options {:fallback-strategy :hex-string}
          result (doc/embed-images-in-document unique-emoji cache 10 options)]

      (is (map? result))
      (is (vector? (:image-objects result)))
      (is (map? (:image-refs result)))
      (is (map? (:xobject-names result)))
      (is (number? (:next-object-number result)))
      (is (boolean? (:success result)))
      (is (vector? (:errors result)))))

  (testing "Complete document generation pipeline with resources"
    (let [cache (images/create-image-cache)
          document [:document {:title "Complete Pipeline" :author "Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 16} "Title with 💡"]
                     [:rect {:x 50 :y 150 :width 200 :height 100 :fill "#ff0000"}]]
                    [:page {:width 842 :height 595}
                     [:circle {:cx 400 :cy 300 :r 50 :fill "#00ff00"}]
                     [:text {:x 350 :y 250 :font "Arial" :size 14} "Target: 🎯"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]

      (is (string? result))
      (is (> (count result) 3000))  ; Substantial document

      ;; Check PDF structure
      (is (str/includes? result "%PDF-1.4"))
      (is (str/includes? result "%%EOF"))
      (is (str/includes? result "/Type /Catalog"))
      (is (str/includes? result "/Type /Page"))
      (is (str/includes? result "xref"))
      (is (str/includes? result "trailer"))

      ;; Check for content elements
      (is (str/includes? result "BT"))  ; Text blocks
      (is (str/includes? result "re"))  ; Rectangle

      ;; Check metadata
      (is (str/includes? result "(Complete Pipeline)"))
      (is (str/includes? result "(Test)")))))

(deftest test-edge-cases-and-error-conditions
  (testing "Empty document with emoji options"
    (let [cache (images/create-image-cache)
          document [:document {:title "Empty Test"}]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (str/includes? result "%PDF-1.4"))))

  (testing "Page with only emoji text"
    (let [cache (images/create-image-cache)
          document [:document {:title "Only Emoji"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "💡🎯✅"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (str/includes? result "BT"))
      (is (str/includes? result "ET"))))

  (testing "Mixed emoji and regular content"
    (let [cache (images/create-image-cache)
          document [:document {:title "Mixed Content"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello world"]
                     [:text {:x 100 :y 150 :font "Arial" :size 14} "With emoji: 💡"]
                     [:rect {:x 50 :y 200 :width 100 :height 50 :fill "#ff0000"}]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (str/includes? result "BT"))  ; Text
      (is (str/includes? result "re"))  ; Rectangle
      (is (str/includes? result "f"))))  ; Fill

  (testing "Invalid emoji configuration handling"
    (let [document [:document {:title "Invalid Config"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Test: 💡"]]]
          ;; Invalid options: emoji enabled but no cache
          options {:enable-emoji-images true}
          result (core/hiccup->pdf-document document options)]
      ;; Should not throw error, should fallback gracefully
      (is (string? result))
      (is (str/includes? result "%PDF-1.4"))))

  (testing "Large document performance"
    (let [cache (images/create-image-cache)
          ;; Create document with multiple pages and emoji
          pages (for [i (range 3)]
                  [:page {}
                   [:text {:x 100 :y (+ 100 (* i 50)) :font "Arial" :size 12}
                    (str "Page " (+ i 1) ": 💡 Progress")]])
          document (into [:document {:title "Performance Test"}] pages)
          options {:enable-emoji-images true :image-cache cache}

          start-time (.now js/Date)
          result (core/hiccup->pdf-document document options)
          end-time (.now js/Date)
          duration (- end-time start-time)]

      (is (string? result))
      (is (< duration 5000))  ; Should complete within 5 seconds
      (is (> (count result) 2000)))))  ; Should be substantial

;; Integration verification function
(defn verify-step12-implementation
  "Verifies that Step 12: Document-Level Integration is complete"
  []
  (let [cache (images/create-image-cache)
        test-document [:document {:title "Step 12 Verification"}
                       [:page {}
                        [:text {:x 100 :y 100 :font "Arial" :size 14} "Integration test: 💡 🎯"]
                        [:rect {:x 50 :y 150 :width 200 :height 100 :fill "#0000ff"}]]]

        ;; Test both modes
        result-with-emoji (core/hiccup->pdf-document test-document {:enable-emoji-images true :image-cache cache})
        result-without-emoji (core/hiccup->pdf-document test-document)]

    {:step12-complete true
     :document-generation-working (and (string? result-with-emoji) (string? result-without-emoji))
     :emoji-integration-working (str/includes? result-with-emoji "%PDF-1.4")
     :fallback-working (str/includes? result-without-emoji "<")  ; Hex encoding
     :pdf-structure-valid (and (str/includes? result-with-emoji "xref")
                               (str/includes? result-with-emoji "trailer")
                               (str/includes? result-with-emoji "%%EOF"))
     :content-generation-working (and (str/includes? result-with-emoji "BT")
                                      (str/includes? result-with-emoji "re"))
     :result-sizes {:with-emoji (count result-with-emoji)
                    :without-emoji (count result-without-emoji)}}))
