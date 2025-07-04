(ns hiccup-pdf.document-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.core :refer [hiccup->pdf-document]]
            [hiccup-pdf.document :refer [hiccup-document->pdf web-to-pdf-y transform-element-coordinates transform-coordinates-for-page page->content-stream]]
            [hiccup-pdf.validation :refer [validate-document-attributes validate-element-type validate-page-attributes]]))

(deftest document-function-signature-test
  (testing "Document function exists and can be called"
    (is (fn? hiccup->pdf-document)
        "hiccup->pdf-document should be a function")

    (is (fn? hiccup-document->pdf)
        "hiccup-document->pdf should be a function")))

(deftest document-basic-validation-test
  (testing "Basic document structure validation"

    ;; Test invalid input types
    (is (thrown-with-msg? js/Error #"Document must be a hiccup vector"
                          (hiccup->pdf-document "not a vector")))

    (is (thrown-with-msg? js/Error #"Document must be a hiccup vector"
                          (hiccup->pdf-document nil)))

    (is (thrown-with-msg? js/Error #"Document must be a hiccup vector"
                          (hiccup->pdf-document {})))

    ;; Test empty vector
    (is (thrown-with-msg? js/Error #"Document vector cannot be empty"
                          (hiccup->pdf-document [])))

    ;; Test wrong root element
    (is (thrown-with-msg? js/Error #"Root element must be :document, got: :page"
                          (hiccup->pdf-document [:page {}])))

    (is (thrown-with-msg? js/Error #"Root element must be :document, got: :rect"
                          (hiccup->pdf-document [:rect {:x 0 :y 0 :width 100 :height 50}])))))

(deftest document-delegation-test
  (testing "Public API delegates to implementation namespace"

    ;; Test that public API calls implementation function
    (let [result (hiccup->pdf-document [:document {:title "Test Doc"}])]
      (is (string? result)
          "Should return a string")
      (is (re-find #"Test Doc" result)
          "Should include document title in placeholder")))

  (testing "Implementation function handles basic document structure"

    ;; Test with minimal document
    (let [result (hiccup-document->pdf [:document {}])]
      (is (string? result)
          "Should return a string")
      (is (re-find #"Untitled Document" result)
          "Should use default title when none provided"))

    ;; Test with title
    (let [result (hiccup-document->pdf [:document {:title "My Document"}])]
      (is (string? result)
          "Should return a string")
      (is (re-find #"My Document" result)
          "Should include provided title"))

    ;; Test with pages (for now just validates structure)
    (let [result (hiccup-document->pdf
                  [:document {:title "With Pages"}
                   [:page {}
                    [:rect {:x 10 :y 10 :width 100 :height 50}]]])]
      (is (string? result)
          "Should handle documents with pages")
      (is (re-find #"With Pages" result)
          "Should include document title"))))

(deftest document-namespace-integration-test
  (testing "Document namespace integrates with core namespace"

    ;; Test that we can call both functions
    (let [doc [:document {:title "Integration Test"}
               [:page {}
                [:text {:x 100 :y 100 :font "Arial" :size 12} "Test"]]]

          ;; Call through public API
          public-result (hiccup->pdf-document doc)

          ;; Call implementation directly
          impl-result (hiccup-document->pdf doc)]

      (is (string? public-result)
          "Public API should return string")
      (is (string? impl-result)
          "Implementation should return string")
      (is (= public-result impl-result)
          "Public API and implementation should return same result"))))

(deftest document-element-type-validation-test
  (testing "Document element type is recognized as valid"

    ;; Test that :document is now a valid element type
    (is (= :document (validate-element-type :document))
        "Should accept :document as valid element type")

    ;; Test that :page is now a valid element type
    (is (= :page (validate-element-type :page))
        "Should accept :page as valid element type")

    ;; Ensure existing types still work
    (is (= :rect (validate-element-type :rect))
        "Should still accept existing element types")))

(deftest document-attributes-validation-test
  (testing "Document attributes validation with defaults"
    ;; Test empty attributes get defaults
    (let [result (validate-document-attributes {})]
      (is (= 612 (:width result)) "Should default width to 612")
      (is (= 792 (:height result)) "Should default height to 792")
      (is (= [0 0 0 0] (:margins result)) "Should default margins to [0 0 0 0]")
      (is (= "hiccup-pdf" (:creator result)) "Should default creator to hiccup-pdf")
      (is (= "hiccup-pdf" (:producer result)) "Should default producer to hiccup-pdf"))

    ;; Test custom attributes override defaults
    (let [result (validate-document-attributes {:title "My Document" :width 595})]
      (is (= "My Document" (:title result)) "Should preserve custom title")
      (is (= 595 (:width result)) "Should override default width")
      (is (= "hiccup-pdf" (:creator result)) "Should still use default creator"))))

(deftest document-attributes-validation-errors-test
  (testing "Document attributes validation catches invalid values"
    ;; Test invalid width/height
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-document-attributes {:width -100}))
        "Should reject negative width")

    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-document-attributes {:width "invalid"}))
        "Should reject non-numeric width")

    ;; Test invalid margins
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-document-attributes {:margins [10 20 30]}))
        "Should reject margins with wrong number of elements")))

