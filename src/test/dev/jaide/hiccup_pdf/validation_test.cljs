(ns dev.jaide.hiccup-pdf.validation-test
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.validation :refer [validate-hiccup-structure
                                           validate-element-type
                                           validate-attributes
                                           validate-rect-attributes
                                           validate-line-attributes
                                           validate-circle-attributes
                                           validate-path-attributes
                                           validate-text-attributes
                                           validate-group-attributes
                                           validate-transform
                                           validate-transforms
                                           validate-color
                                           validate-image-attributes
                                           validate-document-attributes
                                           validate-page-attributes]]))

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
        "Should validate group element type")
    
    (is (= :image (validate-element-type :image))
        "Should validate image element type")))

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
    (is (= "#ff0000" (validate-color "#ff0000"))
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
    (is (= {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}
           (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}))
        "Should validate rectangle with fill")
    
    (is (= {:x 10 :y 20 :width 100 :height 50 :stroke "#0000ff" :stroke-width 2}
           (validate-rect-attributes {:x 10 :y 20 :width 100 :height 50 :stroke "#0000ff" :stroke-width 2}))
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
    (is (= {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#ff0000"}
           (validate-line-attributes {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#ff0000"}))
        "Should validate line with stroke")
    
    (is (= {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#0000ff" :stroke-width 2}
           (validate-line-attributes {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#0000ff" :stroke-width 2}))
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
    (is (= {:cx 50 :cy 50 :r 25 :fill "#ff0000"}
           (validate-circle-attributes {:cx 50 :cy 50 :r 25 :fill "#ff0000"}))
        "Should validate circle with fill")
    
    (is (= {:cx 50 :cy 50 :r 25 :stroke "#0000ff" :stroke-width 2}
           (validate-circle-attributes {:cx 50 :cy 50 :r 25 :stroke "#0000ff" :stroke-width 2}))
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
    (is (= {:d "M10,10 L20,20" :fill "#ff0000"}
           (validate-path-attributes {:d "M10,10 L20,20" :fill "#ff0000"}))
        "Should validate path with fill")
    
    (is (= {:d "M10,10 L20,20" :stroke "#0000ff" :stroke-width 2}
           (validate-path-attributes {:d "M10,10 L20,20" :stroke "#0000ff" :stroke-width 2}))
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

(deftest validate-text-attributes-test
  (testing "Valid text attributes"
    (is (= {:x 10 :y 20 :font "Arial" :size 12}
           (validate-text-attributes {:x 10 :y 20 :font "Arial" :size 12}))
        "Should validate basic text attributes")
    
    (is (= {:x 0 :y 0 :font "Times" :size 8}
           (validate-text-attributes {:x 0 :y 0 :font "Times" :size 8}))
        "Should validate minimal text attributes"))
  
  (testing "Text attributes with styling"
    (is (= {:x 10 :y 20 :font "Arial" :size 12 :fill "#ff0000"}
           (validate-text-attributes {:x 10 :y 20 :font "Arial" :size 12 :fill "#ff0000"}))
        "Should validate text with fill")
    
    (is (= {:x 10 :y 20 :font "Arial" :size 12 :fill "#ff0000"}
           (validate-text-attributes {:x 10 :y 20 :font "Arial" :size 12 :fill "#ff0000"}))
        "Should validate text with hex color fill"))
  
  (testing "Invalid text attributes"
    (is (thrown? js/Error (validate-text-attributes {}))
        "Should throw error for missing attributes")
    
    (is (thrown? js/Error (validate-text-attributes {:x 10 :y 20 :font "Arial"}))
        "Should throw error for missing size")
    
    (is (thrown? js/Error (validate-text-attributes {:x 10 :y 20 :size 12}))
        "Should throw error for missing font")
    
    (is (thrown? js/Error (validate-text-attributes {:x "10" :y 20 :font "Arial" :size 12}))
        "Should throw error for non-numeric x")
    
    (is (thrown? js/Error (validate-text-attributes {:x 10 :y 20 :font "" :size 12}))
        "Should throw error for empty font name")
    
    (is (thrown? js/Error (validate-text-attributes {:x 10 :y 20 :font "Arial" :size 0}))
        "Should throw error for zero size")
    
    (is (thrown? js/Error (validate-text-attributes {:x 10 :y 20 :font "Arial" :size -5}))
        "Should throw error for negative size")
    
    (is (thrown? js/Error (validate-text-attributes {:x 10 :y 20 :font "Arial" :size 12 :fill "invalid"}))
        "Should throw error for invalid fill color")))

(deftest validate-image-attributes-test
  (testing "Valid image attributes"
    (is (= {:src "path/to/image.png" :width 100 :height 50 :x 10 :y 20}
           (validate-image-attributes {:src "path/to/image.png" :width 100 :height 50 :x 10 :y 20}))
        "Should validate complete image attributes")
    
    (is (= {:src "emoji.png" :width 72 :height 72 :x 0 :y 0}
           (validate-image-attributes {:src "emoji.png" :width 72 :height 72 :x 0 :y 0}))
        "Should validate emoji image attributes")
    
    (is (= {:src "emojis/noto-72/emoji_u1f4a1.png" :width 1 :height 1 :x -10 :y -5}
           (validate-image-attributes {:src "emojis/noto-72/emoji_u1f4a1.png" :width 1 :height 1 :x -10 :y -5}))
        "Should validate minimal dimensions and negative coordinates"))
  
  (testing "Invalid image attributes - missing required"
    (is (thrown? js/Error (validate-image-attributes {}))
        "Should throw error for missing all attributes")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height 50 :x 10}))
        "Should throw error for missing y coordinate")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height 50 :y 20}))
        "Should throw error for missing x coordinate")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :x 10 :y 20}))
        "Should throw error for missing height")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :height 50 :x 10 :y 20}))
        "Should throw error for missing width")
    
    (is (thrown? js/Error (validate-image-attributes {:width 100 :height 50 :x 10 :y 20}))
        "Should throw error for missing src"))
  
  (testing "Invalid image attributes - invalid types"
    (is (thrown? js/Error (validate-image-attributes {:src 123 :width 100 :height 50 :x 10 :y 20}))
        "Should throw error for non-string src")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width "100" :height 50 :x 10 :y 20}))
        "Should throw error for non-numeric width")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height "50" :x 10 :y 20}))
        "Should throw error for non-numeric height")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height 50 :x "10" :y 20}))
        "Should throw error for non-numeric x")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height 50 :x 10 :y "20"}))
        "Should throw error for non-numeric y"))
  
  (testing "Invalid image attributes - invalid values"
    (is (thrown? js/Error (validate-image-attributes {:src "" :width 100 :height 50 :x 10 :y 20}))
        "Should throw error for empty src")
    
    (is (thrown? js/Error (validate-image-attributes {:src "   " :width 100 :height 50 :x 10 :y 20}))
        "Should throw error for whitespace-only src")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 0 :height 50 :x 10 :y 20}))
        "Should throw error for zero width")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width -5 :height 50 :x 10 :y 20}))
        "Should throw error for negative width")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height 0 :x 10 :y 20}))
        "Should throw error for zero height")
    
    (is (thrown? js/Error (validate-image-attributes {:src "test.png" :width 100 :height -10 :x 10 :y 20}))
        "Should throw error for negative height"))
  
  (testing "Edge cases"
    (is (= {:src "test.png" :width 0.1 :height 0.1 :x 10.5 :y 20.7}
           (validate-image-attributes {:src "test.png" :width 0.1 :height 0.1 :x 10.5 :y 20.7}))
        "Should validate fractional dimensions and coordinates")
    
    (is (= {:src "very/long/path/to/some/nested/directory/image.png" :width 1000 :height 800 :x -100 :y -200}
           (validate-image-attributes {:src "very/long/path/to/some/nested/directory/image.png" :width 1000 :height 800 :x -100 :y -200}))
        "Should validate long paths and large dimensions")
    
    (is (= {:src "test.PNG" :width 72 :height 72 :x 0 :y 0}
           (validate-image-attributes {:src "test.PNG" :width 72 :height 72 :x 0 :y 0}))
        "Should validate uppercase file extensions")))

