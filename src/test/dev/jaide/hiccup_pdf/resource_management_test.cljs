(ns dev.jaide.hiccup-pdf.resource-management-test
  "Tests for emoji image resource management functions"
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.document :as doc]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest test-collect-page-images
  (testing "Basic emoji collection from page content"
    (let [page-content [[:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 💡 world"]
                        [:text {:x 100 :y 220 :font "Arial" :size 14} "Status: ✅"]]
          result (doc/collect-page-images page-content)]
      (is (= #{"💡" "✅"} result))))
  
  (testing "Nested elements with emoji"
    (let [page-content [[:g {:transforms [[:translate [50 50]]]}
                         [:text {:x 0 :y 0 :font "Arial" :size 12} "Task 💡"]
                         [:text {:x 0 :y 20 :font "Arial" :size 12} "Done ✅"]]]
          result (doc/collect-page-images page-content)]
      (is (= #{"💡" "✅"} result))))
  
  (testing "No emoji in content"
    (let [page-content [[:text {:x 100 :y 200 :font "Arial" :size 14} "Hello world"]
                        [:rect {:x 50 :y 50 :width 100 :height 50 :fill "red"}]]
          result (doc/collect-page-images page-content)]
      (is (= #{} result))))
  
  (testing "Empty page content"
    (let [result (doc/collect-page-images [])]
      (is (= #{} result))))
  
  (testing "Complex nested structure"
    (let [page-content [[:g {:transforms [[:translate [10 10]]]}
                         [:g {:transforms [[:rotate 45]]}
                          [:text {:x 0 :y 0 :font "Arial" :size 10} "Progress: 💡"]
                          [:text {:x 0 :y 15 :font "Arial" :size 10} "Target: 🎯"]]
                         [:text {:x 20 :y 40 :font "Arial" :size 12} "Alert: ⚠️"]]]
          result (doc/collect-page-images page-content)]
      (is (= #{"💡" "🎯" "⚠️"} result))))
  
  (testing "Duplicate emoji in different elements"
    (let [page-content [[:text {:x 100 :y 200 :font "Arial" :size 14} "First 💡"]
                        [:text {:x 100 :y 220 :font "Arial" :size 14} "Second 💡"]
                        [:text {:x 100 :y 240 :font "Arial" :size 14} "Third 💡"]]
          result (doc/collect-page-images page-content)]
      (is (= #{"💡"} result))))  ; Should be unique
  
  (testing "Mixed content with multiple emoji per text element"
    (let [page-content [[:text {:x 100 :y 200 :font "Arial" :size 14} "Status: ✅ Progress: 💡 Target: 🎯"]]
          result (doc/collect-page-images page-content)]
      (is (= #{"✅" "💡" "🎯"} result)))))

(deftest test-generate-image-resources
  (testing "Basic image resource generation"
    (let [image-refs {"💡" 10 "🎯" 11}
          xobject-names {"💡" "Em1" "🎯" "Em2"}
          result (doc/generate-image-resources image-refs xobject-names)]
      (is (clojure.string/includes? result "/XObject <<"))
      (is (clojure.string/includes? result "/Em1 10 0 R"))
      (is (clojure.string/includes? result "/Em2 11 0 R"))
      (is (clojure.string/includes? result ">>")))))
  
  (testing "Empty image resources"
    (let [result (doc/generate-image-resources {} {})]
      (is (= "" result))))
  
  (testing "Missing XObject names"
    (let [image-refs {"💡" 10}
          xobject-names {}  ; No names provided
          result (doc/generate-image-resources image-refs xobject-names)]
      (is (= "" result))))  ; Should return empty string
  
  (testing "Partial XObject names"
    (let [image-refs {"💡" 10 "🎯" 11}
          xobject-names {"💡" "Em1"}  ; Only one name provided
          result (doc/generate-image-resources image-refs xobject-names)]
      (is (clojure.string/includes? result "/Em1 10 0 R"))
      (is (not (clojure.string/includes? result "/Em2")))))
  
  (testing "Many images"
    (let [image-refs {"💡" 10 "🎯" 11 "✅" 12 "⚠️" 13}
          xobject-names {"💡" "Em1" "🎯" "Em2" "✅" "Em3" "⚠️" "Em4"}
          result (doc/generate-image-resources image-refs xobject-names)]
      (is (clojure.string/includes? result "/Em1 10 0 R"))
      (is (clojure.string/includes? result "/Em2 11 0 R"))
      (is (clojure.string/includes? result "/Em3 12 0 R"))
      (is (clojure.string/includes? result "/Em4 13 0 R"))))

(deftest test-update-page-resources
  (testing "Both fonts and images"
    (let [font-dict "/Font <<\n/Arial 3 0 R\n/Times 4 0 R\n>>"
          image-dict "/XObject <<\n/Em1 10 0 R\n/Em2 11 0 R\n>>"
          result (doc/update-page-resources font-dict image-dict)]
      (is (clojure.string/includes? result "/Resources <<"))
      (is (clojure.string/includes? result "/Font <<"))
      (is (clojure.string/includes? result "/XObject <<"))
      (is (clojure.string/includes? result ">>")))))
  
  (testing "Only fonts"
    (let [font-dict "/Font <<\n/Arial 3 0 R\n>>"
          image-dict ""
          result (doc/update-page-resources font-dict image-dict)]
      (is (clojure.string/includes? result "/Resources <<"))
      (is (clojure.string/includes? result "/Font <<"))
      (is (not (clojure.string/includes? result "/XObject <<")))))
  
  (testing "Only images"
    (let [font-dict ""
          image-dict "/XObject <<\n/Em1 10 0 R\n>>"
          result (doc/update-page-resources font-dict image-dict)]
      (is (clojure.string/includes? result "/Resources <<"))
      (is (clojure.string/includes? result "/XObject <<"))
      (is (not (clojure.string/includes? result "/Font <<")))))
  
  (testing "No resources"
    (let [result (doc/update-page-resources "" "")]
      (is (= "" result))))
  
  (testing "Blank strings"
    (let [result (doc/update-page-resources "   " "  ")]
      (is (= "" result)))))

(deftest test-embed-images-in-document
  (testing "No emoji to process"
    (let [cache (images/create-image-cache)
          result (doc/embed-images-in-document #{} cache 100)]
      (is (:success result))
      (is (= [] (:image-objects result)))
      (is (= {} (:image-refs result)))
      (is (= {} (:xobject-names result)))
      (is (= 100 (:next-object-number result)))))
  
  (testing "XObject name generation"
    (let [cache (images/create-image-cache)
          unique-emoji #{"💡" "🎯"}
          result (doc/embed-images-in-document unique-emoji cache 100 {:fallback-strategy :skip})]
      (is (:success result))
      (is (= {"💡" "Em1" "🎯" "Em2"} (:xobject-names result)))))
  
  (testing "Error handling with invalid emoji"
    (let [cache (images/create-image-cache)
          unique-emoji #{"🦄"}  ; Unicorn emoji, likely no image file
          result (doc/embed-images-in-document unique-emoji cache 100)]
      (is (:success result))  ; Should succeed with fallback
      ;; With fallback strategy, should skip XObject generation
      (is (empty? (:image-objects result)))))
  
  (testing "Object number sequencing"
    (let [cache (images/create-image-cache)
          unique-emoji #{"💡" "🎯" "✅"}
          result (doc/embed-images-in-document unique-emoji cache 50 {:fallback-strategy :skip})]
      (is (:success result))
      ;; Should generate sequential XObject names
      (is (= {"💡" "Em1" "🎯" "Em2" "✅" "Em3"} (:xobject-names result)))))
  
  (testing "Empty emoji set"
    (let [cache (images/create-image-cache)
          result (doc/embed-images-in-document #{} cache 100)]
      (is (:success result))
      (is (= 100 (:next-object-number result)))))
  
  (testing "Large emoji set"
    (let [cache (images/create-image-cache)
          unique-emoji #{"💡" "🎯" "✅" "⚠️" "🔥" "⭐" "🚀" "💯"}
          result (doc/embed-images-in-document unique-emoji cache 200 {:fallback-strategy :skip})]
      (is (:success result))
      (is (= 8 (count (:xobject-names result))))
      ;; Check all emoji have XObject names
      (is (every? #(contains? (:xobject-names result) %) unique-emoji)))))

(deftest test-scan-document-for-emoji
  (testing "Single page document"
    (let [document [:document {:title "Test"}
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 💡 world"]]]
          result (doc/scan-document-for-emoji document)]
      (is (= #{"💡"} result))))
  
  (testing "Multi-page document"
    (let [document [:document {:title "Test"}
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "Page 1: 💡"]]
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "Page 2: 🎯"]]]
          result (doc/scan-document-for-emoji document)]
      (is (= #{"💡" "🎯"} result))))
  
  (testing "Document with no emoji"
    (let [document [:document {:title "Test"}
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello world"]]]
          result (doc/scan-document-for-emoji document)]
      (is (= #{} result))))
  
  (testing "Invalid document structure"
    (let [result (doc/scan-document-for-emoji [])]
      (is (= #{} result)))
    (let [result (doc/scan-document-for-emoji [:page {} "content"])]
      (is (= #{} result))))
  
  (testing "Complex nested document"
    (let [document [:document {:title "Complex Test"}
                    [:page {:width 612 :height 792}
                     [:g {:transforms [[:translate [50 50]]]}
                      [:text {:x 0 :y 0 :font "Arial" :size 12} "Task 💡"]
                      [:g {:transforms [[:rotate 45]]}
                       [:text {:x 0 :y 20 :font "Arial" :size 10} "Done ✅"]]]]
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 100 :font "Arial" :size 14} "Target: 🎯"]
                     [:text {:x 100 :y 120 :font "Arial" :size 14} "Alert: ⚠️"]]]
          result (doc/scan-document-for-emoji document)]
      (is (= #{"💡" "✅" "🎯" "⚠️"} result))))
  
  (testing "Duplicate emoji across pages"
    (let [document [:document {:title "Duplicate Test"}
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "First 💡"]]
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "Second 💡"]]]
          result (doc/scan-document-for-emoji document)]
      (is (= #{"💡"} result))))  ; Should be unique
  
  (testing "Empty pages"
    (let [document [:document {:title "Empty Test"}
                    [:page {:width 612 :height 792}]
                    [:page {:width 612 :height 792}
                     [:text {:x 100 :y 200 :font "Arial" :size 14} "Only one 💡"]]]
          result (doc/scan-document-for-emoji document)]
      (is (= #{"💡"} result)))))

(deftest test-integration-page-object-generation
  (testing "Page object with both fonts and images"
    (let [page-data {:width 612 :height 792 :margins [0 0 0 0]}
          font-refs {"Arial" 3 "Times" 4}
          image-refs {"💡" 10 "🎯" 11}
          xobject-names {"💡" "Em1" "🎯" "Em2"}
          result (doc/generate-page-object 5 page-data 2 6 font-refs image-refs xobject-names)]
      ;; Should contain page structure
      (is (clojure.string/includes? result "5 0 obj"))
      (is (clojure.string/includes? result "/Type /Page"))
      (is (clojure.string/includes? result "/Parent 2 0 R"))
      (is (clojure.string/includes? result "/Contents 6 0 R"))
      ;; Should contain both font and image resources
      (is (clojure.string/includes? result "/Resources <<"))
      (is (clojure.string/includes? result "/Font <<"))
      (is (clojure.string/includes? result "/XObject <<"))
      (is (clojure.string/includes? result "/Arial 3 0 R"))
      (is (clojure.string/includes? result "/Em1 10 0 R"))))
  
  (testing "Page object with only fonts (backward compatibility)"
    (let [page-data {:width 612 :height 792 :margins [0 0 0 0]}
          font-refs {"Arial" 3}
          result (doc/generate-page-object 5 page-data 2 6 font-refs)]
      ;; Should work with old signature
      (is (clojure.string/includes? result "5 0 obj"))
      (is (clojure.string/includes? result "/Font <<"))
      (is (not (clojure.string/includes? result "/XObject <<")))))
  
  (testing "Page object with only images"
    (let [page-data {:width 612 :height 792 :margins [0 0 0 0]}
          font-refs {}
          image-refs {"💡" 10}
          xobject-names {"💡" "Em1"}
          result (doc/generate-page-object 5 page-data 2 6 font-refs image-refs xobject-names)]
      (is (clojure.string/includes? result "/XObject <<"))
      (is (not (clojure.string/includes? result "/Font <<")))))
  
  (testing "Page object with no resources"
    (let [page-data {:width 612 :height 792 :margins [0 0 0 0]}
          font-refs {}
          result (doc/generate-page-object 5 page-data 2 6 font-refs)]
      (is (not (clojure.string/includes? result "/Resources <<")))
      (is (not (clojure.string/includes? result "/Font <<")))
      (is (not (clojure.string/includes? result "/XObject <<"))))))

(deftest test-resource-management-edge-cases
  (testing "Malformed page content"
    (let [page-content ["not-a-vector" 123 nil]
          result (doc/collect-page-images page-content)]
      (is (= #{} result))))
  
  (testing "Text elements with nil content"
    (let [page-content [[:text {:x 100 :y 200 :font "Arial" :size 14} nil]
                        [:text {:x 100 :y 220 :font "Arial" :size 14}]]  ; No content
          result (doc/collect-page-images page-content)]
      (is (= #{} result))))
  
  (testing "Very large emoji set"
    (let [cache (images/create-image-cache)
          ;; Create a large set of emoji (some real, some fake)
          unique-emoji (set (map #(str "🔤" %) (range 50)))  ; Fake emoji
          result (doc/embed-images-in-document unique-emoji cache 1000 {:fallback-strategy :skip})]
      (is (:success result))
      (is (= 50 (count (:xobject-names result))))))
  
  (testing "Image resource generation with empty maps"
    (let [result (doc/generate-image-resources nil nil)]
      (is (= "" result))))
  
  (testing "Resource merging with nil values"
    (let [result (doc/update-page-resources nil nil)]
      (is (= "" result)))))

(deftest test-performance-resource-management
  (testing "Performance with many pages"
    (let [;; Create document with 20 pages, each with emoji
          pages (vec (for [i (range 20)]
                       [:page {:width 612 :height 792}
                        [:text {:x 100 :y 200 :font "Arial" :size 14} (str "Page " i " 💡")]]))
          document (into [:document {:title "Performance Test"}] pages)
          result (doc/scan-document-for-emoji document)]
      (is (= #{"💡"} result))  ; Should find unique emoji across all pages
      (is (= 1 (count result)))))  ; Should be efficient
  
  (testing "Performance with deeply nested structures"
    (let [;; Create deeply nested groups
          nested-content (reduce (fn [content level]
                                   [[:g {:transforms [[:translate [level level]]]}
                                     content]])
                                 [:text {:x 0 :y 0 :font "Arial" :size 12} "Deep 💡"]
                                 (range 10))
          page-content [nested-content]
          result (doc/collect-page-images page-content)]
      (is (= #{"💡"} result))))
  
  (testing "Performance with many unique emoji"
    (let [cache (images/create-image-cache)
          ;; Use actual emoji characters that might have files
          unique-emoji #{"💡" "🎯" "✅" "⚠️" "🔥" "⭐" "🚀" "💯" "🎉" "🌟"}
          result (doc/embed-images-in-document unique-emoji cache 100 {:fallback-strategy :skip})]
      (is (:success result))
      (is (= 10 (count (:xobject-names result)))))))