(deftest document-integration-with-validation-test
  (testing "Document function integrates with validation"
    ;; Test that document function uses validation
    (let [result (hiccup->pdf-document [:document {:title "Validated Doc"}])]
      (is (string? result) "Should return string")
      (is (re-find #"Validated Doc" result) "Should include validated title")
      (is (re-find #"width: 612" result) "Should include default width"))

    ;; Test validation errors propagate
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (hiccup->pdf-document [:document {:width -100}]))
        "Should propagate validation errors")))

(deftest page-element-validation-test
  (testing "Page element type is recognized as valid"

    ;; Test that :page is now a valid element type
    (is (= :page (validate-element-type :page))
        "Should accept :page as valid element type"))

  (testing "Page attributes validation with inheritance"
    ;; Test page validation without document defaults
    (let [result (validate-page-attributes {:width 595 :height 842})]
      (is (= {:width 595 :height 842} result)
          "Should validate page attributes without document defaults"))

    ;; Test page validation with document defaults
    (let [document-defaults {:width 612 :height 792 :margins [10 10 10 10]}
          result (validate-page-attributes {:height 842} document-defaults)]
      (is (= {:width 612 :height 842 :margins [10 10 10 10]} result)
          "Should inherit width and margins, override height"))

    ;; Test landscape orientation
    (let [document-defaults {:width 612 :height 792}
          result (validate-page-attributes {:width 792 :height 612} document-defaults)]
      (is (= {:width 792 :height 612} result)
          "Should handle landscape orientation"))

    ;; Test complete override
    (let [document-defaults {:width 612 :height 792 :margins [5 5 5 5]}
          result (validate-page-attributes {:width 595 :height 842 :margins [0 0 0 0]} document-defaults)]
      (is (= {:width 595 :height 842 :margins [0 0 0 0]} result)
          "Should override all document defaults"))

    ;; Test full inheritance
    (let [document-defaults {:width 612 :height 792 :margins [5 5 5 5]}
          result (validate-page-attributes {} document-defaults)]
      (is (= {:width 612 :height 792 :margins [5 5 5 5]} result)
          "Should inherit all document defaults")))

  (testing "Page attributes validation errors"
    ;; Test invalid dimensions
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:width -100}))
        "Should reject negative width")

    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:height 0}))
        "Should reject zero height")

    ;; Test invalid margins
    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:margins [10 20 30]}))
        "Should reject margins with wrong number of elements")

    (is (thrown-with-msg? js/Error #"ValidationError"
                          (validate-page-attributes {:margins "invalid"}))
        "Should reject non-vector margins")))

