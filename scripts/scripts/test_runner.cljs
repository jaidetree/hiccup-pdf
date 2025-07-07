(ns scripts.test-runner
  (:require
   [cljs.test :refer [run-tests]]
   [dev.jaide.hiccup-pdf.core-test]
   [dev.jaide.hiccup-pdf.validation-test]
   [dev.jaide.hiccup-pdf.document-test]
   [dev.jaide.hiccup-pdf.images-test]))

(def tests
  '[dev.jaide.hiccup-pdf.core-test
    dev.jaide.hiccup-pdf.validation-test
    dev.jaide.hiccup-pdf.document-test
    dev.jaide.hiccup-pdf.images-test])

(defn -main
  []
  (apply run-tests tests))

