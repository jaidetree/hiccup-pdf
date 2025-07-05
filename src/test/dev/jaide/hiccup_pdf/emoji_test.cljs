(ns dev.jaide.hiccup-pdf.emoji-test
  "Tests for Unicode emoji processing functions"
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.emoji :as emoji]))

(deftest test-surrogate-pair?
  (testing "High surrogate detection"
    (is (= :high (emoji/surrogate-pair? 0xD800))) ; Start of high range
    (is (= :high (emoji/surrogate-pair? 0xDBFF))) ; End of high range
    (is (= :high (emoji/surrogate-pair? 0xD83D))) ; Common high surrogate
    (is (= :high (emoji/surrogate-pair? 55357)))) ; Decimal equivalent of 0xD83D
  
  (testing "Low surrogate detection"
    (is (= :low (emoji/surrogate-pair? 0xDC00))) ; Start of low range
    (is (= :low (emoji/surrogate-pair? 0xDFFF))) ; End of low range
    (is (= :low (emoji/surrogate-pair? 0xDCA1))) ; Common low surrogate
    (is (= :low (emoji/surrogate-pair? 56481)))) ; Decimal equivalent of 0xDCA1
  
  (testing "Non-surrogate characters"
    (is (nil? (emoji/surrogate-pair? 0x0041))) ; 'A'
    (is (nil? (emoji/surrogate-pair? 0x2022))) ; Bullet
    (is (nil? (emoji/surrogate-pair? 0x1F4A1))) ; Direct emoji codepoint
    (is (nil? (emoji/surrogate-pair? 65)))) ; Decimal 'A'
  
  (testing "Edge cases"
    (is (nil? (emoji/surrogate-pair? 0xD7FF))) ; Just below high range
    (is (nil? (emoji/surrogate-pair? 0xE000))) ; Just above low range
    (is (nil? (emoji/surrogate-pair? 0)))))     ; Zero

(deftest test-emoji?
  (testing "Emoji range 0x1F300-0x1F9FF"
    (is (emoji/emoji? 0x1F4A1)) ; 💡 lightbulb
    (is (emoji/emoji? 0x1F3AF)) ; 🎯 target
    (is (emoji/emoji? 0x1F300)) ; Start of range
    (is (emoji/emoji? 0x1F9FF)) ; End of range
    (is (emoji/emoji? 127825))) ; Decimal 0x1F4A1
  
  (testing "Emoji range 0x2600-0x26FF"
    (is (emoji/emoji? 0x26A0)) ; ⚠️ warning
    (is (emoji/emoji? 0x2600)) ; Start of range
    (is (emoji/emoji? 0x26FF)) ; End of range
    (is (emoji/emoji? 9888)))  ; Decimal 0x26A0
  
  (testing "Emoji range 0x2700-0x27BF"
    (is (emoji/emoji? 0x2705)) ; ✅ check mark
    (is (emoji/emoji? 0x2700)) ; Start of range
    (is (emoji/emoji? 0x27BF)) ; End of range
    (is (emoji/emoji? 9989)))  ; Decimal 0x2705
  
  (testing "Non-emoji characters"
    (is (not (emoji/emoji? 0x0041))) ; 'A'
    (is (not (emoji/emoji? 0x2022))) ; • bullet (not in emoji range)
    (is (not (emoji/emoji? 0x1F2FF))) ; Just below emoji range
    (is (not (emoji/emoji? 0x1FA00))) ; Just above emoji range
    (is (not (emoji/emoji? 65))))     ; Decimal 'A'
  
  (testing "Edge cases"
    (is (not (emoji/emoji? 0)))       ; Zero
    (is (not (emoji/emoji? 0x25FF)))  ; Just below 0x2600
    (is (not (emoji/emoji? 0x2800)))  ; Just above 0x27BF
    (is (not (emoji/emoji? 0x12FF))))) ; Just below 0x1F300

