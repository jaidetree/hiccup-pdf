(ns hiccup-pdf.document-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.core :refer [hiccup->pdf-document]]
            [hiccup-pdf.document :refer [hiccup-document->pdf]]
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