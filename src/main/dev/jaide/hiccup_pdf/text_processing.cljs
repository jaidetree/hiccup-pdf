(ns dev.jaide.hiccup-pdf.text-processing
  "Text processing functions for mixed content rendering with emoji support.

  This namespace handles segmentation of text into alternating text and emoji
  parts for mixed content rendering in PDF documents."
  (:require
   [clojure.string :as s]
   [dev.jaide.hiccup-pdf.emoji :as emoji]
   [dev.jaide.hiccup-pdf.images :as images]))

(defn segment-text
  "Takes text string and returns vector of segment maps with type and content.

  Segments text into consecutive chunks of pure text or single emoji.
  Each segment includes its original position in the source text.

  Args:
    text-content: String to segment

  Returns:
    Vector of segment maps: [{:type :text/:emoji :content \"...\" :start-idx N :end-idx N}]

  Example:
    (segment-text \"Hello 💡 world\") =>
    [{:type :text :content \"Hello \" :start-idx 0 :end-idx 6}
     {:type :emoji :content \"💡\" :start-idx 6 :end-idx 8}
     {:type :text :content \" world\" :start-idx 8 :end-idx 14}]"
  [text-content]
  (if (empty? text-content)
    []
    (let [emoji-positions (emoji/detect-emoji-in-text text-content)]
      (if (empty? emoji-positions)
        ;; No emoji found - return entire text as single segment
        [{:type :text :content text-content :start-idx 0 :end-idx (count text-content)}]
        ;; Build segments alternating between text and emoji
        (loop [current-pos 0
               emoji-list emoji-positions
               segments []]
          (if (empty? emoji-list)
            ;; No more emoji - add remaining text if any
            (if (< current-pos (count text-content))
              (conj segments {:type :text
                              :content (.substring text-content current-pos)
                              :start-idx current-pos
                              :end-idx (count text-content)})
              segments)
            ;; Process next emoji
            (let [emoji-info (first emoji-list)
                  emoji-start (:start-index emoji-info)
                  emoji-end (:end-index emoji-info)
                  remaining-emoji (rest emoji-list)]
              (if (< current-pos emoji-start)
                ;; Add text segment before emoji
                (let [text-segment {:type :text
                                    :content (.substring text-content current-pos emoji-start)
                                    :start-idx current-pos
                                    :end-idx emoji-start}
                      emoji-segment {:type :emoji
                                     :content (:char emoji-info)
                                     :start-idx emoji-start
                                     :end-idx emoji-end}]
                  (recur emoji-end remaining-emoji (conj segments text-segment emoji-segment)))
                ;; No text before emoji - just add emoji
                (let [emoji-segment {:type :emoji
                                     :content (:char emoji-info)
                                     :start-idx emoji-start
                                     :end-idx emoji-end}]
                  (recur emoji-end remaining-emoji (conj segments emoji-segment)))))))))))

(defn prepare-mixed-content
  "Processes segments and adds rendering metadata like widths and positions.

  Takes segments from segment-text and enhances them with rendering information
  needed for PDF generation, such as calculated widths and positioning data.

  Args:
    segments: Vector of segment maps from segment-text
    options: Optional map with rendering options
      :font-size - Font size for width calculations (default 12)
      :base-x - Starting x position (default 0)
      :base-y - Starting y position (default 0)

  Returns:
    Vector of enhanced segment maps with additional metadata:
    {:type :text/:emoji :content \"...\" :start-idx N :end-idx N
     :x N :y N :width N :height N}"
  [segments & [options]]
  (let [opts (merge {:font-size 12 :base-x 0 :base-y 0} options)
        font-size (:font-size opts)
        base-x (:base-x opts)
        base-y (:base-y opts)]
    (loop [segments-remaining segments
           current-x base-x
           enhanced-segments []]
      (if (empty? segments-remaining)
        enhanced-segments
        (let [segment (first segments-remaining)
              remaining (rest segments-remaining)]
          (case (:type segment)
            :text
            ;; Estimate text width (rough approximation: 0.6 * font-size per character)
            (let [char-count (count (:content segment))
                  estimated-width (* char-count (* font-size 0.6))
                  enhanced-segment (assoc segment
                                          :x current-x
                                          :y base-y
                                          :width estimated-width
                                          :height font-size)]
              (recur remaining
                     (+ current-x estimated-width)
                     (conj enhanced-segments enhanced-segment)))

            :emoji
            ;; Emoji width equals font size (square aspect ratio)
            (let [enhanced-segment (assoc segment
                                          :x current-x
                                          :y base-y
                                          :width font-size
                                          :height font-size)]
              (recur remaining
                     (+ current-x font-size)
                     (conj enhanced-segments enhanced-segment)))))))))

(defn validate-segments
  "Ensures segmentation is complete and non-overlapping.

  Validates that segments cover the entire input text without gaps or overlaps.
  Returns validation result with any errors found.

  Args:
    segments: Vector of segment maps
    original-text: Original text string that was segmented

  Returns:
    Map with validation results:
    {:valid? boolean :errors [error-strings] :coverage {:expected N :actual N}}"
  [segments original-text]
  (let [total-length (count original-text)
        _errors []

        ;; Check for gaps and overlaps
        sorted-segments (sort-by :start-idx segments)

        ;; Validate coverage
        coverage-errors
        (loop [segs sorted-segments
               expected-pos 0
               errs []]
          (if (empty? segs)
            ;; Check if we covered the entire text
            (if (< expected-pos total-length)
              (conj errs (str "Missing coverage from position " expected-pos " to " total-length))
              errs)
            (let [seg (first segs)
                  start-idx (:start-idx seg)
                  end-idx (:end-idx seg)
                  remaining (rest segs)]
              (cond
                ;; Gap before this segment
                (< expected-pos start-idx)
                (recur remaining start-idx
                       (conj errs (str "Gap from position " expected-pos " to " start-idx)))

                ;; Overlap with previous segment
                (> expected-pos start-idx)
                (recur remaining end-idx
                       (conj errs (str "Overlap: segment starts at " start-idx " but expected " expected-pos)))

                ;; Normal case - segment fits perfectly
                :else
                (recur remaining end-idx errs)))))

        ;; Validate content reconstruction
        reconstructed-text (apply str (map :content segments))
        content-errors (if (= original-text reconstructed-text)
                         []
                         [(str "Content mismatch: original length " (count original-text)
                               ", reconstructed length " (count reconstructed-text))])

        all-errors (concat coverage-errors content-errors)]

    {:valid? (empty? all-errors)
     :errors all-errors
     :coverage {:expected total-length
                :actual (if (empty? sorted-segments)
                          0
                          (:end-idx (last sorted-segments)))}}))

;; Mixed Content PDF Operator Generation

(defn render-text-segment
  "Generates PDF text operators for text segments.

  Creates proper PDF text operators (BT/ET blocks) for text content
  with font specification, positioning, and text rendering.

  Args:
    segment: Map with :content, :x, :y from prepare-mixed-content
    font-name: Font name (e.g., \"Arial\", \"Times-Roman\")
    font-size: Font size in points
    color: Optional color string (e.g., \"black\", \"#FF0000\")

  Returns:
    String with PDF text operators"
  [segment font-name font-size & [color]]
  (let [content (:content segment)
        x (:x segment)
        y (:y segment)
        ;; Escape special characters in PDF text
        escaped-content (-> content
                            (clojure.string/replace #"\\" "\\\\")
                            (clojure.string/replace #"\(" "\\(")
                            (clojure.string/replace #"\)" "\\)"))
        ;; Color setup if specified
        color-ops (if color
                    (case color
                      "black" "0 0 0 rg\n"
                      "red" "1 0 0 rg\n"
                      "green" "0 1 0 rg\n"
                      "blue" "0 0 1 rg\n"
                      "0 0 0 rg\n") ;; Default to black
                    "")]
    (str "BT\n"
         color-ops
         "/" font-name " " font-size " Tf\n"
         x " " y " Td\n"
         "(" escaped-content ") Tj\n"
         "ET\n")))

(defn render-image-segment
  "Generates PDF image operators for emoji segments.

  Creates proper PDF image operators (q/cm/Do/Q blocks) for emoji images
  with scaling, positioning, and baseline alignment.

  Args:
    segment: Map with :content, :x, :y from prepare-mixed-content
    font-size: Font size for scaling calculations
    xobject-ref: XObject reference name (e.g., \"Em1\")
    baseline-offset: Optional baseline adjustment ratio (default 0.2)

  Returns:
    String with PDF image operators"
  [segment font-size xobject-ref & [baseline-offset]]
  (let [x (:x segment)
        y (:y segment)
        offset-ratio (or baseline-offset 0.2)
        ;; Calculate transform for scaling 72x72 image to font size
        scale-factor (/ font-size 72.0)
        ;; Baseline offset (negative for PDF coordinate system)
        y-offset (* font-size offset-ratio -1)
        final-x x
        final-y (+ y y-offset)]
    (str "q\n"  ; Save graphics state
         scale-factor " 0 0 " scale-factor " " final-x " " final-y " cm\n"  ; Transform matrix
         "/" xobject-ref " Do\n"  ; Draw XObject
         "Q\n")))  ; Restore graphics state

(defn calculate-segment-positions
  "Computes x,y positions for each segment with proper spacing.

  Calculates precise positioning for mixed content segments, accounting for
  text metrics and image dimensions for proper alignment.

  Args:
    segments: Vector of segment maps from segment-text
    base-x: Starting x position
    base-y: Starting y position
    font-size: Font size for calculations
    options: Optional map with:
      :char-width-ratio - Character width ratio (default 0.6)
      :image-spacing - Additional spacing around images (default 0)
      :text-spacing - Additional spacing between text segments (default 0)

  Returns:
    Vector of segments with updated :x, :y, :width, :height"
  [segments base-x base-y font-size & [options]]
  (let [opts (merge {:char-width-ratio 0.6 :image-spacing 0 :text-spacing 0} options)
        char-width-ratio (:char-width-ratio opts)
        image-spacing (:image-spacing opts)
        text-spacing (:text-spacing opts)]
    (loop [remaining-segments segments
           current-x base-x
           positioned-segments []]
      (if (empty? remaining-segments)
        positioned-segments
        (let [segment (first remaining-segments)
              rest-segments (rest remaining-segments)]
          (case (:type segment)
            :text
            (let [content (:content segment)
                  char-count (count content)
                  text-width (* char-count (* font-size char-width-ratio))
                  positioned-segment (assoc segment
                                            :x current-x
                                            :y base-y
                                            :width text-width
                                            :height font-size)
                  next-x (+ current-x text-width text-spacing)]
              (recur rest-segments next-x (conj positioned-segments positioned-segment)))

            :emoji
            (let [image-width font-size  ; Square aspect ratio
                  positioned-segment (assoc segment
                                            :x current-x
                                            :y base-y
                                            :width image-width
                                            :height font-size)
                  next-x (+ current-x image-width image-spacing)]
              (recur rest-segments next-x (conj positioned-segments positioned-segment)))))))))

(defn render-mixed-segments
  "Combines text and image segments into complete PDF operator string.

  Processes all segments and generates the complete PDF content stream
  operators for mixed text and emoji content.

  Args:
    segments: Vector of positioned segment maps
    font-name: Font name for text segments
    font-size: Font size for both text and image scaling
    image-cache: Image cache atom for emoji loading
    options: Optional map with:
      :color - Text color (default \"black\")
      :baseline-offset - Image baseline adjustment (default 0.2)
      :fallback-strategy - Emoji fallback strategy (default :hex-string)
      :xobject-refs - Map from emoji characters to XObject references

  Returns:
    String with complete PDF operators for mixed content"
  [segments font-name font-size image-cache & [options]]
  (let [opts (merge {:color "black" :baseline-offset 0.2 :fallback-strategy :hex-string :xobject-refs {}} options)
        color (:color opts)
        baseline-offset (:baseline-offset opts)
        fallback-strategy (:fallback-strategy opts)
        xobject-refs (:xobject-refs opts)]
    (loop [remaining-segments segments
           operators []]
      (if (empty? remaining-segments)
        (s/join "" operators)
        (let [segment (first remaining-segments)
              rest-segments (rest remaining-segments)]
          (case (:type segment)
            :text
            (let [text-ops (render-text-segment segment font-name font-size color)]
              (recur rest-segments (conj operators text-ops)))

            :emoji
            (let [emoji-char (:content segment)
                  xobject-ref (get xobject-refs emoji-char)]
              (if xobject-ref
                ;; Render as image
                (let [image-ops (render-image-segment segment font-size xobject-ref baseline-offset)]
                  (recur rest-segments (conj operators image-ops)))
                ;; Fallback to text rendering
                (let [fallback-result (images/emoji-image-with-fallback image-cache emoji-char
                                                                        {:fallback-strategy fallback-strategy})
                      text-ops (case (:type fallback-result)
                                 :hex-string 
                                 ;; For hex strings, render directly as hex content
                                 (str "BT\n"
                                      (when (and color (not= color "black"))
                                        (str (cond 
                                               (= color "red") "1 0 0 rg"
                                               (= color "green") "0 1 0 rg"
                                               (= color "blue") "0 0 1 rg"
                                               :else "0 0 0 rg") "\n"))
                                      "/" font-name " " font-size " Tf\n"
                                      (:x segment) " " (:y segment) " Td\n"
                                      (:content fallback-result) " Tj\n"
                                      "ET\n")
                                 
                                 :placeholder
                                 ;; For placeholders, render as escaped text
                                 (let [placeholder-segment (assoc segment :content (:content fallback-result) :type :text)]
                                   (render-text-segment placeholder-segment font-name font-size color))
                                 
                                 :skip
                                 ;; For skip, render nothing
                                 ""
                                 
                                 ;; Default fallback - render original emoji as text
                                 (let [default-segment (assoc segment :content emoji-char :type :text)]
                                   (render-text-segment default-segment font-name font-size color)))]
                  (recur rest-segments (conj operators text-ops)))))))))))

(defn process-mixed-content
  "Main function for processing mixed text and emoji content into PDF operators.

  Combines all the mixed content processing steps into a single function
  that handles segmentation, positioning, and PDF operator generation.

  Args:
    text-content: String with mixed text and emoji
    base-x: Starting x position
    base-y: Starting y position
    font-name: Font name for text rendering
    font-size: Font size for both text and images
    image-cache: Image cache atom for emoji loading
    options: Optional map with all rendering options

  Returns:
    Map with processing results:
    {:operators string :segments vector :success boolean :errors [strings]}"
  [text-content base-x base-y font-name font-size image-cache & [options]]
  (try
    (let [;; Step 1: Segment text into text/emoji parts
          segments (segment-text text-content)

          ;; Step 2: Validate segmentation
          validation (validate-segments segments text-content)

          ;; Continue if validation passes
          result (if (:valid? validation)
                   (let [;; Step 3: Calculate positions
                         positioned-segments (calculate-segment-positions segments base-x base-y font-size options)

                         ;; Step 4: Generate PDF operators
                         operators (render-mixed-segments positioned-segments font-name font-size image-cache options)]
                     {:operators operators
                      :segments positioned-segments
                      :success true
                      :errors []})

                   ;; Validation failed
                   {:operators ""
                    :segments []
                    :success false
                    :errors (:errors validation)})]
      result)

    (catch js/Error e
      {:operators ""
       :segments []
       :success false
       :errors [(str "Mixed content processing failed: " (.-message e))]})))

(defn create-xobject-reference-map
  "Creates a mapping from emoji characters to XObject references.

  Scans text content for unique emoji characters and generates XObject
  references for each one, suitable for use in PDF resource dictionaries.

  Args:
    text-content: String with mixed text and emoji
    starting-ref-number: Optional starting reference number (default 1)

  Returns:
    Map from emoji character to XObject reference name
    e.g., {\"💡\" \"Em1\", \"🎯\" \"Em2\"}"
  [text-content & [starting-ref-number]]
  (let [start-num (or starting-ref-number 1)
        segments (segment-text text-content)
        emoji-chars (distinct (map :content (filter #(= :emoji (:type %)) segments)))]
    (into {} (map-indexed (fn [idx emoji-char]
                            [emoji-char (str "Em" (+ start-num idx))])
                          emoji-chars))))

(defn extract-unique-emoji
  "Extracts unique emoji characters from text content.

  Scans text and returns a set of unique emoji characters found,
  useful for pre-loading images and generating resource dictionaries.

  Args:
    text-content: String with mixed text and emoji

  Returns:
    Set of unique emoji character strings"
  [text-content]
  (let [segments (segment-text text-content)
        emoji-segments (filter #(= :emoji (:type %)) segments)]
    (set (map :content emoji-segments))))
