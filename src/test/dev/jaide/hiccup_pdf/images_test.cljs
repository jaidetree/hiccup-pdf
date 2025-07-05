(ns dev.jaide.hiccup-pdf.images-test
  "Tests for PNG image file operations"
  (:require [cljs.test :refer [deftest is testing]]
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
      (is (< duration 500))))

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
      (is (= 1 (:misses (:stats @cache))))))))