(deftest test-extract-emoji-codepoints
  (testing "Basic emoji extraction"
    (is (= [128161] (emoji/extract-emoji-codepoints "💡"))) ; Lightbulb (U+1F4A1)
    (is (= [127919] (emoji/extract-emoji-codepoints "🎯"))) ; Target (U+1F3AF)
    (is (= [9888] (emoji/extract-emoji-codepoints "⚠️")))   ; Warning (with variation selector)
    (is (= [9989] (emoji/extract-emoji-codepoints "✅"))))  ; Check mark
  
  (testing "Non-emoji characters"
    (is (= [] (emoji/extract-emoji-codepoints "a")))      ; Regular letter
    (is (= [] (emoji/extract-emoji-codepoints "A")))      ; Capital letter
    (is (= [] (emoji/extract-emoji-codepoints "123")))    ; Numbers
    (is (= [] (emoji/extract-emoji-codepoints "Hello")))) ; Regular word
  
  (testing "Mixed content"
    ;; Note: This extracts only emoji codepoints, ignoring regular text
    (is (= [128161] (emoji/extract-emoji-codepoints "Hello 💡 world")))
    (is (= [128161 127919] (emoji/extract-emoji-codepoints "💡🎯")))
    (is (= [9989] (emoji/extract-emoji-codepoints "Status: ✅ complete"))))
  
  (testing "Empty and edge cases"
    (is (= [] (emoji/extract-emoji-codepoints "")))       ; Empty string
    (is (= [] (emoji/extract-emoji-codepoints " ")))      ; Space
    (is (= [] (emoji/extract-emoji-codepoints "•")))      ; Bullet (not emoji range)
    (is (= [] (emoji/extract-emoji-codepoints "!@#"))))  ; Symbols
  
  (testing "Surrogate pair handling"
    ;; Test proper surrogate pair processing
    (let [lightbulb "💡"
          target "🎯"]
      (is (= [128161] (emoji/extract-emoji-codepoints lightbulb)))
      (is (= [127919] (emoji/extract-emoji-codepoints target)))
      ;; Test multiple emoji with surrogate pairs
      (is (= [128161 127919] (emoji/extract-emoji-codepoints (str lightbulb target))))))
  
  (testing "Invalid surrogate pairs"
    ;; Test handling of malformed surrogate pairs
    ;; These would be constructed artificially since normal strings don't have invalid pairs
    (is (= [] (emoji/extract-emoji-codepoints (js/String.fromCharCode 0xD800)))) ; Orphaned high
    (is (= [] (emoji/extract-emoji-codepoints (js/String.fromCharCode 0xDC00))))) ; Orphaned low
  
  (testing "Multiple emoji extraction"
    (is (= [128161 127919 9888 9989] 
           (emoji/extract-emoji-codepoints "💡🎯⚠️✅")))
    (is (= [128161 127919] 
           (emoji/extract-emoji-codepoints "Status: 💡 Goal: 🎯")))))

(deftest test-contains-emoji?
  (testing "Text with emoji"
    (is (emoji/contains-emoji? "Hello 💡"))
    (is (emoji/contains-emoji? "💡"))
    (is (emoji/contains-emoji? "Status: ✅"))
    (is (emoji/contains-emoji? "Target 🎯 achieved")))
  
  (testing "Text without emoji"
    (is (not (emoji/contains-emoji? "Hello world")))
    (is (not (emoji/contains-emoji? "ABC123")))
    (is (not (emoji/contains-emoji? "Symbols: !@# •")))
    (is (not (emoji/contains-emoji? ""))))
  
  (testing "Edge cases"
    (is (not (emoji/contains-emoji? " ")))
    (is (not (emoji/contains-emoji? "\n\t")))
    (is (emoji/contains-emoji? "⚠️"))
    (is (emoji/contains-emoji? "Multiple 💡 emoji 🎯 here"))))

(deftest test-detect-emoji-in-text
  (testing "Single emoji detection"
    (let [result (emoji/detect-emoji-in-text "Hello 💡")]
      (is (= 1 (count result)))
      (is (= "💡" (:char (first result))))
      (is (= 6 (:start-index (first result))))
      (is (= 8 (:end-index (first result))))
      (is (= [128161] (:codepoints (first result))))))
  
  (testing "Multiple emoji detection"
    (let [result (emoji/detect-emoji-in-text "Status: ✅ Target: 🎯")]
      (is (= 2 (count result)))
      ;; First emoji: ✅
      (is (= "✅" (:char (first result))))
      (is (= 8 (:start-index (first result))))
      (is (= 9 (:end-index (first result))))
      (is (= [9989] (:codepoints (first result))))
      ;; Second emoji: 🎯
      (is (= "🎯" (:char (second result))))
      (is (= 18 (:start-index (second result))))
      (is (= 20 (:end-index (second result))))
      (is (= [127919] (:codepoints (second result))))))
  
  (testing "Adjacent emoji"
    (let [result (emoji/detect-emoji-in-text "💡🎯")]
      (is (= 2 (count result)))
      (is (= "💡" (:char (first result))))
      (is (= 0 (:start-index (first result))))
      (is (= 2 (:end-index (first result))))
      (is (= "🎯" (:char (second result))))
      (is (= 2 (:start-index (second result))))
      (is (= 4 (:end-index (second result))))))
  
  (testing "No emoji found"
    (is (= [] (emoji/detect-emoji-in-text "Hello world")))
    (is (= [] (emoji/detect-emoji-in-text "")))
    (is (= [] (emoji/detect-emoji-in-text "Symbols: !@# •"))))
  
  (testing "Emoji at boundaries"
    ;; Emoji at start
    (let [result (emoji/detect-emoji-in-text "💡 Hello")]
      (is (= 1 (count result)))
      (is (= 0 (:start-index (first result))))
      (is (= 2 (:end-index (first result)))))
    ;; Emoji at end
    (let [result (emoji/detect-emoji-in-text "Hello 💡")]
      (is (= 1 (count result)))
      (is (= 6 (:start-index (first result))))
      (is (= 8 (:end-index (first result)))))))

