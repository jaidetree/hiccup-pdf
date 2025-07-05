(ns scripts.test-runner
  (:require [cljs.test :refer [run-tests]]
            [dev.jaide.hiccup-pdf.core-test]
            [dev.jaide.hiccup-pdf.validation-test]
            [dev.jaide.hiccup-pdf.document-test]
            [dev.jaide.hiccup-pdf.emoji-test]
            [dev.jaide.hiccup-pdf.text-processing-test]
            [dev.jaide.hiccup-pdf.images-test]))

(def tests
  '[dev.jaide.hiccup-pdf.core-test
    dev.jaide.hiccup-pdf.validation-test
    dev.jaide.hiccup-pdf.document-test
    dev.jaide.hiccup-pdf.emoji-test
    dev.jaide.hiccup-pdf.text-processing-test
    dev.jaide.hiccup-pdf.images-test])

(defn run-all-tests
  []
  (apply run-tests tests))

(defn -main
  []
  (run-all-tests))

