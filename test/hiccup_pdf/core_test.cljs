(ns hiccup-pdf.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [hiccup-pdf.core :refer [hiccup->pdf-ops]]))

(deftest smoke-test
  (testing "hiccup->pdf-ops function exists and can be called"
    (is (= "10 20 100 50 re\nf" (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}]))
        "Function should return PDF operators for rectangle")
    
    (is (= "10 20 100 50 re\nf" (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50}] {}))
        "Function should accept options parameter")
    
    (is (string? (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}]))
        "Function should return PDF operators for circle")))

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

(deftest circle-element-test
  (testing "Circle element transformation"
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25}])]
      (is (string? result)
          "Should generate string output for basic circle")
      (is (re-find #"50 75 m\n" result)
          "Should start path at top of circle")
      (is (re-find #"c\n.*c\n.*c\n.*c\n" result)
          "Should contain 4 cubic Bézier curves")
      (is (re-find #"f$" result)
          "Should end with fill operator"))
    
    (let [result (hiccup->pdf-ops [:circle {:cx 0 :cy 0 :r 1}])]
      (is (string? result)
          "Should generate string output for minimal circle")
      (is (re-find #"0 1 m\n" result)
          "Should start path at correct position")))
  
  (testing "Circle element validation errors"
    (is (thrown? js/Error (hiccup->pdf-ops [:circle {:cx 50 :cy 50}]))
        "Should throw error for missing radius")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:circle {:cx "50" :cy 50 :r 25}]))
        "Should throw error for non-numeric attributes")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r -5}]))
        "Should throw error for negative radius")))

(deftest circle-styling-test
  (testing "Circle with fill styling"
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :fill "red"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain fill color operator")
      (is (re-find #"f$" result)
          "Should end with fill operator"))
    
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :fill "#ff0000"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should handle hex color fill")))
  
  (testing "Circle with stroke styling"
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "blue"}])]
      (is (re-find #"0 0 1 RG\n" result)
          "Should contain stroke color operator")
      (is (re-find #"S$" result)
          "Should end with stroke operator"))
    
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}])]
      (is (re-find #"2 w\n" result)
          "Should contain stroke width operator")))
  
  (testing "Circle with both fill and stroke"
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :fill "red" :stroke "blue"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain fill color")
      (is (re-find #"0 0 1 RG\n" result)
          "Should contain stroke color")
      (is (re-find #"B$" result)
          "Should end with both fill and stroke operator")))
  
  (testing "Edge case: zero radius circle"
    (let [result (hiccup->pdf-ops [:circle {:cx 10 :cy 20 :r 0}])]
      (is (string? result)
          "Should handle zero radius circle")
      (is (re-find #"10 20 m\n" result)
          "Should start at center point"))))

(deftest path-element-test
  (testing "Path element transformation"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20"}])]
      (is (string? result)
          "Should generate string output for basic path")
      (is (re-find #"10 10 m\n" result)
          "Should contain move command")
      (is (re-find #"20 20 l\n" result)
          "Should contain line command")
      (is (re-find #"f$" result)
          "Should end with fill operator"))
    
    (let [result (hiccup->pdf-ops [:path {:d "M0,0 L100,100 Z"}])]
      (is (string? result)
          "Should generate string output for closed path")
      (is (re-find #"0 0 m\n" result)
          "Should start with move command")
      (is (re-find #"100 100 l\n" result)
          "Should contain line command")
      (is (re-find #"h\n" result)
          "Should contain close path command"))
    
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 C20,20 30,30 40,40"}])]
      (is (string? result)
          "Should generate string output for curve path")
      (is (re-find #"10 10 m\n" result)
          "Should start with move command")
      (is (re-find #"20 20 30 30 40 40 c\n" result)
          "Should contain cubic curve command")))
  
  (testing "Path element validation errors"
    (is (thrown? js/Error (hiccup->pdf-ops [:path {}]))
        "Should throw error for missing d attribute")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:path {:d ""}]))
        "Should throw error for empty d attribute")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:path {:d 123}]))
        "Should throw error for non-string d attribute")))

