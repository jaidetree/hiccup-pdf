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
      (is (< duration 500)))))