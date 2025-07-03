(ns test-runner
  (:require [cljs.test :refer [run-tests]]
            [hiccup-pdf.core-test]))

(defn run-all-tests []
  (run-tests 'hiccup-pdf.core-test))

(run-all-tests)