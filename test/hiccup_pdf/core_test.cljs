(ns hiccup-pdf.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.core :refer [hiccup->pdf-ops]]))

(deftest smoke-test
  (testing "hiccup->pdf-ops function exists and can be called"
    (is (= "" (hiccup->pdf-ops [:rect {:x 10 :y 20}]))
        "Function should return empty string with placeholder implementation")
    
    (is (= "" (hiccup->pdf-ops [:rect {:x 10 :y 20}] {}))
        "Function should accept options parameter")
    
    (is (= "" (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}]))
        "Function should accept different element types without error")))