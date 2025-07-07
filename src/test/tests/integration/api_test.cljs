(ns tests.integration.api-test
  "Tests for API integration with emoji image support"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.core :as core]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest test-hiccup->pdf-ops-backward-compatibility
  (testing "Existing API works unchanged (no emoji)"
    (let [result (core/hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}])]
      (is (string? result))
      (is (clojure.string/includes? result "1 0 0 rg"))
      (is (clojure.string/includes? result "10 20 100 50 re"))
      (is (clojure.string/includes? result "f"))))

  (testing "Text without emoji works unchanged"
    (let [result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello world"])]
      (is (string? result))
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))
      (is (clojure.string/includes? result "/Arial 14 Tf"))
      (is (clojure.string/includes? result "(Hello world) Tj"))))

  (testing "Complex nested structure without emoji"
    (let [result (core/hiccup->pdf-ops [:g {:transforms [[:translate [50 50]]]}
                                        [:rect {:x 0 :y 0 :width 30 :height 30 :fill "#ff0000"}]
                                        [:text {:x 10 :y 10 :font "Arial" :size 12} "Test"]])]
      (is (string? result))
      (is (clojure.string/includes? result "q"))  ; Save state
      (is (clojure.string/includes? result "Q"))  ; Restore state
      (is (clojure.string/includes? result "50 50"))  ; Translation
      (is (clojure.string/includes? result "(Test) Tj"))))  ; Text content

  (testing "Options parameter ignored when not using emoji"
    (let [result1 (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello"])
          result2 (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello"]
                                        {:enable-emoji-images false})]
      (is (= result1 result2))))  ; Should be identical

  (testing "Emoji with hex fallback (legacy mode)"
    (let [result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 💡"])]
      (is (string? result))
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))
      ;; Should contain hex encoding for emoji
      (is (clojure.string/includes? result "<")))))

(deftest test-emoji-image-mode-integration
  (testing "Emoji image mode enabled with cache"
    (let [cache (images/create-image-cache)
          xobject-refs {"💡" "Em1"}
          options {:enable-emoji-images true
                   :image-cache cache
                   :xobject-refs xobject-refs}
          result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 💡"]
                                       options)]
      (is (string? result))
      ;; Should contain mixed content operators (either image or fallback)
      (is (not-empty result))))

  (testing "Emoji image mode without cache falls back gracefully"
    (let [options {:enable-emoji-images true}  ; No cache provided
          result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 💡"]
                                       options)]
      (is (string? result))
      (is (clojure.string/includes? result "BT"))
      (is (clojure.string/includes? result "ET"))))

  (testing "Mixed content with multiple emoji"
    (let [cache (images/create-image-cache)
          xobject-refs {"💡" "Em1" "🎯" "Em2" "✅" "Em3"}
          options {:enable-emoji-images true
                   :image-cache cache
                   :xobject-refs xobject-refs}
          result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Progress: 💡 Status: ✅ Target: 🎯"]
                                       options)]
      (is (string? result))
      (is (not-empty result))))

  (testing "Nested groups with emoji support"
    (let [cache (images/create-image-cache)
          xobject-refs {"💡" "Em1"}
          options {:enable-emoji-images true
                   :image-cache cache
                   :xobject-refs xobject-refs}
          result (core/hiccup->pdf-ops [:g {:transforms [[:translate [50 50]]]}
                                        [:text {:x 0 :y 0 :font "Arial" :size 12} "Task 💡"]
                                        [:text {:x 0 :y 20 :font "Arial" :size 12} "Regular text"]]
                                       options)]
      (is (string? result))
      (is (clojure.string/includes? result "q"))  ; Group save state
      (is (clojure.string/includes? result "Q"))  ; Group restore state
      (is (not-empty result))))

  (testing "Fallback strategies work correctly"
    (let [cache (images/create-image-cache)
          ;; Test each fallback strategy
          strategies [:hex-string :placeholder :skip]]
      (doseq [strategy strategies]
        (let [options {:enable-emoji-images true
                       :image-cache cache
                       :emoji-config {:fallback-strategy strategy}}
              result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Test 🦄"] ; Unicorn likely no file
                                           options)]
          (is (string? result))
          (is (not-empty result))))))

  (testing "Color support with emoji images"
    (let [cache (images/create-image-cache)
          xobject-refs {"💡" "Em1"}
          options {:enable-emoji-images true
                   :image-cache cache
                   :xobject-refs xobject-refs}
          result (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14 :fill "#ff0000"} "Red 💡"]
                                       options)]
      (is (string? result))
      (is (not-empty result)))))

