(ns hiccup-pdf.core
  "Core namespace for transforming hiccup vectors into PDF operators.")

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
  ([hiccup-vector options]
   ""))