(ns scripts.test-runner
  (:require [cljs.test :refer [run-tests]]
            [dev.jaide.hiccup-pdf.core-test]
            [dev.jaide.hiccup-pdf.validation-test]
            [dev.jaide.hiccup-pdf.document-test]
            [dev.jaide.hiccup-pdf.emoji-test]))

(def tests
  '[dev.jaide.hiccup-pdf.core-test
    dev.jaide.hiccup-pdf.validation-test
    dev.jaide.hiccup-pdf.document-test
    dev.jaide.hiccup-pdf.emoji-test])

(defn run-all-tests
  []
  (apply run-tests tests))

(defn -main
  []
  (run-all-tests))