(deftest path-styling-test
  (testing "Path with fill styling"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :fill "red"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain fill color operator")
      (is (re-find #"f$" result)
          "Should end with fill operator"))
    
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :fill "#ff0000"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should handle hex color fill")))
  
  (testing "Path with stroke styling"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :stroke "blue"}])]
      (is (re-find #"0 0 1 RG\n" result)
          "Should contain stroke color operator")
      (is (re-find #"S$" result)
          "Should end with stroke operator"))
    
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :stroke "blue" :stroke-width 2}])]
      (is (re-find #"2 w\n" result)
          "Should contain stroke width operator")))
  
  (testing "Path with both fill and stroke"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :fill "red" :stroke "blue"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain fill color")
      (is (re-find #"0 0 1 RG\n" result)
          "Should contain stroke color")
      (is (re-find #"B$" result)
          "Should end with both fill and stroke operator")))
  
  (testing "Complex path commands"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20 L30,10 Z"}])]
      (is (re-find #"10 10 m\n" result)
          "Should start with move")
      (is (re-find #"20 20 l\n" result)
          "Should contain first line")
      (is (re-find #"30 10 l\n" result)
          "Should contain second line")
      (is (re-find #"h\n" result)
          "Should close path"))))

(deftest text-element-test
  (testing "Text element transformation"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Hello"])]
      (is (string? result)
          "Should generate string output for text")
      (is (re-find #"BT\n" result)
          "Should start with begin text operator")
      (is (re-find #"0 0 0 rg\n" result)
          "Should contain default black color")
      (is (re-find #"/Arial 12 Tf\n" result)
          "Should contain font specification")
      (is (re-find #"10 20 Td\n" result)
          "Should contain text positioning")
      (is (re-find #"\(Hello\) Tj\n" result)
          "Should contain text content")
      (is (re-find #"ET$" result)
          "Should end with end text operator"))
    
    (let [result (hiccup->pdf-ops [:text {:x 0 :y 0 :font "Times" :size 8} "Test"])]
      (is (string? result)
          "Should generate string output for minimal text")
      (is (re-find #"/Times 8 Tf\n" result)
          "Should contain correct font and size")
      (is (re-find #"0 0 Td\n" result)
          "Should contain correct position")
      (is (re-find #"\(Test\) Tj\n" result)
          "Should contain correct text")))
  
  (testing "Text element validation errors"
    (is (thrown? js/Error (hiccup->pdf-ops [:text {:x 10 :y 20} "Hello"]))
        "Should throw error for missing font and size")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:text {:x "10" :y 20 :font "Arial" :size 12} "Hello"]))
        "Should throw error for non-numeric coordinates")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:text {:x 10 :y 20 :font "" :size 12} "Hello"]))
        "Should throw error for empty font name")
    
    (is (thrown? js/Error (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 0} "Hello"]))
        "Should throw error for zero font size")))

(deftest text-styling-test
  (testing "Text with fill styling"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12 :fill "red"} "Hello"])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain red color operator")
      (is (re-find #"BT\n" result)
          "Should contain text block operators"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12 :fill "#ff0000"} "Hello"])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should handle hex color fill")))
  
  (testing "Text content variations"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12}])]
      (is (re-find #"\(\) Tj\n" result)
          "Should handle missing text content"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} ""])]
      (is (re-find #"\(\) Tj\n" result)
          "Should handle empty text content"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Multi word text"])]
      (is (re-find #"\(Multi word text\) Tj\n" result)
          "Should handle multi-word text"))))

