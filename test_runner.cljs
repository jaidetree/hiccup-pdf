(ns test-runner
  (:require [cljs.test :refer [run-tests]]
            [hiccup-pdf.core-test]
            [hiccup-pdf.validation-test]
            [hiccup-pdf.document-test]))

(defn run-all-tests []
  (run-tests 'hiccup-pdf.core-test
             'hiccup-pdf.validation-test
             'hiccup-pdf.document-test))

(run-all-tests)