(deftest validate-transform-test
  (testing "Valid transform operations"
    (is (= [:translate [10 20]]
           (validate-transform [:translate [10 20]]))
        "Should validate translate transform")
    
    (is (= [:rotate 45]
           (validate-transform [:rotate 45]))
        "Should validate rotate transform")
    
    (is (= [:scale [2 3]]
           (validate-transform [:scale [2 3]]))
        "Should validate scale transform"))
  
  (testing "Invalid transform operations"
    (is (thrown? js/Error (validate-transform [:translate [10]]))
        "Should throw error for incomplete translate args")
    
    (is (thrown? js/Error (validate-transform [:rotate [45]]))
        "Should throw error for vector rotate args")
    
    (is (thrown? js/Error (validate-transform [:scale [2]]))
        "Should throw error for incomplete scale args")
    
    (is (thrown? js/Error (validate-transform [:invalid [10 20]]))
        "Should throw error for unsupported transform type")))

(deftest validate-transforms-test
  (testing "Valid transform vectors"
    (is (= [[:translate [10 20]]]
           (validate-transforms [[:translate [10 20]]]))
        "Should validate single transform")
    
    (is (= [[:translate [10 20]] [:rotate 45]]
           (validate-transforms [[:translate [10 20]] [:rotate 45]]))
        "Should validate multiple transforms")
    
    (is (= []
           (validate-transforms []))
        "Should validate empty transforms vector"))
  
  (testing "Invalid transform vectors"
    (is (thrown? js/Error (validate-transforms "not-a-vector"))
        "Should throw error for non-vector")
    
    (is (thrown? js/Error (validate-transforms [[:invalid [10 20]]]))
        "Should throw error for invalid transform type")))