(deftest text-emoji-and-special-chars-test
  (testing "Text with emoji support"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Hello 😀 World"])]
      (is (string? result)
          "Should handle emoji characters")
      (is (re-find #"\(Hello 😀 World\) Tj\n" result)
          "Should contain emoji in text content"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "🎉🚀✨"])]
      (is (re-find #"\(🎉🚀✨\) Tj\n" result)
          "Should handle multiple emojis"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Text with emoji 🎨 and more text"])]
      (is (re-find #"\(Text with emoji 🎨 and more text\) Tj\n" result)
          "Should handle mixed text and emoji")))
  
  (testing "Text with special PDF characters"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Text with (parentheses)"])]
      (is (re-find #"\(Text with \\\(parentheses\\\)\) Tj\n" result)
          "Should escape parentheses in text"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Text with \\ backslash"])]
      (is (and (str/includes? result "BT") (str/includes? result "ET") (str/includes? result "Text with") (str/includes? result "backslash"))
          "Should escape backslashes in text"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Complex (text) with \\ special chars"])]
      (is (and (str/includes? result "BT") (str/includes? result "ET") (str/includes? result "Complex") (str/includes? result "special chars"))
          "Should escape multiple special characters")))
  
  (testing "Text with various fonts and emoji"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Times-Roman" :size 14} "Times font with 📚 book emoji"])]
      (is (re-find #"/Times-Roman 14 Tf\n" result)
          "Should work with Times-Roman font")
      (is (re-find #"\(Times font with 📚 book emoji\) Tj\n" result)
          "Should render emoji with Times font"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Helvetica" :size 16} "Helvetica with 🌟 star"])]
      (is (re-find #"/Helvetica 16 Tf\n" result)
          "Should work with Helvetica font")
      (is (re-find #"\(Helvetica with 🌟 star\) Tj\n" result)
          "Should render emoji with Helvetica font")))
  
  (testing "Edge cases with emoji and special characters"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "🔥(hot)🔥"])]
      (is (re-find #"\(🔥\\\(hot\\\)🔥\) Tj\n" result)
          "Should handle emoji with special characters"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} ""])]
      (is (re-find #"\(\) Tj\n" result)
          "Should handle empty string"))
    
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Line 1\nLine 2"])]
      (is (re-find #"\(Line 1\nLine 2\) Tj\n" result)
          "Should handle newline characters"))))

(deftest group-element-test
  (testing "Group element transformation"
    (let [result (hiccup->pdf-ops [:g {} [:rect {:x 10 :y 20 :width 100 :height 50}]])]
      (is (string? result)
          "Should generate string output for basic group")
      (is (re-find #"q\n" result)
          "Should start with graphics state save operator")
      (is (re-find #"10 20 100 50 re\n" result)
          "Should contain child element operations")
      (is (re-find #"Q$" result)
          "Should end with graphics state restore operator"))
    
    (let [result (hiccup->pdf-ops [:g {} [:circle {:cx 50 :cy 50 :r 25}]])]
      (is (string? result)
          "Should generate string output for group with circle")
      (is (re-find #"q\n" result)
          "Should start with save operator")
      (is (re-find #"Q$" result)
          "Should end with restore operator")))
  
  (testing "Group element validation errors"
    (is (thrown? js/Error (hiccup->pdf-ops [:g "invalid-attributes"]))
        "Should throw error for non-map attributes"))
  
  (testing "Empty group"
    (let [result (hiccup->pdf-ops [:g {}])]
      (is (string? result)
          "Should handle empty group")
      (is (= "q\nQ" result)
          "Should contain only save and restore operators"))))

(deftest nested-group-test
  (testing "Nested groups"
    (let [result (hiccup->pdf-ops [:g {} 
                                   [:rect {:x 10 :y 20 :width 100 :height 50}]
                                   [:g {} 
                                    [:circle {:cx 50 :cy 50 :r 25}]]])]
      (is (string? result)
          "Should generate string output for nested groups")
      (is (re-find #"q\n" result)
          "Should start with outer group save")
      (is (re-find #"10 20 100 50 re\n" result)
          "Should contain rectangle from outer group")
      (is (re-find #"Q$" result)
          "Should end with outer group restore"))
    
    (let [result (hiccup->pdf-ops [:g {}
                                   [:g {} [:rect {:x 0 :y 0 :width 10 :height 10}]]
                                   [:g {} [:rect {:x 20 :y 20 :width 30 :height 30}]]])]
      (is (string? result)
          "Should handle multiple nested groups")
      (is (re-find #"0 0 10 10 re\n" result)
          "Should contain first nested rectangle")
      (is (re-find #"20 20 30 30 re\n" result)
          "Should contain second nested rectangle")))
  
  (testing "Multiple elements in group"
    (let [result (hiccup->pdf-ops [:g {}
                                   [:rect {:x 10 :y 20 :width 100 :height 50}]
                                   [:circle {:cx 50 :cy 50 :r 25}]
                                   [:text {:x 10 :y 10 :font "Arial" :size 12} "Test"]])]
      (is (string? result)
          "Should handle multiple elements in one group")
      (is (re-find #"q\n" result)
          "Should start with save operator")
      (is (re-find #"re\n" result)
          "Should contain rectangle")
      (is (re-find #"c\n" result)
          "Should contain circle curves")
      (is (re-find #"BT\n" result)
          "Should contain text block")
      (is (re-find #"Q$" result)
          "Should end with restore operator"))))

(deftest group-transforms-test
  (testing "Group with translate transform"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 20]]]} 
                                   [:rect {:x 0 :y 0 :width 50 :height 30}]])]
      (is (string? result)
          "Should generate string output for translated group")
      (is (re-find #"q\n" result)
          "Should start with save operator")
      (is (re-find #"1 0 0 1 10 20 cm\n" result)
          "Should contain translate matrix")
      (is (re-find #"0 0 50 30 re\n" result)
          "Should contain child element")
      (is (re-find #"Q$" result)
          "Should end with restore operator")))
  
  (testing "Group with rotate transform"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:rotate 90]]} 
                                   [:rect {:x 0 :y 0 :width 50 :height 30}]])]
      (is (string? result)
          "Should generate string output for rotated group")
      (is (re-find #"q\n" result)
          "Should start with save operator")
      ;; 90 degrees = cos(90)≈0, sin(90)=1, so matrix should be approximately [0 1 -1 0 0 0]
      (is (and (re-find #"1 -1" result) (re-find #"cm\n" result))
          "Should contain 90-degree rotation matrix")
      (is (re-find #"Q$" result)
          "Should end with restore operator")))
  
  (testing "Group with scale transform"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:scale [2 3]]]} 
                                   [:rect {:x 0 :y 0 :width 50 :height 30}]])]
      (is (string? result)
          "Should generate string output for scaled group")
      (is (re-find #"q\n" result)
          "Should start with save operator")
      (is (re-find #"2 0 0 3 0 0 cm\n" result)
          "Should contain scale matrix")
      (is (re-find #"Q$" result)
          "Should end with restore operator")))
  
  (testing "Group with multiple transforms"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 20]] [:scale [2 2]]]} 
                                   [:rect {:x 0 :y 0 :width 50 :height 30}]])]
      (is (string? result)
          "Should generate string output for group with multiple transforms")
      (is (re-find #"q\n" result)
          "Should start with save operator")
      ;; Combined matrix: translate(10,20) * scale(2,2) = [2 0 0 2 20 40]
      (is (re-find #"2 0 0 2 20 40 cm\n" result)
          "Should contain combined transformation matrix")
      (is (re-find #"Q$" result)
          "Should end with restore operator"))))

(deftest nested-group-transforms-test
  (testing "Nested groups with transforms"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 10]]]}
                                   [:rect {:x 0 :y 0 :width 20 :height 20}]
                                   [:g {:transforms [[:scale [2 2]]]}
                                    [:rect {:x 5 :y 5 :width 10 :height 10}]]])]
      (is (string? result)
          "Should generate string output for nested transformed groups")
      (is (re-find #"q\n" result)
          "Should start with outer save operator")
      (is (re-find #"1 0 0 1 10 10 cm\n" result)
          "Should contain outer translate matrix")
      (is (re-find #"0 0 20 20 re\n" result)
          "Should contain outer rectangle")
      (is (re-find #"2 0 0 2 0 0 cm\n" result)
          "Should contain inner scale matrix")
      (is (re-find #"5 5 10 10 re\n" result)
          "Should contain inner rectangle")
      (is (re-find #"Q$" result)
          "Should end with outer restore operator")))
  
  (testing "Transform composition order"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:scale [2 2]] [:translate [5 5]]]}
                                   [:rect {:x 0 :y 0 :width 10 :height 10}]])]
      (is (string? result)
          "Should generate string output for composed transforms")
      ;; scale(2,2) * translate(5,5) = [2 0 0 2 5 5] (scale applied first, then translate)
      (is (re-find #"2 0 0 2 5 5 cm\n" result)
          "Should contain correctly composed transformation matrix")))
  
  (testing "Transform with different elements"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:rotate 45]]}
                                   [:rect {:x 0 :y 0 :width 10 :height 10}]
                                   [:circle {:cx 0 :cy 0 :r 5}]
                                   [:text {:x 0 :y 0 :font "Arial" :size 12} "Test"]])]
      (is (string? result)
          "Should handle transforms with mixed element types")
      (is (re-find #"re\n" result)
          "Should contain rectangle")
      (is (re-find #"c\n" result)
          "Should contain circle curves")
      (is (re-find #"BT\n" result)
          "Should contain text block"))))

