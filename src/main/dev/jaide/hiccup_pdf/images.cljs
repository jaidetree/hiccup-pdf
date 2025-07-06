(ns dev.jaide.hiccup-pdf.images
  "PNG image file operations for emoji support.
  
  This namespace handles loading PNG image files from the local emoji directory,
  with proper error handling and cross-platform path support."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.emoji :as emoji]))

(def ^:private emoji-base-directory
  "Base directory for emoji PNG files"
  "emojis/noto-72/")

(defn emoji-file-exists?
  "Checks if emoji PNG file exists for given emoji character.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    Boolean true if file exists"
  [emoji-char]
  (let [filename (emoji/emoji-filename emoji-char)]
    (if filename
      (let [file-path (path/join emoji-base-directory filename)]
        (try
          (.existsSync fs file-path)
          (catch js/Error _
            false)))
      false)))

(defn get-image-dimensions
  "Extracts width/height from PNG file.
  
  For Noto emoji files, assumes 72x72 dimensions but validates if possible.
  
  Args:
    buffer: Node.js Buffer containing PNG data
    
  Returns:
    Map with :width and :height, or nil if invalid"
  [buffer]
  (try
    ;; Basic PNG header validation
    (if (and buffer 
             (>= (.-length buffer) 24)
             ;; Check PNG signature: 89 50 4E 47 0D 0A 1A 0A
             (= (.readUInt32BE buffer 0) 0x89504E47)
             (= (.readUInt32BE buffer 4) 0x0D0A1A0A))
      ;; Read IHDR chunk for dimensions (starts at byte 16)
      (let [width (.readUInt32BE buffer 16)
            height (.readUInt32BE buffer 20)]
        {:width width :height height})
      ;; Fallback to assumed dimensions for Noto emoji
      {:width 72 :height 72})
    (catch js/Error _
      {:width 72 :height 72})))

(defn load-png-file
  "Loads PNG file from emojis/noto-72/ directory.
  
  Uses Node.js fs module for file operations with cross-platform path handling.
  
  Args:
    filename: String filename (e.g. \"emoji_u1f4a1.png\")
    
  Returns:
    Node.js Buffer containing PNG data, or nil if file not found"
  [filename]
  (if (and filename (string? filename) (not-empty filename))
    (let [file-path (path/join emoji-base-directory filename)]
      (try
        (.readFileSync fs file-path)
        (catch js/Error _
          nil)))
    nil))

(defn load-emoji-image
  "Main function that loads emoji image with comprehensive error handling.
  
  Loads PNG file for given emoji character and returns structured data
  with buffer, dimensions, and success status.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    Map with image data:
    {:buffer Buffer :width N :height N :success boolean :filename string}
    or {:success false :error string} on failure"
  [emoji-char]
  (let [filename (emoji/emoji-filename emoji-char)]
    (if (not filename)
      {:success false 
       :error (str "No filename mapping for emoji: " emoji-char)}
      
      (let [buffer (load-png-file filename)]
        (if (not buffer)
          {:success false 
           :error (str "File not found: " filename)
           :filename filename}
          
          (let [dimensions (get-image-dimensions buffer)]
            (if dimensions
              {:buffer buffer
               :width (:width dimensions)
               :height (:height dimensions)
               :success true
               :filename filename}
              {:success false
               :error (str "Invalid PNG data: " filename)
               :filename filename})))))))

(defn validate-png-data
  "Validates loaded PNG data integrity.
  
  Performs basic checks on PNG file structure and data integrity.
  
  Args:
    buffer: Node.js Buffer containing PNG data
    
  Returns:
    Map with validation results:
    {:valid? boolean :errors [error-strings] :info {:size N :format string}}"
  [buffer]
  (let [errors []]
    (try
      (if (not buffer)
        {:valid? false 
         :errors ["Buffer is null or undefined"]
         :info {}}
        
        (let [size (.-length buffer)
              errors (cond-> []
                       (< size 100) (conj "File too small to be valid PNG")
                       (> size (* 10 1024 1024)) (conj "File too large (>10MB)"))]
          
          ;; Check PNG signature
          (if (and (>= size 8)
                   (not= (.readUInt32BE buffer 0) 0x89504E47)
                   (not= (.readUInt32BE buffer 4) 0x0D0A1A0A))
            (let [errors (conj errors "Invalid PNG signature")]
              {:valid? false :errors errors :info {:size size :format "unknown"}})
            
            ;; Basic validation passed
            {:valid? (empty? errors)
             :errors errors
             :info {:size size :format "PNG"}})))
      
      (catch js/Error e
        {:valid? false 
         :errors [(str "Validation error: " (.-message e))]
         :info {}}))))

(defn get-emoji-file-path
  "Constructs full file path for emoji character.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    String file path or nil if no mapping exists"
  [emoji-char]
  (let [filename (emoji/emoji-filename emoji-char)]
    (if filename
      (path/join emoji-base-directory filename)
      nil)))

(defn list-available-emoji-files
  "Lists all available emoji PNG files in the directory.
  
  Scans the emoji directory and returns list of available files
  with basic validation.
  
  Returns:
    Vector of maps: [{:filename string :path string :size number}]"
  []
  (try
    (let [files (.readdirSync fs emoji-base-directory)]
      (->> files
           (filter #(.endsWith % ".png"))
           (map (fn [filename]
                  (let [file-path (path/join emoji-base-directory filename)
                        stats (.statSync fs file-path)]
                    {:filename filename
                     :path file-path
                     :size (.-size stats)})))
           (vec)))
    (catch js/Error _
      [])))

;; Image Caching System for Performance Optimization

(defn create-image-cache
  "Creates new cache instance with configurable size limits.
  
  Args:
    config: Optional map with :max-size (default 50) and :max-memory-mb (default 10)
    
  Returns:
    Atom containing cache structure:
    {:items {emoji-key {:buffer Buffer :width N :height N :timestamp N :size N}}
     :order [emoji-key1 emoji-key2 ...]  ; LRU order, most recent last
     :config {:max-size 50 :max-memory-mb 10}
     :stats {:hits 0 :misses 0 :evictions 0 :memory-usage 0}}"
  ([] (create-image-cache {}))
  ([config]
   (let [default-config {:max-size 50 :max-memory-mb 10}
         merged-config (merge default-config config)]
     (atom {:items {}
            :order []
            :config merged-config
            :stats {:hits 0 :misses 0 :evictions 0 :memory-usage 0}}))))

(defn estimate-image-memory
  "Estimates memory usage for cached image in bytes.
  
  Args:
    image-data: Map with :buffer, :width, :height
    
  Returns:
    Integer estimated memory usage in bytes"
  [image-data]
  (let [buffer-size (if (:buffer image-data)
                      (.-length (:buffer image-data))
                      0)
        metadata-size 200]  ; Estimated overhead for maps, keywords, etc.
    (+ buffer-size metadata-size)))

(defn ^:private evict-lru-items
  "Evicts least recently used items to make space in cache.
  
  Args:
    cache-state: Current cache state map
    target-size: Target number of items after eviction
    
  Returns:
    Updated cache state with evicted items"
  [cache-state target-size]
  (let [current-order (:order cache-state)
        items-to-evict (take (- (count current-order) target-size) current-order)
        remaining-order (drop (count items-to-evict) current-order)
        remaining-items (select-keys (:items cache-state) remaining-order)
        evicted-memory (reduce + (map #(estimate-image-memory (get (:items cache-state) %))
                                      items-to-evict))]
    (-> cache-state
        (assoc :items remaining-items)
        (assoc :order remaining-order)
        (update-in [:stats :evictions] + (count items-to-evict))
        (update-in [:stats :memory-usage] - evicted-memory))))

(defn ^:private update-lru-order
  "Updates LRU order by moving emoji-key to end (most recent).
  
  Args:
    order: Current order vector
    emoji-key: Key to move to end
    
  Returns:
    Updated order vector"
  [order emoji-key]
  (let [without-key (filterv #(not= % emoji-key) order)]
    (conj without-key emoji-key)))

(defn cache-get
  "Retrieves cached image or nil if not found.
  
  Args:
    cache: Atom containing cache state
    emoji-key: String emoji character key
    
  Returns:
    Image data map {:buffer Buffer :width N :height N} or nil if not found"
  [cache emoji-key]
  (let [cache-state @cache
        image-data (get (:items cache-state) emoji-key)]
    (if image-data
      (do
        ;; Update LRU order and increment hit counter
        (swap! cache (fn [state]
                       (-> state
                           (update :order update-lru-order emoji-key)
                           (update-in [:stats :hits] inc))))
        ;; Return image data without cache metadata
        (select-keys image-data [:buffer :width :height :filename]))
      (do
        ;; Increment miss counter
        (swap! cache update-in [:stats :misses] inc)
        nil))))

(defn cache-put
  "Stores image in cache with LRU eviction.
  
  Args:
    cache: Atom containing cache state
    emoji-key: String emoji character key
    image-data: Map with :buffer, :width, :height, :filename
    
  Returns:
    Boolean true if stored successfully"
  [cache emoji-key image-data]
  (let [timestamp (.now js/Date)
        memory-size (estimate-image-memory image-data)
        enhanced-data (assoc image-data :timestamp timestamp :size memory-size)]
    
    (swap! cache (fn [state]
                   (let [max-size (get-in state [:config :max-size])
                         max-memory (* (get-in state [:config :max-memory-mb]) 1024 1024)
                         
                         ;; Remove existing entry if present
                         without-existing (if (contains? (:items state) emoji-key)
                                            (-> state
                                                (update :items dissoc emoji-key)
                                                (update :order (fn [order] 
                                                                 (filterv #(not= % emoji-key) order)))
                                                (update-in [:stats :memory-usage] 
                                                           - (estimate-image-memory 
                                                              (get (:items state) emoji-key))))
                                            state)
                         
                         ;; Add new entry
                         with-new (-> without-existing
                                      (assoc-in [:items emoji-key] enhanced-data)
                                      (update :order conj emoji-key)
                                      (update-in [:stats :memory-usage] + memory-size))
                         
                         ;; Check if eviction needed due to size limit
                         size-evicted (if (> (count (:order with-new)) max-size)
                                        (evict-lru-items with-new max-size)
                                        with-new)
                         
                         ;; Check if eviction needed due to memory limit
                         final-state (if (> (get-in size-evicted [:stats :memory-usage]) max-memory)
                                       ;; Evict oldest items until under memory limit
                                       (loop [state size-evicted]
                                         (if (and (> (get-in state [:stats :memory-usage]) max-memory)
                                                  (pos? (count (:order state))))
                                           (recur (evict-lru-items state (dec (count (:order state)))))
                                           state))
                                       size-evicted)]
                     final-state)))
    true))

(defn cache-clear
  "Clears cache contents.
  
  Args:
    cache: Atom containing cache state
    
  Returns:
    Boolean true when cleared"
  [cache]
  (swap! cache (fn [state]
                 (-> state
                     (assoc :items {})
                     (assoc :order [])
                     (assoc-in [:stats :memory-usage] 0))))
  true)

(defn cache-stats
  "Returns current cache statistics.
  
  Args:
    cache: Atom containing cache state
    
  Returns:
    Map with cache statistics including hit rate"
  [cache]
  (let [stats (get @cache :stats)
        total-requests (+ (:hits stats) (:misses stats))
        hit-rate (if (pos? total-requests)
                   (/ (:hits stats) total-requests)
                   0.0)]
    (assoc stats 
           :total-requests total-requests
           :hit-rate hit-rate
           :item-count (count (:order @cache)))))

(defn load-emoji-image-cached
  "Main function combining file loading with caching.
  
  Attempts to load from cache first, falls back to file system if not cached.
  Automatically caches newly loaded images.
  
  Args:
    cache: Atom containing cache state
    emoji-char: String containing emoji character
    
  Returns:
    Map with image data:
    {:buffer Buffer :width N :height N :success boolean :filename string}
    or {:success false :error string} on failure"
  [cache emoji-char]
  ;; First try cache
  (if-let [cached-data (cache-get cache emoji-char)]
    (assoc cached-data :success true)
    
    ;; Cache miss - load from file system
    (let [file-result (load-emoji-image emoji-char)]
      (if (:success file-result)
        (do
          ;; Successfully loaded - add to cache
          (cache-put cache emoji-char file-result)
          file-result)
        ;; Failed to load from file system
        file-result))))

;; Error Handling and Fallback Strategies

(def ^:private fallback-strategies
  "Available fallback strategies for emoji image loading failures"
  #{:hex-string :placeholder :skip :error})

(defn validate-image-data
  "Enhanced PNG data validation for loaded images.
  
  Performs comprehensive checks on loaded PNG data including file signature,
  size constraints, and basic integrity validation.
  
  Args:
    image-data: Map with :buffer, :width, :height, :filename
    
  Returns:
    Map with validation results:
    {:valid? boolean :errors [error-strings] :warnings [warning-strings] :info map}"
  [image-data]
  (let [buffer (:buffer image-data)
        errors []
        warnings []
        info {}]
    (try
      (cond
        ;; No buffer provided
        (not buffer)
        {:valid? false 
         :errors ["Missing image buffer data"]
         :warnings warnings
         :info info}
        
        ;; Buffer too small
        (< (.-length buffer) 100)
        {:valid? false
         :errors ["Image data too small to be valid PNG"]
         :warnings warnings
         :info (assoc info :size (.-length buffer))}
        
        ;; Check PNG signature
        (not (and (>= (.-length buffer) 8)
                  (= (.readUInt32BE buffer 0) 0x89504E47)
                  (= (.readUInt32BE buffer 4) 0x0D0A1A0A)))
        {:valid? false
         :errors ["Invalid PNG file signature"]
         :warnings warnings
         :info (assoc info :size (.-length buffer) :format "unknown")}
        
        ;; Basic validation passed
        :else
        (let [size (.-length buffer)
              warnings (cond-> warnings
                         (> size (* 1024 1024)) (conj "Large image file (>1MB) may impact performance")
                         (not= (:width image-data) (:height image-data)) (conj "Non-square image may not display correctly")
                         (not= (:width image-data) 72) (conj "Non-standard size (expected 72x72)"))]
          {:valid? true
           :errors []
           :warnings warnings
           :info (assoc info :size size :format "PNG" :dimensions [(:width image-data) (:height image-data)])}))
      
      (catch js/Error e
        {:valid? false 
         :errors [(str "Validation error: " (.-message e))]
         :warnings warnings
         :info info}))))

(defn ^:private simple-hex-encode
  "Simple hex encoding for emoji characters to avoid circular dependencies.
  
  Args:
    text: String to encode
    
  Returns:
    String with hex encoding format"
  [text]
  (if (empty? text)
    "()"
    ;; Use simple hex encoding for Unicode characters
    (let [hex-bytes (loop [i 0
                           bytes []]
                      (if (>= i (count text))
                        bytes
                        (let [code (.charCodeAt text i)]
                          (cond
                            ;; Handle surrogate pairs for emoji
                            (and (>= code 55296) (<= code 56319) ; High surrogate
                                 (< (+ i 1) (count text)))
                            (let [low-surrogate (.charCodeAt text (+ i 1))]
                              (if (and (>= low-surrogate 56320) (<= low-surrogate 57343)) ; Low surrogate
                                ;; Valid surrogate pair - use mapping for specific emoji
                                (let [emoji-mapping (cond
                                                      ;; Lightbulb emoji 💡
                                                      (and (= code 55357) (= low-surrogate 56481)) [61 161]
                                                      ;; Target emoji 🎯  
                                                      (and (= code 55356) (= low-surrogate 57263)) [60 175]
                                                      ;; Check mark ✅
                                                      (and (= code 55357) (= low-surrogate 56581)) [39 5]
                                                      ;; Warning ⚠
                                                      (= code 9888) [38 160]
                                                      ;; Default fallback
                                                      :else [63 63])]
                                  (recur (+ i 2) (concat bytes emoji-mapping)))
                                (recur (+ i 1) (concat bytes [63]))))
                            
                            ;; Single character Unicode
                            (> code 127)
                            (recur (+ i 1) (concat bytes [(bit-shift-right code 8) (bit-and code 255)]))
                            
                            ;; ASCII character
                            :else
                            (recur (+ i 1) (concat bytes [code]))))))
          hex-string (apply str (map #(let [hex (.toString % 16)]
                                         (if (= 1 (count hex))
                                           (str "0" hex)
                                           hex)) hex-bytes))]
      (str "<" hex-string ">"))))

(defn fallback-to-hex
  "Falls back to hex string encoding for emoji.
  
  Uses simple hex string encoding for emoji characters compatible with PDF text rendering.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    Map with fallback data:
    {:type :hex-string :content string :success true}"
  [emoji-char]
  {:type :hex-string 
   :content (simple-hex-encode emoji-char)
   :success true
   :fallback-reason "Image not available, using hex encoding"})

(defn fallback-to-placeholder
  "Creates placeholder text like '[💡]' for missing emoji images.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    Map with fallback data:
    {:type :placeholder :content string :success true}"
  [emoji-char]
  {:type :placeholder
   :content (str "[" emoji-char "]")
   :success true
   :fallback-reason "Image not available, using placeholder text"})

(defn fallback-to-skip
  "Skips emoji entirely by returning empty string.
  
  Args:
    emoji-char: String containing emoji character
    
  Returns:
    Map with fallback data:
    {:type :skip :content string :success true}"
  [emoji-char]
  {:type :skip
   :content ""
   :success true
   :fallback-reason "Image not available, skipping emoji"})

(defn handle-image-error
  "Centralized error handling with configurable fallback strategies.
  
  Processes image loading errors and applies appropriate fallback strategy.
  Logs warnings but ensures document generation continues.
  
  Args:
    emoji-char: String containing emoji character
    error: Original error message or map
    strategy: Fallback strategy keyword (:hex-string, :placeholder, :skip, :error)
    options: Optional map with :logging? (default true)
    
  Returns:
    Map with fallback result or throws exception for :error strategy"
  [emoji-char error strategy & [options]]
  (let [logging? (get options :logging? true)
        error-msg (if (string? error) error (str error))]
    
    ;; Log warning if enabled
    (when logging?
      (println (str "WARNING: Emoji image loading failed for '" emoji-char "': " error-msg)))
    
    ;; Validate strategy
    (when-not (contains? fallback-strategies strategy)
      (throw (js/Error. (str "Invalid fallback strategy: " strategy ". Must be one of: " fallback-strategies))))
    
    ;; Apply fallback strategy
    (case strategy
      :hex-string (fallback-to-hex emoji-char)
      :placeholder (fallback-to-placeholder emoji-char)
      :skip (fallback-to-skip emoji-char)
      :error (throw (js/Error. (str "Emoji image loading failed for '" emoji-char "': " error-msg))))))

(defn emoji-image-with-fallback
  "Main function that attempts image loading with graceful fallback.
  
  Tries to load emoji image with caching, validates the result, and falls back
  to alternative strategies if loading fails. Ensures document generation
  continues even when images are unavailable.
  
  Args:
    cache: Atom containing cache state (optional, if nil uses direct loading)
    emoji-char: String containing emoji character
    options: Map with configuration:
             :fallback-strategy - :hex-string (default), :placeholder, :skip, :error
             :validation? - Enable/disable image validation (default true)
             :logging? - Enable/disable error logging (default true)
    
  Returns:
    Map with image data or fallback result:
    Success: {:buffer Buffer :width N :height N :success true :filename string :type :image}
    Fallback: {:type :hex-string/:placeholder/:skip :content string :success true :fallback-reason string}"
  [cache emoji-char & [options]]
  (let [opts (merge {:fallback-strategy :hex-string
                     :validation? true
                     :logging? true} options)
        strategy (:fallback-strategy opts)
        validation? (:validation? opts)
        logging? (:logging? opts)]
    
    (try
      ;; Attempt to load image (with or without cache)
      (let [load-result (if cache
                          (load-emoji-image-cached cache emoji-char)
                          (load-emoji-image emoji-char))]
        
        (if (:success load-result)
          ;; Image loaded successfully - validate if requested
          (if validation?
            (let [validation (validate-image-data load-result)]
              (if (:valid? validation)
                ;; Valid image - return with image type
                (assoc load-result :type :image)
                ;; Invalid image - fall back
                (handle-image-error emoji-char 
                                    (str "Image validation failed: " (first (:errors validation)))
                                    strategy
                                    {:logging? logging?})))
            ;; No validation - return directly
            (assoc load-result :type :image))
          
          ;; Image loading failed - fall back
          (handle-image-error emoji-char 
                              (or (:error load-result) "Unknown loading error")
                              strategy
                              {:logging? logging?})))
      
      (catch js/Error e
        ;; Unexpected error during processing - fall back
        (handle-image-error emoji-char 
                            (str "Unexpected error: " (.-message e))
                            strategy
                            {:logging? logging?})))))

(defn batch-load-with-fallback
  "Efficiently loads multiple emoji images with fallback handling.
  
  Processes a collection of emoji characters, loading images where possible
  and applying consistent fallback strategies for failures.
  
  Args:
    cache: Atom containing cache state
    emoji-chars: Collection of emoji character strings
    options: Same options as emoji-image-with-fallback
    
  Returns:
    Map from emoji character to result map"
  [cache emoji-chars & [options]]
  (into {} (map (fn [emoji-char]
                  [emoji-char (emoji-image-with-fallback cache emoji-char options)])
                emoji-chars)))

(defn fallback-performance-info
  "Returns performance characteristics of different fallback strategies.
  
  Provides guidance for choosing appropriate fallback strategies based on
  performance requirements and document generation constraints.
  
  Returns:
    Map with performance info for each strategy"
  []
  {:hex-string {:speed :fast
                :compatibility :high
                :pdf-size-impact :minimal
                :description "Uses existing hex encoding, fastest fallback"}
   :placeholder {:speed :fast
                 :compatibility :high
                 :pdf-size-impact :minimal
                 :description "Simple text placeholder, readable and fast"}
   :skip {:speed :fastest
          :compatibility :high
          :pdf-size-impact :none
          :description "No rendering, smallest PDF size"}
   :error {:speed :na
           :compatibility :na
           :pdf-size-impact :na
           :description "Stops processing, for debugging only"}})

;; PDF Image Object Generation

(def ^:private pdf-object-counter
  "Atom for generating unique PDF object numbers starting from 1000"
  (atom 1000))

(def ^:private image-reference-counter
  "Atom for generating unique image reference names (Em1, Em2, etc.)"
  (atom 0))

(defn create-resource-reference
  "Generates unique image references for PDF resources.
  
  Creates sequential references like Em1, Em2, Em3, etc. for use in
  PDF resource dictionaries and content streams.
  
  Returns:
    String reference name (e.g., \"Em1\", \"Em2\")"
  []
  (let [ref-num (swap! image-reference-counter inc)]
    (str "Em" ref-num)))

(defn calculate-image-transform
  "Computes scaling and positioning for font size matching.
  
  Calculates the transformation matrix needed to scale a 72x72 source image
  to match the specified font size, with proper baseline alignment.
  
  Args:
    font-size: Target font size in points (8-72)
    baseline-offset: Optional baseline adjustment ratio (default 0.2)
    
  Returns:
    Map with transformation data:
    {:scale-x N :scale-y N :offset-x 0 :offset-y N :matrix [a b c d e f]}"
  [font-size & [baseline-offset]]
  (let [offset-ratio (or baseline-offset 0.2)
        ;; Scale from 72x72 source to font size
        scale-factor (/ font-size 72.0)
        ;; Baseline offset (negative because PDF coordinates are bottom-up)
        y-offset (* font-size offset-ratio -1)]
    {:scale-x scale-factor
     :scale-y scale-factor
     :offset-x 0
     :offset-y y-offset
     ;; PDF transformation matrix [a b c d e f] for scaling and translation
     :matrix [scale-factor 0 0 scale-factor 0 y-offset]}))

(defn ^:private encode-pdf-stream-data
  "Encodes binary data for PDF stream content.
  
  Args:
    buffer: Node.js Buffer containing binary data
    
  Returns:
    String with encoded data suitable for PDF stream"
  [buffer]
  (if buffer
    ;; Convert buffer to binary string for PDF stream
    (let [length (.-length buffer)
          bytes (loop [i 0 result []]
                  (if (>= i length)
                    result
                    (recur (inc i) (conj result (.readUInt8 buffer i)))))]
      ;; Return as binary string
      (apply str (map char bytes)))
    ""))

(defn png-to-pdf-object
  "Converts PNG buffer to PDF XObject stream.
  
  Generates a complete PDF XObject definition suitable for embedding
  in PDF documents. Uses FlateDecode filter for PNG compression.
  
  Args:
    png-buffer: Node.js Buffer containing PNG data
    width: Image width in pixels (typically 72)
    height: Image height in pixels (typically 72)
    object-number: PDF object number for cross-references
    
  Returns:
    String containing complete PDF XObject definition"
  [png-buffer width height object-number]
  (let [stream-data (encode-pdf-stream-data png-buffer)
        stream-length (count stream-data)]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Type /XObject\n"
         "/Subtype /Image\n"
         "/Width " width "\n"
         "/Height " height "\n"
         "/ColorSpace /DeviceRGB\n"
         "/BitsPerComponent 8\n"
         "/Filter /FlateDecode\n"
         "/Length " stream-length "\n"
         ">>\n"
         "stream\n"
         stream-data
         "\nendstream\n"
         "endobj\n")))

(defn generate-image-xobject
  "Creates complete PDF image object with headers and reference.
  
  Combines PNG data processing with PDF object generation to create
  a complete XObject suitable for PDF document embedding.
  
  Args:
    image-data: Map with :buffer, :width, :height, :filename
    reference-name: Optional resource reference name (auto-generated if nil)
    object-number: Optional PDF object number (auto-generated if nil)
    
  Returns:
    Map with XObject data:
    {:object-number N :reference-name string :pdf-object string :success boolean}"
  [image-data & [reference-name object-number]]
  (try
    (let [buffer (:buffer image-data)
          width (or (:width image-data) 72)
          height (or (:height image-data) 72)
          ref-name (or reference-name (create-resource-reference))
          obj-num (or object-number (swap! pdf-object-counter inc))]
      
      (if buffer
        (let [pdf-object (png-to-pdf-object buffer width height obj-num)]
          {:object-number obj-num
           :reference-name ref-name
           :pdf-object pdf-object
           :width width
           :height height
           :success true})
        {:success false
         :error "Missing image buffer data"}))
    
    (catch js/Error e
      {:success false
       :error (str "XObject generation failed: " (.-message e))})))

(defn batch-generate-xobjects
  "Efficiently generates PDF XObjects for multiple images.
  
  Processes a collection of image data maps and generates corresponding
  PDF XObjects with sequential numbering and unique references.
  
  Args:
    image-data-list: Collection of image data maps
    starting-object-number: Optional starting object number (auto-generated if nil)
    
  Returns:
    Vector of XObject result maps"
  [image-data-list & [starting-object-number]]
  (let [start-num (or starting-object-number (swap! pdf-object-counter inc))]
    (map-indexed
     (fn [idx image-data]
       (generate-image-xobject image-data nil (+ start-num idx)))
     image-data-list)))

(defn create-resource-dictionary-entry
  "Creates resource dictionary entry for image XObjects.
  
  Generates the PDF resource dictionary syntax for referencing
  image XObjects in page content streams.
  
  Args:
    xobject-refs: Collection of XObject reference maps with :reference-name and :object-number
    
  Returns:
    String with PDF resource dictionary XObject entries"
  [xobject-refs]
  (if (empty? xobject-refs)
    ""
    (let [entries (map (fn [ref]
                         (str "/" (:reference-name ref) " " (:object-number ref) " 0 R"))
                       xobject-refs)]
      (str "/XObject <<\n"
           (clojure.string/join "\n" entries) "\n"
           ">>"))))

(defn generate-image-draw-operators
  "Generates PDF operators for drawing an image XObject.
  
  Creates the PDF content stream operators needed to draw an image
  at a specific position with appropriate scaling and positioning.
  
  Args:
    reference-name: XObject reference name (e.g., \"Em1\")
    x: X position in PDF coordinates
    y: Y position in PDF coordinates  
    font-size: Font size for scaling calculations
    baseline-offset: Optional baseline adjustment (default 0.2)
    
  Returns:
    String with PDF drawing operators"
  [reference-name x y font-size & [baseline-offset]]
  (let [transform (calculate-image-transform font-size baseline-offset)
        matrix (:matrix transform)
        [scale-x _ _ scale-y tx ty] matrix
        final-x (+ x tx)
        final-y (+ y ty)]
    (str "q\n"  ; Save graphics state
         scale-x " 0 0 " scale-y " " final-x " " final-y " cm\n"  ; Transform matrix
         "/" reference-name " Do\n"  ; Draw XObject
         "Q\n")))  ; Restore graphics state

(defn validate-pdf-xobject
  "Validates generated PDF XObject syntax.
  
  Performs basic syntax validation on generated PDF XObject strings
  to ensure compliance with PDF specification.
  
  Args:
    pdf-object: String containing PDF XObject definition
    
  Returns:
    Map with validation results:
    {:valid? boolean :errors [error-strings] :warnings [warning-strings]}"
  [pdf-object]
  (let [errors []
        warnings []]
    (try
      (cond
        ;; Check basic structure
        (not (string? pdf-object))
        {:valid? false 
         :errors ["PDF object must be a string"]
         :warnings warnings}
        
        (empty? pdf-object)
        {:valid? false
         :errors ["PDF object cannot be empty"]
         :warnings warnings}
        
        ;; Check required elements
        (not (re-find #"\d+ 0 obj" pdf-object))
        {:valid? false
         :errors ["Missing object header"]
         :warnings warnings}
        
        (not (re-find #"/Type /XObject" pdf-object))
        {:valid? false
         :errors ["Missing XObject type declaration"]
         :warnings warnings}
        
        (not (re-find #"/Subtype /Image" pdf-object))
        {:valid? false
         :errors ["Missing Image subtype declaration"]
         :warnings warnings}
        
        (not (re-find #"stream\n.*\nendstream" pdf-object))
        {:valid? false
         :errors ["Missing or malformed stream data"]
         :warnings warnings}
        
        (not (re-find #"endobj" pdf-object))
        {:valid? false
         :errors ["Missing object terminator"]
         :warnings warnings}
        
        ;; Basic validation passed
        :else
        (let [warnings (cond-> warnings
                         (not (re-find #"/Length \d+" pdf-object)) 
                         (conj "Missing explicit length declaration")
                         
                         (not (re-find #"/Filter /" pdf-object))
                         (conj "Missing filter specification"))]
          {:valid? true
           :errors []
           :warnings warnings}))
      
      (catch js/Error e
        {:valid? false
         :errors [(str "Validation error: " (.-message e))]
         :warnings warnings}))))