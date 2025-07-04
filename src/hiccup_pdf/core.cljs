(ns hiccup-pdf.core
  "Core namespace for transforming hiccup vectors into PDF operators."
  (:require [hiccup-pdf.validation :as v]))

(defn- color->pdf-color
  "Converts a color string to PDF color values.
  
  Args:
    color: Color string (hex like #ff0000 or named color like red)
    
  Returns:
    String with PDF color operators"
  [color]
  (case color
    "red" "1 0 0"
    "green" "0 1 0"
    "blue" "0 0 1"
    "black" "0 0 0"
    "white" "1 1 1"
    "yellow" "1 1 0"
    "cyan" "0 1 1"
    "magenta" "1 0 1"
    ;; For hex colors, convert to RGB
    (if (re-matches #"^#[0-9a-fA-F]{6}$" color)
      (let [r (/ (js/parseInt (subs color 1 3) 16) 255.0)
            g (/ (js/parseInt (subs color 3 5) 16) 255.0)
            b (/ (js/parseInt (subs color 5 7) 16) 255.0)]
        (str r " " g " " b))
      "0 0 0"))) ; Default to black

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
        ;; Using the standard 4-arc approximation with control points at distance r * 0.552284749831
        k (* r 0.552284749831)
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
  
  Args:
    path-data: String containing SVG path commands
    
  Returns:
    String of PDF path operators"
  [path-data]
  (let [;; Simple regex to match path commands and their parameters
        ;; This handles basic M, L, C, Z commands with number sequences
        commands (re-seq #"[MLCZmlcz][^MLCZmlcz]*" path-data)]
    (apply str
           (map (fn [cmd-str]
                  (let [cmd-char (first cmd-str)
                        params-str (subs cmd-str 1)
                        ;; Extract numbers from the parameter string
                        numbers (map #(js/parseFloat %) (re-seq #"[-+]?[0-9]*\.?[0-9]+" params-str))]
                    (case (str cmd-char)
                      ;; Move to (absolute)
                      "M" (str (nth numbers 0) " " (nth numbers 1) " m\n")
                      "m" (str (nth numbers 0) " " (nth numbers 1) " m\n") ; Treat relative as absolute for simplicity
                      ;; Line to (absolute)
                      "L" (str (nth numbers 0) " " (nth numbers 1) " l\n")
                      "l" (str (nth numbers 0) " " (nth numbers 1) " l\n") ; Treat relative as absolute for simplicity
                      ;; Cubic Bézier curve (absolute)
                      "C" (str (nth numbers 0) " " (nth numbers 1) " "
                               (nth numbers 2) " " (nth numbers 3) " "
                               (nth numbers 4) " " (nth numbers 5) " c\n")
                      "c" (str (nth numbers 0) " " (nth numbers 1) " "
                               (nth numbers 2) " " (nth numbers 3) " "
                               (nth numbers 4) " " (nth numbers 5) " c\n") ; Treat relative as absolute for simplicity
                      ;; Close path
                      "Z" "h\n"
                      "z" "h\n"
                      ;; Unknown command, skip
                      "")))
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

(defn- element->pdf-ops
  "Converts a hiccup element to PDF operators.
  
  Args:
    element: Hiccup vector [tag attributes & content]
    
  Returns:
    String of PDF operators"
  [element]
  (let [validated-element (v/validate-hiccup-structure element)
        [tag attributes & _content] validated-element
        validated-tag (v/validate-element-type tag)]
    (case validated-tag
      :rect (rect->pdf-ops attributes)
      :line (line->pdf-ops attributes)
      :circle (circle->pdf-ops attributes)
      :path (path->pdf-ops attributes)
      (throw (js/Error. (str "Element type " tag " not yet implemented"))))))

(defn hiccup->pdf-ops
  "Transforms hiccup vectors into PDF vector primitives represented as raw PDF operators.
  
  Takes a hiccup vector representing PDF primitives and returns a string of PDF operators
  ready for insertion into PDF content streams.
  
  Args:
    hiccup-vector: A hiccup vector representing PDF primitives (e.g., [:rect {:x 10 :y 20}])
    options: Optional hash-map parameter (reserved for future use)
  
  Returns:
    String of PDF operators ready for PDF content streams
  
  Example:
    (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill \"red\"}])
    ;; => \"10 20 100 50 re\\nf\""
  ([hiccup-vector]
   (hiccup->pdf-ops hiccup-vector nil))
  ([hiccup-vector _options]
   (element->pdf-ops hiccup-vector)))