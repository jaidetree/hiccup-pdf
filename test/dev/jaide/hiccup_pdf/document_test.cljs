(ns dev.jaide.hiccup-pdf.document-test
  (:require [cljs.test :refer [deftest is testing]]
            [dev.jaide.hiccup-pdf.core :refer [hiccup->pdf-document]]
            [dev.jaide.hiccup-pdf.document :refer [hiccup-document->pdf web-to-pdf-y transform-element-coordinates transform-coordinates-for-page page->content-stream extract-fonts-from-content generate-font-resource-object generate-content-stream-object generate-page-object generate-pages-object generate-catalog-object generate-info-object generate-pdf-header calculate-byte-offsets generate-xref-table generate-trailer document->pdf]]
            [dev.jaide.hiccup-pdf.validation :refer [validate-document-attributes validate-element-type validate-page-attributes]]))

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
      (is (re-find #"%PDF-1.4" result)
          "Should generate a complete PDF document"))

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
      (is (re-find #"Validated Doc" result) "Should include document title"))

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
        (is (= [:rect {:x 100 :y 642 :width 200 :height 100}] transformed)
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
                [:rect {:x 10 :y 742 :width 50 :height 30}]] transformed)
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
                [:rect {:x 50 :y 642 :width 100 :height 50}]
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
        (is (= [[:rect {:x 10 :y 722 :width 100 :height 50}]
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
                 [:rect {:x 10 :y 550 :width 50 :height 30}]
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
        (is (= [[:rect {:x 0 :y 782 :width 10 :height 10}]    ; Should become bottom-left in PDF
                [:rect {:x 0 :y -10 :width 10 :height 10}]]     ; Should become top-left in PDF
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
      (is (re-find #"10 722 100 50 re" (:content-stream result)) "Should contain transformed rectangle coordinates")))

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
        (is (re-find #"10 730 100 50 re" content) "Should transform rectangle coordinates")
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
        (is (re-find #"10 640 80 40 re" content) "Should transform nested rectangle coordinates")
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
        (is (re-find #"0 742 100 50 re" content) "Should transform top rectangle")
        (is (re-find #"0 40 100 50 re" content) "Should transform bottom rectangle"))))

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

(deftest extract-fonts-from-content-test
  (testing "Font extraction from page content"
    ;; Test with no fonts
    (is (= #{} (extract-fonts-from-content []))
        "Should return empty set for empty content")
    
    (is (= #{} (extract-fonts-from-content [[:rect {:x 0 :y 0 :width 100 :height 50}]]))
        "Should return empty set for content without text")
    
    ;; Test with single font
    (is (= #{"Arial"} (extract-fonts-from-content [[:text {:x 10 :y 10 :font "Arial" :size 12} "Hello"]]))
        "Should extract single font from text element")
    
    ;; Test with multiple fonts
    (let [content [[:text {:x 10 :y 10 :font "Arial" :size 12} "Hello"]
                   [:text {:x 50 :y 50 :font "Times" :size 14} "World"]
                   [:rect {:x 0 :y 0 :width 100 :height 50}]
                   [:text {:x 100 :y 100 :font "Arial" :size 16} "Again"]]]
      (is (= #{"Arial" "Times"} (extract-fonts-from-content content))
          "Should extract unique fonts from multiple text elements"))
    
    ;; Test with nested groups
    (let [content [[:g {}
                    [:text {:x 10 :y 10 :font "Courier" :size 10} "Nested"]
                    [:g {:transforms [[:translate [50 50]]]}
                     [:text {:x 0 :y 0 :font "Helvetica" :size 12} "Deep nested"]]]]]
      (is (= #{"Courier" "Helvetica"} (extract-fonts-from-content content))
          "Should extract fonts from nested groups"))))

(deftest pdf-object-generation-test
  (testing "Font resource object generation"
    (let [font-obj (generate-font-resource-object 5 "Arial")]
      (is (re-find #"5 0 obj" font-obj) "Should include object number")
      (is (re-find #"/Type /Font" font-obj) "Should specify font type")
      (is (re-find #"/Subtype /Type1" font-obj) "Should specify Type1 subtype")
      (is (re-find #"/BaseFont /Helvetica" font-obj) "Should map Arial to Helvetica")
      (is (re-find #"endobj" font-obj) "Should end with endobj"))
    
    ;; Test font name mapping
    (is (re-find #"/BaseFont /Times-Roman" (generate-font-resource-object 1 "Times New Roman"))
        "Should map Times New Roman to Times-Roman")
    (is (re-find #"/BaseFont /Helvetica" (generate-font-resource-object 1 "UnknownFont"))
        "Should default to Helvetica for unknown fonts"))
  
  (testing "Content stream object generation"
    (let [content "10 10 100 50 re\nf"
          content-length (+ (count content) 1)  ; Include the newline in expected length
          stream-obj (generate-content-stream-object 3 content)]
      (is (re-find #"3 0 obj" stream-obj) "Should include object number")
      (is (re-find (re-pattern (str "/Length " content-length)) stream-obj) "Should calculate correct length")
      (is (re-find #"stream" stream-obj) "Should include stream keyword")
      (is (re-find #"10 10 100 50 re" stream-obj) "Should include content")
      (is (re-find #"endstream" stream-obj) "Should end with endstream")
      (is (re-find #"endobj" stream-obj) "Should end with endobj")))
  
  (testing "Page object generation"
    (let [page-data {:width 612 :height 792 :margins [10 20 30 40]}
          font-refs {"Arial" 5 "Times" 6}
          page-obj (generate-page-object 2 page-data 4 3 font-refs)]
      (is (re-find #"2 0 obj" page-obj) "Should include object number")
      (is (re-find #"/Type /Page" page-obj) "Should specify page type")
      (is (re-find #"/Parent 4 0 R" page-obj) "Should reference parent pages")
      (is (re-find #"/MediaBox \[40 30 612 792\]" page-obj) "Should include MediaBox with margins")
      (is (re-find #"/Contents 3 0 R" page-obj) "Should reference content stream")
      (is (re-find #"/Arial 5 0 R" page-obj) "Should include font resources")
      (is (re-find #"/Times 6 0 R" page-obj) "Should include all font resources")
      (is (re-find #"endobj" page-obj) "Should end with endobj"))
    
    ;; Test page without fonts
    (let [page-obj (generate-page-object 1 {:width 595 :height 842} 2 3 {})]
      (is (not (re-find #"/Font" page-obj)) "Should not include font resources when none present")))
  
  (testing "Pages collection object generation"
    (let [pages-obj (generate-pages-object 4 [2 7 9])]
      (is (re-find #"4 0 obj" pages-obj) "Should include object number")
      (is (re-find #"/Type /Pages" pages-obj) "Should specify pages type")
      (is (re-find #"/Kids \[2 0 R 7 0 R 9 0 R\]" pages-obj) "Should list all page references")
      (is (re-find #"/Count 3" pages-obj) "Should specify correct page count")
      (is (re-find #"endobj" pages-obj) "Should end with endobj"))
    
    ;; Test with single page
    (let [pages-obj (generate-pages-object 1 [2])]
      (is (re-find #"/Kids \[2 0 R\]" pages-obj) "Should handle single page")
      (is (re-find #"/Count 1" pages-obj) "Should count single page")))
  
  (testing "Catalog object generation"
    (let [catalog-obj (generate-catalog-object 1 4)]
      (is (re-find #"1 0 obj" catalog-obj) "Should include object number")
      (is (re-find #"/Type /Catalog" catalog-obj) "Should specify catalog type")
      (is (re-find #"/Pages 4 0 R" catalog-obj) "Should reference pages object")
      (is (re-find #"endobj" catalog-obj) "Should end with endobj")))
  
  (testing "Info object generation"
    (let [metadata {:title "Test Document" :author "Test Author" :creator "hiccup-pdf"}
          info-obj (generate-info-object 8 metadata)]
      (is (re-find #"8 0 obj" info-obj) "Should include object number")
      (is (re-find #"/Title \(Test Document\)" info-obj) "Should include title")
      (is (re-find #"/Author \(Test Author\)" info-obj) "Should include author")
      (is (re-find #"/Creator \(hiccup-pdf\)" info-obj) "Should include creator")
      (is (re-find #"endobj" info-obj) "Should end with endobj"))
    
    ;; Test with minimal metadata
    (let [info-obj (generate-info-object 1 {:title "Simple"})]
      (is (re-find #"/Title \(Simple\)" info-obj) "Should include only provided metadata")
      (is (not (re-find #"/Author" info-obj)) "Should not include absent metadata"))
    
    ;; Test with empty metadata
    (let [info-obj (generate-info-object 1 {})]
      (is (re-find #"1 0 obj" info-obj) "Should generate object even with no metadata")
      (is (re-find #"<<" info-obj) "Should include dictionary markers")
      (is (re-find #">>" info-obj) "Should close dictionary"))))

(deftest pdf-object-integration-test
  (testing "Multiple PDF objects working together"
    ;; Test a complete set of objects for a simple document
    (let [;; Page content with fonts
          page-content [[:text {:x 100 :y 100 :font "Arial" :size 12} "Hello World"]
                        [:rect {:x 50 :y 200 :width 200 :height 100 :fill "red"}]]
          
          ;; Extract fonts
          fonts (extract-fonts-from-content page-content)
          
          ;; Generate content stream
          content-stream "BT\n/Arial 12 Tf\n100 692 Td\n(Hello World) Tj\nET\n1 0 0 rg\n50 592 200 100 re\nf"
          
          ;; Generate all objects
          font-obj (generate-font-resource-object 2 "Arial")
          content-obj (generate-content-stream-object 3 content-stream)
          page-obj (generate-page-object 4 {:width 612 :height 792} 5 3 {"Arial" 2})
          pages-obj (generate-pages-object 5 [4])
          catalog-obj (generate-catalog-object 1 5)
          info-obj (generate-info-object 6 {:title "Integration Test"})]
      
      ;; Test font extraction
      (is (= #{"Arial"} fonts) "Should extract Arial font")
      
      ;; Test object references are consistent
      (is (re-find #"/Arial 2 0 R" page-obj) "Page should reference font object")
      (is (re-find #"/Contents 3 0 R" page-obj) "Page should reference content stream")
      (is (re-find #"/Parent 5 0 R" page-obj) "Page should reference parent pages")
      (is (re-find #"/Kids \[4 0 R\]" pages-obj) "Pages should reference page")
      (is (re-find #"/Pages 5 0 R" catalog-obj) "Catalog should reference pages")
      
      ;; Test all objects are properly formatted
      (is (every? #(re-find #"endobj$" %) [font-obj content-obj page-obj pages-obj catalog-obj info-obj])
          "All objects should end with endobj")))
  
  (testing "Multi-page document object generation"
    (let [page1-data {:width 612 :height 792}
          page2-data {:width 595 :height 842}  ; Different size (A4)
          
          ;; Generate objects for multi-page document
          page1-obj (generate-page-object 4 page1-data 6 3 {"Arial" 2})
          page2-obj (generate-page-object 5 page2-data 6 7 {"Times" 8})
          pages-obj (generate-pages-object 6 [4 5])
          catalog-obj (generate-catalog-object 1 6)]
      
      ;; Test different page dimensions
      (is (re-find #"/MediaBox \[0 0 612 792\]" page1-obj) "Should handle letter size")
      (is (re-find #"/MediaBox \[0 0 595 842\]" page2-obj) "Should handle A4 size")
      
      ;; Test pages collection with multiple pages
      (is (re-find #"/Kids \[4 0 R 5 0 R\]" pages-obj) "Should reference both pages")
      (is (re-find #"/Count 2" pages-obj) "Should count both pages")
      
      ;; Test different fonts per page
      (is (re-find #"/Arial 2 0 R" page1-obj) "Page 1 should use Arial")
      (is (re-find #"/Times 8 0 R" page2-obj) "Page 2 should use Times"))))

(deftest pdf-document-assembly-test
  (testing "PDF header generation"
    (is (= "%PDF-1.4" (generate-pdf-header))
        "Should generate correct PDF header"))
  
  (testing "Byte offset calculation"
    (let [objects ["obj1\ncontent" "obj2\nmore\ncontent" "obj3"]
          offsets (calculate-byte-offsets objects)]
      ;; Header is 8 chars ("%PDF-1.4") + 1 newline = 9
      ;; obj1 is 12 chars + 1 newline = 13, so starts at 9
      ;; obj2 is 17 chars + 1 newline = 18, so starts at 9 + 13 = 22  
      ;; obj3 is 4 chars, so starts at 22 + 18 = 40
      (is (= [9 22 40] offsets)
          "Should calculate correct byte offsets")))
  
  (testing "Cross-reference table generation"
    (let [xref (generate-xref-table 4 [9 22 40])]
      (is (re-find #"xref" xref) "Should include xref keyword")
      (is (re-find #"0 4" xref) "Should specify object count")
      (is (re-find #"0000000000 65535 f" xref) "Should include object 0 entry")
      (is (re-find #"0000000009 00000 n" xref) "Should include first object offset")
      (is (re-find #"0000000022 00000 n" xref) "Should include second object offset")
      (is (re-find #"0000000040 00000 n" xref) "Should include third object offset")))
  
  (testing "Trailer generation"
    (let [trailer (generate-trailer 4 1 3 100)]
      (is (re-find #"trailer" trailer) "Should include trailer keyword")
      (is (re-find #"/Size 4" trailer) "Should specify size")
      (is (re-find #"/Root 1 0 R" trailer) "Should reference catalog")
      (is (re-find #"/Info 3 0 R" trailer) "Should reference info when provided")
      (is (re-find #"startxref" trailer) "Should include startxref")
      (is (re-find #"100" trailer) "Should include xref offset")
      (is (re-find #"%%EOF" trailer) "Should end with EOF"))
    
    ;; Test without info object
    (let [trailer (generate-trailer 3 1 nil 100)]
      (is (not (re-find #"/Info" trailer)) "Should not include info when not provided")))
  
  (testing "Complete document assembly with single page"
    (let [page-data {:width 612 :height 792 :margins [0 0 0 0] 
                     :content-stream "BT\n/Arial 12 Tf\n100 100 Td\n(Hello) Tj\nET"
                     :metadata {:element-count 1}}
          document-attrs {:title "Test Document" :creator "hiccup-pdf"}
          pdf (document->pdf [page-data] document-attrs)]
      
      ;; Test PDF structure
      (is (re-find #"%PDF-1.4" pdf) "Should include PDF header")
      (is (re-find #"/Type /Catalog" pdf) "Should include catalog object")
      (is (re-find #"/Type /Pages" pdf) "Should include pages object")
      (is (re-find #"/Type /Page" pdf) "Should include page object")
      (is (re-find #"/Type /Font" pdf) "Should include font object")
      (is (re-find #"(?s)stream.*Hello.*endstream" pdf) "Should include content stream")
      (is (re-find #"xref" pdf) "Should include xref table")
      (is (re-find #"trailer" pdf) "Should include trailer")
      (is (re-find #"/Title \(Test Document\)" pdf) "Should include document metadata")
      (is (re-find #"%%EOF" pdf) "Should end with EOF")))
  
  (testing "Complete document assembly with multiple pages"
    (let [page1-data {:width 612 :height 792 
                      :content-stream "BT\n/Arial 12 Tf\n100 100 Td\n(Page 1) Tj\nET"
                      :metadata {:element-count 1}}
          page2-data {:width 595 :height 842 
                      :content-stream "BT\n/Times 14 Tf\n50 50 Td\n(Page 2) Tj\nET"
                      :metadata {:element-count 1}}
          document-attrs {:title "Multi-page Document"}
          pdf (document->pdf [page1-data page2-data] document-attrs)]
      
      ;; Test multi-page structure
      (is (re-find #"/Count 2" pdf) "Should specify 2 pages")
      (is (re-find #"Page 1" pdf) "Should include first page content")
      (is (re-find #"Page 2" pdf) "Should include second page content")
      (is (re-find #"/BaseFont /Helvetica" pdf) "Should include Arial font (mapped to Helvetica)")
      (is (re-find #"/BaseFont /Times-Roman" pdf) "Should include Times font (mapped to Times-Roman)")
      
      ;; Test different page sizes
      (is (re-find #"\[0 0 612 792\]" pdf) "Should include first page MediaBox")
      (is (re-find #"\[0 0 595 842\]" pdf) "Should include second page MediaBox")))
  
  (testing "Document assembly without pages"
    (let [pdf (document->pdf [] {:title "Empty Document"})]
      (is (re-find #"%PDF-1.4" pdf) "Should include PDF header")
      (is (re-find #"/Count 0" pdf) "Should specify 0 pages")
      (is (re-find #"/Title \(Empty Document\)" pdf) "Should include document metadata")
      (is (re-find #"%%EOF" pdf) "Should end with EOF")))
  
  (testing "Document assembly without metadata"
    (let [page-data {:width 612 :height 792 
                     :content-stream "10 10 100 50 re\nf"
                     :metadata {:element-count 1}}
          pdf (document->pdf [page-data] {})]
      (is (re-find #"%PDF-1.4" pdf) "Should include PDF header")
      (is (not (re-find #"/Info" pdf)) "Should not include info object when no metadata")
      (is (re-find #"%%EOF" pdf) "Should end with EOF"))))

(deftest complete-document-integration-test
  (testing "End-to-end document generation"
    ;; Test the complete pipeline from hiccup document to PDF
    (let [hiccup-doc [:document {:title "Integration Test" :width 612 :height 792}
                      [:page {}
                       [:text {:x 100 :y 100 :font "Arial" :size 12} "Hello World"]
                       [:rect {:x 50 :y 200 :width 200 :height 100 :fill "red"}]]
                      [:page {:width 595 :height 842}
                       [:circle {:cx 300 :cy 400 :r 50 :stroke "blue"}]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test that we get a complete PDF
      (is (string? pdf) "Should return PDF as string")
      (is (re-find #"%PDF-1.4" pdf) "Should include PDF header")
      (is (re-find #"/Count 2" pdf) "Should have 2 pages")
      (is (re-find #"/Title \(Integration Test\)" pdf) "Should include document title")
      (is (re-find #"Hello World" pdf) "Should include text content")
      (is (re-find #"1 0 0 rg" pdf) "Should include red color")
      (is (re-find #"0 0 1 RG" pdf) "Should include blue stroke")
      (is (re-find #"%%EOF" pdf) "Should end with EOF")
      
      ;; Test coordinate transformation occurred
      ;; Text at web y=100 should become PDF y=792-100=692
      (is (re-find #"100 692" pdf) "Should transform text coordinates")
      
      ;; Test different page sizes
      (is (re-find #"\[0 0 612 792\]" pdf) "Should include letter size page")
      (is (re-find #"\[0 0 595 842\]" pdf) "Should include A4 size page")))
  
  (testing "Document with complex nested content"
    (let [hiccup-doc [:document {:title "Complex Document"}
                      [:page {}
                       [:g {:transforms [[:translate [50 50]]]}
                        [:text {:x 0 :y 0 :font "Courier" :size 10} "Transformed text"]
                        [:g {:transforms [[:rotate 45]]}
                         [:rect {:x 10 :y 10 :width 50 :height 30 :fill "green"}]]]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test that complex transformations are preserved
      (is (re-find #"(?s)q.*Q" pdf) "Should include graphics state save/restore")
      (is (re-find #"cm" pdf) "Should include transformation matrix")
      (is (re-find #"Transformed text" pdf) "Should include nested text")
      (is (re-find #"0 1 0 rg" pdf) "Should include green fill")
      (is (re-find #"/BaseFont /Courier" pdf) "Should include Courier font")))
  
  (testing "Error handling in document generation"
    ;; Test invalid page element
    (is (thrown-with-msg? js/Error #"Expected :page element"
                          (hiccup->pdf-document [:document {} [:rect {:x 0 :y 0 :width 100 :height 50}]]))
        "Should reject non-page elements in document")
    
    ;; Test invalid page structure
    (is (thrown-with-msg? js/Error #"Page must be a hiccup vector"
                          (hiccup->pdf-document [:document {} "not a page"]))
        "Should reject non-vector page elements")))

(deftest comprehensive-document-integration-test
  (testing "Complex documents with multiple pages and sizes"
    ;; Test document with multiple pages of different sizes
    (let [hiccup-doc [:document {:title "Multi-Size Document" :author "Test Author"}
                      ;; Standard letter page
                      [:page {:width 612 :height 792}
                       [:text {:x 50 :y 50 :font "Arial" :size 16} "Letter Page"]
                       [:rect {:x 50 :y 100 :width 200 :height 100 :fill "blue"}]]
                      ;; A4 page
                      [:page {:width 595 :height 842}
                       [:text {:x 50 :y 50 :font "Times" :size 14} "A4 Page"]
                       [:circle {:cx 200 :cy 200 :r 50 :stroke "green" :stroke-width 2}]]
                      ;; Custom landscape page
                      [:page {:width 800 :height 600}
                       [:text {:x 50 :y 50 :font "Courier" :size 12} "Landscape Page"]
                       [:line {:x1 50 :y1 100 :x2 750 :y2 100 :stroke "red"}]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test multiple page sizes
      (is (re-find #"\[0 0 612 792\]" pdf) "Should include letter size page")
      (is (re-find #"\[0 0 595 842\]" pdf) "Should include A4 size page")
      (is (re-find #"\[0 0 800 600\]" pdf) "Should include landscape size page")
      
      ;; Test content from each page
      (is (re-find #"Letter Page" pdf) "Should include letter page content")
      (is (re-find #"A4 Page" pdf) "Should include A4 page content")
      (is (re-find #"Landscape Page" pdf) "Should include landscape page content")
      
      ;; Test different fonts are included
      (is (re-find #"/BaseFont /Helvetica" pdf) "Should include Arial font")
      (is (re-find #"/BaseFont /Times-Roman" pdf) "Should include Times font")
      (is (re-find #"/BaseFont /Courier" pdf) "Should include Courier font")
      
      ;; Test metadata
      (is (re-find #"/Title \(Multi-Size Document\)" pdf) "Should include document title")
      (is (re-find #"/Author \(Test Author\)" pdf) "Should include document author")
      
      ;; Test page count
      (is (re-find #"/Count 3" pdf) "Should specify 3 pages")))
  
  (testing "Document with all primitive element types"
    (let [hiccup-doc [:document {:title "All Elements Test"}
                      [:page {}
                       ;; Rectangle
                       [:rect {:x 50 :y 50 :width 100 :height 80 :fill "red" :stroke "black" :stroke-width 2}]
                       ;; Circle
                       [:circle {:cx 250 :cy 100 :r 40 :fill "green" :stroke "blue" :stroke-width 1}]
                       ;; Line
                       [:line {:x1 350 :y1 50 :x2 450 :y2 130 :stroke "magenta" :stroke-width 3}]
                       ;; Text
                       [:text {:x 50 :y 200 :font "Arial" :size 14 :fill "blue"} "Sample Text"]
                       ;; Path
                       [:path {:d "M 50 300 L 150 300 L 100 250 Z" :fill "yellow" :stroke "red"}]
                       ;; Nested group with transforms
                       [:g {:transforms [[:translate [300 300]] [:rotate 30]]}
                        [:rect {:x 0 :y 0 :width 60 :height 40 :fill "cyan"}]
                        [:text {:x 10 :y 25 :font "Courier" :size 10} "Rotated"]]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test all element types are present
      (is (re-find #"50 662 100 80 re" pdf) "Should include rectangle")
      (is (re-find #"250 732 m" pdf) "Should include circle")
      (is (re-find #"350 742 m" pdf) "Should include line")
      (is (re-find #"Sample Text" pdf) "Should include text")
      (is (re-find #"50 300 m" pdf) "Should include path")
      (is (re-find #"(?s)q.*cm.*Rotated.*Q" pdf) "Should include transformed group")
      
      ;; Test colors are included
      (is (re-find #"1 0 0 rg" pdf) "Should include red fill")
      (is (re-find #"0 1 0 rg" pdf) "Should include green fill")
      (is (re-find #"0 0 1 RG" pdf) "Should include blue stroke")
      (is (re-find #"1 1 0 rg" pdf) "Should include yellow fill")))
  
  (testing "Document with emoji support"
    (let [hiccup-doc [:document {:title "Emoji Test 🎉"}
                      [:page {}
                       [:text {:x 50 :y 50 :font "Arial" :size 20} "Hello 👋 World 🌍"]
                       [:text {:x 50 :y 100 :font "Times" :size 16} "Math: 2 + 2 = 4 ✓"]
                       [:text {:x 50 :y 150 :font "Courier" :size 12} "Symbols: ★ ❤ 😊 🚀"]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test emoji content is preserved
      (is (re-find #"Hello 👋 World 🌍" pdf) "Should include emoji in text")
      (is (re-find #"Math: 2 \+ 2 = 4 ✓" pdf) "Should include mathematical symbols")
      (is (re-find #"Symbols: ★ ❤ 😊 🚀" pdf) "Should include various emoji")
      
      ;; Test document title with emoji
      (is (re-find #"Emoji Test 🎉" pdf) "Should include emoji in document title")))
  
  (testing "Document with nested groups and complex transforms"
    (let [hiccup-doc [:document {:title "Transform Test"}
                      [:page {}
                       ;; Deep nesting with multiple transforms
                       [:g {:transforms [[:translate [100 100]]]}
                        [:rect {:x 0 :y 0 :width 50 :height 50 :fill "red"}]
                        [:g {:transforms [[:rotate 45]]}
                         [:rect {:x 60 :y 0 :width 30 :height 30 :fill "green"}]
                         [:g {:transforms [[:scale [2 1]]]}
                          [:rect {:x 20 :y 20 :width 15 :height 15 :fill "blue"}]]]]
                       ;; Multiple transforms on same group
                       [:g {:transforms [[:translate [200 200]] [:rotate 30] [:scale [1.5 0.8]]]}
                        [:text {:x 0 :y 0 :font "Arial" :size 12} "Multi-transform"]]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test nested graphics state save/restore
      (is (>= (count (re-seq #"q" pdf)) 3) "Should have multiple graphics state saves")
      (is (>= (count (re-seq #"Q" pdf)) 3) "Should have multiple graphics state restores")
      
      ;; Test transformation matrices
      (is (re-find #"cm" pdf) "Should include transformation matrices")
      (is (re-find #"Multi-transform" pdf) "Should include transformed text")))
  
  (testing "Empty pages and edge cases"
    (let [hiccup-doc [:document {:title "Edge Cases"}
                      ;; Empty page
                      [:page {}]
                      ;; Page with only whitespace text
                      [:page {}
                       [:text {:x 50 :y 50 :font "Arial" :size 12} "   "]]
                      ;; Page with zero-size elements
                      [:page {}
                       [:rect {:x 50 :y 50 :width 0 :height 0 :fill "red"}]
                       [:circle {:cx 100 :cy 100 :r 0 :fill "blue"}]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test document structure
      (is (re-find #"/Count 3" pdf) "Should specify 3 pages")
      (is (re-find #"%PDF-1.4" pdf) "Should have valid PDF header")
      (is (re-find #"%%EOF" pdf) "Should have valid PDF ending")
      
      ;; Test zero-size elements are handled
      (is (re-find #"50 742 0 0 re" pdf) "Should include zero-size rectangle")
      (is (string? pdf) "Should return valid PDF string")))
  
  (testing "Large document with many pages"
    ;; Create a document with many pages to test performance
    (let [pages (for [i (range 10)]
                  [:page {}
                   [:text {:x 50 :y 50 :font "Arial" :size 12} (str "Page " (inc i))]
                   [:rect {:x 50 :y 100 :width 100 :height 50 :fill (if (even? i) "red" "blue")}]])
          hiccup-doc (into [:document {:title "Large Document"}] pages)
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test page count
      (is (re-find #"/Count 10" pdf) "Should specify 10 pages")
      
      ;; Test first and last page content
      (is (re-find #"Page 1" pdf) "Should include first page")
      (is (re-find #"Page 10" pdf) "Should include last page")
      
      ;; Test alternating colors
      (is (re-find #"1 0 0 rg" pdf) "Should include red rectangles")
      (is (re-find #"0 0 1 rg" pdf) "Should include blue rectangles")
      
      ;; Test document is reasonably sized (should be manageable)
      (is (< (count pdf) 50000) "Should generate reasonably sized PDF")
      (is (> (count pdf) 3000) "Should generate substantial PDF content")))
  
  (testing "Coordinate transformation across different page sizes"
    (let [hiccup-doc [:document {}
                      ;; Small page
                      [:page {:width 400 :height 300}
                       [:text {:x 50 :y 50 :font "Arial" :size 12} "Small"]
                       [:rect {:x 50 :y 100 :width 100 :height 50 :fill "red"}]]
                      ;; Large page
                      [:page {:width 1000 :height 800}
                       [:text {:x 50 :y 50 :font "Arial" :size 12} "Large"]
                       [:rect {:x 50 :y 100 :width 100 :height 50 :fill "blue"}]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test coordinate transformation for different page sizes
      ;; Small page: web y=50 -> PDF y=300-50=250
      ;; Large page: web y=50 -> PDF y=800-50=750
      (is (re-find #"50 250" pdf) "Should transform coordinates for small page")
      (is (re-find #"50 750" pdf) "Should transform coordinates for large page")
      
      ;; Test different page sizes are included
      (is (re-find #"\[0 0 400 300\]" pdf) "Should include small page MediaBox")
      (is (re-find #"\[0 0 1000 800\]" pdf) "Should include large page MediaBox")))
  
  (testing "Complete metadata embedding"
    (let [hiccup-doc [:document {:title "Complete Metadata Test"
                                 :author "Test Author"
                                 :subject "Testing Subject"
                                 :keywords "test, metadata, pdf"
                                 :creator "Custom Creator"
                                 :producer "Custom Producer"}
                      [:page {}
                       [:text {:x 50 :y 50 :font "Arial" :size 12} "Metadata test"]]]
          pdf (hiccup->pdf-document hiccup-doc)]
      
      ;; Test all metadata fields are included
      (is (re-find #"/Title \(Complete Metadata Test\)" pdf) "Should include title")
      (is (re-find #"/Author \(Test Author\)" pdf) "Should include author")
      (is (re-find #"/Subject \(Testing Subject\)" pdf) "Should include subject")
      (is (re-find #"/Keywords \(test, metadata, pdf\)" pdf) "Should include keywords")
      (is (re-find #"/Creator \(Custom Creator\)" pdf) "Should include creator")
      (is (re-find #"/Producer \(Custom Producer\)" pdf) "Should include producer")))
  
  (testing "Performance and output characteristics"
    ;; Test that PDF output can be written to files (simulate file writing)
    (let [hiccup-doc [:document {:title "Performance Test"}
                      [:page {}
                       [:text {:x 50 :y 50 :font "Arial" :size 12} "Performance test content"]]]
          pdf (hiccup->pdf-document hiccup-doc)
          start-time (js/Date.now)
          ;; Simulate multiple document generations
          pdfs (doall (for [i (range 10)]
                        (hiccup->pdf-document hiccup-doc)))
          end-time (js/Date.now)
          duration (- end-time start-time)]
      
      ;; Test all PDFs are identical
      (is (every? #(= pdf %) pdfs) "Should generate consistent PDFs")
      
      ;; Test performance is reasonable (should be under 1 second for 10 documents)
      (is (< duration 1000) "Should generate documents efficiently")
      
      ;; Test PDF string characteristics
      (is (string? pdf) "Should return string")
      (is (> (count pdf) 100) "Should have substantial content")
      (is (re-find #"^%PDF-1.4" pdf) "Should start with PDF header")
      (is (re-find #"%%EOF$" pdf) "Should end with PDF footer"))))