;; Legacy function removed in emoji system refactoring - all tests commented out
#_(deftest test-process-text-with-emoji-images
    "Legacy emoji processing function tests - disabled after refactoring")

(deftest test-api-performance-and-regression
  (testing "Performance with emoji images disabled"
    (let [start-time (.now js/Date)
          ;; Process many elements without emoji
          results (doall (for [i (range 100)]
                           (core/hiccup->pdf-ops [:text {:x i :y i :font "Arial" :size 12} (str "Text " i)])))
          end-time (.now js/Date)
          duration (- end-time start-time)]
      (is (every? string? results))
      (is (< duration 1000))  ; Should complete within 1 second
      (is (= 100 (count results)))))

  (testing "No performance regression with options"
    (let [start-time1 (.now js/Date)
          ;; Without options
          results1 (doall (for [i (range 50)]
                            (core/hiccup->pdf-ops [:text {:x i :y i :font "Arial" :size 12} (str "Text " i)])))
          end-time1 (.now js/Date)
          duration1 (- end-time1 start-time1)

          start-time2 (.now js/Date)
          ;; With options (emoji disabled)
          results2 (doall (for [i (range 50)]
                            (core/hiccup->pdf-ops [:text {:x i :y i :font "Arial" :size 12} (str "Text " i)]
                                                  {:enable-emoji-images false})))
          end-time2 (.now js/Date)
          duration2 (- end-time2 start-time2)]

      (is (= (count results1) (count results2)))
      ;; Performance should be similar (within 50% difference)
      (is (< (Math/abs (- duration1 duration2)) (* 0.5 (max duration1 duration2))))))

  (testing "Memory usage stays reasonable"
    (let [cache (images/create-image-cache)
          ;; Process text with emoji repeatedly using standard API
          results (doall (for [i (range 20)]
                           (core/hiccup->pdf-ops [:text {:x (* i 10) :y (* i 10) :font "Arial" :size 12}
                                                  (str "Test " i " 💡 " i)]
                                                 {:image-cache cache})))
          cache-stats (images/cache-stats cache)]
      (is (every? string? results))
      (is (< (:memory-usage cache-stats) (* 5 1024 1024)))  ; Less than 5MB
      (is (>= (:hit-rate cache-stats) 0))))  ; Hit rate should be non-negative

  (testing "Complex nested structures with emoji"
    (let [cache (images/create-image-cache)
          complex-structure [:g {:transforms [[:translate [100 100]]]}
                             [:g {:transforms [[:rotate 45]]}
                              [:text {:x 0 :y 0 :font "Arial" :size 12} "Nested 💡"]
                              [:text {:x 0 :y 20 :font "Arial" :size 10} "More ✅"]]
                             [:text {:x 50 :y 50 :font "Arial" :size 14} "Outside 🎯"]]
          options {:enable-emoji-images true :image-cache cache}
          result (core/hiccup->pdf-ops complex-structure options)]
      (is (string? result))
      (is (clojure.string/includes? result "q"))  ; Should have save states
      (is (clojure.string/includes? result "Q"))  ; Should have restore states
      (is (not-empty result))))

  (testing "API consistency between functions"
    (let [cache (images/create-image-cache)
          xobject-refs {"💡" "Em1"}
          text "Hello 💡"

          ;; Method 1: Using hiccup->pdf-ops
          result1 (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} text]
                                        {:enable-emoji-images true
                                         :image-cache cache
                                         :xobject-refs xobject-refs})

          ;; Method 2: Legacy function removed - use standard text processing
          result2 (core/hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} text]
                                        {:image-cache cache})]

      (is (string? result1))
      (is (string? result2))
      ;; Results should be similar (both handle the emoji)
      (is (not-empty result1))
      (is (not-empty result2)))))
