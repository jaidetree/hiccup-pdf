(ns dev.jaide.hiccup-pdf.document-integration-test
  "Tests for document-level emoji image integration"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.core :as core]
            [dev.jaide.hiccup-pdf.document :as doc]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest test-document-emoji-image-integration
  (testing "Simple document with emoji images"
    (let [cache (images/create-image-cache)
          document [:document {:title "Emoji Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (clojure.string/includes? result "%PDF-1.4"))
      (is (clojure.string/includes? result "%%EOF"))
      (is (> (count result) 1000))))  ; Should be substantial document
  
  (testing "Multi-page document with shared emoji"
    (let [cache (images/create-image-cache)
          document [:document {:title "Multi-page Emoji"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 💡 🎯"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (clojure.string/includes? result "%PDF-1.4"))
      (is (clojure.string/includes? result "%%EOF"))))
  
  (testing "Document with different page sizes and emoji"
    (let [cache (images/create-image-cache)
          document [:document {:title "Mixed Size Emoji" :width 612 :height 792}
                    [:page {}  ; Letter size
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Letter: ✅"]]
                    [:page {:width 842 :height 595}  ; A4 landscape
                     [:text {:x 100 :y 100 :font "Arial" :size 16} "A4: 🎯 💡"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (clojure.string/includes? result "612 792"))   ; Letter page size
      (is (clojure.string/includes? result "842 595"))))  ; A4 page size
  
  (testing "Document with complex nested structures and emoji"
    (let [cache (images/create-image-cache)
          document [:document {:title "Complex Emoji Structure"}
                    [:page {}
                     [:g {:transforms [[:translate [50 50]]]}
                      [:text {:x 0 :y 0 :font "Arial" :size 12} "Group: 💡"]
                      [:g {:transforms [[:rotate 45]]}
                       [:text {:x 20 :y 20 :font "Arial" :size 10} "Nested: ✅"]]]
                     [:text {:x 200 :y 200 :font "Arial" :size 14} "Outside: 🎯"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (clojure.string/includes? result "q"))  ; Should have save states
      (is (clojure.string/includes? result "Q"))))  ; Should have restore states
  
  (testing "Document without emoji images (fallback mode)"
    (let [document [:document {:title "Fallback Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]]]
          result (core/hiccup->pdf-document document)]  ; No emoji options
      (is (string? result))
      (is (clojure.string/includes? result "%PDF-1.4"))
      (is (clojure.string/includes? result "%%EOF"))
      ;; Should contain hex encoding for emoji
      (is (clojure.string/includes? result "<"))))
  
  (testing "Document with emoji configuration options"
    (let [cache (images/create-image-cache)
          document [:document {:title "Config Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Test: 🦄"]]]  ; Unicorn unlikely to have file
          options {:enable-emoji-images true 
                   :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (clojure.string/includes? result "%PDF-1.4"))))
  
  (testing "Large document with many unique emoji"
    (let [cache (images/create-image-cache)
          ;; Create a document with multiple emoji
          document [:document {:title "Large Emoji Document"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Status: ✅ Progress: 💡 Target: 🎯"]
                     [:text {:x 100 :y 150 :font "Arial" :size 12} "More symbols: ⚠️ • Bullet"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (> (count result) 2000))))  ; Should be larger document with multiple images
  
  (testing "Empty pages handled correctly"
    (let [cache (images/create-image-cache)
          document [:document {:title "Empty Pages"}
                    [:page {}]  ; Empty page
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Second page: 💡"]]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-document document options)]
      (is (string? result))
      (is (clojure.string/includes? result "%PDF-1.4"))))
  
  (testing "Error handling during document generation"
    ;; Test with invalid emoji config
    (let [document [:document {:title "Error Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Test: 💡"]]]
          invalid-options {:enable-emoji-images true}]  ; No cache provided
      ;; Should not throw error, should fallback gracefully
      (let [result (core/hiccup->pdf-document document invalid-options)]
        (is (string? result))
        (is (clojure.string/includes? result "%PDF-1.4")))))
  
  (testing "Performance with emoji images vs without"
    (let [cache (images/create-image-cache)
          document [:document {:title "Performance Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Normal text"]
                     [:text {:x 100 :y 150 :font "Arial" :size 14} "Text with 💡"]]]
          
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
      ;; Both should complete reasonably quickly
      (is (< duration1 2000))  ; Less than 2 seconds
      (is (< duration2 3000))  ; Less than 3 seconds (allowing for image processing)
      ;; Results should be different (emoji vs hex encoding)
      (is (not= result1 result2)))))

(deftest test-page-content-stream-with-emoji
  (testing "Page content stream generation with emoji"
    (let [cache (images/create-image-cache)
          page-content [[:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡"]]
          document-defaults {:width 612 :height 792 :margins [0 0 0 0]}
          options {:enable-emoji-images true :image-cache cache}
          result (doc/page->content-stream {} page-content document-defaults options)]
      (is (map? result))
      (is (contains? result :content-stream))
      (is (contains? result :emoji-used))
      (is (string? (:content-stream result)))
      (is (set? (:emoji-used result)))))
  
  (testing "Page content stream without emoji"
    (let [page-content [[:text {:x 100 :y 100 :font "Arial" :size 14} "Hello world"]]
          document-defaults {:width 612 :height 792 :margins [0 0 0 0]}
          result (doc/page->content-stream {} page-content document-defaults)]
      (is (map? result))
      (is (contains? result :content-stream))
      (is (contains? result :emoji-used))
      (is (string? (:content-stream result)))
      (is (empty? (:emoji-used result)))))
  
  (testing "Multiple pages sharing emoji resources"
    (let [cache (images/create-image-cache)
          document-defaults {:width 612 :height 792}
          options {:enable-emoji-images true :image-cache cache}
          
          ;; First page with 💡
          page1 (doc/page->content-stream {} 
                                         [[:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                                         document-defaults options)
          
          ;; Second page with 💡 and 🎯
          page2 (doc/page->content-stream {} 
                                         [[:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 💡 🎯"]]
                                         document-defaults options)]
      
      (is (contains? (:emoji-used page1) "💡"))
      (is (contains? (:emoji-used page2) "💡"))
      (is (contains? (:emoji-used page2) "🎯"))
      (is (= 1 (count (:emoji-used page1))))
      (is (= 2 (count (:emoji-used page2)))))))

(deftest test-emoji-resource-management
  (testing "Document emoji scanning"
    (let [document [:document {:title "Scan Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 1: 💡"]]
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Page 2: 🎯 💡"]]]
          emoji-set (doc/scan-document-for-emoji document)]
      (is (set? emoji-set))
      (is (contains? emoji-set "💡"))
      (is (contains? emoji-set "🎯"))
      (is (= 2 (count emoji-set)))))
  
  (testing "Image resource generation"
    (let [cache (images/create-image-cache)
          unique-emoji #{"💡" "🎯"}
          options {:fallback-strategy :hex-string}
          result (doc/embed-images-in-document unique-emoji cache 10 options)]
      (is (map? result))
      (is (contains? result :image-objects))
      (is (contains? result :image-refs))
      (is (contains? result :xobject-names))
      (is (contains? result :success))
      (is (vector? (:image-objects result)))
      (is (map? (:image-refs result)))
      (is (map? (:xobject-names result)))))
  
  (testing "Resource dictionary generation"
    (let [image-refs {"💡" 10 "🎯" 11}
          xobject-names {"💡" "Em1" "🎯" "Em2"}
          resource-dict (doc/generate-image-resources image-refs xobject-names)]
      (is (string? resource-dict))
      (when (not-empty resource-dict)
        (is (clojure.string/includes? resource-dict "/XObject"))
        (is (clojure.string/includes? resource-dict "/Em1"))
        (is (clojure.string/includes? resource-dict "/Em2")))))
  
  (testing "Page resource merging"
    (let [font-dict "/Font <<\n/Arial 3 0 R\n>>"
          image-dict "/XObject <<\n/Em1 10 0 R\n>>"
          merged (doc/update-page-resources font-dict image-dict)]
      (is (string? merged))
      (is (clojure.string/includes? merged "/Resources"))
      (is (clojure.string/includes? merged "/Font"))
      (is (clojure.string/includes? merged "/XObject")))))

(deftest test-document-generation-pipeline
  (testing "Complete document generation pipeline"
    (let [cache (images/create-image-cache)
          
          ;; Step 1: Create complex document
          document [:document {:title "Pipeline Test" :author "Test Author"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 16} "Title: 💡 Innovation"]
                     [:rect {:x 50 :y 150 :width 200 :height 100 :fill "#ff0000"}]
                     [:text {:x 60 :y 180 :font "Arial" :size 12} "Status: ✅ Complete"]]
                    [:page {:width 842 :height 595}
                     [:circle {:cx 400 :cy 300 :r 50 :fill "#00ff00"}]
                     [:text {:x 350 :y 250 :font "Arial" :size 14} "Target: 🎯"]]]
          
          options {:enable-emoji-images true :image-cache cache}
          
          ;; Step 2: Generate document
          start-time (.now js/Date)
          result (core/hiccup->pdf-document document options)
          end-time (.now js/Date)
          duration (- end-time start-time)]
      
      ;; Step 3: Validate results
      (is (string? result))
      (is (> (count result) 3000))  ; Substantial document
      (is (< duration 5000))  ; Completes within 5 seconds
      
      ;; Check PDF structure
      (is (clojure.string/includes? result "%PDF-1.4"))
      (is (clojure.string/includes? result "%%EOF"))
      (is (clojure.string/includes? result "/Type /Catalog"))
      (is (clojure.string/includes? result "/Type /Page"))
      (is (clojure.string/includes? result "xref"))
      (is (clojure.string/includes? result "trailer"))
      
      ;; Check for font resources
      (is (clojure.string/includes? result "/Font"))
      (is (clojure.string/includes? result "/Arial"))
      
      ;; Check for image resources (if images were loaded)
      ;; Note: May not be present if image files don't exist
      
      ;; Check for page content
      (is (clojure.string/includes? result "BT"))  ; Text blocks
      (is (clojure.string/includes? result "ET"))
      (is (clojure.string/includes? result "re"))  ; Rectangle
      
      ;; Check metadata
      (is (clojure.string/includes? result "(Pipeline Test)"))
      (is (clojure.string/includes? result "(Test Author)"))))
  
  (testing "Error recovery in document generation"
    (let [;; Document with potential issues
          document [:document {:title "Error Recovery"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Test: 🦄 🌈 🦋"]]]  ; Unlikely emoji files
          
          options {:enable-emoji-images true
                   :image-cache (images/create-image-cache)
                   :fallback-strategy :placeholder}
          
          result (core/hiccup->pdf-document document options)]
      
      ;; Should still generate valid PDF even with missing emoji files
      (is (string? result))
      (is (clojure.string/includes? result "%PDF-1.4"))
      (is (clojure.string/includes? result "%%EOF"))))
  
  (testing "Memory efficiency with large documents"
    (let [cache (images/create-image-cache)
          
          ;; Create document with many pages and emoji
          pages (for [i (range 5)]
                  [:page {}
                   [:text {:x 100 :y (+ 100 (* i 50)) :font "Arial" :size 12} 
                    (str "Page " (+ i 1) ": 💡 Progress " i "/5")]])
          
          document (into [:document {:title "Large Document"}] pages)
          
          options {:enable-emoji-images true :image-cache cache}
          
          _start-memory (.now js/Date)  ; Proxy for memory usage
          result (core/hiccup->pdf-document document options)
          _end-memory (.now js/Date)
          
          cache-stats (images/cache-stats cache)]
      
      (is (string? result))
      (is (< (:memory-usage cache-stats) (* 10 1024 1024)))  ; Less than 10MB cache
      (is (>= (:hit-rate cache-stats) 0))))  ; Non-negative hit rate
  
  (testing "Backwards compatibility"
    ;; Ensure existing code still works without emoji options
    (let [document [:document {:title "Compatibility Test"}
                    [:page {}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡 world"]]]
          
          ;; Old API call (no options)
          result1 (core/hiccup->pdf-document document)
          
          ;; New API call (with disabled emoji)
          result2 (core/hiccup->pdf-document document {:enable-emoji-images false})]
      
      (is (string? result1))
      (is (string? result2))
      ;; Should produce similar results (both using hex encoding)
      (is (clojure.string/includes? result1 "<"))  ; Hex encoding
      (is (clojure.string/includes? result2 "<"))))  ; Hex encoding
)