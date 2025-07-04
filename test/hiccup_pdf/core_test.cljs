(ns hiccup-pdf.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.core :refer [hiccup->pdf-ops]]))

(deftest smoke-test
  (testing "hiccup->pdf-ops function exists and can be called"
    (is (= "10 20 100 50 re\nf" (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}]))
        "Function should return PDF operators for rectangle")
    
    (is (= "10 20 100 50 re\nf" (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}] {}))
        "Function should accept options parameter")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}]))
        "Function should throw error for unimplemented element types")))

(deftest basic-function-calls
  (testing "Function accepts various hiccup element types"
    (is (string? (hiccup->pdf-ops [:rect {:x 0 :y 0 :width 100 :height 50}]))
        "Rectangle elements should return a string")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}]))
        "Circle elements should throw error (not implemented)")
    
    (is (string? (hiccup->pdf-ops [:line {:x1 0 :y1 0 :x2 100 :y2 100}]))
        "Line elements should return a string")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Hello"]))
        "Text elements should throw error (not implemented)")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:path {:d "M10,10 L20,20"}]))
        "Path elements should throw error (not implemented)")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:g {} [:rect {:x 0 :y 0 :width 50 :height 50}]]))
        "Group elements should throw error (not implemented)")))

(deftest function-signature-tests
  (testing "Function signature variations"
    (is (thrown? js/Error (hiccup->pdf-ops [:rect {:x 10 :y 20}]))
        "Single argument call with missing attributes should throw error")
    
    (is (= "10 20 100 50 re\nf" (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}] nil))
        "Two argument call with nil options should work")
    
    (is (= "10 20 100 50 re\nf" (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}] {:some-option true}))
        "Two argument call with options map should work")))

(deftest rect-element-test
  (testing "Rectangle element transformation"
    (is (= "10 20 100 50 re\nf"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}]))
        "Should generate correct PDF operators for rectangle")
    
    (is (= "0 0 1 1 re\nf"
           (hiccup->pdf-ops [:rect {:x 0 :y 0 :width 1 :height 1}]))
        "Should generate correct PDF operators for minimal rectangle"))
  
  (testing "Rectangle element validation errors"
    (is (thrown? js/Error (hiccup->pdf-ops [:rect {:x 10 :y 20}]))
        "Should throw error for missing width and height")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:rect {:x "10" :y 20 :width 100 :height 50}]))
        "Should throw error for non-numeric attributes")))

(deftest rect-styling-test
  (testing "Rectangle with fill styling"
    (is (= "1 0 0 rg\n10 20 100 50 re\nf"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]))
        "Should generate PDF operators for filled rectangle")
    
    (is (= "1 0 0 rg\n10 20 100 50 re\nf"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}]))
        "Should generate PDF operators for hex color fill"))
  
  (testing "Rectangle with stroke styling"
    (is (= "0 0 1 RG\n10 20 100 50 re\nS"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :stroke "blue"}]))
        "Should generate PDF operators for stroked rectangle")
    
    (is (= "2 w\n0 0 1 RG\n10 20 100 50 re\nS"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :stroke "blue" :stroke-width 2}]))
        "Should generate PDF operators for stroked rectangle with width"))
  
  (testing "Rectangle with both fill and stroke"
    (is (= "1 0 0 rg\n0 0 1 RG\n10 20 100 50 re\nB"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red" :stroke "blue"}]))
        "Should generate PDF operators for filled and stroked rectangle")
    
    (is (= "1.5 w\n1 0 0 rg\n0 0 1 RG\n10 20 100 50 re\nB"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red" :stroke "blue" :stroke-width 1.5}]))
        "Should generate PDF operators for filled and stroked rectangle with width")))

(deftest line-element-test
  (testing "Line element transformation"
    (is (= "0 0 0 RG\n10 20 m\n100 50 l\nS"
           (hiccup->pdf-ops [:line {:x1 10 :y1 20 :x2 100 :y2 50}]))
        "Should generate correct PDF operators for basic line")
    
    (is (= "0 0 0 RG\n0 0 m\n1 1 l\nS"
           (hiccup->pdf-ops [:line {:x1 0 :y1 0 :x2 1 :y2 1}]))
        "Should generate correct PDF operators for minimal line"))
  
  (testing "Line element validation errors"
    (is (thrown? js/Error (hiccup->pdf-ops [:line {:x1 10 :y1 20}]))
        "Should throw error for missing x2 and y2")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:line {:x1 "10" :y1 20 :x2 100 :y2 50}]))
        "Should throw error for non-numeric attributes")))

(deftest line-styling-test
  (testing "Line with stroke styling"
    (is (= "1 0 0 RG\n10 20 m\n100 50 l\nS"
           (hiccup->pdf-ops [:line {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "red"}]))
        "Should generate PDF operators for colored line")
    
    (is (= "2 w\n0 0 1 RG\n10 20 m\n100 50 l\nS"
           (hiccup->pdf-ops [:line {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "blue" :stroke-width 2}]))
        "Should generate PDF operators for line with stroke width")
    
    (is (= "1 0 0 RG\n10 20 m\n100 50 l\nS"
           (hiccup->pdf-ops [:line {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#ff0000"}]))
        "Should generate PDF operators for hex color line")))