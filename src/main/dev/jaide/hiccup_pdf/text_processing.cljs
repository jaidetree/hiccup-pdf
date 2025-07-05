(ns dev.jaide.hiccup-pdf.text-processing
  "Text processing functions for mixed content rendering with emoji support.
  
  This namespace handles segmentation of text into alternating text and emoji
  parts for mixed content rendering in PDF documents."
  (:require [dev.jaide.hiccup-pdf.emoji :as emoji]))

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
        errors []
        
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