(deftest validate-group-attributes-test
  (testing "Valid group attributes"
    (is (= {}
           (validate-group-attributes {}))
        "Should validate empty group attributes")
    
    (is (= {:transforms [[:translate [10 20]]]}
           (validate-group-attributes {:transforms [[:translate [10 20]]]}))
        "Should validate group with transforms")
    
    (is (= {:transforms [[:translate [10 20]] [:rotate 45] [:scale [2 3]]]}
           (validate-group-attributes {:transforms [[:translate [10 20]] [:rotate 45] [:scale [2 3]]]}))
        "Should validate group with multiple transforms"))
  
  (testing "Invalid group attributes"
    (is (thrown? js/Error (validate-group-attributes {:transforms "not-a-vector"}))
        "Should throw error for non-vector transforms")
    
    (is (thrown? js/Error (validate-group-attributes {:transforms [[:invalid [10 20]]]}))
        "Should throw error for invalid transform type")))

(deftest validate-page-attributes-test
  (testing "Page attributes with no document defaults"
    ;; Test with empty page attributes
    (let [result (validate-page-attributes {})]
      (is (= {} result)
          "Should handle empty page attributes"))
    
    ;; Test with page-specific attributes
    (let [result (validate-page-attributes {:width 595 :height 842})]
      (is (= {:width 595 :height 842} result)
          "Should validate page dimensions"))
    
    ;; Test with margins
    (let [result (validate-page-attributes {:margins [10 20 30 40]})]
      (is (= {:margins [10 20 30 40]} result)
          "Should validate page margins")))
  
  (testing "Page attributes with document defaults inheritance"
    ;; Test full inheritance
    (let [document-defaults {:width 612 :height 792 :margins [5 5 5 5]}
          result (validate-page-attributes {} document-defaults)]
      (is (= {:width 612 :height 792 :margins [5 5 5 5]} result)
          "Should inherit all document defaults"))
    
    ;; Test partial override
    (let [document-defaults {:width 612 :height 792 :margins [5 5 5 5]}
          result (validate-page-attributes {:width 595} document-defaults)]
      (is (= {:width 595 :height 792 :margins [5 5 5 5]} result)
          "Should override width but inherit height and margins"))
    
    ;; Test complete override
    (let [document-defaults {:width 612 :height 792 :margins [5 5 5 5]}
          result (validate-page-attributes {:width 595 :height 842 :margins [10 10 10 10]} document-defaults)]
      (is (= {:width 595 :height 842 :margins [10 10 10 10]} result)
          "Should override all document defaults"))
    
    ;; Test landscape orientation (width/height swap)
    (let [document-defaults {:width 612 :height 792}
          result (validate-page-attributes {:width 792 :height 612} document-defaults)]
      (is (= {:width 792 :height 612} result)
          "Should handle landscape orientation (width/height swap)"))
    
    ;; Test non-inheritable attributes are not inherited
    (let [document-defaults {:width 612 :height 792 :title "Document Title" :author "Author"}
          result (validate-page-attributes {} document-defaults)]
      (is (= {:width 612 :height 792} result)
          "Should only inherit width, height, and margins")))
  
  (testing "Page attributes validation errors"
    ;; Test invalid width
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:width -100}))
        "Should reject negative width")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:width "invalid"}))
        "Should reject non-numeric width")
    
    ;; Test invalid height
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:height 0}))
        "Should reject zero height")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:height "invalid"}))
        "Should reject non-numeric height")
    
    ;; Test invalid margins
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:margins [10 20 30]}))
        "Should reject margins with wrong number of elements")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:margins "invalid"}))
        "Should reject non-vector margins")
    
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:margins [10 "invalid" 30 40]}))
        "Should reject margins with non-numeric elements"))
  
  (testing "Page attributes inheritance edge cases"
    ;; Test inheritance with invalid document defaults (should still work)
    (let [document-defaults {:width 612 :height 792 :invalid-key "should-be-ignored"}
          result (validate-page-attributes {:margins [1 2 3 4]} document-defaults)]
      (is (= {:width 612 :height 792 :margins [1 2 3 4]} result)
          "Should inherit valid attributes and ignore invalid ones"))
    
    ;; Test with nil document defaults
    (let [result (validate-page-attributes {:width 595} nil)]
      (is (= {:width 595} result)
          "Should handle nil document defaults"))
    
    ;; Test complex inheritance scenario
    (let [document-defaults {:width 612 :height 792 :margins [5 5 5 5]}
          result (validate-page-attributes {:height 842} document-defaults)]
      (is (= {:width 612 :height 842 :margins [5 5 5 5]} result)
          "Should inherit width and margins, override height"))))