(deftest test-split-by-emoji
  (testing "Text with single emoji"
    (let [result (emoji/split-by-emoji "Hello 💡 world")]
      (is (= 3 (count result)))
      (is (= {:type :text :content "Hello "} (first result)))
      (is (= {:type :emoji :content "💡"} (second result)))
      (is (= {:type :text :content " world"} (nth result 2)))))
  
  (testing "Text with multiple emoji"
    (let [result (emoji/split-by-emoji "Status: ✅ Target: 🎯")]
      (is (= 4 (count result)))
      (is (= {:type :text :content "Status: "} (first result)))
      (is (= {:type :emoji :content "✅"} (second result)))
      (is (= {:type :text :content " Target: "} (nth result 2)))
      (is (= {:type :emoji :content "🎯"} (nth result 3)))))
  
  (testing "Adjacent emoji"
    (let [result (emoji/split-by-emoji "💡🎯")]
      (is (= 2 (count result)))
      (is (= {:type :emoji :content "💡"} (first result)))
      (is (= {:type :emoji :content "🎯"} (second result)))))
  
  (testing "Text with no emoji"
    (let [result (emoji/split-by-emoji "Hello world")]
      (is (= 1 (count result)))
      (is (= {:type :text :content "Hello world"} (first result)))))
  
  (testing "Empty string"
    (is (= [] (emoji/split-by-emoji ""))))
  
  (testing "Only emoji"
    (let [result (emoji/split-by-emoji "💡")]
      (is (= 1 (count result)))
      (is (= {:type :emoji :content "💡"} (first result)))))
  
  (testing "Emoji at boundaries"
    ;; Emoji at start
    (let [result (emoji/split-by-emoji "💡 Hello")]
      (is (= 2 (count result)))
      (is (= {:type :emoji :content "💡"} (first result)))
      (is (= {:type :text :content " Hello"} (second result))))
    ;; Emoji at end
    (let [result (emoji/split-by-emoji "Hello 💡")]
      (is (= 2 (count result)))
      (is (= {:type :text :content "Hello "} (first result)))
      (is (= {:type :emoji :content "💡"} (second result)))))
  
  (testing "Complex mixed content"
    (let [result (emoji/split-by-emoji "Task 💡 Done ✅ Next 🎯 Warning ⚠️")]
      (is (= 9 (count result)))
      (is (= {:type :text :content "Task "} (nth result 0)))
      (is (= {:type :emoji :content "💡"} (nth result 1)))
      (is (= {:type :text :content " Done "} (nth result 2)))
      (is (= {:type :emoji :content "✅"} (nth result 3)))
      (is (= {:type :text :content " Next "} (nth result 4)))
      (is (= {:type :emoji :content "🎯"} (nth result 5)))
      (is (= {:type :text :content " Warning "} (nth result 6)))
      (is (= {:type :emoji :content "⚠"} (nth result 7)))  ; Note: ⚠ without variation selector
      (is (= {:type :text :content "️"} (nth result 8))))))

(deftest test-unicode-to-filename
  (testing "Single codepoint conversion"
    (is (= "emoji_u1f4a1.png" (emoji/unicode-to-filename 128161))) ; 💡 lightbulb
    (is (= "emoji_u1f3af.png" (emoji/unicode-to-filename 127919))) ; 🎯 target
    (is (= "emoji_u26a0.png" (emoji/unicode-to-filename 9888)))    ; ⚠️ warning
    (is (= "emoji_u2705.png" (emoji/unicode-to-filename 9989)))    ; ✅ check mark
    (is (= "emoji_u2022.png" (emoji/unicode-to-filename 8226))))   ; • bullet
  
  (testing "Vector codepoint conversion"
    (is (= "emoji_u1f4a1.png" (emoji/unicode-to-filename [128161]))) ; Single in vector
    (is (= "emoji_u1f3af.png" (emoji/unicode-to-filename [127919]))))
  
  (testing "Multiple codepoints (composite emoji)"
    (is (= "emoji_u1f468_200d_1f4bb.png" 
           (emoji/unicode-to-filename [128104 8205 128187]))) ; 👨‍💻 man technologist
    (is (= "emoji_u1f44b_1f3fd.png" 
           (emoji/unicode-to-filename [128075 127997])))) ; 👋🏽 waving hand with skin tone
  
  (testing "Hex format validation"
    ;; Verify lowercase hex formatting
    (is (= "emoji_u41.png" (emoji/unicode-to-filename 65)))     ; 'A' -> 0x41
    (is (= "emoji_ua.png" (emoji/unicode-to-filename 10)))      ; 10 -> 0xa
    (is (= "emoji_uff.png" (emoji/unicode-to-filename 255))))   ; 255 -> 0xff
  
  (testing "Edge cases"
    (is (= "emoji_u0.png" (emoji/unicode-to-filename 0)))       ; Zero codepoint
    (is (= "emoji_u1fffff.png" (emoji/unicode-to-filename 2097151))))) ; Large codepoint