(deftest complex-nested-groups-test
  (testing "Deeply nested groups with transforms"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [100 100]]]}
                                   [:g {:transforms [[:rotate 45]]}
                                    [:g {:transforms [[:scale [0.5 0.5]]]}
                                     [:rect {:x 0 :y 0 :width 20 :height 20}]]]])]
      (is (string? result)
          "Should handle deeply nested groups")
      ;; Should have 3 q/Q pairs for 3 nested groups
      (is (= 3 (count (re-seq #"q\n" result)))
          "Should have 3 save operators for 3 nested groups")
      (is (= 3 (count (re-seq #"Q" result)))
          "Should have 3 restore operators for 3 nested groups")
      (is (re-find #"1 0 0 1 100 100 cm\n" result)
          "Should contain outer translate transform")
      (is (re-find #"0.5 0 0 0.5 0 0 cm\n" result)
          "Should contain inner scale transform")
      (is (re-find #"0 0 20 20 re\n" result)
          "Should contain rectangle")))
  
  (testing "Complex transform composition in nested groups"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 20]] [:scale [2 2]]]}
                                   [:rect {:x 5 :y 5 :width 10 :height 10}]
                                   [:g {:transforms [[:rotate 90] [:translate [5 0]]]}
                                    [:circle {:cx 0 :cy 0 :r 3}]
                                    [:g {:transforms [[:scale [3 1]]]}
                                     [:text {:x 0 :y 0 :font "Arial" :size 10} "Test"]]]])]
      (is (string? result)
          "Should handle complex nested transforms")
      (is (re-find #"BT\n" result)
          "Should contain text within deeply nested group")
      (is (re-find #"re\n" result)
          "Should contain rectangle in outer group")
      (is (re-find #"c\n" result)
          "Should contain circle in middle group")))
  
  (testing "Transform isolation between sibling groups"
    (let [result (hiccup->pdf-ops [:g {}
                                   [:g {:transforms [[:scale [2 2]]]}
                                    [:rect {:x 0 :y 0 :width 5 :height 5}]]
                                   [:g {:transforms [[:translate [10 10]]]}
                                    [:rect {:x 0 :y 0 :width 5 :height 5}]]])]
      (is (string? result)
          "Should handle sibling groups with different transforms")
      (is (re-find #"2 0 0 2 0 0 cm\n" result)
          "Should contain scale transform for first group")
      (is (re-find #"1 0 0 1 10 10 cm\n" result)
          "Should contain translate transform for second group")
      ;; Both rectangles should appear with same coordinates but different transforms
      (is (= 2 (count (re-seq #"0 0 5 5 re\n" result)))
          "Should contain two rectangles with same coordinates"))))

(deftest coordinate-system-tests
  (testing "Coordinate system transformations"
    ;; Test that transforms work as expected mathematically
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 20]]]}
                                   [:rect {:x 5 :y 5 :width 10 :height 10}]])]
      (is (re-find #"1 0 0 1 10 20 cm\n" result)
          "Translate should move coordinate system origin")
      (is (re-find #"5 5 10 10 re\n" result)
          "Rectangle coordinates should remain unchanged in local space")))
  
  (testing "Multiple transform applications"
    ;; Test that multiple transforms compose correctly
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [5 5]] [:scale [2 2]] [:translate [10 10]]]}
                                   [:rect {:x 0 :y 0 :width 1 :height 1}]])]
      (is (string? result)
          "Should handle multiple sequential transforms")
      ;; Final matrix should be: translate(5,5) * scale(2,2) * translate(10,10)
      ;; = [1 0 0 1 5 5] * [2 0 0 2 0 0] * [1 0 0 1 10 10] = [2 0 0 2 20 20]
      (is (re-find #"2 0 0 2 20 20 cm\n" result)
          "Should compose multiple transforms correctly")))
  
  (testing "Identity transforms"
    ;; Test edge cases with identity-like transforms
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [0 0]] [:scale [1 1]] [:rotate 0]]}
                                   [:rect {:x 10 :y 20 :width 30 :height 40}]])]
      (is (string? result)
          "Should handle identity transforms")
      ;; Identity matrix: [1 0 0 1 0 0]
      (is (re-find #"1 0 0 1 0 0 cm\n" result)
          "Should result in identity matrix for identity transforms"))))

