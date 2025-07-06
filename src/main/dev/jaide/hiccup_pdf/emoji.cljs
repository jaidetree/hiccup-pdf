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
  (seq (extract-emoji-codepoints text)))

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

;; Configuration System for Emoji Image Features

(def default-emoji-config
  "Default configuration map with sensible defaults for emoji image features.
  
  Configuration options:
  - :enable-emoji-images - Master switch for emoji image processing (default false)
  - :emoji-directory - Directory path for PNG emoji files (default \"emojis/noto-72/\")
  - :fallback-strategy - Strategy when images unavailable (:hex-string, :placeholder, :skip, :error)
  - :cache-size - Maximum number of cached images (default 50)
  - :cache-memory-mb - Memory limit for image cache in MB (default 10)
  - :baseline-adjust - Baseline offset ratio for image alignment (default 0.2)
  - :min-font-size - Minimum font size for image emoji rendering (default 8)
  - :max-font-size - Maximum font size for image emoji rendering (default 72)
  - :spacing-adjust - Horizontal spacing multiplier around images (default 1.0)
  - :debug - Enable debug logging for emoji processing (default false)
  - :validation? - Enable image validation (default true)
  - :logging? - Enable error/warning logging (default true)"
  {:enable-emoji-images false           ; Feature flag - must be explicitly enabled
   :emoji-directory "emojis/noto-72/"   ; PNG file location
   :fallback-strategy :hex-string       ; :hex-string, :placeholder, :skip, :error
   :cache-size 50                       ; Max cached images
   :cache-memory-mb 10                  ; Memory limit
   :baseline-adjust 0.2                 ; Baseline offset ratio
   :min-font-size 8                     ; Min size for image emoji
   :max-font-size 72                    ; Max size for image emoji
   :spacing-adjust 1.0                  ; Horizontal spacing multiplier
   :debug false                         ; Debug logging
   :validation? true                    ; Enable image validation
   :logging? true})                     ; Enable error/warning logging

(def ^:private valid-fallback-strategies
  "Set of valid fallback strategy keywords"
  #{:hex-string :placeholder :skip :error})

(defn validate-emoji-config
  "Validates user configuration options with helpful error messages.
  
  Performs comprehensive validation of emoji configuration options and returns
  detailed error information for invalid values.
  
  Args:
    config: Map with user configuration options
    
  Returns:
    Map with validation results:
    {:valid? boolean 
     :errors [error-strings] 
     :warnings [warning-strings]
     :normalized-config map}"
  [config]
  (let [errors []
        warnings []]
    (try
      (let [;; Validate enable-emoji-images
            errors (if (contains? config :enable-emoji-images)
                     (if (boolean? (:enable-emoji-images config))
                       errors
                       (conj errors ":enable-emoji-images must be true or false"))
                     errors)
            
            ;; Validate emoji-directory
            errors (if (contains? config :emoji-directory)
                     (if (string? (:emoji-directory config))
                       (if (not-empty (:emoji-directory config))
                         errors
                         (conj errors ":emoji-directory cannot be empty"))
                       (conj errors ":emoji-directory must be a string"))
                     errors)
            
            ;; Validate fallback-strategy
            errors (if (contains? config :fallback-strategy)
                     (if (contains? valid-fallback-strategies (:fallback-strategy config))
                       errors
                       (conj errors (str ":fallback-strategy must be one of: " valid-fallback-strategies)))
                     errors)
            
            ;; Validate cache-size
            errors (if (contains? config :cache-size)
                     (if (and (number? (:cache-size config)) (pos? (:cache-size config)))
                       errors
                       (conj errors ":cache-size must be a positive number"))
                     errors)
            
            ;; Validate cache-memory-mb
            errors (if (contains? config :cache-memory-mb)
                     (if (and (number? (:cache-memory-mb config)) (pos? (:cache-memory-mb config)))
                       errors
                       (conj errors ":cache-memory-mb must be a positive number"))
                     errors)
            
            ;; Validate baseline-adjust
            errors (if (contains? config :baseline-adjust)
                     (if (number? (:baseline-adjust config))
                       errors
                       (conj errors ":baseline-adjust must be a number"))
                     errors)
            
            ;; Validate min-font-size
            errors (if (contains? config :min-font-size)
                     (if (and (number? (:min-font-size config)) (pos? (:min-font-size config)))
                       errors
                       (conj errors ":min-font-size must be a positive number"))
                     errors)
            
            ;; Validate max-font-size
            errors (if (contains? config :max-font-size)
                     (if (and (number? (:max-font-size config)) (pos? (:max-font-size config)))
                       errors
                       (conj errors ":max-font-size must be a positive number"))
                     errors)
            
            ;; Validate font size relationship
            errors (if (and (contains? config :min-font-size) 
                           (contains? config :max-font-size)
                           (number? (:min-font-size config))
                           (number? (:max-font-size config)))
                     (if (< (:min-font-size config) (:max-font-size config))
                       errors
                       (conj errors ":min-font-size must be less than :max-font-size"))
                     errors)
            
            ;; Validate spacing-adjust
            errors (if (contains? config :spacing-adjust)
                     (if (and (number? (:spacing-adjust config)) (>= (:spacing-adjust config) 0))
                       errors
                       (conj errors ":spacing-adjust must be a non-negative number"))
                     errors)
            
            ;; Validate debug flag
            errors (if (contains? config :debug)
                     (if (boolean? (:debug config))
                       errors
                       (conj errors ":debug must be true or false"))
                     errors)
            
            ;; Validate validation flag
            errors (if (contains? config :validation?)
                     (if (boolean? (:validation? config))
                       errors
                       (conj errors ":validation? must be true or false"))
                     errors)
            
            ;; Validate logging flag
            errors (if (contains? config :logging?)
                     (if (boolean? (:logging? config))
                       errors
                       (conj errors ":logging? must be true or false"))
                     errors)
            
            ;; Generate warnings for potentially problematic values
            warnings (cond-> warnings
                       (and (contains? config :cache-size) (> (:cache-size config) 200))
                       (conj "Large cache-size may impact memory usage")
                       
                       (and (contains? config :cache-memory-mb) (> (:cache-memory-mb config) 100))
                       (conj "Large cache-memory-mb may impact system performance")
                       
                       (and (contains? config :baseline-adjust) 
                            (or (< (:baseline-adjust config) -0.5) (> (:baseline-adjust config) 0.5)))
                       (conj "Extreme baseline-adjust values may cause poor alignment")
                       
                       (and (contains? config :min-font-size) (< (:min-font-size config) 6))
                       (conj "Very small min-font-size may result in illegible emoji")
                       
                       (and (contains? config :max-font-size) (> (:max-font-size config) 144))
                       (conj "Very large max-font-size may impact performance"))]
        
        {:valid? (empty? errors)
         :errors errors
         :warnings warnings
         :normalized-config config})
      
      (catch js/Error e
        {:valid? false
         :errors [(str "Configuration validation error: " (.-message e))]
         :warnings []
         :normalized-config {}}))))

(defn merge-emoji-config
  "Merges user config with defaults, validating the result.
  
  Takes user configuration options and merges them with sensible defaults,
  ensuring all required options are present and valid.
  
  Args:
    user-config: Map with user-specified configuration options (can be nil or empty)
    
  Returns:
    Map with merged configuration or throws error for invalid config
    
  Example:
    (merge-emoji-config {:enable-emoji-images true :cache-size 100})
    => {:enable-emoji-images true :cache-size 100 :emoji-directory \"emojis/noto-72/\" ...}"
  [user-config]
  (let [user-opts (or user-config {})
        ;; Validate user config first
        validation (validate-emoji-config user-opts)]
    (if (:valid? validation)
      ;; Merge with defaults
      (let [merged-config (merge default-emoji-config user-opts)
            final-validation (validate-emoji-config merged-config)]
        (if (:valid? final-validation)
          merged-config
          (throw (js/Error. (str "Invalid merged configuration: " 
                                (clojure.string/join ", " (:errors final-validation)))))))
      ;; User config invalid
      (throw (js/Error. (str "Invalid emoji configuration: " 
                            (clojure.string/join ", " (:errors validation))))))))

(defn emoji-config-enabled?
  "Checks if emoji images are enabled in configuration.
  
  Convenience function to check the master emoji images feature flag.
  
  Args:
    config: Emoji configuration map
    
  Returns:
    Boolean true if emoji images should be processed"
  [config]
  (boolean (get config :enable-emoji-images false)))

(defn get-emoji-config-value
  "Gets a configuration value with fallback to default.
  
  Safe accessor for configuration values that provides defaults
  when configuration is missing or invalid.
  
  Args:
    config: Emoji configuration map (can be nil)
    key: Configuration key to retrieve
    
  Returns:
    Configuration value or default value for the key"
  [config key]
  (let [safe-config (or config default-emoji-config)]
    (get safe-config key (get default-emoji-config key))))

(defn create-emoji-config
  "Creates a validated emoji configuration from user options.
  
  Convenience function for creating emoji configurations with validation
  and helpful error messages.
  
  Args:
    options: Map with emoji configuration options
    
  Returns:
    Validated and merged emoji configuration map
    
  Example:
    (create-emoji-config {:enable-emoji-images true
                          :fallback-strategy :placeholder
                          :cache-size 75})"
  [options]
  (merge-emoji-config options))

(defn emoji-config->cache-config
  "Converts emoji config to image cache configuration.
  
  Extracts cache-specific configuration options for creating image caches.
  
  Args:
    emoji-config: Emoji configuration map
    
  Returns:
    Map with cache configuration options"
  [emoji-config]
  {:max-size (get emoji-config :cache-size 50)
   :max-memory-mb (get emoji-config :cache-memory-mb 10)})

(defn print-emoji-config
  "Prints emoji configuration in a readable format.
  
  Utility function for debugging and configuration inspection.
  
  Args:
    config: Emoji configuration map
    
  Returns:
    String representation of configuration"
  [config]
  (let [safe-config (or config default-emoji-config)]
    (str "Emoji Configuration:\n"
         "  Enabled: " (emoji-config-enabled? safe-config) "\n"
         "  Directory: " (get safe-config :emoji-directory) "\n"
         "  Fallback: " (get safe-config :fallback-strategy) "\n"
         "  Cache Size: " (get safe-config :cache-size) "\n"
         "  Cache Memory: " (get safe-config :cache-memory-mb) "MB\n"
         "  Baseline Adjust: " (get safe-config :baseline-adjust) "\n"
         "  Font Size Range: " (get safe-config :min-font-size) "-" (get safe-config :max-font-size) "\n"
         "  Spacing Adjust: " (get safe-config :spacing-adjust) "\n"
         "  Debug: " (get safe-config :debug) "\n"
         "  Validation: " (get safe-config :validation?) "\n"
         "  Logging: " (get safe-config :logging?))))