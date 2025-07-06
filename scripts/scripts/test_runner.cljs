(ns scripts.test-runner
  (:require
   [cljs.test :refer [run-all-tests]]
   [dev.jaide.hiccup-pdf.core-test]
   [dev.jaide.hiccup-pdf.validation-test]
   [dev.jaide.hiccup-pdf.document-test]
   [dev.jaide.hiccup-pdf.emoji-test]
   [dev.jaide.hiccup-pdf.text-processing-test]
   [dev.jaide.hiccup-pdf.images-test]))

(defn -main
  []
  (run-all-tests))

