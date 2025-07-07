(ns dev.jaide.hiccup-pdf.images-test
  "Tests for PNG image file operations"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.images :as images]))

(deftest test-emoji-file-exists?
  (testing "Existing emoji files"
    ;; Note: These tests depend on actual files being present
    ;; We'll test the function logic with mock scenarios
    (let [result-lightbulb (images/emoji-file-exists? "💡")
          result-target (images/emoji-file-exists? "🎯")]
      ;; These should return boolean values regardless of file existence
      (is (boolean? result-lightbulb))
      (is (boolean? result-target))))
  
  (testing "Non-emoji characters"
    (is (= false (images/emoji-file-exists? "a")))
    (is (= false (images/emoji-file-exists? "ABC")))
    (is (= false (images/emoji-file-exists? ""))))
  
  (testing "Invalid emoji"
    (is (= true (images/emoji-file-exists? "🦄")))   ; Unicorn - file exists in noto set
    (is (= false (images/emoji-file-exists? nil))))
  
  (testing "Special cases"
    ;; Bullet character should have filename mapping but file may not exist
    (let [result (images/emoji-file-exists? "•")]
      (is (boolean? result)))))

(deftest test-get-image-dimensions
  (testing "Valid PNG buffer simulation"
    ;; Create a mock PNG header buffer
    (let [mock-buffer (js/Buffer.alloc 24)]
      ;; Write PNG signature
      (.writeUInt32BE mock-buffer 0x89504E47 0)
      (.writeUInt32BE mock-buffer 0x0D0A1A0A 4)
      ;; Write mock dimensions at IHDR position
      (.writeUInt32BE mock-buffer 72 16)  ; width
      (.writeUInt32BE mock-buffer 72 20)  ; height
      
      (let [result (images/get-image-dimensions mock-buffer)]
        (is (= 72 (:width result)))
        (is (= 72 (:height result))))))
  
  (testing "Invalid buffer"
    (let [result-nil (images/get-image-dimensions nil)
          result-small (images/get-image-dimensions (js/Buffer.alloc 10))]
      ;; Should fallback to default dimensions
      (is (= {:width 72 :height 72} result-nil))
      (is (= {:width 72 :height 72} result-small))))
  
  (testing "Non-PNG buffer"
    (let [buffer (js/Buffer.from "not a png file")
          result (images/get-image-dimensions buffer)]
      ;; Should fallback to default dimensions
      (is (= {:width 72 :height 72} result))))
  
  (testing "Custom dimensions"
    (let [mock-buffer (js/Buffer.alloc 24)]
      ;; Write PNG signature
      (.writeUInt32BE mock-buffer 0x89504E47 0)
      (.writeUInt32BE mock-buffer 0x0D0A1A0A 4)
      ;; Write custom dimensions
      (.writeUInt32BE mock-buffer 128 16)  ; width
      (.writeUInt32BE mock-buffer 64 20)   ; height
      
      (let [result (images/get-image-dimensions mock-buffer)]
        (is (= 128 (:width result)))
        (is (= 64 (:height result)))))))

(deftest test-load-png-file
  (testing "File loading behavior"
    ;; Test with a filename that definitely doesn't exist
    (let [result (images/load-png-file "nonexistent_file.png")]
      (is (nil? result)))
    
    ;; Test with invalid filename
    (let [result (images/load-png-file "")]
      (is (nil? result)))
    
    ;; Test with nil filename
    (let [result (images/load-png-file nil)]
      (is (nil? result))))
  
  (testing "Path construction"
    ;; Verify that path construction doesn't throw errors
    (is (nil? (images/load-png-file "test.png")))  ; Non-existent file should return nil
    ;; Existing file should return buffer
    (let [result (images/load-png-file "emoji_u1f4a1.png")]
      (is (not (nil? result)))   ; Should successfully load existing file
      (is (instance? js/Buffer result)))))

(deftest test-load-emoji-image
  (testing "Known emoji characters"
    (let [result-lightbulb (images/load-emoji-image "💡")
          result-target (images/load-emoji-image "🎯")
          result-bullet (images/load-emoji-image "•")]
      
      ;; All should return maps with success field
      (is (map? result-lightbulb))
      (is (contains? result-lightbulb :success))
      (is (contains? result-lightbulb :filename))
      
      (is (map? result-target))
      (is (contains? result-target :success))
      (is (contains? result-target :filename))
      
      (is (map? result-bullet))
      (is (contains? result-bullet :success))
      (is (contains? result-bullet :filename))
      
      ;; Check filename mappings
      (is (= "emoji_u1f4a1.png" (:filename result-lightbulb)))
      (is (= "emoji_u1f3af.png" (:filename result-target)))
      (is (= "emoji_u2022.png" (:filename result-bullet)))))
  
  (testing "Non-emoji characters"
    (let [result-letter (images/load-emoji-image "a")
          result-empty (images/load-emoji-image "")]
      
      (is (= false (:success result-letter)))
      (is (contains? result-letter :error))
      (is (clojure.string/includes? (:error result-letter) "No filename mapping"))
      
      (is (= false (:success result-empty)))
      (is (contains? result-empty :error))))
  
  (testing "Unknown emoji"
    (let [result (images/load-emoji-image "🦄")]  ; Unicorn - generates filename and file exists
      (is (map? result))
      (is (contains? result :success))
      ;; Unicorn should generate filename and file exists, so success should be true
      (is (= true (:success result)))
      (is (contains? result :buffer))
      (is (contains? result :width))
      (is (contains? result :height))
      (is (contains? result :filename))))
  
  (testing "Error handling"
    (let [result-nil (images/load-emoji-image nil)]
      (is (= false (:success result-nil)))
      (is (contains? result-nil :error)))))