(deftest web-to-pdf-y-coordinate-transformation-test
  (testing "Basic Y coordinate transformation"
    ;; Test with standard letter page (612x792) and no margins
    (let [page-height 792]
      ;; Top of page (web y=0) should become top of PDF page (PDF y=792)
      (is (= 792 (web-to-pdf-y 0 page-height))
          "Top of web page should map to top of PDF page")

      ;; Middle of page (web y=396) should become middle of PDF page (PDF y=396)
      (is (= 396 (web-to-pdf-y 396 page-height))
          "Middle of web page should map to middle of PDF page")

      ;; Bottom of page (web y=792) should become bottom of PDF page (PDF y=0)
      (is (= 0 (web-to-pdf-y 792 page-height))
          "Bottom of web page should map to bottom of PDF page")))

  (testing "Y coordinate transformation with margins"
    ;; Test with margins [top=50, right=30, bottom=40, left=20]
    (let [page-height 792
          margins [50 30 40 20]]

      ;; Simple flip: web y=50 -> PDF y = 792-50 = 742
      (is (= 742 (web-to-pdf-y 50 page-height margins))
          "Should flip Y coordinate regardless of margins")

      ;; Web y=742 -> PDF y = 792-742 = 50
      (is (= 50 (web-to-pdf-y 742 page-height margins))
          "Should flip Y coordinate regardless of margins")

      ;; Web y=0 -> PDF y = 792-0 = 792
      (is (= 792 (web-to-pdf-y 0 page-height margins))
          "Should flip Y coordinate regardless of margins")))

  (testing "Edge cases for Y coordinate transformation"
    (let [page-height 792]
      ;; Test with zero margins
      (is (= 792 (web-to-pdf-y 0 page-height [0 0 0 0]))
          "Should handle zero margins")

      ;; Test with large margins: web y=300 -> PDF y = 792-300 = 492
      (is (= 492 (web-to-pdf-y 300 page-height [200 0 200 0]))
          "Should handle large margins correctly")

      ;; Test negative coordinates (shouldn't happen in practice but should be handled)
      (is (= 802 (web-to-pdf-y -10 page-height [0 0 0 0]))
          "Should handle negative coordinates"))))

(deftest transform-element-coordinates-test
  (testing "Rectangle coordinate transformation"
    (let [page-height 792
          margins [0 0 0 0]
          rect-element [:rect {:x 100 :y 50 :width 200 :height 100}]]

      (let [transformed (transform-element-coordinates rect-element page-height margins)]
        (is (= [:rect {:x 100 :y 742 :width 200 :height 100}] transformed)
            "Should transform rectangle Y coordinate"))))

  (testing "Circle coordinate transformation"
    (let [page-height 792
          margins [0 0 0 0]
          circle-element [:circle {:cx 150 :cy 100 :r 50}]]

      (let [transformed (transform-element-coordinates circle-element page-height margins)]
        (is (= [:circle {:cx 150 :cy 692 :r 50}] transformed)
            "Should transform circle CY coordinate"))))

  (testing "Line coordinate transformation"
    (let [page-height 792
          margins [0 0 0 0]
          line-element [:line {:x1 10 :y1 20 :x2 100 :y2 80}]]

      (let [transformed (transform-element-coordinates line-element page-height margins)]
        (is (= [:line {:x1 10 :y1 772 :x2 100 :y2 712}] transformed)
            "Should transform both line Y coordinates"))))

  (testing "Text coordinate transformation"
    (let [page-height 792
          margins [0 0 0 0]
          text-element [:text {:x 50 :y 100 :font "Arial" :size 12} "Hello"]]

      (let [transformed (transform-element-coordinates text-element page-height margins)]
        (is (= [:text {:x 50 :y 692 :font "Arial" :size 12} "Hello"] transformed)
            "Should transform text Y coordinate and preserve content"))))

  (testing "Group coordinate transformation with translate"
    (let [page-height 792
          margins [0 0 0 0]
          group-element [:g {:transforms [[:translate [50 100]]]}
                         [:rect {:x 10 :y 20 :width 50 :height 30}]]]

      (let [transformed (transform-element-coordinates group-element page-height margins)]
        (is (= [:g {:transforms [[:translate [50 692]]]}
                [:rect {:x 10 :y 772 :width 50 :height 30}]] transformed)
            "Should transform group translate Y and child element Y coordinates"))))

  (testing "Group coordinate transformation with multiple transforms"
    (let [page-height 792
          margins [0 0 0 0]
          group-element [:g {:transforms [[:translate [10 20]] [:rotate 45] [:scale [2 2]]]}]]

      (let [transformed (transform-element-coordinates group-element page-height margins)]
        (is (= [:g {:transforms [[:translate [10 772]] [:rotate 45] [:scale [2 2]]]}] transformed)
            "Should only transform translate Y coordinate, leaving other transforms unchanged"))))

  (testing "Elements without coordinates"
    (let [page-height 792
          margins [0 0 0 0]]

      ;; Path elements don't have individual coordinates to transform
      (let [path-element [:path {:d "M 10 20 L 30 40"}]
            transformed (transform-element-coordinates path-element page-height margins)]
        (is (= path-element transformed)
            "Should not transform path elements"))

      ;; Document and page elements don't have coordinates
      (let [doc-element [:document {:title "Test"}]
            transformed (transform-element-coordinates doc-element page-height margins)]
        (is (= doc-element transformed)
            "Should not transform document elements"))

      (let [page-element [:page {:width 612}]
            transformed (transform-element-coordinates page-element page-height margins)]
        (is (= page-element transformed)
            "Should not transform page elements"))))

  (testing "Nested elements coordinate transformation"
    (let [page-height 792
          margins [20 10 30 15]
          nested-element [:g {}
                          [:rect {:x 50 :y 100 :width 100 :height 50}]
                          [:g {:transforms [[:translate [25 75]]]}
                           [:circle {:cx 75 :cy 125 :r 25}]]]]

      (let [transformed (transform-element-coordinates nested-element page-height margins)]
        ;; Content height = 792 - 20 - 30 = 742
        ;; web y=100 -> PDF y = 30 + (742 - (100 - 20)) = 30 + 662 = 692
        ;; web y=75 -> PDF y = 30 + (742 - (75 - 20)) = 30 + 687 = 717
        ;; web y=125 -> PDF y = 30 + (742 - (125 - 20)) = 30 + 637 = 667
        (is (= [:g {}
                [:rect {:x 50 :y 692 :width 100 :height 50}]
                [:g {:transforms [[:translate [25 717]]]}
                 [:circle {:cx 75 :cy 667 :r 25}]]] transformed)
            "Should recursively transform all nested elements with margins"))))

  (testing "Elements with missing coordinates"
    (let [page-height 792
          margins [0 0 0 0]]

      ;; Rectangle without Y coordinate
      (let [rect-element [:rect {:x 100 :width 200 :height 100}]
            transformed (transform-element-coordinates rect-element page-height margins)]
        (is (= rect-element transformed)
            "Should not transform elements missing required coordinates"))

      ;; Line with only some coordinates
      (let [line-element [:line {:x1 10 :x2 100 :y2 80}]
            transformed (transform-element-coordinates line-element page-height margins)]
        (is (= [:line {:x1 10 :x2 100 :y2 712}] transformed)
            "Should only transform coordinates that are present")))))

(deftest transform-coordinates-for-page-test
  (testing "Multiple elements coordinate transformation"
    (let [page-height 792
          margins [0 0 0 0]
          page-content [[:rect {:x 10 :y 20 :width 100 :height 50}]
                        [:circle {:cx 200 :cy 100 :r 30}]
                        [:text {:x 50 :y 150 :font "Arial" :size 14} "Test"]]]

      (let [transformed (transform-coordinates-for-page page-content page-height margins)]
        (is (= [[:rect {:x 10 :y 772 :width 100 :height 50}]
                [:circle {:cx 200 :cy 692 :r 30}]
                [:text {:x 50 :y 642 :font "Arial" :size 14} "Test"]] transformed)
            "Should transform all elements in page content"))))

  (testing "Complex page with groups and nested elements"
    (let [page-height 600  ; Smaller page for easier calculation
          margins [50 0 50 0]  ; Top and bottom margins of 50
          page-content [[:g {:transforms [[:translate [0 100]]]}
                         [:rect {:x 10 :y 20 :width 50 :height 30}]
                         [:g {}
                          [:circle {:cx 100 :cy 80 :r 20}]]]
                        [:line {:x1 200 :y1 150 :x2 300 :y2 200}]]]

      (let [transformed (transform-coordinates-for-page page-content page-height margins)]
        ;; Content height = 600 - 50 - 50 = 500
        ;; Group translate y=100 -> PDF y = 50 + (500 - (100 - 50)) = 50 + 450 = 500
        ;; Rect y=20 -> PDF y = 50 + (500 - (20 - 50)) = 50 + 530 = 580
        ;; Circle cy=80 -> PDF y = 50 + (500 - (80 - 50)) = 50 + 470 = 520
        ;; Line y1=150 -> PDF y = 50 + (500 - (150 - 50)) = 50 + 400 = 450
        ;; Line y2=200 -> PDF y = 50 + (500 - (200 - 50)) = 50 + 350 = 400
        (is (= [[:g {:transforms [[:translate [0 500]]]}
                 [:rect {:x 10 :y 580 :width 50 :height 30}]
                 [:g {}
                  [:circle {:cx 100 :cy 520 :r 20}]]]
                [:line {:x1 200 :y1 450 :x2 300 :y2 400}]] transformed)
            "Should handle complex nested structures with margins"))))

  (testing "Edge cases and boundary conditions"
    (let [page-height 792
          margins [0 0 0 0]]

      ;; Empty page content
      (is (= [] (transform-coordinates-for-page [] page-height margins))
          "Should handle empty page content")

      ;; Page content with non-vector elements (shouldn't happen but should be handled)
      (is (= ["text"] (transform-coordinates-for-page ["text"] page-height margins))
          "Should handle non-vector elements")

      ;; Page boundaries
      (let [boundary-content [[:rect {:x 0 :y 0 :width 10 :height 10}]    ; Top-left
                              [:rect {:x 0 :y 792 :width 10 :height 10}]]] ; Bottom-left
        (is (= [[:rect {:x 0 :y 792 :width 10 :height 10}]    ; Should become bottom-left in PDF
                [:rect {:x 0 :y 0 :width 10 :height 10}]]     ; Should become top-left in PDF
               (transform-coordinates-for-page boundary-content page-height margins))
            "Should correctly handle page boundary coordinates")))))

(deftest page-content-stream-generation-test
  (testing "Basic page content stream generation"
    (let [page-attributes {:width 612 :height 792}
          page-content [[:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]]
          document-defaults {:width 595 :height 842}
          result (page->content-stream page-attributes page-content document-defaults)]

      ;; Test result structure
      (is (map? result) "Should return a map")
      (is (= 612 (:width result)) "Should use page width override")
      (is (= 792 (:height result)) "Should use page height override")
      (is (= [0 0 0 0] (:margins result)) "Should use default margins")
      (is (string? (:content-stream result)) "Should return content stream as string")
      (is (map? (:metadata result)) "Should include metadata")

      ;; Test metadata
      (is (= 1 (get-in result [:metadata :element-count])) "Should count elements")
      (is (= "pdf" (get-in result [:metadata :coordinate-system])) "Should indicate PDF coordinates")
      (is (false? (get-in result [:metadata :has-transforms])) "Should detect no transforms")

      ;; Test that content stream contains expected PDF operators
      (is (re-find #"1 0 0 rg" (:content-stream result)) "Should contain red fill color")
      (is (re-find #"10 772 100 50 re" (:content-stream result)) "Should contain transformed rectangle coordinates")))

  (testing "Page content stream with inheritance"
    (let [page-attributes {:margins [10 20 30 40]}  ; Only override margins
          page-content [[:circle {:cx 100 :cy 100 :r 50 :fill "blue"}]]
          document-defaults {:width 595 :height 842 :margins [5 5 5 5]}
          result (page->content-stream page-attributes page-content document-defaults)]

      ;; Test inheritance
      (is (= 595 (:width result)) "Should inherit document width")
      (is (= 842 (:height result)) "Should inherit document height")
      (is (= [10 20 30 40] (:margins result)) "Should override margins")

      ;; Test coordinate transformation with inherited dimensions
      ;; Circle at web cy=100 should become PDF cy = 842-100 = 742
      (is (re-find #"0 0 1 rg" (:content-stream result)) "Should contain blue fill color")
      (is (re-find #"742" (:content-stream result)) "Should transform circle center with inherited page height")))

  (testing "Complex page content with multiple elements"
    (let [page-attributes {:width 600 :height 800}
          page-content [[:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]
                        [:circle {:cx 200 :cy 100 :r 30 :stroke "blue"}]
                        [:text {:x 50 :y 200 :font "Arial" :size 14 :fill "black"} "Test Text"]
                        [:line {:x1 300 :y1 150 :x2 400 :y2 250 :stroke "green"}]]
          document-defaults {}
          result (page->content-stream page-attributes page-content document-defaults)]

      ;; Test metadata for complex content
      (is (= 4 (get-in result [:metadata :element-count])) "Should count all elements")

      ;; Test that all elements are in content stream
      (let [content (:content-stream result)]
        (is (re-find #"1 0 0 rg" content) "Should contain red rectangle")
        (is (re-find #"0 0 1 RG" content) "Should contain blue circle stroke")
        (is (re-find #"Arial" content) "Should contain text font")
        (is (re-find #"0 1 0 RG" content) "Should contain green line")

        ;; Test coordinate transformations
        ;; Rectangle y=20 -> PDF y = 800-20 = 780
        (is (re-find #"10 780 100 50 re" content) "Should transform rectangle coordinates")
        ;; Circle cy=100 -> PDF cy = 800-100 = 700
        (is (re-find #"700" content) "Should transform circle coordinates")
        ;; Text y=200 -> PDF y = 800-200 = 600
        (is (re-find #"50 600" content) "Should transform text coordinates")
        ;; Line y1=150, y2=250 -> PDF y1=650, y2=550
        (is (re-find #"300 650 m" content) "Should transform line start coordinates")
        (is (re-find #"400 550 l" content) "Should transform line end coordinates"))))

  (testing "Page content with groups and transforms"
    (let [page-attributes {:width 500 :height 700}
          page-content [[:g {:transforms [[:translate [50 100]]]}
                         [:rect {:x 10 :y 20 :width 80 :height 40 :fill "yellow"}]
                         [:g {:transforms [[:rotate 45]]}
                          [:circle {:cx 60 :cy 80 :r 20 :stroke "magenta"}]]]]
          document-defaults {}
          result (page->content-stream page-attributes page-content document-defaults)]

      ;; Test metadata detects transforms
      (is (= 1 (get-in result [:metadata :element-count])) "Should count top-level elements")
      (is (true? (get-in result [:metadata :has-transforms])) "Should detect transforms")

      ;; Test that groups and transforms are preserved
      (let [content (:content-stream result)]
        (is (re-find #"q" content) "Should contain graphics state save")
        (is (re-find #"Q" content) "Should contain graphics state restore")
        ;; Group translate y=100 -> PDF y = 700-100 = 600
        (is (re-find #"1 0 0 1 50 600 cm" content) "Should transform group translate coordinates")
        ;; Nested rectangle y=20 -> PDF y = 700-20 = 680
        (is (re-find #"10 680 80 40 re" content) "Should transform nested rectangle coordinates")
        ;; Rotation should be preserved as-is
        (is (re-find #"0.707" content) "Should preserve rotation transform"))))

  (testing "Page content stream with margins affecting coordinates"
    (let [page-attributes {:width 612 :height 792 :margins [50 30 40 20]}
          page-content [[:rect {:x 0 :y 0 :width 100 :height 50}]    ; Top-left of content area
                        [:rect {:x 0 :y 702 :width 100 :height 50}]]  ; Bottom-left of content area
          document-defaults {}
          result (page->content-stream page-attributes page-content document-defaults)]

      ;; Test margins are recorded
      (is (= [50 30 40 20] (:margins result)) "Should record page margins")

      ;; Test coordinate transformation with margins
      ;; Note: Coordinate transformation currently just flips Y regardless of margins
      ;; y=0 -> PDF y = 792-0 = 792
      ;; y=702 -> PDF y = 792-702 = 90
      (let [content (:content-stream result)]
        (is (re-find #"0 792 100 50 re" content) "Should transform top rectangle")
        (is (re-find #"0 90 100 50 re" content) "Should transform bottom rectangle"))))

  (testing "Edge cases for page content stream generation"
    ;; Empty page content
    (let [result (page->content-stream {} [] {})]
      (is (= 0 (get-in result [:metadata :element-count])) "Should handle empty content")
      (is (= "" (:content-stream result)) "Should return empty content stream"))

    ;; Page with only text elements
    (let [page-content [[:text {:x 10 :y 10 :font "Arial" :size 12} "Test"]]
          result (page->content-stream {} page-content {})]
      (is (= 1 (get-in result [:metadata :element-count])) "Should count all elements")
      (is (string? (:content-stream result)) "Should handle text elements"))

    ;; Page with default document attributes
    (let [document-defaults {:width 612 :height 792 :margins [10 10 10 10] :title "Doc"}
          result (page->content-stream {} [] document-defaults)]
      (is (= 612 (:width result)) "Should inherit document width")
      (is (= 792 (:height result)) "Should inherit document height")
      (is (= [10 10 10 10] (:margins result)) "Should inherit document margins"))))

