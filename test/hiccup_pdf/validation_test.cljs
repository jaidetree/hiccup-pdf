(ns hiccup-pdf.validation-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.validation :refer [validate-hiccup-structure
                                           validate-element-type
                                           validate-attributes
                                           validate-rect-attributes
                                           validate-line-attributes
                                           validate-circle-attributes
                                           validate-path-attributes
                                           validate-color]]))

(deftest validate-hiccup-structure-test
  (testing "Valid hiccup structures"
    (is (= [:rect {:x 10 :y 20}]
           (validate-hiccup-structure [:rect {:x 10 :y 20}]))
        "Should validate basic hiccup structure")
    
    (is (= [:text {:x 10 :y 20} "Hello"]
           (validate-hiccup-structure [:text {:x 10 :y 20} "Hello"]))
        "Should validate hiccup structure with content")))

(deftest validate-element-type-test
  (testing "Valid element types"
    (is (= :rect (validate-element-type :rect))
        "Should validate rect element type")
    
    (is (= :circle (validate-element-type :circle))
        "Should validate circle element type")
    
    (is (= :line (validate-element-type :line))
        "Should validate line element type")
    
    (is (= :text (validate-element-type :text))
        "Should validate text element type")
    
    (is (= :path (validate-element-type :path))
        "Should validate path element type")
    
    (is (= :g (validate-element-type :g))
        "Should validate group element type")))

(deftest validate-attributes-test
  (testing "Valid attributes"
    (is (= {:x 10 :y 20} (validate-attributes {:x 10 :y 20}))
        "Should validate attributes map")
    
    (is (= {} (validate-attributes {}))
        "Should validate empty attributes map")))

(deftest validate-rect-attributes-test
  (testing "Valid rectangle attributes"
    (is (= {:x 10 :y 20 :width 100 :height 50}
           (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50}))
        "Should validate complete rectangle attributes")
    
    (is (= {:x 0 :y 0 :width 1 :height 1}
           (validate-rect-attributes {:x 0 :y 0 :width 1 :height 1}))
        "Should validate minimal rectangle attributes"))
  
  (testing "Invalid rectangle attributes"
    (is (thrown? js/Error (validate-rect-attributes {:x 10 :y 20 :width 100}))
        "Should throw error for missing height")
    
    (is (thrown? js/Error (validate-rect-attributes {:x 10 :y 20 :height 50}))
        "Should throw error for missing width")
    
    (is (thrown? js/Error (validate-rect-attributes {:x "10" :y 20 :width 100 :height 50}))
        "Should throw error for non-numeric x")
    
    (is (thrown? js/Error (validate-rect-attributes {}))
        "Should throw error for empty attributes")))

(deftest validate-color-test
  (testing "Valid colors"
    (is (= "red" (validate-color "red"))
        "Should validate named color")
    
    (is (= "#ff0000" (validate-color "#ff0000"))
        "Should validate hex color")
    
    (is (= "#FFFFFF" (validate-color "#FFFFFF"))
        "Should validate uppercase hex color"))
  
  (testing "Invalid colors"
    (is (thrown? js/Error (validate-color "#ff00"))
        "Should throw error for invalid hex format")
    
    (is (thrown? js/Error (validate-color 123))
        "Should throw error for non-string color")
    
    (is (thrown? js/Error (validate-color "invalid-color"))
        "Should throw error for invalid color name")))

(deftest validate-rect-attributes-with-styling-test
  (testing "Valid rectangle attributes with styling"
    (is (= {:x 10 :y 20 :width 100 :height 50 :fill "red"}
           (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :fill "red"}))
        "Should validate rectangle with fill")
    
    (is (= {:x 10 :y 20 :width 100 :height 50 :stroke "blue" :stroke-width 2}
           (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :stroke "blue" :stroke-width 2}))
        "Should validate rectangle with stroke")
    
    (is (= {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000" :stroke "#0000ff" :stroke-width 1.5}
           (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000" :stroke "#0000ff" :stroke-width 1.5}))
        "Should validate rectangle with all styling"))
  
  (testing "Invalid rectangle styling"
    (is (thrown? js/Error (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :fill "invalid"}))
        "Should throw error for invalid fill color")
    
    (is (thrown? js/Error (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :stroke-width "thick"}))
        "Should throw error for non-numeric stroke-width")))