(deftest test-build-emoji-file-map
  (testing "Default known emoji map"
    (let [map (emoji/build-emoji-file-map)]
      (is (= "emoji_u1f4a1.png" (get map 128161))) ; 💡
      (is (= "emoji_u1f3af.png" (get map 127919))) ; 🎯
      (is (= "emoji_u26a0.png" (get map 9888)))    ; ⚠️
      (is (= "emoji_u2705.png" (get map 9989)))    ; ✅
      (is (= "emoji_u2022.png" (get map 8226)))    ; •
      (is (= 5 (count map)))))                      ; Only predefined entries
  
  (testing "Extended map with additional codepoints"
    (let [additional [65 66 67]  ; A, B, C
          map (emoji/build-emoji-file-map additional)]
      ;; Should contain original entries
      (is (= "emoji_u1f4a1.png" (get map 128161)))
      ;; Should contain new entries
      (is (= "emoji_u41.png" (get map 65)))        ; A
      (is (= "emoji_u42.png" (get map 66)))        ; B  
      (is (= "emoji_u43.png" (get map 67)))        ; C
      (is (= 8 (count map)))))                     ; 5 original + 3 new
  
  (testing "Empty additional codepoints"
    (let [map (emoji/build-emoji-file-map [])]
      (is (= 5 (count map)))                       ; Only original entries
      (is (= "emoji_u1f4a1.png" (get map 128161))))))

(deftest test-emoji-filename
  (testing "Known emoji characters"
    (is (= "emoji_u1f4a1.png" (emoji/emoji-filename "💡"))) ; Lightbulb
    (is (= "emoji_u1f3af.png" (emoji/emoji-filename "🎯"))) ; Target
    (is (= "emoji_u26a0.png" (emoji/emoji-filename "⚠")))   ; Warning (without variation selector)
    (is (= "emoji_u2705.png" (emoji/emoji-filename "✅")))  ; Check mark
    (is (= "emoji_u2022.png" (emoji/emoji-filename "•"))))  ; Bullet
  
  (testing "Unknown emoji characters"
    ;; These should generate filenames using unicode-to-filename
    (let [result (emoji/emoji-filename "🌟")] ; Star (not in known map)
      (is (some? result))
      (is (.startsWith result "emoji_u"))
      (is (.endsWith result ".png"))))
  
  (testing "Non-emoji characters"
    (is (nil? (emoji/emoji-filename "a")))      ; Regular letter
    (is (nil? (emoji/emoji-filename "A")))      ; Capital letter
    (is (nil? (emoji/emoji-filename "123")))    ; Numbers
    (is (nil? (emoji/emoji-filename ""))))      ; Empty string
  
  (testing "Multi-codepoint emoji"
    ;; These should generate composite filenames
    (let [warning-with-variation (emoji/emoji-filename "⚠️")] ; ⚠ + ️
      (is (some? warning-with-variation))
      (is (.startsWith warning-with-variation "emoji_u"))
      (is (clojure.string/includes? warning-with-variation "_"))  ; Should have underscore for multiple codepoints
      (is (.endsWith warning-with-variation ".png"))))
  
  (testing "Fallback behavior"
    ;; Test that unknown emoji still get reasonable filenames
    (let [unknown-emoji-filename (emoji/emoji-filename "🦄")] ; Unicorn
      (is (some? unknown-emoji-filename))
      (is (= "emoji_u1f984.png" unknown-emoji-filename))))  ; Should generate correct hex
  
  (testing "Edge cases"
    (is (nil? (emoji/emoji-filename nil)))      ; Nil input
    (is (nil? (emoji/emoji-filename " ")))      ; Space
    (is (nil? (emoji/emoji-filename "\n")))))   ; Newline