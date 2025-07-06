#!/usr/bin/env nbb

;; Simple syntax check for text processing namespace
(require '[dev.jaide.hiccup-pdf.text-processing :as text-proc])

(println "✓ text-processing namespace loads successfully")

;; Test a simple function
(let [result (text-proc/segment-text "Hello 💡 world")]
  (println "✓ segment-text function works:" (count result) "segments"))

(println "Text processing namespace syntax is correct!")