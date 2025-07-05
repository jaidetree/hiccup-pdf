(ns dev.jaide.hiccup-pdf.emoji
  "Unicode emoji processing for hiccup-pdf library.
  
  This namespace provides functions for detecting, extracting, and processing 
  Unicode emoji characters in text content for PDF generation."
  (:require [clojure.string :as str]))

(defn surrogate-pair?
  "Determines if a character code is part of a UTF-16 surrogate pair.
  
  Args:
    char-code: Integer character code
    
  Returns:
    :high if high surrogate (0xD800-0xDBFF)
    :low if low surrogate (0xDC00-0xDFFF)  
    nil if not a surrogate"
  [char-code]
  (cond
    (and (>= char-code 0xD800) (<= char-code 0xDBFF)) :high
    (and (>= char-code 0xDC00) (<= char-code 0xDFFF)) :low
    :else nil))

(defn emoji?
  "Predicate function that determines if a character is an emoji using Unicode ranges.
  
  Supports emoji in ranges:
  - 0x1F300-0x1F9FF (Miscellaneous Symbols and Pictographs, Transport and Map Symbols, etc.)
  - 0x2600-0x26FF (Miscellaneous Symbols)
  - 0x2700-0x27BF (Dingbats)
  
  Args:
    char-code: Integer character code
    
  Returns:
    Boolean true if character is an emoji"
  [char-code]
  (or (and (>= char-code 0x1F300) (<= char-code 0x1F9FF))
      (and (>= char-code 0x2600) (<= char-code 0x26FF))
      (and (>= char-code 0x2700) (<= char-code 0x27BF))))