(deftest pdf-operator-ordering-test
  (testing "PDF operator ordering in groups"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 10]]]}
                                   [:rect {:x 0 :y 0 :width 5 :height 5 :fill "red"}]
                                   [:text {:x 0 :y 0 :font "Arial" :size 12} "Test"]])]
      (is (string? result)
          "Should generate valid PDF operators")
      ;; Check that operators appear in correct order
      (let [parts (str/split result #"\n")]
        (is (= "q" (first parts))
            "Should start with graphics state save")
        (is (some #(str/includes? % "cm") parts)
            "Should contain transformation matrix")
        (is (some #(str/includes? % "re") parts)
            "Should contain rectangle operator")
        (is (some #(str/includes? % "BT") parts)
            "Should contain text begin operator")
        (is (str/ends-with? (str/join "" (remove str/blank? parts)) "Q")
            "Should end with graphics state restore"))))
  
  (testing "Nested group operator ordering"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:scale [2 2]]]}
                                   [:g {:transforms [[:translate [5 5]]]}
                                    [:rect {:x 0 :y 0 :width 10 :height 10}]]])]
      ;; Should have pattern: q cm q cm rect Q Q
      (is (re-find #"q\n.*cm\n.*q\n.*cm\n.*re\n.*Q.*Q" result)
          "Should have correct nesting pattern for nested groups")))
  
  (testing "Graphics state isolation verification"
    ;; Test that transforms don't leak between groups
    (let [result (hiccup->pdf-ops [:g {}
                                   [:g {:transforms [[:scale [10 10]]]}
                                    [:rect {:x 1 :y 1 :width 1 :height 1}]]
                                   [:rect {:x 2 :y 2 :width 2 :height 2}]])]
      (is (string? result)
          "Should isolate transforms between groups")
      (is (re-find #"1 1 1 1 re\n" result)
          "Should contain first rectangle")
      (is (re-find #"2 2 2 2 re\n" result)
          "Should contain second rectangle with original coordinates"))))

(deftest integration-complex-scenarios-test
  (testing "Complex document with mixed elements and transforms"
    ;; Simulate a complex PDF document structure
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [50 50]]]}
                                   ;; Header section
                                   [:text {:x 0 :y 0 :font "Arial" :size 16 :fill "blue"} "Document Title"]
                                   
                                   ;; Transformed content section
                                   [:g {:transforms [[:scale [0.8 0.8]] [:translate [0 30]]]}
                                    [:rect {:x 0 :y 0 :width 200 :height 1 :fill "black"}] ; Line
                                    [:text {:x 0 :y 15 :font "Arial" :size 12} "Content Section"]]
                                   
                                   ;; Graphics section with nested transforms
                                   [:g {:transforms [[:translate [0 100]]]}
                                    [:g {:transforms [[:rotate 45]]}
                                     [:rect {:x 0 :y 0 :width 20 :height 20 :fill "red"}]]
                                    [:g {:transforms [[:translate [50 0]] [:scale [1.5 1.5]]]}
                                     [:circle {:cx 0 :cy 0 :r 10 :fill "green"}]]]])]
      (is (string? result)
          "Should handle complex document structure")
      (is (re-find #"BT\n" result)
          "Should contain text elements")
      (is (re-find #"re\n" result)
          "Should contain rectangle elements")
      (is (re-find #"c\n" result)
          "Should contain circle elements")
      ;; Verify multiple transform levels
      (is (>= (count (re-seq #"q\n" result)) 4)
          "Should have multiple graphics state saves")
      (is (>= (count (re-seq #"Q" result)) 4)
          "Should have matching graphics state restores")))
  
  (testing "Edge case: deeply nested empty groups"
    (let [result (hiccup->pdf-ops [:g {:transforms [[:translate [10 10]]]}
                                   [:g {}
                                    [:g {:transforms [[:scale [2 2]]]}
                                     [:g {}
                                      [:g {:transforms [[:rotate 90]]}]]]]])]
      (is (string? result)
          "Should handle deeply nested empty groups")
      (is (= 5 (count (re-seq #"q\n" result)))
          "Should have save operators for all groups")
      (is (= 5 (count (re-seq #"Q" result)))
          "Should have restore operators for all groups")))
  
  (testing "Performance test: many sibling groups"
    (let [many-groups (vec (concat [:g {}]
                                   (for [i (range 10)]
                                     [:g {:transforms [[:translate [i i]]]}
                                      [:rect {:x 0 :y 0 :width 1 :height 1}]])))
          result (hiccup->pdf-ops many-groups)]
      (is (string? result)
          "Should handle many sibling groups")
      (is (= 11 (count (re-seq #"q\n" result)))
          "Should have save operators for all groups")
      (is (= 10 (count (re-seq #"re\n" result)))
          "Should have rectangles for all child groups")))
  
  (testing "Mixed group and non-group elements"
    (let [result (hiccup->pdf-ops [:g {}
                                   [:rect {:x 0 :y 0 :width 10 :height 10}]
                                   [:g {:transforms [[:translate [20 20]]]}
                                    [:circle {:cx 0 :cy 0 :r 5}]]
                                   [:text {:x 5 :y 5 :font "Arial" :size 10} "Mixed"]
                                   [:g {:transforms [[:scale [2 2]]]}
                                    [:path {:d "M0,0 L5,5"}]]])]
      (is (string? result)
          "Should handle mixed element types")
      (is (re-find #"re\n" result)
          "Should contain rectangle")
      (is (re-find #"c\n" result)
          "Should contain circle")
      (is (re-find #"BT\n" result)
          "Should contain text")
      (is (re-find #"m\n" result)
          "Should contain path"))))

(deftest comprehensive-error-handling-test
  (testing "Rectangle validation errors"
    ;; Test missing required attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:rect {}]))
        "Should throw validation error for missing rect attributes")
    
    (is (thrown-with-msg? js/Error #"Expected number"
                          (hiccup->pdf-ops [:rect {:x 10}]))
        "Should throw error for incomplete rect attributes")
    
    ;; Test invalid attribute types
    (is (thrown-with-msg? js/Error #"Expected number.*invalid"
                          (hiccup->pdf-ops [:rect {:x "invalid" :y 10 :width 20 :height 30}]))
        "Should throw error for non-numeric rect coordinate")
    
    (is (thrown-with-msg? js/Error #"Assert failed.*invalid-color"
                          (hiccup->pdf-ops [:rect {:x 10 :y 10 :width 20 :height 30 :fill "invalid-color"}]))
        "Should throw error for invalid rect fill color"))
  
  (testing "Circle validation errors"
    ;; Test missing required attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:circle {}]))
        "Should throw error for missing circle attributes")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:circle {:cx 10 :cy 10}]))
        "Should throw error for missing circle radius")
    
    ;; Test invalid attribute values
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:circle {:cx 10 :cy 10 :r -5}]))
        "Should throw error for negative circle radius")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:circle {:cx "invalid" :cy 10 :r 5}]))
        "Should throw error for non-numeric circle coordinate"))
  
  (testing "Line validation errors"
    ;; Test missing required attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:line {}]))
        "Should throw error for missing line attributes")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:line {:x1 10 :y1 10}]))
        "Should throw error for incomplete line coordinates")
    
    ;; Test invalid attribute types
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:line {:x1 "invalid" :y1 10 :x2 20 :y2 30}]))
        "Should throw error for non-numeric line coordinate"))
  
  (testing "Text validation errors"
    ;; Test missing required attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:text {} "Hello"]))
        "Should throw error for missing text attributes")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:text {:x 10 :y 10} "Hello"]))
        "Should throw error for missing text font/size")
    
    ;; Test invalid attribute values
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:text {:x 10 :y 10 :font "" :size 12} "Hello"]))
        "Should throw error for empty text font")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:text {:x 10 :y 10 :font "Arial" :size 0} "Hello"]))
        "Should throw error for zero text size"))
  
  (testing "Path validation errors"
    ;; Test missing required attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:path {}]))
        "Should throw error for missing path data")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:path {:d ""}]))
        "Should throw error for empty path data")
    
    ;; Test invalid attribute types
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:path {:d 123}]))
        "Should throw error for non-string path data")))

