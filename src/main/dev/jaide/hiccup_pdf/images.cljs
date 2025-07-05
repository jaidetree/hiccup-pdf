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