(defn extract-emoji-codepoints
  "Extracts Unicode codepoints from emoji character string, handling surrogate pairs correctly.
  
  Takes an emoji character string and returns vector of Unicode codepoints as decimal integers.
  Properly handles UTF-16 surrogate pairs for 4-byte Unicode characters.
  
  Args:
    emoji-char: String containing emoji character(s)
    
  Returns:
    Vector of Unicode codepoints as decimal integers, or empty vector if no emoji found
    
  Example:
    (extract-emoji-codepoints \"💡\") => [128161]
    (extract-emoji-codepoints \"🎯\") => [127919]  
    (extract-emoji-codepoints \"a\") => []"
  [emoji-char]
  (if (empty? emoji-char)
    []
    (loop [i 0
           codepoints []]
      (if (>= i (count emoji-char))
        codepoints
        (let [char-code (.charCodeAt emoji-char i)]
          (case (surrogate-pair? char-code)
            :high
            ;; High surrogate - look for low surrogate
            (if (< (+ i 1) (count emoji-char))
              (let [low-surrogate (.charCodeAt emoji-char (+ i 1))]
                (if (= (surrogate-pair? low-surrogate) :low)
                  ;; Valid surrogate pair - calculate actual codepoint
                  (let [codepoint (+ 0x10000 
                                     (bit-shift-left (- char-code 0xD800) 10)
                                     (- low-surrogate 0xDC00))]
                    (if (emoji? codepoint)
                      (recur (+ i 2) (conj codepoints codepoint))
                      (recur (+ i 2) codepoints)))
                  ;; Invalid surrogate pair
                  (recur (+ i 1) codepoints)))
              ;; High surrogate at end of string
              (recur (+ i 1) codepoints))
            
            :low
            ;; Orphaned low surrogate - skip
            (recur (+ i 1) codepoints)
            
            ;; Regular character
            (if (emoji? char-code)
              (recur (+ i 1) (conj codepoints char-code))
              (recur (+ i 1) codepoints))))))))

(defn contains-emoji?
  "Simple predicate to check if text contains any emoji.
  
  Args:
    text: String to check for emoji
    
  Returns:
    Boolean true if text contains at least one emoji character"
  [text]
  (not (empty? (extract-emoji-codepoints text))))

(defn detect-emoji-in-text
  "Scans text and returns vector of maps with emoji info.
  
  Each emoji is represented as:
  {:char \"💡\" :start-index 5 :end-index 7 :codepoints [128161]}
  
  Args:
    text-content: String to scan for emoji
    
  Returns:
    Vector of emoji info maps with character, position, and codepoints"
  [text-content]
  (if (empty? text-content)
    []
    (loop [i 0
           emoji-found []]
      (if (>= i (count text-content))
        emoji-found
        (let [char-code (.charCodeAt text-content i)]
          (case (surrogate-pair? char-code)
            :high
            ;; High surrogate - look for low surrogate  
            (if (< (+ i 1) (count text-content))
              (let [low-surrogate (.charCodeAt text-content (+ i 1))]
                (if (= (surrogate-pair? low-surrogate) :low)
                  ;; Valid surrogate pair - calculate actual codepoint
                  (let [codepoint (+ 0x10000 
                                     (bit-shift-left (- char-code 0xD800) 10)
                                     (- low-surrogate 0xDC00))]
                    (if (emoji? codepoint)
                      (let [emoji-char (.substring text-content i (+ i 2))
                            emoji-info {:char emoji-char
                                        :start-index i
                                        :end-index (+ i 2)
                                        :codepoints [codepoint]}]
                        (recur (+ i 2) (conj emoji-found emoji-info)))
                      (recur (+ i 2) emoji-found)))
                  ;; Invalid surrogate pair
                  (recur (+ i 1) emoji-found)))
              ;; High surrogate at end of string
              (recur (+ i 1) emoji-found))
            
            :low
            ;; Orphaned low surrogate - skip
            (recur (+ i 1) emoji-found)
            
            ;; Regular character
            (if (emoji? char-code)
              (let [emoji-char (.substring text-content i (+ i 1))
                    emoji-info {:char emoji-char
                                :start-index i
                                :end-index (+ i 1)
                                :codepoints [char-code]}]
                (recur (+ i 1) (conj emoji-found emoji-info)))
              (recur (+ i 1) emoji-found))))))))

(defn split-by-emoji
  "Splits text into alternating segments of regular text and emoji.
  
  Returns vector of maps with :type (:text or :emoji) and :content.
  
  Args:
    text-content: String to split
    
  Returns:
    Vector of segment maps: [{:type :text :content \"Hello \"} 
                             {:type :emoji :content \"💡\"}
                             {:type :text :content \" world\"}]"
  [text-content]
  (if (empty? text-content)
    []
    (let [emoji-positions (detect-emoji-in-text text-content)]
      (if (empty? emoji-positions)
        ;; No emoji found - return entire text as single segment
        [{:type :text :content text-content}]
        ;; Build segments alternating between text and emoji
        (loop [current-pos 0
               emoji-list emoji-positions
               segments []]
          (if (empty? emoji-list)
            ;; No more emoji - add remaining text if any
            (if (< current-pos (count text-content))
              (conj segments {:type :text :content (.substring text-content current-pos)})
              segments)
            ;; Process next emoji
            (let [emoji-info (first emoji-list)
                  emoji-start (:start-index emoji-info)
                  emoji-end (:end-index emoji-info)
                  remaining-emoji (rest emoji-list)]
              (if (< current-pos emoji-start)
                ;; Add text segment before emoji
                (let [text-segment {:type :text :content (.substring text-content current-pos emoji-start)}
                      emoji-segment {:type :emoji :content (:char emoji-info)}]
                  (recur emoji-end remaining-emoji (conj segments text-segment emoji-segment)))
                ;; No text before emoji - just add emoji
                (let [emoji-segment {:type :emoji :content (:char emoji-info)}]
                  (recur emoji-end remaining-emoji (conj segments emoji-segment)))))))))))

;; Filename mapping functionality for Noto emoji files

(def ^:private known-emoji-map
  "Predefined mapping from Unicode codepoints to Noto emoji filenames.
  
  Maps decimal codepoints to their corresponding PNG filenames in the
  emojis/noto-72/ directory following Noto naming convention."
  {128161 "emoji_u1f4a1.png"  ; 💡 lightbulb
   127919 "emoji_u1f3af.png"  ; 🎯 target  
   9888   "emoji_u26a0.png"   ; ⚠️ warning sign
   9989   "emoji_u2705.png"   ; ✅ check mark
   8226   "emoji_u2022.png"}) ; • bullet point

(defn unicode-to-filename
  "Converts Unicode codepoint(s) to Noto emoji filename format.
  
  Follows Noto emoji naming convention: emoji_u{codepoint}.png (lowercase hex).
  
  Args:
    codepoints: Single codepoint integer or vector of codepoints
    
  Returns:
    String filename in Noto format, or nil if conversion fails
    
  Example:
    (unicode-to-filename 127825) => \"emoji_u1f4a1.png\"
    (unicode-to-filename [127825]) => \"emoji_u1f4a1.png\""
  [codepoints]
  (let [codes (if (vector? codepoints) codepoints [codepoints])]
    (if (= 1 (count codes))
      ;; Single codepoint - convert to lowercase hex
      (let [codepoint (first codes)
            hex-string (.toString codepoint 16)]
        (str "emoji_u" (.toLowerCase hex-string) ".png"))
      ;; Multiple codepoints - join with underscores (for composite emoji)
      (let [hex-parts (map #(.toLowerCase (.toString % 16)) codes)]
        (str "emoji_u" (str/join "_" hex-parts) ".png")))))

(defn build-emoji-file-map
  "Creates a mapping from common emoji codepoints to their filenames.
  
  Includes predefined mappings for known emoji and generates filenames
  for additional codepoints using the standard Noto convention.
  
  Args:
    additional-codepoints: Optional vector of additional codepoints to include
    
  Returns:
    Map from codepoint integers to filename strings"
  ([] known-emoji-map)
  ([additional-codepoints]
   (let [additional-map (into {} (map (fn [cp] [cp (unicode-to-filename cp)]) 
                                      additional-codepoints))]
     (merge known-emoji-map additional-map))))

(defn emoji-filename
  "Main function that takes emoji character and returns corresponding PNG filename.
  
  First extracts Unicode codepoints from the emoji character, then looks up
  or generates the appropriate Noto emoji filename. Also handles special cases
  like bullet character that have PNG files but aren't technically emoji.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    String filename or nil if emoji not supported
    
  Example:
    (emoji-filename \"💡\") => \"emoji_u1f4a1.png\"
    (emoji-filename \"🎯\") => \"emoji_u1f3af.png\"
    (emoji-filename \"•\") => \"emoji_u2022.png\""
  [emoji-char]
  (let [codepoints (extract-emoji-codepoints emoji-char)]
    (if (empty? codepoints)
      ;; No emoji codepoints found - check for special cases
      (if (= emoji-char "•")  ; Bullet character special case
        "emoji_u2022.png"
        nil)
      (if (= 1 (count codepoints))
        ;; Single emoji - check known map first, then generate
        (let [codepoint (first codepoints)]
          (or (get known-emoji-map codepoint)
              (unicode-to-filename codepoint)))
        ;; Multiple codepoints - generate composite filename
        (unicode-to-filename codepoints)))))