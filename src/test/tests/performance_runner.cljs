#!/usr/bin/env node

(ns performance-runner
  (:require [cljs.test :refer [run-tests]]
            [hiccup-pdf.performance-test :as pt]))

(defn -main []
  (println "=== Hiccup-PDF Performance Test Suite ===")
  (println "Running performance tests...")
  (println "")
  
  (run-tests 'hiccup-pdf.performance-test)
  
  (println "")
  (println "Performance testing complete."))

(-main)