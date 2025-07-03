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

(deftest basic-function-calls
  (testing "Function accepts various hiccup element types"
    (is (string? (hiccup->pdf-ops [:rect {:x 0 :y 0 :width 100 :height 50}]))
        "Rectangle elements should return a string")
    
    (is (string? (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}]))
        "Circle elements should return a string")
    
    (is (string? (hiccup->pdf-ops [:line {:x1 0 :y1 0 :x2 100 :y2 100}]))
        "Line elements should return a string")
    
    (is (string? (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Hello"]))
        "Text elements should return a string")
    
    (is (string? (hiccup->pdf-ops [:path {:d "M10,10 L20,20"}]))
        "Path elements should return a string")
    
    (is (string? (hiccup->pdf-ops [:g {} [:rect {:x 0 :y 0 :width 50 :height 50}]]))
        "Group elements should return a string")))

(deftest function-signature-tests
  (testing "Function signature variations"
    (is (= "" (hiccup->pdf-ops [:rect {:x 10 :y 20}]))
        "Single argument call should work")
    
    (is (= "" (hiccup->pdf-ops [:rect {:x 10 :y 20}] nil))
        "Two argument call with nil options should work")
    
    (is (= "" (hiccup->pdf-ops [:rect {:x 10 :y 20}] {:some-option true}))
        "Two argument call with options map should work")))