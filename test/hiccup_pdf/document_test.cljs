(ns hiccup-pdf.document-test
  (:require [cljs.test :refer [deftest is testing]]
            [hiccup-pdf.core :refer [hiccup->pdf-document]]
            [hiccup-pdf.document :refer [hiccup-document->pdf]]))

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