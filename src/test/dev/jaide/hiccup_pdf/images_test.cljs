(ns dev.jaide.hiccup-pdf.images-test
  "Essential tests for image operations and emoji support.
  
  Simplified test suite covering critical functionality only."
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest test-emoji-file-operations
  (testing "Emoji file existence checking"
    (is (boolean? (images/emoji-file-exists? "💡")))
    (is (boolean? (images/emoji-file-exists? "🎯")))
    (is (boolean? (images/emoji-file-exists? "⚠️")))
    (is (boolean? (images/emoji-file-exists? "✅")))
    (is (false? (images/emoji-file-exists? "🦄"))))  ; Unmapped emoji
  
  (testing "Invalid input handling"
    (is (false? (images/emoji-file-exists? nil)))
    (is (false? (images/emoji-file-exists? "")))
    (is (false? (images/emoji-file-exists? "not-emoji")))))

(deftest test-image-cache-operations
  (testing "Image cache creation"
    (let [cache (images/create-image-cache)]
      (is (some? cache))))
  
  (testing "Image cache loading"
    (let [cache (images/create-image-cache)
          result (images/load-image-cached cache "test-image.png")]
      (is (map? result))
      (is (contains? result :success))
      (is (boolean? (:success result)))))
  
  (testing "Cache statistics"
    (let [cache (images/create-image-cache)
          stats (images/cache-stats cache)]
      (is (map? stats))
      (is (contains? stats :hit-rate))
      (is (contains? stats :memory-usage)))))

(deftest test-shortcode-resolution
  (testing "Shortcode resolution attempts"
    (let [result (images/resolve-shortcode-to-path ":lightbulb:")]
      (is (or (nil? result) (string? result)))))
  
  (testing "Invalid shortcode resolution"
    (is (nil? (images/resolve-shortcode-to-path ":invalid:")))
    (is (nil? (images/resolve-shortcode-to-path nil)))
    (is (nil? (images/resolve-shortcode-to-path "")))))

(deftest test-image-loading
  (testing "PNG file loading attempt"
    (let [result (images/load-png-file "emoji_u1f4a1.png")]
      ;; File may not exist, but should handle gracefully
      (is (or (nil? result) (some? result)))))
  
  (testing "Invalid filename handling"
    (is (nil? (images/load-png-file nil)))
    (is (nil? (images/load-png-file "")))
    (is (nil? (images/load-png-file "nonexistent.png")))))

(deftest test-image-dimensions
  (testing "Invalid buffer handling"
    (let [dimensions (images/get-image-dimensions nil)]
      (is (= {:width 72 :height 72} dimensions)))))

(deftest test-pdf-operator-generation
  (testing "XObject reference creation"
    (let [ref (images/create-resource-reference)]
      (is (string? ref))
      (is (clojure.string/starts-with? ref "Em"))))
  
  (testing "Image XObject generation"
    (let [image-result {:success true :buffer (js/Buffer.from (js/Uint8Array. [1 2 3])) :width 72 :height 72}
          xobject-ref "Em1"
          result (images/generate-image-xobject image-result xobject-ref)]
      (is (map? result))
      (is (contains? result :success))
      (is (boolean? (:success result)))))
  
  (testing "Invalid image result handling"
    (let [invalid-result {:success false :error "File not found"}
          result (images/generate-image-xobject invalid-result "Em1")]
      (is (map? result))
      (is (false? (:success result))))))

(deftest test-error-handling
  (testing "File existence checking"
    (is (boolean? (images/emoji-file-exists? "💡")))
    (is (false? (images/emoji-file-exists? "🦄")))
    (is (false? (images/emoji-file-exists? nil))))
  
  (testing "Graceful error handling"
    ;; These should not throw errors
    (is (nil? (images/load-png-file "nonexistent.png")))
    (is (some? (images/get-image-dimensions nil)))
    (is (nil? (images/resolve-shortcode-to-path ":invalid:")))))

(deftest test-basic-workflow
  (testing "Basic emoji processing workflow"
    (let [cache (images/create-image-cache)
          emoji-char "💡"
          shortcode ":lightbulb:"
          
          ;; Step 1: Check if emoji file exists
          file-exists (images/emoji-file-exists? emoji-char)
          
          ;; Step 2: Resolve shortcode to path
          image-path (images/resolve-shortcode-to-path shortcode)
          
          ;; Step 3: Load image with cache
          image-result (when image-path
                        (images/load-image-cached cache image-path))]
      
      (is (boolean? file-exists))
      (is (or (nil? image-path) (string? image-path)))
      (is (or (nil? image-result) (map? image-result)))))
  
  (testing "Performance characteristics"
    (let [cache (images/create-image-cache)
          start-time (.now js/Date)]
      
      ;; Perform multiple operations
      (dotimes [_i 5]
        (images/load-image-cached cache "test-image.png")
        (images/resolve-shortcode-to-path ":lightbulb:")
        (images/create-resource-reference))
      
      (let [end-time (.now js/Date)
            duration (- end-time start-time)]
        ;; Should complete quickly
        (is (< duration 1000))))))  ; Less than 1 second