(deftest transform-error-handling-test
  (testing "Transform validation errors"
    ;; Test invalid transform structure
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms ["invalid"]} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for invalid transform structure")
    
    ;; Test unsupported transform type
    (is (thrown-with-msg? js/Error #"Unsupported transform type.*invalid"
                          (hiccup->pdf-ops [:g {:transforms [[:invalid [10 20]]]} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for unsupported transform type")
    
    ;; Test invalid translate arguments
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms [[:translate [10]]]} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for incomplete translate arguments")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms [[:translate ["invalid" 20]]]} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for non-numeric translate arguments")
    
    ;; Test invalid rotate arguments
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms [[:rotate "invalid"]]} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for non-numeric rotate argument")
    
    ;; Test invalid scale arguments
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms [[:scale [2]]]} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for incomplete scale arguments"))
  
  (testing "Group validation errors"
    ;; Test invalid group attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms "invalid"} 
                                           [:rect {:x 0 :y 0 :width 10 :height 10}]]))
        "Should throw error for invalid group transforms attribute")))

(deftest hiccup-structure-error-handling-test
  (testing "Basic hiccup structure validation errors"
    ;; Test non-vector input
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops "not-a-vector"))
        "Should throw error for non-vector hiccup")
    
    ;; Test empty vector
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops []))
        "Should throw error for empty hiccup vector")
    
    ;; Test missing attributes map
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:rect]))
        "Should throw error for missing attributes map")
    
    ;; Test non-keyword element type
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops ["rect" {:x 10 :y 10 :width 20 :height 30}]))
        "Should throw error for non-keyword element type")
    
    ;; Test non-map attributes
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:rect "not-a-map"]))
        "Should throw error for non-map attributes"))
  
  (testing "Element type validation errors"
    ;; Test unsupported element type
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:unsupported {:x 10 :y 10}]))
        "Should throw error for unsupported element type"))
  
  (testing "Incremental processing error verification"
    ;; Test that errors are thrown immediately, not after processing other elements
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {}
                                           [:rect {:x 10 :y 10 :width 20 :height 30}] ; Valid
                                           [:rect {:invalid "attributes"}]             ; Invalid
                                           [:rect {:x 20 :y 20 :width 30 :height 40}]])) ; Would be valid
        "Should throw error immediately when invalid element is encountered")))