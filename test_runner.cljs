(ns test-runner
  (:require [cljs.test :refer [run-tests]]
            [hiccup-pdf.core-test]
            [hiccup-pdf.validation-test]))

(defn run-all-tests []
  (run-tests 'hiccup-pdf.core-test
             'hiccup-pdf.validation-test))

(run-all-tests)