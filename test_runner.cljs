(ns test-runner
  (:require [cljs.test :refer [run-tests]]
            [dev.jaide.hiccup-pdf.core-test]
            [dev.jaide.hiccup-pdf.validation-test]
            [dev.jaide.hiccup-pdf.document-test]))

(defn run-all-tests []
  (run-tests 'dev.jaide.hiccup-pdf.core-test
             'dev.jaide.hiccup-pdf.validation-test
             'dev.jaide.hiccup-pdf.document-test))

(run-all-tests)