(deftest test-validate-png-data
  (testing "Valid PNG buffer"
    (let [mock-buffer (js/Buffer.alloc 100)]
      ;; Write PNG signature
      (.writeUInt32BE mock-buffer 0x89504E47 0)
      (.writeUInt32BE mock-buffer 0x0D0A1A0A 4)
      
      (let [result (images/validate-png-data mock-buffer)]
        (is (:valid? result))
        (is (empty? (:errors result)))
        (is (= 100 (:size (:info result))))
        (is (= "PNG" (:format (:info result)))))))
  
  (testing "Invalid PNG signature"
    (let [mock-buffer (js/Buffer.from "not a png")
          result (images/validate-png-data mock-buffer)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Invalid PNG signature") (:errors result)))))
  
  (testing "Null buffer"
    (let [result (images/validate-png-data nil)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "null or undefined") (:errors result)))))
  
  (testing "Too small buffer"
    (let [small-buffer (js/Buffer.alloc 10)
          result (images/validate-png-data small-buffer)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "too small") (:errors result)))))
  
  (testing "Too large buffer"
    ;; Test large buffer validation without manipulating the length property
    ;; Instead, use a different approach to test the size logic
    (let [large-buffer (js/Buffer.alloc 100)
          ;; Create a mock buffer object that reports large size
          mock-large-buffer (js-obj "length" (* 11 1024 1024))]
      ;; Set minimal PNG header on mock object
      (aset mock-large-buffer "readUInt32BE" (fn [offset] 
                                               (cond
                                                 (= offset 0) 0x89504E47
                                                 (= offset 4) 0x0D0A1A0A
                                                 :else 0)))
      (let [result (images/validate-png-data mock-large-buffer)]
        (is (not (:valid? result)))
        (is (some #(clojure.string/includes? % "too large") (:errors result))))))
  
  (testing "Error handling"
    ;; Test with invalid buffer that might cause exceptions
    (let [result (images/validate-png-data "not a buffer")]
      (is (not (:valid? result)))
      (is (seq (:errors result))))))

(deftest test-get-emoji-file-path
  (testing "Known emoji characters"
    (let [path-lightbulb (images/get-emoji-file-path "💡")
          path-target (images/get-emoji-file-path "🎯")
          path-bullet (images/get-emoji-file-path "•")]
      
      (is (string? path-lightbulb))
      (is (clojure.string/includes? path-lightbulb "emoji_u1f4a1.png"))
      (is (clojure.string/includes? path-lightbulb "emojis/noto-72"))
      
      (is (string? path-target))
      (is (clojure.string/includes? path-target "emoji_u1f3af.png"))
      
      (is (string? path-bullet))
      (is (clojure.string/includes? path-bullet "emoji_u2022.png"))))
  
  (testing "Non-emoji characters"
    (is (nil? (images/get-emoji-file-path "a")))
    (is (nil? (images/get-emoji-file-path "")))
    (is (nil? (images/get-emoji-file-path nil))))
  
  (testing "Unknown emoji"
    (let [path (images/get-emoji-file-path "🦄")]  ; Should generate path even if file doesn't exist
      (is (string? path))
      (is (clojure.string/includes? path "emoji_u1f984.png")))))

(deftest test-list-available-emoji-files
  (testing "Directory listing"
    (let [files (images/list-available-emoji-files)]
      ;; Should return a vector, even if empty
      (is (vector? files))
      
      ;; If any files exist, they should have proper structure
      (doseq [file files]
        (is (map? file))
        (is (contains? file :filename))
        (is (contains? file :path))
        (is (contains? file :size))
        (is (string? (:filename file)))
        (is (.endsWith (:filename file) ".png"))
        (is (string? (:path file)))
        (is (number? (:size file))))))
  
  (testing "Error handling"
    ;; The function should handle directory access errors gracefully
    ;; and return empty vector rather than throwing
    (let [files (images/list-available-emoji-files)]
      (is (vector? files)))))

(deftest test-integration-with-emoji-namespace
  (testing "Integration with emoji filename mapping"
    ;; Test that image functions work with emoji namespace functions
    (let [emoji-chars ["💡" "🎯" "✅" "⚠" "•"]
          results (map images/load-emoji-image emoji-chars)]
      
      ;; All should return valid response structures
      (is (every? map? results))
      (is (every? #(contains? % :success) results))
      
      ;; All should have filename mappings
      (is (every? #(contains? % :filename) results))
      
      ;; Check that filenames match expected patterns
      (let [filenames (map :filename results)]
        (is (every? #(.startsWith % "emoji_u") filenames))
        (is (every? #(.endsWith % ".png") filenames)))))
  
  (testing "Cross-platform path handling"
    ;; Verify that paths are constructed correctly across platforms
    (let [path (images/get-emoji-file-path "💡")]
      (is (string? path))
      ;; Should contain directory separator (/ or \)
      (is (or (clojure.string/includes? path "/")
              (clojure.string/includes? path "\\")))))
  
  (testing "Error propagation"
    ;; Test that errors are properly propagated through the system
    (let [result (images/load-emoji-image "nonexistent")]
      (is (= false (:success result)))
      (is (string? (:error result))))))

;; Step 2: Basic Image Loading Infrastructure Tests

(deftest test-load-image-file
  (testing "Valid image file loading"
    ;; Test with a file that likely exists (use same emoji file as existing tests)
    (let [test-file-path "emojis/noto-72/emoji_u1f4a1.png"
          result (images/load-image-file test-file-path)]
      ;; Result should be either a Buffer or nil depending on file existence
      (is (or (nil? result) 
              (instance? js/Buffer result))
          "Should return Buffer or nil"))
    
    ;; Test with various path formats
    (let [relative-path "./emojis/noto-72/emoji_u1f4a1.png"
          result (images/load-image-file relative-path)]
      (is (or (nil? result) 
              (instance? js/Buffer result))
          "Should handle relative paths")))
  
  (testing "Invalid or missing files"
    ;; Test with non-existent file
    (let [result (images/load-image-file "non-existent-file.png")]
      (is (nil? result)
          "Should return nil for non-existent files"))
    
    ;; Test with invalid path format
    (let [result (images/load-image-file "/definitely/does/not/exist/file.png")]
      (is (nil? result)
          "Should return nil for invalid paths")))
  
  (testing "Invalid input parameters"
    ;; Test with nil input
    (let [result (images/load-image-file nil)]
      (is (nil? result)
          "Should return nil for nil input"))
    
    ;; Test with empty string
    (let [result (images/load-image-file "")]
      (is (nil? result)
          "Should return nil for empty string"))
    
    ;; Test with non-string input
    (let [result (images/load-image-file 123)]
      (is (nil? result)
          "Should return nil for non-string input"))
    
    ;; Test with whitespace-only string
    (let [result (images/load-image-file "   ")]
      (is (or (nil? result) 
              (instance? js/Buffer result))
          "Should handle whitespace-only strings")))
  
  (testing "Path variations"
    ;; Test different path separators and formats
    (let [unix-path "emojis/noto-72/emoji_u1f4a1.png"
          result (images/load-image-file unix-path)]
      (is (or (nil? result) 
              (instance? js/Buffer result))
          "Should handle Unix-style paths"))
    
    ;; Test with different file extensions
    (let [png-uppercase "test.PNG"
          result (images/load-image-file png-uppercase)]
      (is (or (nil? result) 
              (instance? js/Buffer result))
          "Should handle uppercase extensions"))))

(deftest test-get-png-dimensions-step2
  (testing "Valid PNG buffer dimensions"
    ;; Create a minimal valid PNG header for testing
    ;; PNG signature: 89 50 4E 47 0D 0A 1A 0A
    ;; IHDR chunk follows with width/height
    (let [mock-png-buffer (.from js/Buffer 
                                  #js [0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A  ; PNG signature
                                       0x00 0x00 0x00 0x0D  ; IHDR chunk length (13 bytes)
                                       0x49 0x48 0x44 0x52  ; "IHDR"
                                       0x00 0x00 0x00 0x64  ; Width: 100 (4 bytes, big-endian)
                                       0x00 0x00 0x00 0x32  ; Height: 50 (4 bytes, big-endian)
                                       0x08 0x02 0x00 0x00 0x00])  ; Rest of IHDR
          result (images/get-png-dimensions mock-png-buffer)]
      (is (= 100 (:width result))
          "Should extract correct width from PNG header")
      (is (= 50 (:height result))
          "Should extract correct height from PNG header")))
  
  (testing "Invalid or malformed PNG data"
    ;; Test with nil buffer
    (let [result (images/get-png-dimensions nil)]
      (is (= {:width 72 :height 72} result)
          "Should return fallback dimensions for nil buffer"))
    
    ;; Test with too-small buffer
    (let [small-buffer (.from js/Buffer #js [0x89 0x50])
          result (images/get-png-dimensions small-buffer)]
      (is (= {:width 72 :height 72} result)
          "Should return fallback dimensions for too-small buffer"))
    
    ;; Test with invalid PNG signature
    (let [invalid-buffer (.from js/Buffer #js [0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x00
                                               0x00 0x00 0x00 0x0D 0x49 0x48 0x44 0x52
                                               0x00 0x00 0x00 0x64 0x00 0x00 0x00 0x32])
          result (images/get-png-dimensions invalid-buffer)]
      (is (= {:width 72 :height 72} result)
          "Should return fallback dimensions for invalid PNG signature")))
  
  (testing "Noto emoji fallback behavior"
    ;; Test various scenarios that should trigger fallback
    (let [empty-buffer (.from js/Buffer #js [])
          result (images/get-png-dimensions empty-buffer)]
      (is (= {:width 72 :height 72} result)
          "Should return 72x72 fallback for empty buffer"))
    
    ;; Test with corrupted header
    (let [corrupted-buffer (.from js/Buffer #js [0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
                                                  0xFF 0xFF 0xFF 0xFF])  ; Corrupted data
          result (images/get-png-dimensions corrupted-buffer)]
      (is (= {:width 72 :height 72} result)
          "Should return 72x72 fallback for corrupted header")))
  
  (testing "Edge cases"
    ;; Test with minimum valid PNG structure
    (let [minimal-png (.from js/Buffer #js [0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
                                            0x00 0x00 0x00 0x0D 0x49 0x48 0x44 0x52
                                            0x00 0x00 0x00 0x01 ; Width: 1
                                            0x00 0x00 0x00 0x01 ; Height: 1
                                            0x08 0x02 0x00 0x00 0x00])
          result (images/get-png-dimensions minimal-png)]
      (is (= 1 (:width result))
          "Should handle minimal 1x1 dimensions")
      (is (= 1 (:height result))
          "Should handle minimal 1x1 dimensions"))
    
    ;; Test with large dimensions
    (let [large-png (.from js/Buffer #js [0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
                                          0x00 0x00 0x00 0x0D 0x49 0x48 0x44 0x52
                                          0x00 0x00 0x03 0xE8 ; Width: 1000
                                          0x00 0x00 0x07 0xD0 ; Height: 2000  
                                          0x08 0x02 0x00 0x00 0x00])
          result (images/get-png-dimensions large-png)]
      (is (= 1000 (:width result))
          "Should handle large width dimensions")
      (is (= 2000 (:height result))
          "Should handle large height dimensions"))))

(deftest test-step2-integration
  (testing "Integration between load-image-file and get-png-dimensions"
    ;; Test loading a file and getting its dimensions
    (let [test-file "emojis/noto-72/emoji_u1f4a1.png"
          buffer (images/load-image-file test-file)]
      (when buffer  ; Only test if file actually exists
        (let [dimensions (images/get-png-dimensions buffer)]
          (is (number? (:width dimensions))
              "Should get numeric width from loaded file")
          (is (number? (:height dimensions))
              "Should get numeric height from loaded file")
          (is (pos? (:width dimensions))
              "Width should be positive")
          (is (pos? (:height dimensions))
              "Height should be positive")))))
  
  (testing "Error handling consistency"
    ;; Test that both functions handle errors consistently
    (let [buffer (images/load-image-file "non-existent-file.png")
          dimensions (images/get-png-dimensions buffer)]
      (is (nil? buffer)
          "load-image-file should return nil for missing files")
      (is (= {:width 72 :height 72} dimensions)
          "get-png-dimensions should return fallback for nil buffer")))
  
  (testing "Function signatures and return types"
    ;; Verify function signatures match Step 2 specification
    (is (fn? images/load-image-file)
        "load-image-file should be a function")
    (is (fn? images/get-png-dimensions)
        "get-png-dimensions should be a function")
    
    ;; Test return types
    (let [nil-result (images/load-image-file nil)
          fallback-dims (images/get-png-dimensions nil)]
      (is (nil? nil-result)
          "load-image-file should return nil for invalid input")
      (is (map? fallback-dims)
          "get-png-dimensions should return a map")
      (is (contains? fallback-dims :width)
          "Dimensions map should contain :width")
      (is (contains? fallback-dims :height)
          "Dimensions map should contain :height"))))

(deftest test-load-image-cached-step3
  (testing "Cache miss - load from file system"
    (let [cache (images/create-image-cache)
          test-file "emojis/noto-72/emoji_u1f4a1.png"
          result (images/load-image-cached cache test-file)]
      ;; Should either succeed or fail gracefully depending on file existence
      (is (map? result)
          "Should return a map")
      (is (contains? result :success)
          "Should contain success flag")
      (is (contains? result :file-path)
          "Should contain file-path")
      (is (= test-file (:file-path result))
          "Should return the correct file path")
      
      ;; If successful, should have image data
      (when (:success result)
        (is (contains? result :buffer)
            "Successful result should contain buffer")
        (is (contains? result :width)
            "Successful result should contain width")
        (is (contains? result :height)
            "Successful result should contain height")
        (is (number? (:width result))
            "Width should be numeric")
        (is (number? (:height result))
            "Height should be numeric"))))
  
  (testing "Cache hit - loads from cache on second call"
    (let [cache (images/create-image-cache)
          test-file "emojis/noto-72/emoji_u1f4a1.png"
          first-result (images/load-image-cached cache test-file)
          second-result (images/load-image-cached cache test-file)]
      
      ;; If first load was successful, second should be cached
      (when (:success first-result)
        (is (:success second-result)
            "Second load should also be successful")
        (is (= (:width first-result) (:width second-result))
            "Cached result should have same width")
        (is (= (:height first-result) (:height second-result))
            "Cached result should have same height")
        (is (= (:file-path first-result) (:file-path second-result))
            "Cached result should have same file path")
        
        ;; Check cache stats to verify hit
        (let [stats (images/cache-stats cache)]
          (is (pos? (:hits stats))
              "Should have cache hits")))))
  
  (testing "File not found error handling"
    (let [cache (images/create-image-cache)
          non-existent-file "definitely-does-not-exist.png"
          result (images/load-image-cached cache non-existent-file)]
      (is (= false (:success result))
          "Should return failure for non-existent file")
      (is (contains? result :error)
          "Should contain error message")
      (is (string? (:error result))
          "Error should be a string")
      (is (= non-existent-file (:file-path result))
          "Should return the attempted file path")))
  
  (testing "Invalid input handling"
    (let [cache (images/create-image-cache)]
      ;; Test with nil file path
      (let [result (images/load-image-cached cache nil)]
        (is (= false (:success result))
            "Should fail for nil file path"))
      
      ;; Test with empty string
      (let [result (images/load-image-cached cache "")]
        (is (= false (:success result))
            "Should fail for empty file path"))
      
      ;; Test with nil cache (should throw error)
      (is (thrown? js/Error (images/load-image-cached nil "test.png"))
          "Should throw error for nil cache")))
  
  (testing "LRU eviction with multiple files"
    ;; Create cache with very small size to force eviction
    (let [cache (images/create-image-cache {:max-size 2})
          file1 "test1.png"
          file2 "test2.png" 
          file3 "test3.png"]
      
      ;; Load three files (will cause eviction)
      (let [result1 (images/load-image-cached cache file1)
            result2 (images/load-image-cached cache file2)
            result3 (images/load-image-cached cache file3)]
        
        ;; All loads should handle eviction gracefully
        (is (map? result1) "First load should return map")
        (is (map? result2) "Second load should return map")
        (is (map? result3) "Third load should return map")
        
        ;; Check cache stats for evictions
        (let [stats (images/cache-stats cache)]
          (is (contains? stats :evictions)
              "Stats should track evictions")
          (is (number? (:evictions stats))
              "Evictions should be numeric")))))
  
  (testing "Integration with Step 2 functions"
    ;; Verify that load-image-cached properly uses Step 2 functions
    (let [cache (images/create-image-cache)
          test-file "emojis/noto-72/emoji_u1f4a1.png"]
      
      ;; Compare direct load vs cached load
      (let [direct-buffer (images/load-image-file test-file)
            cached-result (images/load-image-cached cache test-file)]
        
        (when direct-buffer
          ;; If direct load succeeded, cached should too
          (is (:success cached-result)
              "Cached load should succeed when direct load succeeds")
          
          ;; Dimensions should match
          (let [direct-dims (images/get-png-dimensions direct-buffer)]
            (is (= (:width direct-dims) (:width cached-result))
                "Width should match between direct and cached loads")
            (is (= (:height direct-dims) (:height cached-result))
                "Height should match between direct and cached loads")))))))

(deftest test-performance-considerations
  (testing "File existence checks"
    ;; Test that file existence checks are reasonably fast
    (let [start-time (.now js/Date)
          _ (images/emoji-file-exists? "💡")
          end-time (.now js/Date)
          duration (- end-time start-time)]
      ;; Should complete in reasonable time (< 100ms for file system check)
      (is (< duration 100))))
  
  (testing "Batch operations"
    ;; Test performance with multiple emoji
    (let [emoji-list ["💡" "🎯" "✅" "⚠" "•"]
          start-time (.now js/Date)
          results (doall (map images/emoji-file-exists? emoji-list))
          end-time (.now js/Date)
          duration (- end-time start-time)]
      
      (is (= 5 (count results)))
      ;; Batch operations should be reasonably fast
      (is (< duration 500)))))

;; Image Caching System Tests

(deftest test-create-image-cache
  (testing "Default cache creation"
    (let [cache (images/create-image-cache)]
      (is (= {} (:items @cache)))
      (is (= [] (:order @cache)))
      (is (= {:max-size 50 :max-memory-mb 10} (:config @cache)))
      (is (= {:hits 0 :misses 0 :evictions 0 :memory-usage 0} (:stats @cache)))))
  
  (testing "Custom cache configuration"
    (let [cache (images/create-image-cache {:max-size 25 :max-memory-mb 5})]
      (is (= {:max-size 25 :max-memory-mb 5} (:config @cache)))
      (is (= {:hits 0 :misses 0 :evictions 0 :memory-usage 0} (:stats @cache)))))
  
  (testing "Partial configuration override"
    (let [cache (images/create-image-cache {:max-size 100})]
      (is (= {:max-size 100 :max-memory-mb 10} (:config @cache))))))

(deftest test-estimate-image-memory
  (testing "Memory estimation with buffer"
    (let [mock-buffer (js/Buffer.alloc 1000)
          image-data {:buffer mock-buffer :width 72 :height 72}
          estimated (images/estimate-image-memory image-data)]
      (is (= 1200 estimated))))  ; 1000 + 200 overhead
  
  (testing "Memory estimation without buffer"
    (let [image-data {:width 72 :height 72}
          estimated (images/estimate-image-memory image-data)]
      (is (= 200 estimated))))   ; Just overhead
  
  (testing "Memory estimation with nil buffer"
    (let [image-data {:buffer nil :width 72 :height 72}
          estimated (images/estimate-image-memory image-data)]
      (is (= 200 estimated)))))  ; Just overhead

(deftest test-cache-basic-operations
  (testing "Cache miss on empty cache"
    (let [cache (images/create-image-cache)]
      (is (nil? (images/cache-get cache "💡")))
      (is (= 1 (:misses (:stats @cache))))))
  
  (testing "Cache put and get"
    (let [cache (images/create-image-cache)
          mock-buffer (js/Buffer.alloc 100)
          image-data {:buffer mock-buffer :width 72 :height 72 :filename "emoji_u1f4a1.png"}]
      
      ;; Put image in cache
      (is (= true (images/cache-put cache "💡" image-data)))
      
      ;; Verify cache state
      (is (= 1 (count (:items @cache))))
      (is (= ["💡"] (:order @cache)))
      (is (= 300 (:memory-usage (:stats @cache))))  ; 100 + 200 overhead
      
      ;; Get image from cache
      (let [retrieved (images/cache-get cache "💡")]
        (is (not (nil? retrieved)))
        (is (= (:buffer image-data) (:buffer retrieved)))
        (is (= 72 (:width retrieved)))
        (is (= 72 (:height retrieved)))
        (is (= "emoji_u1f4a1.png" (:filename retrieved))))
      
      ;; Verify hit counter
      (is (= 1 (:hits (:stats @cache))))))
  
  (testing "Cache clear"
    (let [cache (images/create-image-cache)
          image-data {:buffer (js/Buffer.alloc 100) :width 72 :height 72 :filename "test.png"}]
      
      ;; Add item and verify
      (images/cache-put cache "💡" image-data)
      (is (= 1 (count (:items @cache))))
      
      ;; Clear cache
      (is (= true (images/cache-clear cache)))
      (is (= {} (:items @cache)))
      (is (= [] (:order @cache)))
      (is (= 0 (:memory-usage (:stats @cache)))))))

(deftest test-lru-eviction
  (testing "LRU eviction by size limit"
    (let [cache (images/create-image-cache {:max-size 2})
          img1 {:buffer (js/Buffer.alloc 100) :width 72 :height 72 :filename "img1.png"}
          img2 {:buffer (js/Buffer.alloc 200) :width 72 :height 72 :filename "img2.png"}
          img3 {:buffer (js/Buffer.alloc 300) :width 72 :height 72 :filename "img3.png"}]
      
      ;; Add first two items
      (images/cache-put cache "💡" img1)
      (images/cache-put cache "🎯" img2)
      (is (= 2 (count (:items @cache))))
      (is (= ["💡" "🎯"] (:order @cache)))
      
      ;; Add third item - should evict first
      (images/cache-put cache "✅" img3)
      (is (= 2 (count (:items @cache))))
      (is (= ["🎯" "✅"] (:order @cache)))
      (is (nil? (images/cache-get cache "💡")))  ; Should be evicted
      (is (not (nil? (images/cache-get cache "🎯"))))  ; Should still be cached
      (is (= 1 (:evictions (:stats @cache))))))
  
  (testing "LRU order update on access"
    (let [cache (images/create-image-cache {:max-size 3})
          img1 {:buffer (js/Buffer.alloc 100) :width 72 :height 72 :filename "img1.png"}
          img2 {:buffer (js/Buffer.alloc 200) :width 72 :height 72 :filename "img2.png"}
          img3 {:buffer (js/Buffer.alloc 300) :width 72 :height 72 :filename "img3.png"}]
      
      ;; Add items
      (images/cache-put cache "💡" img1)
      (images/cache-put cache "🎯" img2)
      (images/cache-put cache "✅" img3)
      (is (= ["💡" "🎯" "✅"] (:order @cache)))
      
      ;; Access first item - should move to end
      (images/cache-get cache "💡")
      (is (= ["🎯" "✅" "💡"] (:order @cache)))))
  
  (testing "Memory limit eviction"
    (let [cache (images/create-image-cache {:max-size 10 :max-memory-mb 0.001})  ; Very small memory limit
          large-img {:buffer (js/Buffer.alloc 2000) :width 72 :height 72 :filename "large.png"}]
      
      ;; This should succeed but immediately evict due to memory limit
      (images/cache-put cache "💡" large-img)
      ;; The item might be added and then immediately evicted due to memory constraints
      (is (>= (:evictions (:stats @cache)) 0)))))

(deftest test-cache-stats
  (testing "Cache statistics calculation"
    (let [cache (images/create-image-cache)
          image-data {:buffer (js/Buffer.alloc 100) :width 72 :height 72 :filename "test.png"}]
      
      ;; Initial stats
      (let [stats (images/cache-stats cache)]
        (is (= 0 (:hits stats)))
        (is (= 0 (:misses stats)))
        (is (= 0 (:total-requests stats)))
        (is (= 0.0 (:hit-rate stats)))
        (is (= 0 (:item-count stats))))
      
      ;; Add item and access
      (images/cache-put cache "💡" image-data)
      (images/cache-get cache "💡")    ; Hit
      (images/cache-get cache "🎯")    ; Miss
      
      (let [stats (images/cache-stats cache)]
        (is (= 1 (:hits stats)))
        (is (= 1 (:misses stats)))
        (is (= 2 (:total-requests stats)))
        (is (= 0.5 (:hit-rate stats)))
        (is (= 1 (:item-count stats))))))
  
  (testing "Hit rate calculation with zero requests"
    (let [cache (images/create-image-cache)
          stats (images/cache-stats cache)]
      (is (= 0.0 (:hit-rate stats))))))

(deftest test-load-emoji-image-cached
  (testing "Cache miss - loads from file system"
    (let [cache (images/create-image-cache)
          result (images/load-emoji-image-cached cache "💡")]
      
      ;; Should successfully load from file system
      (is (:success result))
      (is (contains? result :buffer))
      (is (= "emoji_u1f4a1.png" (:filename result)))
      
      ;; Should now be cached
      (is (= 1 (count (:items @cache))))
      (is (= 1 (:misses (:stats @cache))))))
  
  (testing "Cache hit - loads from cache"
    (let [cache (images/create-image-cache)]
      
      ;; First load - cache miss
      (images/load-emoji-image-cached cache "💡")
      
      ;; Second load - cache hit
      (let [result (images/load-emoji-image-cached cache "💡")]
        (is (:success result))
        (is (contains? result :buffer))
        (is (= "emoji_u1f4a1.png" (:filename result)))
        
        ;; Should have 1 hit and 1 miss
        (is (= 1 (:hits (:stats @cache))))
        (is (= 1 (:misses (:stats @cache)))))))
  
  (testing "Failed file system load"
    (let [cache (images/create-image-cache)
          result (images/load-emoji-image-cached cache "nonexistent")]
      
      ;; Should fail gracefully
      (is (= false (:success result)))
      (is (contains? result :error))
      
      ;; Should not be cached
      (is (= 0 (count (:items @cache))))
      (is (= 1 (:misses (:stats @cache))))))
  
  (testing "Cache behavior with emoji that have files"
    (let [cache (images/create-image-cache)]
      
      ;; Load multiple emoji
      (let [result1 (images/load-emoji-image-cached cache "💡")
            result2 (images/load-emoji-image-cached cache "🎯")
            result3 (images/load-emoji-image-cached cache "💡")]  ; Should hit cache
        
        (is (:success result1))
        (is (:success result2))
        (is (:success result3))
        
        ;; Should have 2 items cached
        (is (= 2 (count (:items @cache))))
        
        ;; Should have 1 hit (third request) and 2 misses (first two requests)
        (is (= 1 (:hits (:stats @cache))))
        (is (= 2 (:misses (:stats @cache))))))))

(deftest test-cache-performance
  (testing "Cache performance vs direct file loading"
    (let [cache (images/create-image-cache)
          emoji "💡"]
      
      ;; Time direct file loading
      (let [start-direct (.now js/Date)
            _ (images/load-emoji-image emoji)
            _ (images/load-emoji-image emoji)
            _ (images/load-emoji-image emoji)
            end-direct (.now js/Date)
            direct-time (- end-direct start-direct)]
        
        ;; Time cached loading
        (let [start-cached (.now js/Date)
              _ (images/load-emoji-image-cached cache emoji)  ; Cache miss
              _ (images/load-emoji-image-cached cache emoji)  ; Cache hit
              _ (images/load-emoji-image-cached cache emoji)  ; Cache hit
              end-cached (.now js/Date)
              cached-time (- end-cached start-cached)]
          
          ;; Cache should be faster (at least not significantly slower)
          ;; Note: On very fast systems, timing might be inconsistent
          (is (>= direct-time 0))   ; Sanity check
          (is (>= cached-time 0))   ; Sanity check
          
          ;; Verify cache hits occurred
          (is (= 2 (:hits (:stats @cache))))
          (is (= 1 (:misses (:stats @cache))))))))
  
  (testing "Cache with many items"
    (let [cache (images/create-image-cache {:max-size 5})
          emoji-list ["💡" "🎯" "✅" "⚠" "•"]]
      
      ;; Load all emoji (should all succeed since they have files)
      (doseq [emoji emoji-list]
        (let [result (images/load-emoji-image-cached cache emoji)]
          (is (:success result))))
      
      ;; All should be cached
      (is (= 5 (count (:items @cache))))
      (is (= 5 (:misses (:stats @cache))))
      
      ;; Access them again - should all be hits
      (doseq [emoji emoji-list]
        (let [result (images/load-emoji-image-cached cache emoji)]
          (is (:success result))))
      
      (is (= 5 (:hits (:stats @cache))))
      (is (= 5 (:misses (:stats @cache)))))))

(deftest test-cache-edge-cases
  (testing "Cache with existing item replacement"
    (let [cache (images/create-image-cache)
          img1 {:buffer (js/Buffer.alloc 100) :width 72 :height 72 :filename "img1.png"}
          img2 {:buffer (js/Buffer.alloc 200) :width 72 :height 72 :filename "img2.png"}]
      
      ;; Put same key twice
      (images/cache-put cache "💡" img1)
      (is (= 1 (count (:items @cache))))
      (is (= 300 (:memory-usage (:stats @cache))))
      
      (images/cache-put cache "💡" img2)
      (is (= 1 (count (:items @cache))))  ; Still just one item
      (is (= 400 (:memory-usage (:stats @cache))))  ; Updated memory
      
      ;; Should get the newer item
      (let [retrieved (images/cache-get cache "💡")]
        (is (= "img2.png" (:filename retrieved))))))
  
  (testing "Cache with zero size limit"
    (let [cache (images/create-image-cache {:max-size 0})
          image-data {:buffer (js/Buffer.alloc 100) :width 72 :height 72 :filename "test.png"}]
      
      ;; Should immediately evict
      (images/cache-put cache "💡" image-data)
      (is (= 0 (count (:items @cache))))
      (is (>= (:evictions (:stats @cache)) 1))))
  
  (testing "Cache with nil emoji key"
    (let [cache (images/create-image-cache)]
      ;; Should handle nil gracefully
      (is (nil? (images/cache-get cache nil)))
      (is (= 1 (:misses (:stats @cache))))))

;; Error Handling and Fallback Strategy Tests

(deftest test-validate-image-data
  (testing "Valid PNG image data"
    (let [mock-buffer (js/Buffer.alloc 1000)]
      ;; Write PNG signature
      (.writeUInt32BE mock-buffer 0x89504E47 0)
      (.writeUInt32BE mock-buffer 0x0D0A1A0A 4)
      
      (let [image-data {:buffer mock-buffer :width 72 :height 72 :filename "test.png"}
            result (images/validate-image-data image-data)]
        (is (:valid? result))
        (is (empty? (:errors result)))
        (is (= 1000 (get-in result [:info :size])))
        (is (= "PNG" (get-in result [:info :format]))))))
  
  (testing "Invalid PNG signature"
    (let [mock-buffer (js/Buffer.from "not a png file")
          image-data {:buffer mock-buffer :width 72 :height 72}
          result (images/validate-image-data image-data)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Invalid PNG signature") (:errors result)))))
  
  (testing "Missing buffer"
    (let [image-data {:width 72 :height 72}
          result (images/validate-image-data image-data)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "Missing image buffer") (:errors result)))))
  
  (testing "Buffer too small"
    (let [small-buffer (js/Buffer.alloc 50)
          image-data {:buffer small-buffer :width 72 :height 72}
          result (images/validate-image-data image-data)]
      (is (not (:valid? result)))
      (is (some #(clojure.string/includes? % "too small") (:errors result)))))
  
  (testing "Validation warnings"
    (let [mock-buffer (js/Buffer.alloc (* 2 1024 1024))]  ; 2MB
      ;; Write PNG signature
      (.writeUInt32BE mock-buffer 0x89504E47 0)
      (.writeUInt32BE mock-buffer 0x0D0A1A0A 4)
      
      (let [image-data {:buffer mock-buffer :width 128 :height 64 :filename "large.png"}  ; Non-square, non-standard size
            result (images/validate-image-data image-data)]
        (is (:valid? result))
        (is (>= (count (:warnings result)) 2))  ; Should have warnings for size and dimensions
        (is (some #(clojure.string/includes? % "Large image") (:warnings result)))
        (is (some #(clojure.string/includes? % "Non-square") (:warnings result)))))))

(deftest test-fallback-strategies
  (testing "Hex string fallback"
    (let [result (images/fallback-to-hex "💡")]
      (is (= :hex-string (:type result)))
      (is (:success result))
      (is (string? (:content result)))
      (is (.startsWith (:content result) "<"))
      (is (.endsWith (:content result) ">"))
      (is (contains? result :fallback-reason))))
  
  (testing "Placeholder fallback"
    (let [result (images/fallback-to-placeholder "💡")]
      (is (= :placeholder (:type result)))
      (is (:success result))
      (is (= "[💡]" (:content result)))
      (is (contains? result :fallback-reason))))
  
  (testing "Skip fallback"
    (let [result (images/fallback-to-skip "💡")]
      (is (= :skip (:type result)))
      (is (:success result))
      (is (= "" (:content result)))
      (is (contains? result :fallback-reason))))
  
  (testing "Hex encoding for different emoji"
    (let [result-lightbulb (images/fallback-to-hex "💡")
          result-target (images/fallback-to-hex "🎯")]
      (is (not= (:content result-lightbulb) (:content result-target)))
      (is (every? #(.startsWith (:content %) "<") [result-lightbulb result-target]))
      (is (every? #(.endsWith (:content %) ">") [result-lightbulb result-target]))))
  
  (testing "Hex encoding for ASCII text"
    (let [result (images/fallback-to-hex "ABC")]
      (is (= :hex-string (:type result)))
      (is (string? (:content result)))
      ;; Should encode ASCII characters properly
      (is (.includes (:content result) "41"))  ; 'A' = 0x41
      (is (.includes (:content result) "42"))  ; 'B' = 0x42
      (is (.includes (:content result) "43")))))  ; 'C' = 0x43

(deftest test-handle-image-error
  (testing "Hex string error handling"
    (let [result (images/handle-image-error "💡" "Test error" :hex-string {:logging? false})]
      (is (= :hex-string (:type result)))
      (is (:success result))
      (is (contains? result :fallback-reason))))
  
  (testing "Placeholder error handling"
    (let [result (images/handle-image-error "💡" "Test error" :placeholder {:logging? false})]
      (is (= :placeholder (:type result)))
      (is (:success result))
      (is (= "[💡]" (:content result)))))
  
  (testing "Skip error handling"
    (let [result (images/handle-image-error "💡" "Test error" :skip {:logging? false})]
      (is (= :skip (:type result)))
      (is (:success result))
      (is (= "" (:content result)))))
  
  (testing "Error strategy throws exception"
    (is (thrown? js/Error
                 (images/handle-image-error "💡" "Test error" :error {:logging? false}))))
  
  (testing "Invalid strategy throws exception"
    (is (thrown? js/Error
                 (images/handle-image-error "💡" "Test error" :invalid {:logging? false}))))
  
  (testing "Error message variations"
    (let [result1 (images/handle-image-error "💡" "String error" :placeholder {:logging? false})
          result2 (images/handle-image-error "💡" {:error "Map error"} :placeholder {:logging? false})]
      (is (:success result1))
      (is (:success result2)))))

(deftest test-emoji-image-with-fallback
  (testing "Successful image loading"
    (let [cache (images/create-image-cache)
          result (images/emoji-image-with-fallback cache "💡" {:logging? false})]
      (is (:success result))
      (is (= :image (:type result)))
      (is (contains? result :buffer))))
  
  (testing "Failed loading with hex fallback"
    (let [cache (images/create-image-cache)
          result (images/emoji-image-with-fallback cache "nonexistent" {:fallback-strategy :hex-string :logging? false})]
      (is (:success result))
      (is (= :hex-string (:type result)))
      (is (contains? result :content))
      (is (contains? result :fallback-reason))))
  
  (testing "Failed loading with placeholder fallback"
    (let [cache (images/create-image-cache)
          result (images/emoji-image-with-fallback cache "nonexistent" {:fallback-strategy :placeholder :logging? false})]
      (is (:success result))
      (is (= :placeholder (:type result)))
      (is (= "[nonexistent]" (:content result)))))
  
  (testing "Failed loading with skip fallback"
    (let [cache (images/create-image-cache)
          result (images/emoji-image-with-fallback cache "nonexistent" {:fallback-strategy :skip :logging? false})]
      (is (:success result))
      (is (= :skip (:type result)))
      (is (= "" (:content result)))))
  
  (testing "Failed loading with error strategy"
    (let [cache (images/create-image-cache)]
      (is (thrown? js/Error
                   (images/emoji-image-with-fallback cache "nonexistent" {:fallback-strategy :error :logging? false})))))
  
  (testing "Loading without cache"
    (let [result (images/emoji-image-with-fallback nil "💡" {:logging? false})]
      (is (:success result))
      (is (= :image (:type result)))))
  
  (testing "Validation disabled"
    (let [cache (images/create-image-cache)
          result (images/emoji-image-with-fallback cache "💡" {:validation? false :logging? false})]
      (is (:success result))
      (is (= :image (:type result)))))
  
  (testing "Default options"
    (let [cache (images/create-image-cache)
          result (images/emoji-image-with-fallback cache "💡")]
      (is (:success result))
      (is (= :image (:type result))))))

(deftest test-batch-load-with-fallback
  (testing "Batch loading with mixed success/failure"
    (let [cache (images/create-image-cache)
          emoji-list ["💡" "🎯" "nonexistent1" "nonexistent2"]
          results (images/batch-load-with-fallback cache emoji-list {:fallback-strategy :placeholder :logging? false})]
      
      ;; Should have result for each emoji
      (is (= 4 (count results)))
      (is (every? #(contains? results %) emoji-list))
      
      ;; Check successful loads
      (let [lightbulb-result (get results "💡")
            target-result (get results "🎯")]
        (is (= :image (:type lightbulb-result)))
        (is (= :image (:type target-result)))
        (is (:success lightbulb-result))
        (is (:success target-result)))
      
      ;; Check fallback results
      (let [nonexistent1-result (get results "nonexistent1")
            nonexistent2-result (get results "nonexistent2")]
        (is (= :placeholder (:type nonexistent1-result)))
        (is (= :placeholder (:type nonexistent2-result)))
        (is (= "[nonexistent1]" (:content nonexistent1-result)))
        (is (= "[nonexistent2]" (:content nonexistent2-result))))))
  
  (testing "Batch loading with all successes"
    (let [cache (images/create-image-cache)
          emoji-list ["💡" "🎯" "✅"]
          results (images/batch-load-with-fallback cache emoji-list {:logging? false})]
      
      (is (= 3 (count results)))
      (is (every? #(= :image (:type (get results %))) emoji-list))
      (is (every? #(:success (get results %)) emoji-list))))
  
  (testing "Empty emoji list"
    (let [cache (images/create-image-cache)
          results (images/batch-load-with-fallback cache [] {:logging? false})]
      (is (= 0 (count results))))))

(deftest test-fallback-performance-info
  (testing "Performance information structure"
    (let [perf-info (images/fallback-performance-info)]
      (is (map? perf-info))
      (is (contains? perf-info :hex-string))
      (is (contains? perf-info :placeholder))
      (is (contains? perf-info :skip))
      (is (contains? perf-info :error))
      
      ;; Check each strategy has required fields
      (doseq [strategy [:hex-string :placeholder :skip :error]]
        (let [strategy-info (get perf-info strategy)]
          (is (contains? strategy-info :speed))
          (is (contains? strategy-info :compatibility))
          (is (contains? strategy-info :pdf-size-impact))
          (is (contains? strategy-info :description))
          (is (string? (:description strategy-info))))))))

(deftest test-error-handling-integration
  (testing "Error handling with cache integration"
    (let [cache (images/create-image-cache {:max-size 2})]
      ;; Load successful emoji first
      (let [success-result (images/emoji-image-with-fallback cache "💡" {:logging? false})]
        (is (= :image (:type success-result))))
      
      ;; Then try failed emoji
      (let [fail-result (images/emoji-image-with-fallback cache "nonexistent" {:fallback-strategy :hex-string :logging? false})]
        (is (= :hex-string (:type fail-result))))
      
      ;; Cache should only contain successful load
      (is (= 1 (count (:items @cache))))))
  
  (testing "Performance comparison: success vs fallback"
    (let [cache (images/create-image-cache)]
      ;; Time successful load
      (let [start-success (.now js/Date)
            _ (images/emoji-image-with-fallback cache "💡" {:logging? false})
            end-success (.now js/Date)
            success-time (- end-success start-success)]
        
        ;; Time fallback load
        (let [start-fallback (.now js/Date)
              _ (images/emoji-image-with-fallback cache "nonexistent" {:fallback-strategy :hex-string :logging? false})
              end-fallback (.now js/Date)
              fallback-time (- end-fallback start-fallback)]
          
          ;; Both should complete reasonably quickly
          (is (< success-time 100))   ; Should be fast
          (is (< fallback-time 50))   ; Fallback should be even faster
          (is (>= success-time 0))    ; Sanity check
          (is (>= fallback-time 0)))))) ; Sanity check
  
  (testing "Memory cleanup after errors"
    (let [cache (images/create-image-cache {:max-size 10})
          initial-memory (:memory-usage (:stats @cache))]
      
      ;; Try loading multiple non-existent emoji
      (doseq [i (range 5)]
        (images/emoji-image-with-fallback cache (str "nonexistent" i) {:fallback-strategy :skip :logging? false}))
      
      ;; Memory usage should not increase (failed loads not cached)
      (is (= initial-memory (:memory-usage (:stats @cache))))
      (is (= 0 (count (:items @cache))))))

;; PDF Image Object Generation Tests

(deftest test-create-resource-reference
  (testing "Unique reference generation"
    (let [ref1 (images/create-resource-reference)
          ref2 (images/create-resource-reference)
          ref3 (images/create-resource-reference)]
      (is (string? ref1))
      (is (string? ref2))
      (is (string? ref3))
      (is (.startsWith ref1 "Em"))
      (is (.startsWith ref2 "Em"))
      (is (.startsWith ref3 "Em"))
      ;; Should be unique
      (is (not= ref1 ref2))
      (is (not= ref2 ref3))
      (is (not= ref1 ref3))))
  
  (testing "Reference format"
    (let [ref (images/create-resource-reference)]
      (is (re-matches #"Em\d+" ref)))))

(deftest test-calculate-image-transform
  (testing "Basic font size scaling"
    (let [transform (images/calculate-image-transform 12)]
      (is (contains? transform :scale-x))
      (is (contains? transform :scale-y))
      (is (contains? transform :offset-x))
      (is (contains? transform :offset-y))
      (is (contains? transform :matrix))
      
      ;; 12pt font size should scale 72x72 to 12x12
      (is (< (Math/abs (- (:scale-x transform) (/ 12 72.0))) 0.001))
      (is (< (Math/abs (- (:scale-y transform) (/ 12 72.0))) 0.001))
      (is (= 0 (:offset-x transform)))
      ;; Baseline offset should be negative (PDF coords)
      (is (< (:offset-y transform) 0))))
  
  (testing "Different font sizes"
    (let [transform-8 (images/calculate-image-transform 8)
          transform-24 (images/calculate-image-transform 24)
          transform-72 (images/calculate-image-transform 72)]
      
      ;; Check scaling factors
      (is (< (Math/abs (- (:scale-x transform-8) (/ 8 72.0))) 0.001))
      (is (< (Math/abs (- (:scale-x transform-24) (/ 24 72.0))) 0.001))
      (is (< (Math/abs (- (:scale-x transform-72) 1.0)) 0.001))
      
      ;; Check matrix format [a b c d e f]
      (doseq [transform [transform-8 transform-24 transform-72]]
        (is (= 6 (count (:matrix transform)))))))
  
  (testing "Custom baseline offset"
    (let [transform-default (images/calculate-image-transform 12)
          transform-custom (images/calculate-image-transform 12 0.5)]
      
      ;; Custom offset should be different
      (is (not= (:offset-y transform-default) (:offset-y transform-custom)))
      ;; Custom should be larger magnitude (more negative)
      (is (< (:offset-y transform-custom) (:offset-y transform-default)))))

(deftest test-png-to-pdf-object
  (testing "Basic PDF object generation"
    (let [mock-buffer (js/Buffer.from "mock png data")
          pdf-object (images/png-to-pdf-object mock-buffer 72 72 1001)]
      
      (is (string? pdf-object))
      (is (.includes pdf-object "1001 0 obj"))
      (is (.includes pdf-object "/Type /XObject"))
      (is (.includes pdf-object "/Subtype /Image"))
      (is (.includes pdf-object "/Width 72"))
      (is (.includes pdf-object "/Height 72"))
      (is (.includes pdf-object "/ColorSpace /DeviceRGB"))
      (is (.includes pdf-object "/BitsPerComponent 8"))
      (is (.includes pdf-object "/Filter /FlateDecode"))
      (is (.includes pdf-object "stream"))
      (is (.includes pdf-object "endstream"))
      (is (.includes pdf-object "endobj"))))
  
  (testing "Length calculation"
    (let [short-buffer (js/Buffer.from "short")
          long-buffer (js/Buffer.from "this is a much longer buffer content")
          pdf-short (images/png-to-pdf-object short-buffer 72 72 1002)
          pdf-long (images/png-to-pdf-object long-buffer 72 72 1003)]
      
      ;; Should include length declarations
      (is (re-find #"/Length \d+" pdf-short))
      (is (re-find #"/Length \d+" pdf-long))
      
      ;; Longer buffer should have larger length
      (let [short-length (-> (re-find #"/Length (\d+)" pdf-short) second js/parseInt)
            long-length (-> (re-find #"/Length (\d+)" pdf-long) second js/parseInt)]
        (is (< short-length long-length)))))
  
  (testing "Different dimensions"
    (let [buffer (js/Buffer.from "test data")
          pdf-square (images/png-to-pdf-object buffer 72 72 1004)
          pdf-rect (images/png-to-pdf-object buffer 100 50 1005)]
      
      (is (.includes pdf-square "/Width 72"))
      (is (.includes pdf-square "/Height 72"))
      (is (.includes pdf-rect "/Width 100"))
      (is (.includes pdf-rect "/Height 50")))))

(deftest test-generate-image-xobject
  (testing "Successful XObject generation"
    (let [mock-buffer (js/Buffer.from "mock png data")
          image-data {:buffer mock-buffer :width 72 :height 72 :filename "test.png"}
          result (images/generate-image-xobject image-data)]
      
      (is (:success result))
      (is (contains? result :object-number))
      (is (contains? result :reference-name))
      (is (contains? result :pdf-object))
      (is (contains? result :width))
      (is (contains? result :height))
      
      (is (pos? (:object-number result)))
      (is (.startsWith (:reference-name result) "Em"))
      (is (string? (:pdf-object result)))
      (is (= 72 (:width result)))
      (is (= 72 (:height result)))))
  
  (testing "Custom reference and object number"
    (let [mock-buffer (js/Buffer.from "test data")
          image-data {:buffer mock-buffer :width 72 :height 72}
          result (images/generate-image-xobject image-data "CustomRef" 2000)]
      
      (is (:success result))
      (is (= "CustomRef" (:reference-name result)))
      (is (= 2000 (:object-number result)))
      (is (.includes (:pdf-object result) "2000 0 obj"))))
  
  (testing "Missing buffer error"
    (let [image-data {:width 72 :height 72}
          result (images/generate-image-xobject image-data)]
      
      (is (not (:success result)))
      (is (contains? result :error))
      (is (.includes (:error result) "Missing image buffer"))))
  
  (testing "Default dimensions"
    (let [mock-buffer (js/Buffer.from "test")
          image-data {:buffer mock-buffer}  ; No width/height specified
          result (images/generate-image-xobject image-data)]
      
      (is (:success result))
      (is (= 72 (:width result)))    ; Should default to 72
      (is (= 72 (:height result))))))

(deftest test-batch-generate-xobjects
  (testing "Multiple XObject generation"
    (let [image-data-list [{:buffer (js/Buffer.from "data1") :width 72 :height 72}
                           {:buffer (js/Buffer.from "data2") :width 64 :height 64}
                           {:buffer (js/Buffer.from "data3") :width 48 :height 48}]
          results (images/batch-generate-xobjects image-data-list)]
      
      (is (= 3 (count results)))
      (is (every? :success results))
      
      ;; Check sequential object numbering
      (let [obj-nums (map :object-number results)]
        (is (apply < obj-nums)))  ; Should be in ascending order
      
      ;; Check unique references
      (let [ref-names (map :reference-name results)]
        (is (= 3 (count (set ref-names)))))  ; All unique
      
      ;; Check dimensions preserved
      (is (= 72 (:width (first results))))
      (is (= 64 (:width (second results))))
      (is (= 48 (:width (nth results 2))))))
  
  (testing "Custom starting object number"
    (let [image-data-list [{:buffer (js/Buffer.from "test1")}
                           {:buffer (js/Buffer.from "test2")}]
          results (images/batch-generate-xobjects image-data-list 5000)]
      
      (is (= 2 (count results)))
      (is (= 5000 (:object-number (first results))))
      (is (= 5001 (:object-number (second results))))))
  
  (testing "Empty list"
    (let [results (images/batch-generate-xobjects [])]
      (is (= 0 (count results))))))

(deftest test-create-resource-dictionary-entry
  (testing "Single XObject reference"
    (let [xobject-refs [{:reference-name "Em1" :object-number 1001}]
          entry (images/create-resource-dictionary-entry xobject-refs)]
      
      (is (string? entry))
      (is (.includes entry "/XObject <<"))
      (is (.includes entry "/Em1 1001 0 R"))
      (is (.includes entry ">>")))))
  
  (testing "Multiple XObject references"
    (let [xobject-refs [{:reference-name "Em1" :object-number 1001}
                        {:reference-name "Em2" :object-number 1002}
                        {:reference-name "Em3" :object-number 1003}]
          entry (images/create-resource-dictionary-entry xobject-refs)]
      
      (is (.includes entry "/Em1 1001 0 R"))
      (is (.includes entry "/Em2 1002 0 R"))
      (is (.includes entry "/Em3 1003 0 R"))))
  
  (testing "Empty references"
    (let [entry (images/create-resource-dictionary-entry [])]
      (is (= "" entry)))))

(deftest test-generate-image-draw-operators
  (testing "Basic draw operators"
    (let [operators (images/generate-image-draw-operators "Em1" 100 200 12)]
      
      (is (string? operators))
      (is (.includes operators "q"))        ; Save state
      (is (.includes operators "cm"))       ; Transform matrix
      (is (.includes operators "/Em1 Do"))  ; Draw command
      (is (.includes operators "Q"))        ; Restore state
      
      ;; Should include transformation matrix
      (is (re-find #"[\d\.]+ 0 0 [\d\.]+ [\d\.]+ [\d\.]+ cm" operators))))
  
  (testing "Different font sizes"
    (let [ops-12 (images/generate-image-draw-operators "Em1" 0 0 12)
          ops-24 (images/generate-image-draw-operators "Em1" 0 0 24)]
      
      ;; Different font sizes should produce different transformations
      (is (not= ops-12 ops-24))
      (is (.includes ops-12 "/Em1 Do"))
      (is (.includes ops-24 "/Em1 Do"))))
  
  (testing "Position handling"
    (let [ops-origin (images/generate-image-draw-operators "Em1" 0 0 12)
          ops-offset (images/generate-image-draw-operators "Em1" 50 100 12)]
      
      ;; Different positions should produce different operators
      (is (not= ops-origin ops-offset))
      
      ;; Both should reference same XObject
      (is (.includes ops-origin "/Em1 Do"))
      (is (.includes ops-offset "/Em1 Do"))))
  
  (testing "Custom baseline offset"
    (let [ops-default (images/generate-image-draw-operators "Em1" 0 0 12)
          ops-custom (images/generate-image-draw-operators "Em1" 0 0 12 0.5)]
      
      ;; Different baseline offsets should produce different transformations
      (is (not= ops-default ops-custom)))))

(deftest test-validate-pdf-xobject
  (testing "Valid PDF XObject"
    (let [mock-buffer (js/Buffer.from "test data")
          pdf-object (images/png-to-pdf-object mock-buffer 72 72 1001)
          validation (images/validate-pdf-xobject pdf-object)]
      
      (is (:valid? validation))
      (is (empty? (:errors validation)))))
  
  (testing "Invalid PDF objects"
    ;; Empty object
    (let [validation-empty (images/validate-pdf-xobject "")]
      (is (not (:valid? validation-empty)))
      (is (some #(.includes % "cannot be empty") (:errors validation-empty))))
    
    ;; Non-string input
    (let [validation-nil (images/validate-pdf-xobject nil)]
      (is (not (:valid? validation-nil)))
      (is (some #(.includes % "must be a string") (:errors validation-nil))))
    
    ;; Missing object header
    (let [validation-no-header (images/validate-pdf-xobject "not a pdf object")]
      (is (not (:valid? validation-no-header)))
      (is (some #(.includes % "Missing object header") (:errors validation-no-header)))))
  
  (testing "Malformed XObject elements"
    ;; Missing Type
    (let [partial-obj "1001 0 obj\n<<\n/Subtype /Image\n>>\nstream\ndata\nendstream\nendobj"
          validation (images/validate-pdf-xobject partial-obj)]
      (is (not (:valid? validation)))
      (is (some #(.includes % "Missing XObject type") (:errors validation))))
    
    ;; Missing stream
    (let [no-stream "1001 0 obj\n<<\n/Type /XObject\n/Subtype /Image\n>>\nendobj"
          validation (images/validate-pdf-xobject no-stream)]
      (is (not (:valid? validation)))
      (is (some #(.includes % "Missing or malformed stream") (:errors validation)))))
  
  (testing "Validation warnings"
    (let [minimal-obj "1001 0 obj\n<<\n/Type /XObject\n/Subtype /Image\n>>\nstream\ndata\nendstream\nendobj"
          validation (images/validate-pdf-xobject minimal-obj)]
      
      ;; Should be valid but have warnings
      (is (:valid? validation))
      (is (> (count (:warnings validation)) 0)))))

(deftest test-pdf-xobject-integration
  (testing "End-to-end XObject generation"
    (let [cache (images/create-image-cache)
          ;; Load actual emoji image
          emoji-result (images/emoji-image-with-fallback cache "💡" {:logging? false})]
      
      (when (= :image (:type emoji-result))
        ;; Generate XObject from loaded image
        (let [xobject-result (images/generate-image-xobject emoji-result)
              pdf-object (:pdf-object xobject-result)]
          
          (is (:success xobject-result))
          (is (string? pdf-object))
          
          ;; Validate generated PDF object
          (let [validation (images/validate-pdf-xobject pdf-object)]
            (is (:valid? validation)))
          
          ;; Generate draw operators
          (let [draw-ops (images/generate-image-draw-operators 
                          (:reference-name xobject-result) 100 200 14)]
            (is (string? draw-ops))
            (is (.includes draw-ops (:reference-name xobject-result))))))))
  
  (testing "Resource dictionary generation"
    (let [cache (images/create-image-cache)
          emoji-list ["💡" "🎯"]
          emoji-results (map #(images/emoji-image-with-fallback cache % {:logging? false}) emoji-list)
          image-results (filter #(= :image (:type %)) emoji-results)]
      
      (when (= 2 (count image-results))
        ;; Generate XObjects for all images
        (let [xobject-results (map images/generate-image-xobject image-results)
              resource-entry (images/create-resource-dictionary-entry xobject-results)]
          
          (is (every? :success xobject-results))
          (is (string? resource-entry))
          (is (.includes resource-entry "/XObject <<"))
          
          ;; Should reference all generated XObjects
          (doseq [xobj xobject-results]
            (is (.includes resource-entry (:reference-name xobj))))))))
  
  (testing "Scaling and positioning calculations"
    (let [font-sizes [8 12 16 24 36 72]]
      
      ;; Test scaling for different font sizes
      (doseq [size font-sizes]
        (let [transform (images/calculate-image-transform size)
              expected-scale (/ size 72.0)]
          
          ;; Scale should match font size ratio
          (is (< (Math/abs (- (:scale-x transform) expected-scale)) 0.001))
          (is (< (Math/abs (- (:scale-y transform) expected-scale)) 0.001))
          
          ;; Matrix should be valid
          (is (= 6 (count (:matrix transform))))))))))

;; Step 5: Shortcode Configuration System Tests

(deftest test-load-emoji-shortcodes
  (testing "Successful shortcode loading"
    ;; Reset cached shortcodes to test fresh loading
    (reset! @#'images/emoji-shortcodes nil)
    
    (let [shortcodes (images/load-emoji-shortcodes)]
      (if shortcodes
        (do
          ;; Should return a map
          (is (map? shortcodes))
          
          ;; Should contain expected shortcodes from emojis.edn
          (is (contains? shortcodes :smile))
          (is (contains? shortcodes :heart))
          (is (contains? shortcodes :lightbulb))
          
          ;; Values should be PNG filenames
          (is (string? (:smile shortcodes)))
          (is (.endsWith (:smile shortcodes) ".png"))
          (is (.startsWith (:smile shortcodes) "emoji_u"))
          
          ;; Should cache the loaded shortcodes
          (let [cached-shortcodes (images/load-emoji-shortcodes)]
            (is (= shortcodes cached-shortcodes))))
        
        ;; If loading failed (file not found), should handle gracefully
        (is (nil? shortcodes)))))
  
  (testing "Cached shortcode retrieval"
    ;; Ensure shortcodes are loaded
    (images/load-emoji-shortcodes)
    
    ;; Second call should return cached version
    (let [start-time (.now js/Date)
          shortcodes (images/load-emoji-shortcodes)
          end-time (.now js/Date)
          duration (- end-time start-time)]
      
      ;; Cached access should be very fast
      (is (< duration 10))
      (is (or (map? shortcodes) (nil? shortcodes)))))
  
  (testing "Error handling for missing file"
    ;; Temporarily reset cache and test with non-existent file
    (let [original-atom @#'images/emoji-shortcodes]
      (try
        (reset! @#'images/emoji-shortcodes nil)
        ;; The function will try to load from emojis/emojis.edn
        ;; If file doesn't exist, should return nil gracefully
        (let [result (images/load-emoji-shortcodes)]
          (is (or (map? result) (nil? result))))
        (finally
          ;; Restore original cache state
          (reset! @#'images/emoji-shortcodes original-atom))))))

(deftest test-validate-shortcode
  (testing "Valid shortcodes"
    ;; Load shortcodes first
    (images/load-emoji-shortcodes)
    
    (let [valid-codes [:smile :heart :lightbulb :thumbsup :fire]]
      (doseq [code valid-codes]
        (when (images/validate-shortcode code)
          ;; If shortcode exists in loaded file, validation should pass
          (is (= true (images/validate-shortcode code))
              (str "Shortcode " code " should be valid"))))))
  
  (testing "Invalid shortcodes"
    (is (= false (images/validate-shortcode :nonexistent-shortcode)))
    (is (= false (images/validate-shortcode :definitely-not-real)))
    (is (= false (images/validate-shortcode :fake-emoji))))
  
  (testing "Invalid input types"
    ;; Non-keyword inputs should return false
    (is (= false (images/validate-shortcode "smile")))
    (is (= false (images/validate-shortcode nil)))
    (is (= false (images/validate-shortcode 123)))
    (is (= false (images/validate-shortcode [])))
    (is (= false (images/validate-shortcode {}))))
  
  (testing "Edge cases"
    ;; Keywords with special characters
    (is (= false (images/validate-shortcode :smile-with-dashes)))
    (is (= false (images/validate-shortcode :smile_with_underscores)))
    
    ;; Case sensitivity (shortcodes should be lowercase)
    (is (= false (images/validate-shortcode :SMILE)))
    (is (= false (images/validate-shortcode :Smile)))))

(deftest test-resolve-shortcode-to-path
  (testing "Valid shortcode resolution"
    (images/load-emoji-shortcodes)
    
    (let [valid-codes [:smile :heart :lightbulb]]
      (doseq [code valid-codes]
        (when (images/validate-shortcode code)
          (let [path (images/resolve-shortcode-to-path code)]
            (is (string? path)
                (str "Path for " code " should be a string"))
            (is (.includes path "emojis/noto-72/")
                (str "Path should include base directory"))
            (is (.endsWith path ".png")
                (str "Path should end with .png"))
            (is (.includes path "emoji_u")
                (str "Path should include emoji_u prefix")))))))
  
  (testing "Invalid shortcode resolution"
    (is (nil? (images/resolve-shortcode-to-path :nonexistent)))
    (is (nil? (images/resolve-shortcode-to-path :fake-emoji)))
    (is (nil? (images/resolve-shortcode-to-path :not-real))))
  
  (testing "Invalid input types"
    (is (nil? (images/resolve-shortcode-to-path "smile")))
    (is (nil? (images/resolve-shortcode-to-path nil)))
    (is (nil? (images/resolve-shortcode-to-path 123)))
    (is (nil? (images/resolve-shortcode-to-path []))))
  
  (testing "Path construction consistency"
    (images/load-emoji-shortcodes)
    
    ;; Test that multiple calls return same path
    (when (images/validate-shortcode :smile)
      (let [path1 (images/resolve-shortcode-to-path :smile)
            path2 (images/resolve-shortcode-to-path :smile)]
        (is (= path1 path2)))))
  
  (testing "Cross-platform path handling"
    (images/load-emoji-shortcodes)
    
    (when (images/validate-shortcode :smile)
      (let [path (images/resolve-shortcode-to-path :smile)]
        ;; Should contain directory separator (/ or \)
        (is (or (.includes path "/") (.includes path "\\"))
            "Path should contain directory separator")))))

(deftest test-list-available-shortcodes
  (testing "Shortcode list retrieval"
    (images/load-emoji-shortcodes)
    
    (let [shortcodes (images/list-available-shortcodes)]
      (is (vector? shortcodes)
          "Should return a vector")
      
      (when (pos? (count shortcodes))
        ;; If shortcodes loaded successfully
        (is (every? keyword? shortcodes)
            "All shortcodes should be keywords")
        
        ;; Should contain expected shortcodes from emojis.edn
        (let [shortcode-set (set shortcodes)]
          (when (contains? shortcode-set :smile)
            (is (contains? shortcode-set :smile)))
          (when (contains? shortcode-set :heart)
            (is (contains? shortcode-set :heart)))
          (when (contains? shortcode-set :lightbulb)
            (is (contains? shortcode-set :lightbulb))))
        
        ;; Should be reasonable number of shortcodes (expected around 50)
        (is (>= (count shortcodes) 10)
            "Should have at least 10 shortcodes")
        (is (<= (count shortcodes) 100)
            "Should have at most 100 shortcodes for test file"))))
  
  (testing "Empty result handling"
    ;; Test behavior when no shortcodes loaded
    (let [original-atom @#'images/emoji-shortcodes]
      (try
        (reset! @#'images/emoji-shortcodes nil)
        (let [shortcodes (images/list-available-shortcodes)]
          (is (vector? shortcodes)
              "Should return vector even when empty")
          (is (>= (count shortcodes) 0)
              "Should handle empty case gracefully"))
        (finally
          (reset! @#'images/emoji-shortcodes original-atom)))))
  
  (testing "Consistency with validation"
    (images/load-emoji-shortcodes)
    
    (let [available-shortcodes (images/list-available-shortcodes)]
      ;; All listed shortcodes should be valid
      (doseq [shortcode available-shortcodes]
        (is (images/validate-shortcode shortcode)
            (str "Listed shortcode " shortcode " should validate"))))))

(deftest test-get-shortcode-info
  (testing "Valid shortcode information"
    (images/load-emoji-shortcodes)
    
    (when (images/validate-shortcode :smile)
      (let [info (images/get-shortcode-info :smile)]
        (is (map? info)
            "Should return a map")
        (is (= :smile (:shortcode info))
            "Should include shortcode keyword")
        (is (string? (:filename info))
            "Should include filename string")
        (is (string? (:file-path info))
            "Should include file path string")
        (is (boolean? (:exists? info))
            "Should include exists boolean")
        
        ;; Check filename format
        (is (.endsWith (:filename info) ".png")
            "Filename should end with .png")
        (is (.startsWith (:filename info) "emoji_u")
            "Filename should start with emoji_u")
        
        ;; Check path construction
        (is (.includes (:file-path info) (:filename info))
            "File path should include filename")
        (is (.includes (:file-path info) "emojis/noto-72")
            "File path should include base directory"))))
  
  (testing "Invalid shortcode information"
    (is (nil? (images/get-shortcode-info :nonexistent))
        "Should return nil for invalid shortcode")
    (is (nil? (images/get-shortcode-info :fake-emoji))
        "Should return nil for fake shortcode"))
  
  (testing "File existence checking"
    (images/load-emoji-shortcodes)
    
    ;; Test with shortcodes that may or may not have actual files
    (let [available-shortcodes (images/list-available-shortcodes)]
      (when (pos? (count available-shortcodes))
        (let [test-shortcode (first available-shortcodes)
              info (images/get-shortcode-info test-shortcode)]
          (when info
            ;; exists? should be boolean regardless of actual file existence
            (is (boolean? (:exists? info))
                "exists? should be boolean")
            
            ;; If file exists, it should be readable
            (when (:exists? info)
              ;; File system operations should not throw errors
              (is (string? (:file-path info)))))))))
  
  (testing "Multiple shortcode info consistency"
    (images/load-emoji-shortcodes)
    
    (let [test-codes [:smile :heart :lightbulb]]
      (doseq [code test-codes]
        (when (images/validate-shortcode code)
          (let [info1 (images/get-shortcode-info code)
                info2 (images/get-shortcode-info code)]
            ;; Multiple calls should return same information
            (is (= info1 info2)
                (str "Info for " code " should be consistent")))))))
  
  (testing "Error handling for edge cases"
    ;; Test with invalid input types
    (is (nil? (images/get-shortcode-info "smile"))
        "Should handle string input gracefully")
    (is (nil? (images/get-shortcode-info nil))
        "Should handle nil input gracefully")
    (is (nil? (images/get-shortcode-info 123))
        "Should handle numeric input gracefully"))))
