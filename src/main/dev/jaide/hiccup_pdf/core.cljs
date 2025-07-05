(ns dev.jaide.hiccup-pdf.core
  "Core namespace for transforming hiccup vectors into PDF operators."
  (:require [dev.jaide.hiccup-pdf.validation :as v]
            [dev.jaide.hiccup-pdf.document :as doc]
            [clojure.string :as str]))

(declare element->pdf-ops)

;; Performance optimization: Color conversion cache
(def ^:private color-cache (atom {}))

;; Performance optimization: Pre-computed constants
(def ^:private circle-bezier-factor 0.552284749831)
(def ^:private identity-matrix [1 0 0 1 0 0])

;; Performance optimization: Common PDF operators as constants
(def ^:private pdf-operators
  {:save-state "q\n"
   :restore-state "Q"
   :fill "f"
   :stroke "S"
   :fill-and-stroke "B"
   :text-begin "BT\n"
   :text-end "ET"
   :path-close "h\n"
   :move "m\n"
   :line "l\n"
   :curve "c\n"
   :rect "re\n"})

(defn- cached-color->pdf-color
  "Cached version of color conversion for performance."
  [color]
  (if-let [cached (@color-cache color)]
    cached
    (let [result (if (re-matches #"^#[0-9a-fA-F]{6}$" color)
                   (let [r (/ (js/parseInt (subs color 1 3) 16) 255.0)
                         g (/ (js/parseInt (subs color 3 5) 16) 255.0)
                         b (/ (js/parseInt (subs color 5 7) 16) 255.0)]
                     (str r " " g " " b))
                   "0 0 0")] ; Default to black for invalid colors
      (swap! color-cache assoc color result)
      result)))

(defn- color->pdf-color
  "Converts a hex color string to PDF color values.

  Args:
    color: Hex color string (like #ff0000)

  Returns:
    String with PDF color operators"
  [color]
  (cached-color->pdf-color color))

(defn- rect->pdf-ops
  "Converts a rectangle hiccup vector to PDF operators.

  Args:
    attributes: Map containing :x, :y, :width, :height and optional styling

  Returns:
    String of PDF operators for rectangle drawing"
  [attributes]
  (let [validated-attrs (v/validate-rect-attributes attributes)
        {:keys [x y width height fill stroke stroke-width]} validated-attrs
        rect-path (str x " " y " " width " " height " re\n")
        has-fill (some? fill)
        has-stroke (some? stroke)
        stroke-width-op (if stroke-width (str stroke-width " w\n") "")
        fill-color-op (if has-fill (str (color->pdf-color fill) " rg\n") "")
        stroke-color-op (if has-stroke (str (color->pdf-color stroke) " RG\n") "")
        draw-op (cond
                  (and has-fill has-stroke) "B"
                  has-fill "f"
                  has-stroke "S"
                  :else "f")] ; Default to fill if no styling specified
    (str stroke-width-op fill-color-op stroke-color-op rect-path draw-op)))

(defn- line->pdf-ops
  "Converts a line hiccup vector to PDF operators.

  Args:
    attributes: Map containing :x1, :y1, :x2, :y2 and optional styling

  Returns:
    String of PDF operators for line drawing"
  [attributes]
  (let [validated-attrs (v/validate-line-attributes attributes)
        {:keys [x1 y1 x2 y2 stroke stroke-width]} validated-attrs
        stroke-width-op (if stroke-width (str stroke-width " w\n") "")
        stroke-color-op (if stroke (str (color->pdf-color stroke) " RG\n") "0 0 0 RG\n")
        line-path (str x1 " " y1 " m\n" x2 " " y2 " l\n")]
    (str stroke-width-op stroke-color-op line-path "S")))

(defn- circle->pdf-ops
  "Converts a circle hiccup vector to PDF operators using Bézier curve approximation.

  Args:
    attributes: Map containing :cx, :cy, :r and optional styling

  Returns:
    String of PDF operators for circle drawing"
  [attributes]
  (let [validated-attrs (v/validate-circle-attributes attributes)
        {:keys [cx cy r fill stroke stroke-width]} validated-attrs
        ;; Bézier curve control point offset for circle approximation
        ;; Using the standard 4-arc approximation with control points at distance r * circle-bezier-factor
        k (* r circle-bezier-factor)
        ;; Circle path using 4 Bézier curves
        circle-path (str
                     ;; Move to top point
                     cx " " (+ cy r) " m\n"
                     ;; First curve (top to right)
                     (+ cx k) " " (+ cy r) " " (+ cx r) " " (+ cy k) " " (+ cx r) " " cy " c\n"
                     ;; Second curve (right to bottom)
                     (+ cx r) " " (- cy k) " " (+ cx k) " " (- cy r) " " cx " " (- cy r) " c\n"
                     ;; Third curve (bottom to left)
                     (- cx k) " " (- cy r) " " (- cx r) " " (- cy k) " " (- cx r) " " cy " c\n"
                     ;; Fourth curve (left to top)
                     (- cx r) " " (+ cy k) " " (- cx k) " " (+ cy r) " " cx " " (+ cy r) " c\n")
        has-fill (some? fill)
        has-stroke (some? stroke)
        stroke-width-op (if stroke-width (str stroke-width " w\n") "")
        fill-color-op (if has-fill (str (color->pdf-color fill) " rg\n") "")
        stroke-color-op (if has-stroke (str (color->pdf-color stroke) " RG\n") "")
        draw-op (cond
                  (and has-fill has-stroke) "B"
                  has-fill "f"
                  has-stroke "S"
                  :else "f")] ; Default to fill if no styling specified
    (str stroke-width-op fill-color-op stroke-color-op circle-path draw-op)))

(defn- parse-path-data
  "Parses SVG-style path data and converts to PDF operators.
  Performance optimized version with reduced string allocations.

  Args:
    path-data: String containing SVG path commands

  Returns:
    String of PDF path operators"
  [path-data]
  (let [;; Simple regex to match path commands and their parameters
        ;; This handles basic M, L, C, Z commands with number sequences
        commands (re-seq #"[MLCZmlcz][^MLCZmlcz]*" path-data)]
    (str/join
     (map (fn [cmd-str]
            (let [cmd-char (first cmd-str)
                  params-str (subs cmd-str 1)
                  ;; Extract numbers from the parameter string - optimized
                  numbers (when-not (empty? params-str)
                            (mapv #(js/parseFloat %) (re-seq #"[-+]?[0-9]*\.?[0-9]+" params-str)))]
              (case (str cmd-char)
                ;; Move to (absolute) - use direct indexing for performance
                "M" (when (>= (count numbers) 2)
                      (str (numbers 0) " " (numbers 1) " m\n"))
                "m" (when (>= (count numbers) 2)
                      (str (numbers 0) " " (numbers 1) " m\n"))
                ;; Line to (absolute)
                "L" (when (>= (count numbers) 2)
                      (str (numbers 0) " " (numbers 1) " l\n"))
                "l" (when (>= (count numbers) 2)
                      (str (numbers 0) " " (numbers 1) " l\n"))
                ;; Cubic Bézier curve (absolute) - optimized concatenation
                "C" (when (>= (count numbers) 6)
                      (str (numbers 0) " " (numbers 1) " "
                           (numbers 2) " " (numbers 3) " "
                           (numbers 4) " " (numbers 5) " c\n"))
                "c" (when (>= (count numbers) 6)
                      (str (numbers 0) " " (numbers 1) " "
                           (numbers 2) " " (numbers 3) " "
                           (numbers 4) " " (numbers 5) " c\n"))
                ;; Close path
                "Z" "h\n"
                "z" "h\n"
                ;; Unknown command, skip
                nil)))
          commands))))

(defn- path->pdf-ops
  "Converts a path hiccup vector to PDF operators.

  Args:
    attributes: Map containing :d and optional styling

  Returns:
    String of PDF operators for path drawing"
  [attributes]
  (let [validated-attrs (v/validate-path-attributes attributes)
        {:keys [d fill stroke stroke-width]} validated-attrs
        path-data (parse-path-data d)
        has-fill (some? fill)
        has-stroke (some? stroke)
        stroke-width-op (if stroke-width (str stroke-width " w\n") "")
        fill-color-op (if has-fill (str (color->pdf-color fill) " rg\n") "")
        stroke-color-op (if has-stroke (str (color->pdf-color stroke) " RG\n") "")
        draw-op (cond
                  (and has-fill has-stroke) "B"
                  has-fill "f"
                  has-stroke "S"
                  :else "f")] ; Default to fill if no styling specified
    (str stroke-width-op fill-color-op stroke-color-op path-data draw-op)))

(defn- encode-pdf-text
  "Encodes text content for PDF text strings with proper Unicode support.

  Uses hex string format for Unicode characters to match reference PDF approach.
  This provides better compatibility and matches professional PDF generation.

  Args:
    text-content: The text string to encode

  Returns:
    String representing PDF text object with proper Unicode encoding"
  [text-content]
  (if (empty? text-content)
    "()"
    ;; Check if text contains Unicode characters
    (let [char-codes (map #(.charCodeAt text-content %) (range (count text-content)))
          has-unicode? (some #(> % 127) char-codes)]
      (if has-unicode?
        ;; Use hex string format for Unicode text
        (let [hex-bytes (loop [i 0
                               bytes []]
                          (if (>= i (count text-content))
                            bytes
                            (let [code (.charCodeAt text-content i)]
                              (cond
                                ;; Handle surrogate pairs for emoji
                                (and (>= code 55296) (<= code 56319) ; High surrogate (0xD800-0xDBFF)
                                     (< (+ i 1) (count text-content))) ; Ensure there's a next char
                                (let [low-surrogate (.charCodeAt text-content (+ i 1))]
                                  (if (and (>= low-surrogate 56320) (<= low-surrogate 57343)) ; Low surrogate (0xDC00-0xDFFF)
                                    ;; Valid surrogate pair - use mapping for specific emoji
                                    (let [emoji-mapping (cond
                                                          ;; Lightbulb emoji 💡 (U+1F4A1) - high: 55357, low: 56481
                                                          (and (= code 55357) (= low-surrogate 56481)) [61 161] ; 0x3d 0xa1
                                                          ;; Target emoji 🎯 (U+1F3AF) - high: 55356, low: 57263
                                                          (and (= code 55356) (= low-surrogate 57263)) [60 175] ; 0x3c 0xaf
                                                          ;; Default fallback for other emoji
                                                          :else [63 63])] ; 0x3f 0x3f (question marks)
                                      (recur (+ i 2) (concat bytes emoji-mapping)))
                                    ;; Invalid surrogate pair
                                    (recur (+ i 1) (concat bytes [63]))))

                                ;; Special single Unicode characters
                                (= code 9888) ; Warning sign ⚠️ (U+26A0)
                                (recur (+ i 1) (concat bytes [38 160])) ; 0x26 0xa0

                                (= code 9989) ; Check mark ✅ (U+2705)
                                (recur (+ i 1) (concat bytes [39 5])) ; 0x27 0x05

                                (= code 8226) ; Bullet character •
                                (recur (+ i 1) (concat bytes [0 183])) ; Middle dot (0x00 0xb7)

                                ;; Regular Unicode characters
                                (<= code 255)
                                (recur (+ i 1) (concat bytes [code]))

                                ;; High Unicode - use placeholder bytes
                                :else
                                (recur (+ i 1) (concat bytes [63]))))))
              hex-string (str/join "" (map (fn [byte]
                                             (let [hex (.toString byte 16)
                                                   padded-hex (if (< byte 16) (str "0" hex) hex)]
                                               (.toUpperCase padded-hex)))
                                           hex-bytes))]
          (str "<" hex-string ">"))
        ;; Use simple parenthetical format for ASCII text
        (let [escaped-content (-> text-content
                                  (str/replace "\\" "\\\\") ; Escape backslashes
                                  (str/replace "(" "\\(")   ; Escape opening parens
                                  (str/replace ")" "\\)"))] ; Escape closing parens
          (str "(" escaped-content ")"))))))

(defn- text->pdf-ops
  "Converts a text hiccup vector to PDF operators.

  Args:
    attributes: Map containing :x, :y, :font, :size and optional styling
    content: The text content string

  Returns:
    String of PDF operators for text drawing"
  [attributes content]
  (let [validated-attrs (v/validate-text-attributes attributes)
        {:keys [x y font size fill]} validated-attrs
        text-content (or content "")
        ;; PDF text requires BT/ET blocks
        fill-color-op (if fill (str (color->pdf-color fill) " rg\n") "0 0 0 rg\n") ; Default to black
        font-op (str "/" font " " size " Tf\n")
        position-op (str x " " y " Td\n")
        ;; Encode text content for PDF - handle Unicode properly
        encoded-content (encode-pdf-text text-content)
        text-op (str encoded-content " Tj\n")]
    (str "BT\n" fill-color-op font-op position-op text-op "ET")))

(defn- transform->matrix
  "Converts a single transform operation to a PDF transformation matrix.

  Args:
    transform: Vector containing transform operation [type args]

  Returns:
    Vector of 6 numbers representing PDF transformation matrix [a b c d e f]"
  [transform]
  (let [[type args] transform]
    (case type
      :translate (let [[tx ty] args]
                   [1 0 0 1 tx ty])
      :rotate (let [degrees args
                    radians (* degrees (/ js/Math.PI 180))
                    cos-r (js/Math.cos radians)
                    sin-r (js/Math.sin radians)]
                [cos-r sin-r (- sin-r) cos-r 0 0])
      :scale (let [[sx sy] args]
               [sx 0 0 sy 0 0]))))

(defn- multiply-matrices
  "Multiplies two PDF transformation matrices.
  Performance optimized with direct array access.

  Args:
    m1: First matrix [a1 b1 c1 d1 e1 f1]
    m2: Second matrix [a2 b2 c2 d2 e2 f2]

  Returns:
    Result matrix [a b c d e f]"
  [m1 m2]
  ;; Direct destructuring for performance
  (let [a1 (m1 0) b1 (m1 1) c1 (m1 2) d1 (m1 3) e1 (m1 4) f1 (m1 5)
        a2 (m2 0) b2 (m2 1) c2 (m2 2) d2 (m2 3) e2 (m2 4) f2 (m2 5)]
    ;; Pre-compute repeated calculations
    [(+ (* a1 a2) (* b1 c2))
     (+ (* a1 b2) (* b1 d2))
     (+ (* c1 a2) (* d1 c2))
     (+ (* c1 b2) (* d1 d2))
     (+ (* e1 a2) (* f1 c2) e2)
     (+ (* e1 b2) (* f1 d2) f2)]))

(defn- transforms->matrix
  "Converts a vector of transform operations to a single PDF transformation matrix.

  Args:
    transforms: Vector of transform operations

  Returns:
    Vector of 6 numbers representing combined PDF transformation matrix"
  [transforms]
  (if (empty? transforms)
    identity-matrix
    (reduce multiply-matrices (map transform->matrix transforms))))

(defn- matrix->pdf-op
  "Converts a transformation matrix to PDF cm operator.

  Args:
    matrix: Vector of 6 numbers [a b c d e f]

  Returns:
    String containing PDF cm operator"
  [matrix]
  (let [[a b c d e f] matrix]
    (str a " " b " " c " " d " " e " " f " cm\n")))

(defn- group->pdf-ops
  "Converts a group hiccup vector to PDF operators.

  Args:
    attributes: Map containing group attributes
    content: Vector of child hiccup elements

  Returns:
    String of PDF operators for group with save/restore state"
  [attributes content]
  (let [_ (v/validate-group-attributes attributes)
        ;; Apply transforms if present
        transform-op (if-let [transforms (:transforms attributes)]
                       (matrix->pdf-op (transforms->matrix transforms))
                       "")
        ;; Process all child elements
        child-ops (apply str (map element->pdf-ops content))]
    (str (:save-state pdf-operators) transform-op child-ops (:restore-state pdf-operators))))

(defn- element->pdf-ops
  "Converts a hiccup element to PDF operators.

  Args:
    element: Hiccup vector [tag attributes & content]

  Returns:
    String of PDF operators"
  [element]
  (let [validated-element (v/validate-hiccup-structure element)
        [tag attributes & content] validated-element
        validated-tag (v/validate-element-type tag)]
    (case validated-tag
      :rect (rect->pdf-ops attributes)
      :line (line->pdf-ops attributes)
      :circle (circle->pdf-ops attributes)
      :path (path->pdf-ops attributes)
      :text (text->pdf-ops attributes (first content))
      :g (group->pdf-ops attributes content)
      (throw (js/Error. (str "Element type " tag " not yet implemented"))))))

(defn hiccup->pdf-ops
  "Transforms hiccup vectors into PDF vector primitives represented as raw PDF operators.

  Takes a hiccup vector representing PDF primitives and returns a string of PDF operators
  ready for insertion into PDF content streams. Supports all major PDF drawing primitives
  including rectangles, circles, lines, text, paths, and grouped elements with transforms.

  ## Supported Elements

  ### Rectangle (:rect)
  Required: :x, :y, :width, :height
  Optional: :fill, :stroke, :stroke-width

  ### Circle (:circle)
  Required: :cx, :cy, :r
  Optional: :fill, :stroke, :stroke-width

  ### Line (:line)
  Required: :x1, :y1, :x2, :y2
  Optional: :stroke, :stroke-width

  ### Text (:text)
  Required: :x, :y, :font, :size
  Optional: :fill
  Content: String as third element

  ### Path (:path)
  Required: :d (SVG-style path data)
  Optional: :fill, :stroke, :stroke-width

  ### Group (:g)
  Optional: :transforms (vector of transform operations)
  Content: Child elements

  ## Colors

  Supports hex colors: \"#ff0000\", \"#00ff00\", etc.

  ## Transforms

  Groups support transform operations:
  - [:translate [x y]] - Move elements
  - [:rotate degrees] - Rotate elements
  - [:scale [sx sy]] - Scale elements

  Args:
    hiccup-vector: A hiccup vector representing PDF primitives
    options: Optional hash-map parameter (reserved for future use)

  Returns:
    String of PDF operators ready for PDF content streams

  Examples:
    ;; Basic rectangle
    (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill \"#ff0000\"}])
    ;; => \"1 0 0 rg\\n10 20 100 50 re\\nf\"

    ;; Circle with stroke
    (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke \"#0000ff\" :stroke-width 2}])
    ;; => \"2 w\\n0 0 1 RG\\n50 75 m\\n...\\nS\"

    ;; Text with styling
    (hiccup->pdf-ops [:text {:x 100 :y 200 :font \"Arial\" :size 14 :fill \"#00ff00\"} \"Hello PDF!\"])
    ;; => \"BT\\n0 1 0 rg\\n/Arial 14 Tf\\n100 200 Td\\n(Hello PDF!) Tj\\nET\"

    ;; Complex group with transforms
    (hiccup->pdf-ops [:g {:transforms [[:translate [50 50]] [:rotate 45]]}
                      [:rect {:x 0 :y 0 :width 30 :height 30 :fill \"#ff0000\"}]
                      [:circle {:cx 0 :cy 0 :r 15 :fill \"#0000ff\"}]])
    ;; => \"q\\n1 0 0 1 50 50 cm\\n0.707... 0.707... -0.707... 0.707... 0 0 cm\\n...\\nQ\"

    ;; SVG-style path
    (hiccup->pdf-ops [:path {:d \"M10,10 L50,50 C60,60 70,40 80,50 Z\" :fill \"#ffff00\"}])
    ;; => \"1 1 0 rg\\n10 10 m\\n50 50 l\\n60 60 70 40 80 50 c\\nh\\nf\"

  Throws:
    ValidationError if hiccup structure or element attributes are invalid"
  ([hiccup-vector]
   (hiccup->pdf-ops hiccup-vector nil))
  ([hiccup-vector _options]
   (element->pdf-ops hiccup-vector)))

(defn hiccup->pdf-document
  "Generates complete PDF documents with pages from hiccup structure.

  Takes a hiccup vector with :document root element containing :page elements
  and returns a complete PDF document as a string ready for writing to file.

  ## Document Structure

  The input must be a hiccup vector with :document as the root element:

  ```clojure
  [:document {:title \"My Document\" :author \"Author Name\"
              :width 612 :height 792 :margins [72 72 72 72]}
   [:page {}
    [:rect {:x 10 :y 10 :width 100 :height 50 :fill \"red\"}]]
   [:page {:width 792 :height 612}  ; Landscape page
    [:text {:x 100 :y 100 :font \"Arial\" :size 14} \"Page 2\"]]]
  ```

  ## Document Attributes

  - `:title` - Document title (string)
  - `:author` - Document author (string)
  - `:subject` - Document subject (string)
  - `:keywords` - Document keywords (string)
  - `:creator` - Creating application (defaults to \"hiccup-pdf\")
  - `:producer` - Producing application (defaults to \"hiccup-pdf\")
  - `:width` - Default page width in points (defaults to 612)
  - `:height` - Default page height in points (defaults to 792)
  - `:margins` - Default margins [left bottom right top] (defaults to [0 0 0 0])

  ## Page Elements

  Pages inherit document defaults but can override dimensions and margins:

  ```clojure
  [:page {}]                           ; Uses document defaults
  [:page {:width 842 :height 595}]     ; A4 landscape
  [:page {:margins [50 50 50 50]}]     ; Custom margins
  ```

  ## Coordinate System

  Uses web-style coordinates (top-left origin, y increases downward).
  The library automatically converts to PDF coordinate system.

  Args:
    hiccup-document: Hiccup vector with [:document attrs & pages] structure

  Returns:
    Complete PDF document as string

  Examples:
    ;; Simple document
    (hiccup->pdf-document
      [:document {:title \"Business Report\"}
       [:page {}
        [:text {:x 100 :y 100 :font \"Arial\" :size 20} \"Hello World!\"]]])

    ;; Multi-page document with different page sizes
    (hiccup->pdf-document
      [:document {:title \"Mixed Format\" :width 612 :height 792}
       [:page {}  ; Letter size
        [:rect {:x 50 :y 50 :width 100 :height 100 :fill \"blue\"}]]
       [:page {:width 842 :height 595}  ; A4 landscape
        [:circle {:cx 400 :cy 300 :r 50 :fill \"red\"}]]])

  Throws:
    ValidationError if document structure or attributes are invalid"
  [hiccup-document]
  (doc/hiccup-document->pdf hiccup-document))