(deftest validate-line-attributes-test
  (testing "Valid line attributes"
    (is (= {:x1 10 :y1 20 :x2 100 :y2 50}
           (validate-line-attributes {:x1 10 :y1 20 :x2 100 :y2 50}))
        "Should validate basic line attributes")
    
    (is (= {:x1 0 :y1 0 :x2 1 :y2 1}
           (validate-line-attributes {:x1 0 :y1 0 :x2 1 :y2 1}))
        "Should validate minimal line attributes"))
  
  (testing "Line attributes with styling"
    (is (= {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "red"}
           (validate-line-attributes {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "red"}))
        "Should validate line with stroke")
    
    (is (= {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "blue" :stroke-width 2}
           (validate-line-attributes {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "blue" :stroke-width 2}))
        "Should validate line with stroke and width"))
  
  (testing "Invalid line attributes"
    (is (thrown? js/Error (validate-line-attributes {:x1 10 :y1 20 :x2 100}))
        "Should throw error for missing y2")
    
    (is (thrown? js/Error (validate-line-attributes {:x1 10 :y1 20}))
        "Should throw error for missing x2 and y2")
    
    (is (thrown? js/Error (validate-line-attributes {:x1 "10" :y1 20 :x2 100 :y2 50}))
        "Should throw error for non-numeric x1")
    
    (is (thrown? js/Error (validate-line-attributes {}))
        "Should throw error for empty attributes")
    
    (is (thrown? js/Error (validate-line-attributes {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "invalid"}))
        "Should throw error for invalid stroke color")))

(deftest validate-circle-attributes-test
  (testing "Valid circle attributes"
    (is (= {:cx 50 :cy 50 :r 25}
           (validate-circle-attributes {:cx 50 :cy 50 :r 25}))
        "Should validate basic circle attributes")
    
    (is (= {:cx 0 :cy 0 :r 1}
           (validate-circle-attributes {:cx 0 :cy 0 :r 1}))
        "Should validate minimal circle attributes")
    
    (is (= {:cx 10 :cy 20 :r 0}
           (validate-circle-attributes {:cx 10 :cy 20 :r 0}))
        "Should validate circle with zero radius"))
  
  (testing "Circle attributes with styling"
    (is (= {:cx 50 :cy 50 :r 25 :fill "red"}
           (validate-circle-attributes {:cx 50 :cy 50 :r 25 :fill "red"}))
        "Should validate circle with fill")
    
    (is (= {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}
           (validate-circle-attributes {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}))
        "Should validate circle with stroke"))
  
  (testing "Invalid circle attributes"
    (is (thrown? js/Error (validate-circle-attributes {:cx 50 :cy 50}))
        "Should throw error for missing radius")
    
    (is (thrown? js/Error (validate-circle-attributes {:cx 50 :r 25}))
        "Should throw error for missing cy")
    
    (is (thrown? js/Error (validate-circle-attributes {:cx "50" :cy 50 :r 25}))
        "Should throw error for non-numeric cx")
    
    (is (thrown? js/Error (validate-circle-attributes {:cx 50 :cy 50 :r -5}))
        "Should throw error for negative radius")
    
    (is (thrown? js/Error (validate-circle-attributes {}))
        "Should throw error for empty attributes")
    
    (is (thrown? js/Error (validate-circle-attributes {:cx 50 :cy 50 :r 25 :fill "invalid"}))
        "Should throw error for invalid fill color")))

(deftest validate-path-attributes-test
  (testing "Valid path attributes"
    (is (= {:d "M10,10 L20,20"}
           (validate-path-attributes {:d "M10,10 L20,20"}))
        "Should validate basic path with d attribute")
    
    (is (= {:d "M0,0 L100,100 Z"}
           (validate-path-attributes {:d "M0,0 L100,100 Z"}))
        "Should validate path with close command")
    
    (is (= {:d "M10,10 C20,20 30,30 40,40"}
           (validate-path-attributes {:d "M10,10 C20,20 30,30 40,40"}))
        "Should validate path with curve command"))
  
  (testing "Path attributes with styling"
    (is (= {:d "M10,10 L20,20" :fill "red"}
           (validate-path-attributes {:d "M10,10 L20,20" :fill "red"}))
        "Should validate path with fill")
    
    (is (= {:d "M10,10 L20,20" :stroke "blue" :stroke-width 2}
           (validate-path-attributes {:d "M10,10 L20,20" :stroke "blue" :stroke-width 2}))
        "Should validate path with stroke"))
  
  (testing "Invalid path attributes"
    (is (thrown? js/Error (validate-path-attributes {}))
        "Should throw error for missing d attribute")
    
    (is (thrown? js/Error (validate-path-attributes {:d ""}))
        "Should throw error for empty d attribute")
    
    (is (thrown? js/Error (validate-path-attributes {:d "   "}))
        "Should throw error for whitespace-only d attribute")
    
    (is (thrown? js/Error (validate-path-attributes {:d "\t\n"}))
        "Should throw error for whitespace characters in d attribute")
    
    (is (thrown? js/Error (validate-path-attributes {:d 123}))
        "Should throw error for non-string d attribute")
    
    (is (thrown? js/Error (validate-path-attributes {:d "M10,10 L20,20" :fill "invalid"}))
        "Should throw error for invalid fill color")))