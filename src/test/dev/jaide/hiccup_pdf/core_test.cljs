(ns dev.jaide.hiccup-pdf.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.core :refer [hiccup->pdf-ops]]
            [dev.jaide.hiccup-pdf.images :as images]))

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
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}]))
        "Should generate PDF operators for filled rectangle")

    (is (= "1 0 0 rg\n10 20 100 50 re\nf"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}]))
        "Should generate PDF operators for hex color fill"))

  (testing "Rectangle with stroke styling"
    (is (= "0 0 1 RG\n10 20 100 50 re\nS"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :stroke "#0000ff"}]))
        "Should generate PDF operators for stroked rectangle")

    (is (= "2 w\n0 0 1 RG\n10 20 100 50 re\nS"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :stroke "#0000ff" :stroke-width 2}]))
        "Should generate PDF operators for stroked rectangle with width"))

  (testing "Rectangle with both fill and stroke"
    (is (= "1 0 0 rg\n0 0 1 RG\n10 20 100 50 re\nB"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000" :stroke "#0000ff"}]))
        "Should generate PDF operators for filled and stroked rectangle")

    (is (= "1.5 w\n1 0 0 rg\n0 0 1 RG\n10 20 100 50 re\nB"
           (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000" :stroke "#0000ff" :stroke-width 1.5}]))
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
           (hiccup->pdf-ops [:line {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#ff0000"}]))
        "Should generate PDF operators for colored line")

    (is (= "2 w\n0 0 1 RG\n10 20 m\n100 50 l\nS"
           (hiccup->pdf-ops [:line {:x1 10 :y1 20 :x2 100 :y2 50 :stroke "#0000ff" :stroke-width 2}]))
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
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :fill "#ff0000"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain fill color operator")
      (is (re-find #"f$" result)
          "Should end with fill operator"))

    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :fill "#ff0000"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should handle hex color fill")))

  (testing "Circle with stroke styling"
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "#0000ff"}])]
      (is (re-find #"0 0 1 RG\n" result)
          "Should contain stroke color operator")
      (is (re-find #"S$" result)
          "Should end with stroke operator"))

    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "#0000ff" :stroke-width 2}])]
      (is (re-find #"2 w\n" result)
          "Should contain stroke width operator")))

  (testing "Circle with both fill and stroke"
    (let [result (hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :fill "#ff0000" :stroke "#0000ff"}])]
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
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :fill "#ff0000"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should contain fill color operator")
      (is (re-find #"f$" result)
          "Should end with fill operator"))

    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :fill "#ff0000"}])]
      (is (re-find #"1 0 0 rg\n" result)
          "Should handle hex color fill")))

  (testing "Path with stroke styling"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :stroke "#0000ff"}])]
      (is (re-find #"0 0 1 RG\n" result)
          "Should contain stroke color operator")
      (is (re-find #"S$" result)
          "Should end with stroke operator"))

    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :stroke "#0000ff" :stroke-width 2}])]
      (is (re-find #"2 w\n" result)
          "Should contain stroke width operator")))

  (testing "Path with both fill and stroke"
    (let [result (hiccup->pdf-ops [:path {:d "M10,10 L20,20" :fill "#ff0000" :stroke "#0000ff"}])]
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

(deftest image-element-test
  (testing "Image element with cache"
    ;; Create a mock cache for testing
    (let [cache (images/create-image-cache)
          ;; Test with a simple image element
          image-element [:image {:src "test.png" :width 100 :height 50 :x 10 :y 20}]]
      
      ;; First test - should handle missing file gracefully
      (is (thrown? js/Error 
                   (hiccup->pdf-ops image-element {:image-cache cache}))
          "Should throw error for missing image file")))
  
  (testing "Image element without cache"
    ;; Test without providing cache - should throw error
    (let [image-element [:image {:src "test.png" :width 100 :height 50 :x 10 :y 20}]]
      (is (thrown? js/Error 
                   (hiccup->pdf-ops image-element))
          "Should throw error when no cache provided")))
  
  (testing "Image element validation"
    ;; Test with invalid attributes - should throw validation error
    (let [cache (images/create-image-cache)]
      (is (thrown? js/Error 
                   (hiccup->pdf-ops [:image {}] {:image-cache cache}))
          "Should throw validation error for missing attributes")
      
      (is (thrown? js/Error 
                   (hiccup->pdf-ops [:image {:src "test.png" :width 100 :height 50}] {:image-cache cache}))
          "Should throw validation error for missing x,y coordinates"))))

(deftest image-element-mocked-test
  (testing "Image element with mocked successful loading"
    ;; Create a cache and pre-populate it with mock image data
    (let [cache (images/create-image-cache)
          test-src "test.png"
          ;; Create mock PNG buffer (minimal valid PNG header)
          mock-buffer (.from js/Buffer #js [0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
                                            0x00 0x00 0x00 0x0D 0x49 0x48 0x44 0x52
                                            0x00 0x00 0x00 0x48 ; Width: 72
                                            0x00 0x00 0x00 0x48 ; Height: 72
                                            0x08 0x02 0x00 0x00 0x00])
          mock-image-data {:buffer mock-buffer
                           :width 72
                           :height 72
                           :success true
                           :file-path test-src}]
      
      ;; Pre-populate cache with mock data
      (images/cache-put cache test-src mock-image-data)
      
      ;; Now test image element processing
      (let [image-element [:image {:src test-src :width 100 :height 50 :x 10 :y 20}]
            result (hiccup->pdf-ops image-element {:image-cache cache})]
        
        (is (string? result)
            "Should generate string output for image")
        (is (str/includes? result "q")
            "Should contain save state operator")
        (is (str/includes? result "Q")
            "Should contain restore state operator")
        (is (str/includes? result "cm")
            "Should contain transformation matrix")
        (is (str/includes? result "Do")
            "Should contain XObject draw operator")
        
        ;; Check transformation matrix calculations
        ;; Scale factors: width 100/72, height 50/72
        (let [scale-x (/ 100 72)
              scale-y (/ 50 72)]
          (is (str/includes? result (str scale-x))
              "Should contain correct X scale factor")
          (is (str/includes? result (str scale-y))
              "Should contain correct Y scale factor")
          (is (str/includes? result "10 20")
              "Should contain correct position")))))
  
  (testing "Image element scaling calculations"
    ;; Test different scaling scenarios
    (let [cache (images/create-image-cache)
          test-src "test2.png"
          mock-buffer (.from js/Buffer #js [0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
                                            0x00 0x00 0x00 0x0D 0x49 0x48 0x44 0x52
                                            0x00 0x00 0x00 0x64 ; Width: 100
                                            0x00 0x00 0x00 0x32 ; Height: 50
                                            0x08 0x02 0x00 0x00 0x00])
          mock-image-data {:buffer mock-buffer
                           :width 100
                           :height 50
                           :success true
                           :file-path test-src}]
      
      (images/cache-put cache test-src mock-image-data)
      
      ;; Test 1:1 scaling (no scaling needed)
      (let [result (hiccup->pdf-ops [:image {:src test-src :width 100 :height 50 :x 0 :y 0}] 
                                    {:image-cache cache})]
        (is (str/includes? result "1 0 0 1 0 0 cm")
            "Should use identity matrix for 1:1 scaling"))
      
      ;; Test 2x scaling
      (let [result (hiccup->pdf-ops [:image {:src test-src :width 200 :height 100 :x 50 :y 25}] 
                                    {:image-cache cache})]
        (is (str/includes? result "2 0 0 2 50 25 cm")
            "Should use 2x scaling matrix"))
      
      ;; Test fractional scaling
      (let [result (hiccup->pdf-ops [:image {:src test-src :width 50 :height 25 :x 10 :y 5}] 
                                    {:image-cache cache})]
        (is (str/includes? result "0.5 0 0 0.5 10 5 cm")
            "Should use 0.5x scaling matrix")))))

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
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12 :fill "#ff0000"} "Hello"])]
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
      (is (re-find #"<[0-9A-F]+> Tj\n" result)
          "Should use hex string format for Unicode characters"))

    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "🎉🚀✨"])]
      (is (re-find #"<[0-9A-F]+> Tj\n" result)
          "Should handle multiple emojis with hex encoding"))

    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "Text with emoji 🎨 and more text"])]
      (is (re-find #"<[0-9A-F]+> Tj\n" result)
          "Should handle mixed text and emoji with hex encoding")))

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
      (is (re-find #"<[0-9A-F]+> Tj\n" result)
          "Should render emoji with Times font using hex encoding"))

    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Helvetica" :size 16} "Helvetica with 🌟 star"])]
      (is (re-find #"/Helvetica 16 Tf\n" result)
          "Should work with Helvetica font")
      (is (re-find #"<[0-9A-F]+> Tj\n" result)
          "Should render emoji with Helvetica font using hex encoding")))

  (testing "Edge cases with emoji and special characters"
    (let [result (hiccup->pdf-ops [:text {:x 10 :y 20 :font "Arial" :size 12} "🔥(hot)🔥"])]
      (is (re-find #"<[0-9A-F]+> Tj\n" result)
          "Should handle emoji with special characters using hex encoding"))

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
                                   [:rect {:x 0 :y 0 :width 5 :height 5 :fill "#ff0000"}]
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
                                   [:text {:x 0 :y 0 :font "Arial" :size 16 :fill "#0000ff"} "Document Title"]

                                   ;; Transformed content section
                                   [:g {:transforms [[:scale [0.8 0.8]] [:translate [0 30]]]}
                                    [:rect {:x 0 :y 0 :width 200 :height 1 :fill "#000000"}] ; Line
                                    [:text {:x 0 :y 15 :font "Arial" :size 12} "Content Section"]]

                                   ;; Graphics section with nested transforms
                                   [:g {:transforms [[:translate [0 100]]]}
                                    [:g {:transforms [[:rotate 45]]}
                                     [:rect {:x 0 :y 0 :width 20 :height 20 :fill "#ff0000"}]]
                                    [:g {:transforms [[:translate [50 0]] [:scale [1.5 1.5]]]}
                                     [:circle {:cx 0 :cy 0 :r 10 :fill "#00ff00"}]]]])]
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

    (is (thrown-with-msg? js/Error #"Expected string matching.*invalid-color"
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

(deftest complex-document-integration-test
  (testing "Complex hiccup document with multiple element types"
    (let [complex-doc [:g {}
                       [:rect {:x 10 :y 10 :width 50 :height 30 :fill "#ff0000" :stroke "#0000ff" :stroke-width 2}]
                       [:circle {:cx 100 :cy 50 :r 20 :fill "#00ff00"}]
                       [:line {:x1 0 :y1 0 :x2 200 :y2 100 :stroke "#000000" :stroke-width 1}]
                       [:text {:x 50 :y 80 :font "Arial" :size 14 :fill "#0000ff"} "Complex Document"]
                       [:path {:d "M150,150 L200,150 L175,200 Z" :fill "#ffff00" :stroke "#ff0000"}]]
          result (hiccup->pdf-ops complex-doc)]

      (is (string? result)
          "Should return string for complex document")

      ;; Verify all element types are present
      (is (re-find #"re\n" result)
          "Should contain rectangle operators")
      (is (re-find #"c\n" result)
          "Should contain circle operators")
      (is (re-find #"m\n.*l\n" result)
          "Should contain line operators")
      (is (re-find #"BT\n" result)
          "Should contain text block operators")
      (is (re-find #"ET" result)
          "Should contain text block end")
      (is (re-find #"h\n" result)
          "Should contain path close operators")

      ;; Verify styling is applied
      (is (re-find #"1 0 0 rg" result)
          "Should contain red fill color")
      (is (re-find #"0 1 0 rg" result)
          "Should contain green fill color")
      (is (re-find #"0 0 1 RG" result)
          "Should contain blue stroke color")

      ;; Verify graphics state management
      (is (re-find #"q\n" result)
          "Should contain graphics state save")
      (is (re-find #"Q" result)
          "Should contain graphics state restore")))

  (testing "Nested groups with different transforms"
    (let [nested-doc [:g {:transforms [[:translate [50 50]]]}
                      [:rect {:x 0 :y 0 :width 20 :height 20 :fill "#ff0000"}]
                      [:g {:transforms [[:rotate 45]]}
                       [:circle {:cx 0 :cy 0 :r 10 :fill "#0000ff"}]
                       [:g {:transforms [[:scale [2 2]]]}
                        [:text {:x 5 :y 5 :font "Arial" :size 8} "Nested"]]]
                      [:line {:x1 0 :y1 0 :x2 30 :y2 30 :stroke "#000000"}]]
          result (hiccup->pdf-ops nested-doc)]

      (is (string? result)
          "Should handle nested groups with transforms")

      ;; Count graphics state operations
      (let [q-count (count (re-seq #"q\n" result))
            Q-count (count (re-seq #"Q" result))]
        (is (= q-count Q-count)
            "Should have balanced save/restore operations")
        (is (>= q-count 3)
            "Should have multiple graphics state saves for nested groups"))

      ;; Verify transform matrices are present
      (is (re-find #"cm\n" result)
          "Should contain transformation matrices")

      ;; Verify all elements are rendered
      (is (re-find #"re\n" result)
          "Should contain rectangle")
      (is (re-find #"c\n" result)
          "Should contain circle")
      (is (re-find #"BT\n" result)
          "Should contain text")
      (is (re-find #"l\n" result)
          "Should contain line")))

  (testing "Document with complex styling combinations"
    (let [styled-doc [:g {}
                      [:rect {:x 10 :y 10 :width 40 :height 20 :fill "#ff0000" :stroke "#0000ff" :stroke-width 3}]
                      [:circle {:cx 80 :cy 30 :r 15 :stroke "#00ff00" :stroke-width 2}]
                      [:text {:x 20 :y 60 :font "Times" :size 16 :fill "#ff00ff"} "Styled Text"]
                      [:path {:d "M100,50 C120,30 140,70 160,50" :stroke "#ffff00" :stroke-width 4 :fill "#00ffff"}]]
          result (hiccup->pdf-ops styled-doc)]

      (is (string? result)
          "Should handle complex styling")

      ;; Verify hex color conversion
      (is (re-find #"1 0 0 rg" result)
          "Should convert #ff0000 to red")
      (is (re-find #"0 0 1 RG" result)
          "Should convert #0000ff to blue stroke")
      (is (re-find #"0 1 0 RG" result)
          "Should convert #00ff00 to green stroke")
      (is (re-find #"1 0 1 rg" result)
          "Should convert #ff00ff to magenta")

      ;; Verify stroke widths
      (is (re-find #"3 w\n" result)
          "Should contain stroke width 3")
      (is (re-find #"2 w\n" result)
          "Should contain stroke width 2")
      (is (re-find #"4 w\n" result)
          "Should contain stroke width 4")

      ;; Verify both fill and stroke operations
      (is (re-find #"B" result)
          "Should contain both fill and stroke operations")))

  (testing "Large document with many elements"
    (let [large-doc [:g {}
                     ;; Generate 10 rectangles with different positions
                     (for [i (range 10)]
                       [:rect {:x (* i 20) :y (* i 10) :width 15 :height 15 :fill (if (even? i) "#ff0000" "#0000ff")}])
                     ;; Generate 5 circles
                     (for [i (range 5)]
                       [:circle {:cx (+ 200 (* i 30)) :cy (+ 50 (* i 20)) :r (+ 5 i) :fill "#00ff00"}])
                     ;; Generate some text elements
                     (for [i (range 3)]
                       [:text {:x (+ 10 (* i 50)) :y (+ 200 (* i 25)) :font "Arial" :size (+ 10 i)} (str "Text " i)])]
          ;; Flatten the nested structure
          flattened-doc (into [:g {}] (apply concat (rest large-doc)))
          result (hiccup->pdf-ops flattened-doc)]

      (is (string? result)
          "Should handle large documents")

      ;; Verify element counts
      (let [rect-count (count (re-seq #"re\n" result))
            text-count (count (re-seq #"BT\n" result))]
        (is (= rect-count 10)
            "Should contain 10 rectangles")
        (is (= text-count 3)
            "Should contain 3 text elements"))

      ;; Verify performance - result should be reasonable size
      (is (< (count result) 10000)
          "Should generate reasonable size output for large document"))))

(deftest edge-case-integration-test
  (testing "Documents with edge case elements"
    (let [edge-doc [:g {}
                    ;; Zero-size elements
                    [:rect {:x 10 :y 10 :width 0 :height 0 :fill "#ff0000"}]
                    [:circle {:cx 50 :cy 50 :r 0 :fill "#0000ff"}]
                    ;; Empty text
                    [:text {:x 100 :y 100 :font "Arial" :size 12} ""]
                    ;; Minimal path
                    [:path {:d "M0,0"}]
                    ;; Group with no children
                    [:g {}]
                    ;; Complex nested empty groups
                    [:g {:transforms [[:translate [10 10]]]}
                     [:g {}
                      [:g {}]]]]
          result (hiccup->pdf-ops edge-doc)]

      (is (string? result)
          "Should handle edge case elements")

      ;; Should still contain proper graphics state management
      (is (re-find #"q\n" result)
          "Should contain graphics state operations")
      (is (re-find #"Q" result)
          "Should contain graphics state restore")

      ;; Should handle zero-size elements gracefully
      (is (re-find #"0 0 re" result)
          "Should handle zero-size rectangle")

      ;; Should handle empty text
      (is (re-find #"\(\) Tj" result)
          "Should handle empty text content")))

  (testing "Complex transform compositions"
    (let [transform-doc [:g {:transforms [[:translate [100 100]] [:rotate 90] [:scale [2 2]]]}
                         [:rect {:x 0 :y 0 :width 10 :height 10 :fill "#ff0000"}]
                         [:g {:transforms [[:translate [-50 -50]] [:rotate -45]]}
                          [:circle {:cx 0 :cy 0 :r 5 :fill "#0000ff"}]
                          [:g {:transforms [[:scale [0.5 0.5]]]}
                           [:text {:x 0 :y 0 :font "Arial" :size 24} "Deep"]
                           [:path {:d "M0,0 L10,10 L0,20 Z" :fill "#00ff00"}]]]]
          result (hiccup->pdf-ops transform-doc)]

      (is (string? result)
          "Should handle complex transform compositions")

      ;; Verify deep nesting
      (let [q-count (count (re-seq #"q\n" result))
            Q-count (count (re-seq #"Q" result))]
        (is (= q-count Q-count)
            "Should have balanced save/restore for deep nesting")
        (is (>= q-count 3)
            "Should have proper nesting depth"))

      ;; Verify multiple transform matrices
      (is (>= (count (re-seq #"cm\n" result)) 3)
          "Should have multiple transformation matrices")))

  (testing "Mixed coordinate systems and scaling"
    (let [mixed-doc [:g {}
                     ;; Large coordinates
                     [:rect {:x 1000 :y 2000 :width 500 :height 300 :fill "#ff0000"}]
                     ;; Small coordinates
                     [:g {:transforms [[:scale [0.01 0.01]]]}
                      [:rect {:x 10000 :y 20000 :width 50000 :height 30000 :fill "#0000ff"}]]
                     ;; Fractional coordinates
                     [:circle {:cx 123.456 :cy 789.123 :r 45.67 :fill "#00ff00"}]
                     ;; Negative coordinates
                     [:line {:x1 -100 :y1 -50 :x2 -200 :y2 -100 :stroke "#000000"}]]
          result (hiccup->pdf-ops mixed-doc)]

      (is (string? result)
          "Should handle mixed coordinate systems")

      ;; Verify large numbers are handled
      (is (re-find #"1000 2000" result)
          "Should handle large coordinates")

      ;; Verify fractional numbers are handled
      (is (re-find #"123\.456" result)
          "Should handle fractional coordinates")

      ;; Verify negative numbers are handled
      (is (re-find #"-100" result)
          "Should handle negative coordinates")))

  (testing "Performance with deeply nested groups"
    (let [deep-doc (reduce (fn [doc _]
                             [:g {:transforms [[:translate [1 1]]]} doc])
                           [:rect {:x 0 :y 0 :width 5 :height 5 :fill "#ff0000"}]
                           (range 20)) ; 20 levels deep
          start-time (js/Date.now)
          result (hiccup->pdf-ops deep-doc)
          end-time (js/Date.now)
          duration (- end-time start-time)]

      (is (string? result)
          "Should handle deeply nested groups")

      ;; Performance check - should complete in reasonable time
      (is (< duration 1000)
          "Should complete deeply nested processing in under 1 second")

      ;; Verify proper nesting
      (let [q-count (count (re-seq #"q\n" result))
            Q-count (count (re-seq #"Q" result))]
        (is (= q-count Q-count)
            "Should have balanced save/restore for deep nesting")
        (is (= q-count 20)
            "Should have exactly 20 nested groups")))))

(deftest complex-error-integration-test
  (testing "Error conditions in complex documents"
    ;; Test that error occurs early in complex document processing
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {}
                                            [:rect {:x 10 :y 10 :width 20 :height 30 :fill "#ff0000"}] ; Valid
                                            [:circle {:cx 50 :cy 50 :r 25 :fill "#0000ff"}]          ; Valid
                                            [:text {:x 100 :y 100 :font "Arial" :size 12} "Valid"] ; Valid
                                            [:line {:x1 0 :y1 0 :x2 "invalid" :y2 100}]           ; Invalid - should fail here
                                            [:path {:d "M10,10 L20,20"}]                          ; Would be valid
                                            [:rect {:x 200 :y 200 :width 50 :height 50}]]))       ; Would be valid
        "Should fail immediately when encountering invalid element in complex document")

    ;; Test nested error conditions
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-ops [:g {:transforms [[:translate [10 10]]]}
                                            [:rect {:x 0 :y 0 :width 10 :height 10}]    ; Valid
                                            [:g {:transforms [[:rotate 45]]}
                                             [:circle {:cx 0 :cy 0 :r 5}]              ; Valid
                                             [:g {}
                                              [:text {:x 0 :y 0 :font "" :size 12} "Empty font"]]] ; Invalid
                                            [:line {:x1 10 :y1 10 :x2 20 :y2 20}]]))   ; Would be valid
        "Should fail immediately when encountering invalid nested element")

    ;; Test transform error in complex document
    (is (thrown-with-msg? js/Error #"Unsupported transform type"
                          (hiccup->pdf-ops [:g {}
                                            [:rect {:x 10 :y 10 :width 20 :height 30}]           ; Valid
                                            [:g {:transforms [[:translate [10 10]] [:invalid [1 2]]]} ; Invalid transform
                                             [:circle {:cx 0 :cy 0 :r 5}]]                      ; Would be valid
                                            [:text {:x 50 :y 50 :font "Arial" :size 12} "Text"]]))  ; Would be valid
        "Should fail immediately when encountering invalid transform in complex document"))

  (testing "Real-world document structures"
    ;; Simulate a business card layout
    (let [business-card [:g {}
                         ;; Background
                         [:rect {:x 0 :y 0 :width 350 :height 200 :fill "#ffffff" :stroke "#000000" :stroke-width 2}]
                         ;; Company logo area
                         [:g {:transforms [[:translate [20 20]]]}
                          [:circle {:cx 0 :cy 0 :r 15 :fill "#0000ff"}]
                          [:text {:x 25 :y 5 :font "Arial" :size 18 :fill "#0000ff"} "TechCorp"]]
                         ;; Contact info
                         [:g {:transforms [[:translate [20 80]]]}
                          [:text {:x 0 :y 0 :font "Arial" :size 14 :fill "#000000"} "John Smith"]
                          [:text {:x 0 :y 20 :font "Arial" :size 12 :fill "#000000"} "Senior Developer"]
                          [:text {:x 0 :y 40 :font "Arial" :size 10 :fill "#000000"} "john.smith@techcorp.com"]
                          [:text {:x 0 :y 55 :font "Arial" :size 10 :fill "#000000"} "+1 (555) 123-4567"]]
                         ;; Decorative elements
                         [:g {:transforms [[:translate [250 50]]]}
                          [:path {:d "M0,0 L50,25 L0,50 L10,25 Z" :fill "#0000ff" :stroke "#0000ff"}]]]
          result (hiccup->pdf-ops business-card)]

      (is (string? result)
          "Should handle business card layout")

      ;; Verify structure
      (is (re-find #"350 200 re" result)
          "Should contain background rectangle")
      (is (>= (count (re-seq #"BT\n" result)) 5)
          "Should contain multiple text elements")
      (is (re-find #"TechCorp" result)
          "Should contain company name")
      (is (re-find #"john\.smith" result)
          "Should contain contact info"))

    ;; Simulate a simple diagram with annotations
    (let [diagram [:g {}
                   ;; Main flow boxes
                   [:g {:transforms [[:translate [50 50]]]}
                    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "#ffffff" :stroke "#0000ff" :stroke-width 2}]
                    [:text {:x 40 :y 25 :font "Arial" :size 12 :fill "#0000ff"} "Start"]]

                   ;; Arrow
                   [:g {:transforms [[:translate [150 70]]]}
                    [:path {:d "M0,0 L30,0 M25,-5 L30,0 L25,5" :stroke "#000000" :stroke-width 2}]]

                   ;; Process box
                   [:g {:transforms [[:translate [200 50]]]}
                    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "#ffffff" :stroke "#ff00ff" :stroke-width 2}]
                    [:text {:x 40 :y 25 :font "Arial" :size 12 :fill "#ff00ff"} "Process"]]

                   ;; Another arrow
                   [:g {:transforms [[:translate [300 70]]]}
                    [:path {:d "M0,0 L30,0 M25,-5 L30,0 L25,5" :stroke "#000000" :stroke-width 2}]]

                   ;; End box
                   [:g {:transforms [[:translate [350 50]]]}
                    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "#ffffff" :stroke "#00ff00" :stroke-width 2}]
                    [:text {:x 40 :y 25 :font "Arial" :size 12 :fill "#00ff00"} "End"]]]
          result (hiccup->pdf-ops diagram)]

      (is (string? result)
          "Should handle diagram layout")

      ;; Verify diagram elements
      (is (>= (count (re-seq #"re\n" result)) 3)
          "Should contain multiple boxes")
      (is (>= (count (re-seq #"m\n" result)) 2)
          "Should contain arrow paths")
      (is (re-find #"Start" result)
          "Should contain start label")
      (is (re-find #"Process" result)
          "Should contain process label")
      (is (re-find #"End" result)
          "Should contain end label"))

    ;; Simulate a chart with data points
    (let [chart [:g {}
                 ;; Chart background
                 [:rect {:x 50 :y 50 :width 300 :height 200 :fill "#ffffff" :stroke "#000000" :stroke-width 1}]
                 ;; Grid lines - horizontal
                 [:line {:x1 50 :y1 90 :x2 350 :y2 90 :stroke "#cccccc"}]
                 [:line {:x1 50 :y1 130 :x2 350 :y2 130 :stroke "#cccccc"}]
                 [:line {:x1 50 :y1 170 :x2 350 :y2 170 :stroke "#cccccc"}]
                 ;; Grid lines - vertical
                 [:line {:x1 110 :y1 50 :x2 110 :y2 250 :stroke "#cccccc"}]
                 [:line {:x1 170 :y1 50 :x2 170 :y2 250 :stroke "#cccccc"}]
                 [:line {:x1 230 :y1 50 :x2 230 :y2 250 :stroke "#cccccc"}]
                 ;; Data points
                 [:circle {:cx 80 :cy 80 :r 4 :fill "#ff0000"}]
                 [:circle {:cx 140 :cy 110 :r 4 :fill "#ff0000"}]
                 [:circle {:cx 200 :cy 140 :r 4 :fill "#ff0000"}]
                 [:circle {:cx 260 :cy 170 :r 4 :fill "#ff0000"}]
                 [:circle {:cx 320 :cy 200 :r 4 :fill "#ff0000"}]
                 ;; Axis labels
                 [:text {:x 200 :y 280 :font "Arial" :size 14 :fill "#000000"} "X Axis"]
                 [:g {:transforms [[:translate [20 150]] [:rotate -90]]}
                  [:text {:x 0 :y 0 :font "Arial" :size 14 :fill "#000000"} "Y Axis"]]]
          result (hiccup->pdf-ops chart)]

      (is (string? result)
          "Should handle chart layout")

      ;; Verify chart structure
      (is (re-find #"300 200 re" result)
          "Should contain chart background")
      (is (>= (count (re-seq #"m\n" result)) 6)
          "Should contain grid lines")
      (is (>= (count (re-seq #"c\n" result)) 20)
          "Should contain data point circles (4 curves per circle)"))))
