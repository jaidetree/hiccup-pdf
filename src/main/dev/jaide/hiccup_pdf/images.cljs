(ns dev.jaide.hiccup-pdf.images
  "PNG image file operations for emoji support.
  
  This namespace handles loading PNG image files from the local emoji directory,
  with proper error handling and cross-platform path support."
  (:require ["node:fs" :as fs]
            ["node:path" :as path]
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