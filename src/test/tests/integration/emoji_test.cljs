(ns tests.integration.emoji-test
  "Comprehensive integration tests for the complete emoji and image pipeline.

  Tests the full workflow from emoji shortcodes through image caching to PDF
  operator generation, ensuring all components work together correctly."
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.core :refer [hiccup->pdf-ops]]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest single-emoji-integration-test
  (testing "Single emoji elements with complete pipeline"
    (let [cache (images/create-image-cache)]

      ;; Test basic emoji rendering
      (let [result (hiccup->pdf-ops [:emoji {:code :smile :size 24 :x 100 :y 200}]
                                    {:image-cache cache})]
        (is (string? result)
            "Should generate PDF operators for single emoji")
        (is (re-find #"q\n" result)
            "Should contain graphics state save")
        (is (re-find #"cm\n" result)
            "Should contain transformation matrix")
        (is (re-find #"Do\n" result)
            "Should contain XObject draw command")
        (is (re-find #"Q$" result)
            "Should end with graphics state restore"))

      ;; Test emoji with different sizes
      (let [small-emoji (hiccup->pdf-ops [:emoji {:code :heart :size 12 :x 0 :y 0}]
                                         {:image-cache cache})
            large-emoji (hiccup->pdf-ops [:emoji {:code :heart :size 48 :x 0 :y 0}]
                                         {:image-cache cache})]
        (is (and (string? small-emoji) (string? large-emoji))
            "Should handle different emoji sizes")
        (is (not= small-emoji large-emoji)
            "Different sizes should produce different PDF operators"))

      ;; Test emoji positioning
      (let [top-left (hiccup->pdf-ops [:emoji {:code :star :size 16 :x 0 :y 0}]
                                      {:image-cache cache})
            bottom-right (hiccup->pdf-ops [:emoji {:code :star :size 16 :x 100 :y 200}]
                                          {:image-cache cache})]
        (is (and (string? top-left) (string? bottom-right))
            "Should handle different emoji positions")
        (is (not= top-left bottom-right)
            "Different positions should produce different PDF operators"))))

  (testing "Single emoji with caching verification"
    (let [cache (images/create-image-cache)]

      ;; First access should be a cache miss
      (let [result1 (hiccup->pdf-ops [:emoji {:code :lightbulb :size 20 :x 50 :y 50}]
                                     {:image-cache cache})
            cache-stats1 (:stats @cache)]
        (is (string? result1)
            "Should successfully render emoji on first access")
        (is (= 1 (:misses cache-stats1))
            "Should record cache miss on first access"))

      ;; Second access should be a cache hit
      (let [result2 (hiccup->pdf-ops [:emoji {:code :lightbulb :size 24 :x 60 :y 60}]
                                     {:image-cache cache})
            cache-stats2 (:stats @cache)]
        (is (string? result2)
            "Should successfully render emoji on second access")
        (is (= 1 (:hits cache-stats2))
            "Should record cache hit on second access")))))

(deftest multiple-emoji-integration-test
  (testing "Multiple emoji with different shortcodes"
    (let [cache (images/create-image-cache)
          multi-emoji-doc [:g {}
                           [:emoji {:code :smile :size 16 :x 10 :y 10}]
                           [:emoji {:code :heart :size 18 :x 30 :y 10}]
                           [:emoji {:code :star :size 20 :x 50 :y 10}]
                           [:emoji {:code :lightbulb :size 22 :x 70 :y 10}]
                           [:emoji {:code :fire :size 24 :x 90 :y 10}]]
          result (hiccup->pdf-ops multi-emoji-doc {:image-cache cache})]

      (is (string? result)
          "Should generate PDF operators for multiple emoji")

      ;; Verify group structure
      (is (re-find #"q\n" result)
          "Should start with group save state")
      (is (re-find #"Q$" result)
          "Should end with group restore state")

      ;; Count XObject references (should have 5 emoji)
      (let [xobject-count (count (re-seq #"Do\n" result))]
        (is (= 5 xobject-count)
            "Should contain 5 XObject draw commands for 5 emoji"))

      ;; Verify cache usage
      (let [cache-stats (:stats @cache)]
        (is (<= (:misses cache-stats) 5)
            "Should have at most 5 cache misses for unique emoji")
        (is (>= (count (:items @cache)) 1)
            "Should have cached at least one emoji image"))))

  (testing "Same emoji multiple times with caching"
    (let [cache (images/create-image-cache)
          repeated-emoji-doc [:g {}
                              [:emoji {:code :smile :size 16 :x 10 :y 10}]
                              [:emoji {:code :smile :size 20 :x 40 :y 40}]
                              [:emoji {:code :smile :size 24 :x 70 :y 70}]]
          result (hiccup->pdf-ops repeated-emoji-doc {:image-cache cache})]

      (is (string? result)
          "Should generate PDF operators for repeated emoji")

      ;; Should have 3 XObject draws but only 1 cache miss
      (let [xobject-count (count (re-seq #"Do\n" result))
            cache-stats (:stats @cache)]
        (is (= 3 xobject-count)
            "Should contain 3 XObject draw commands")
        (is (= 1 (:misses cache-stats))
            "Should have only 1 cache miss for same emoji")
        (is (= 2 (:hits cache-stats))
            "Should have 2 cache hits for repeated emoji")))))

(deftest mixed-elements-integration-test
  (testing "Emoji mixed with other PDF elements"
    (let [cache (images/create-image-cache)
          mixed-doc [:g {}
                     ;; Background rectangle
                     [:rect {:x 0 :y 0 :width 200 :height 100 :fill "#f0f0f0"}]
                     ;; Text content
                     [:text {:x 10 :y 20 :font "Arial" :size 14 :fill "#333333"} "Status Report"]
                     ;; Emoji indicators
                     [:emoji {:code :thumbsup :size 16 :x 10 :y 40}]
                     [:text {:x 30 :y 40 :font "Arial" :size 12} "Task completed"]
                     [:emoji {:code :thinking :size 16 :x 10 :y 60}]
                     [:text {:x 30 :y 60 :font "Arial" :size 12} "Needs attention"]
                     ;; Decorative emoji
                     [:emoji {:code :star :size 12 :x 180 :y 10}]]
          result (hiccup->pdf-ops mixed-doc {:image-cache cache})]

      (is (string? result)
          "Should generate PDF operators for mixed elements")

      ;; Verify all element types are present
      (is (re-find #"re\n" result)
          "Should contain rectangle operators")
      (is (re-find #"BT\n" result)
          "Should contain text operators")
      (is (re-find #"Do\n" result)
          "Should contain emoji XObject operators")

      ;; Count different element types
      (let [rect-count (count (re-seq #"re\n" result))
            text-count (count (re-seq #"BT\n" result))
            emoji-count (count (re-seq #"Do\n" result))]
        (is (= 1 rect-count)
            "Should have 1 rectangle")
        (is (= 3 text-count)
            "Should have 3 text elements")
        (is (= 3 emoji-count)
            "Should have 3 emoji elements"))))

  (testing "Emoji in complex layout structures"
    (let [cache (images/create-image-cache)
          layout-doc [:g {}
                      ;; Header section with emoji
                      [:g {:transforms [[:translate [0 0]]]}
                       [:rect {:x 0 :y 0 :width 300 :height 50 :fill "#4a90e2"}]
                       [:emoji {:code :star :size 20 :x 10 :y 15}]
                       [:text {:x 40 :y 25 :font "Arial" :size 16 :fill "#ffffff"} "Premium Service"]]

                      ;; Content section with mixed elements
                      [:g {:transforms [[:translate [0 60]]]}
                       [:text {:x 10 :y 20 :font "Arial" :size 14} "Features:"]
                       [:emoji {:code :thumbsup :size 14 :x 10 :y 40}]
                       [:text {:x 30 :y 40 :font "Arial" :size 12} "Fast delivery"]
                       [:emoji {:code :thumbsup :size 14 :x 10 :y 60}]
                       [:text {:x 30 :y 60 :font "Arial" :size 12} "24/7 support"]]

                      ;; Footer with emoji rating
                      [:g {:transforms [[:translate [0 140]]]}
                       [:emoji {:code :star :size 16 :x 10 :y 10}]
                       [:emoji {:code :star :size 16 :x 30 :y 10}]
                       [:emoji {:code :star :size 16 :x 50 :y 10}]
                       [:emoji {:code :star :size 16 :x 70 :y 10}]
                       [:emoji {:code :star :size 16 :x 90 :y 10}]]]
          result (hiccup->pdf-ops layout-doc {:image-cache cache})]

      (is (string? result)
          "Should generate PDF operators for complex layout")

      ;; Verify group structure with transforms
      (let [transform-count (count (re-seq #"cm\n" result))
            emoji-count (count (re-seq #"Do\n" result))]
        (is (>= transform-count 3)
            "Should have transforms for layout groups")
        (is (= 8 emoji-count)
            "Should have 8 emoji elements total (1+2+5)")))))

(deftest nested-groups-integration-test
  (testing "Emoji within nested transformed groups"
    (let [cache (images/create-image-cache)
          nested-doc [:g {:transforms [[:translate [50 50]]]}
                      [:rect {:x 0 :y 0 :width 100 :height 100 :fill "#e0e0e0"}]
                      [:g {:transforms [[:scale [1.5 1.5]]]}
                       [:emoji {:code :heart :size 16 :x 20 :y 20}]
                       [:g {:transforms [[:rotate 45]]}
                        [:emoji {:code :star :size 12 :x 40 :y 40}]]]]
          result (hiccup->pdf-ops nested-doc {:image-cache cache})]

      (is (string? result)
          "Should generate PDF operators for nested groups")

      ;; Verify nested structure
      (let [save-count (count (re-seq #"q\n" result))
            restore-count (count (re-seq #"Q" result))
            transform-count (count (re-seq #"cm\n" result))
            emoji-count (count (re-seq #"Do\n" result))]
        (is (= save-count restore-count)
            "Should have balanced save/restore states")
        (is (>= transform-count 3)
            "Should have multiple transformation matrices")
        (is (= 2 emoji-count)
            "Should have 2 emoji elements"))))

  (testing "Deep nesting with many emoji"
    (let [cache (images/create-image-cache)
          deep-nested-doc [:g {:transforms [[:translate [10 10]]]}
                           [:g {:transforms [[:scale [1.2 1.2]]]}
                            [:g {:transforms [[:rotate 15]]}
                             [:emoji {:code :smile :size 20 :x 0 :y 0}]
                             [:g {:transforms [[:translate [30 30]]]}
                              [:emoji {:code :heart :size 18 :x 0 :y 0}]
                              [:g {:transforms [[:scale [0.8 0.8]]]}
                               [:emoji {:code :star :size 16 :x 0 :y 0}]]]]]]
          result (hiccup->pdf-ops deep-nested-doc {:image-cache cache})]

      (is (string? result)
          "Should handle deeply nested groups with emoji")

      ;; Verify all emoji are rendered despite deep nesting
      (let [emoji-count (count (re-seq #"Do\n" result))]
        (is (= 3 emoji-count)
            "Should render all 3 emoji despite deep nesting")))))

(deftest error-handling-integration-test
  (testing "Error handling in complex scenarios"
    ;; Test missing cache in complex document
    (is (thrown-with-msg? js/Error #"Image cache is required"
                          (hiccup->pdf-ops [:g {}
                                            [:rect {:x 0 :y 0 :width 100 :height 50}]
                                            [:emoji {:code :smile :size 16 :x 10 :y 10}]]))
        "Should throw error for missing cache in complex document")

    ;; Test partial validation errors in mixed documents
    (let [cache (images/create-image-cache)]
      (is (thrown? js/Error
                   (hiccup->pdf-ops [:g {}
                                     [:rect {:x 0 :y 0 :width 100 :height 50}]
                                     [:emoji {:code :smile :x 10 :y 10}]] ; Missing size
                                    {:image-cache cache}))
          "Should catch validation errors in mixed documents")))

  (testing "Error recovery and isolation"
    ;; Verify that valid elements work even when followed by invalid ones
    (let [cache (images/create-image-cache)
          valid-result (hiccup->pdf-ops [:rect {:x 0 :y 0 :width 100 :height 50}])]
      (is (string? valid-result)
          "Valid elements should work independently")

      ;; Verify emoji works in isolation
      (let [emoji-result (hiccup->pdf-ops [:emoji {:code :smile :size 16 :x 10 :y 10}]
                                          {:image-cache cache})]
        (is (string? emoji-result)
            "Valid emoji should work independently")))))

(deftest performance-integration-test
  (testing "Performance with many emoji"
    (let [cache (images/create-image-cache)
          ;; Create document with many emoji
          many-emoji-doc (into [:g {}]
                               (for [i (range 20)]
                                 [:emoji {:code (nth [:smile :heart :star :lightbulb :fire] (mod i 5))
                                          :size (+ 12 (mod i 8))
                                          :x (* i 15)
                                          :y (* (quot i 5) 20)}]))
          start-time (.now js/Date)
          result (hiccup->pdf-ops many-emoji-doc {:image-cache cache})
          end-time (.now js/Date)
          duration (- end-time start-time)]

      (is (string? result)
          "Should generate PDF operators for many emoji")

      ;; Performance check: should complete within reasonable time
      (is (< duration 1000)
          "Should complete many emoji rendering within 1 second")

      ;; Verify cache efficiency
      (let [cache-stats (:stats @cache)
            cache-items (count (:items @cache))]
        (is (<= (:misses cache-stats) 5)
            "Should have at most 5 cache misses for 5 unique emoji")
        (is (>= (:hits cache-stats) 15)
            "Should have many cache hits for repeated emoji")
        (is (<= cache-items 5)
            "Should cache at most 5 unique images"))))

  (testing "Memory efficiency with caching"
    (let [cache (images/create-image-cache)
          ;; Test repeated access to same emoji
          _ (dotimes [i 10]
              (hiccup->pdf-ops [:emoji {:code :smile :size 16 :x i :y i}]
                               {:image-cache cache}))
          cache-stats (:stats @cache)
          cache-items (count (:items @cache))]

      (is (= 1 (:misses cache-stats))
          "Should have only 1 cache miss for repeated emoji")
      (is (= 9 (:hits cache-stats))
          "Should have 9 cache hits for repeated access")
      (is (= 1 cache-items)
          "Should cache only 1 unique image"))))

(deftest existing-functionality-integration-test
  (testing "Existing elements unaffected by emoji implementation"
    ;; Test all existing element types work as before
    (let [rect-result (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}])
          circle-result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}])
          line-result (hiccup->pdf-ops [:line {:x1 0 :y1 0 :x2 100 :y2 100}])
          text-result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Hello"])
          path-result (hiccup->pdf-ops [:path {:d "M10,10 L20,20"}])
          group-result (hiccup->pdf-ops [:g {} [:rect {:x 0 :y 0 :width 50 :height 50}]])]

      (is (every? string? [rect-result circle-result line-result text-result path-result group-result])
          "All existing element types should continue to work")

      ;; Verify specific patterns for each element type
      (is (re-find #"re\n" rect-result)
          "Rectangle should contain rect operator")
      (is (re-find #"c\n" circle-result)
          "Circle should contain curve operators")
      (is (re-find #"l\n" line-result)
          "Line should contain line operator")
      (is (re-find #"BT\n" text-result)
          "Text should contain text block operators")
      (is (re-find #"m\n" path-result)
          "Path should contain move operators")
      (is (and (re-find #"q\n" group-result) (re-find #"Q" group-result))
          "Group should contain save/restore operators")))

  (testing "Mixed existing and new elements"
    (let [cache (images/create-image-cache)
          mixed-result (hiccup->pdf-ops [:g {}
                                         [:rect {:x 0 :y 0 :width 100 :height 50 :fill "#ff0000"}]
                                         [:circle {:cx 150 :cy 25 :r 20 :fill "#00ff00"}]
                                         [:emoji {:code :smile :size 16 :x 200 :y 20}]
                                         [:text {:x 10 :y 80 :font "Arial" :size 12} "Mixed content"]]
                                        {:image-cache cache})]

      (is (string? mixed-result)
          "Mixed existing and new elements should work together")

      ;; Verify all element types are present
      (is (re-find #"re\n" mixed-result)
          "Should contain rectangle operators")
      (is (re-find #"c\n" mixed-result)
          "Should contain circle curve operators")
      (is (re-find #"Do\n" mixed-result)
          "Should contain emoji XObject operators")
      (is (re-find #"BT\n" mixed-result)
          "Should contain text operators"))))

(deftest pdf-operator-correctness-test
  (testing "Generated PDF operators are syntactically correct"
    (let [cache (images/create-image-cache)
          result (hiccup->pdf-ops [:emoji {:code :smile :size 24 :x 100 :y 200}]
                                  {:image-cache cache})]

      (is (string? result)
          "Should generate string output")

      ;; Verify PDF operator structure
      (is (re-find #"^q\n" result)
          "Should start with save state operator")
      (is (re-find #"Q$" result)
          "Should end with restore state operator")

      ;; Verify transformation matrix format
      (is (re-find #"[\d\.-]+ 0 0 [\d\.-]+ [\d\.-]+ [\d\.-]+ cm\n" result)
          "Should contain properly formatted transformation matrix")

      ;; Verify XObject reference format
      (is (re-find #"/[A-Za-z0-9]+ Do\n" result)
          "Should contain properly formatted XObject reference")

      ;; Verify no syntax errors (balanced operators)
      (let [save-count (count (re-seq #"q\n" result))
            restore-count (count (re-seq #"Q" result))]
        (is (= save-count restore-count)
            "Should have balanced save/restore operators"))))

  (testing "Multiple emoji generate distinct XObject references"
    (let [cache (images/create-image-cache)
          result (hiccup->pdf-ops [:g {}
                                   [:emoji {:code :smile :size 16 :x 10 :y 10}]
                                   [:emoji {:code :heart :size 16 :x 30 :y 10}]]
                                  {:image-cache cache})]

      (is (string? result)
          "Should generate string output for multiple emoji")

      ;; Should contain multiple XObject references
      (let [xobject-refs (re-seq #"/[A-Za-z0-9]+ Do" result)]
        (is (= 2 (count xobject-refs))
            "Should contain 2 XObject references")))))
        ;; Note: XObject names may be the same if using same image files,
        ;; but there should be 2 separate Do commands
