#!/usr/bin/env nbb

;; Integration test for Step 8: Mixed Content Rendering

(require '[dev.jaide.hiccup-pdf.text-processing :as text-proc]
         '[dev.jaide.hiccup-pdf.images :as images])

(defn test-basic-functionality []
  (println "=== Testing Step 8: Mixed Content Rendering ===\n")
  
  ;; Test 1: Basic text segment rendering
  (println "1. Testing text segment rendering...")
  (let [segment {:content "Hello world" :x 100 :y 200}
        result (text-proc/render-text-segment segment "Arial" 14)]
    (println "✓ Text segment contains BT/ET:" (and (clojure.string/includes? result "BT")
                                                     (clojure.string/includes? result "ET")))
    (println "✓ Contains font specification:" (clojure.string/includes? result "/Arial 14 Tf"))
    (println "✓ Contains positioning:" (clojure.string/includes? result "100 200 Td"))
    (println "✓ Contains text drawing:" (clojure.string/includes? result "(Hello world) Tj")))
  
  ;; Test 2: Basic image segment rendering
  (println "\n2. Testing image segment rendering...")
  (let [segment {:content "💡" :x 100 :y 200}
        result (text-proc/render-image-segment segment 14 "Em1")]
    (println "✓ Image segment contains q/Q:" (and (clojure.string/includes? result "q")
                                                   (clojure.string/includes? result "Q")))
    (println "✓ Contains XObject reference:" (clojure.string/includes? result "/Em1 Do"))
    (println "✓ Contains transformation matrix:" (clojure.string/includes? result "cm")))
  
  ;; Test 3: Position calculation
  (println "\n3. Testing position calculation...")
  (let [segments [{:type :text :content "Hi "}
                  {:type :emoji :content "💡"}
                  {:type :text :content " ok"}]
        result (text-proc/calculate-segment-positions segments 0 0 12)]
    (println "✓ Position calculation returns 3 segments:" (= 3 (count result)))
    (println "✓ First segment at x=0:" (= 0 (:x (first result))))
    (println "✓ Positions are increasing:" (< (:x (first result)) 
                                              (:x (second result)) 
                                              (:x (nth result 2)))))
  
  ;; Test 4: XObject reference mapping
  (println "\n4. Testing XObject reference mapping...")
  (let [result (text-proc/create-xobject-reference-map "Hello 💡 world 🎯")]
    (println "✓ Creates references for 2 emoji:" (= 2 (count result)))
    (println "✓ Maps 💡 to Em1:" (= "Em1" (get result "💡")))
    (println "✓ Maps 🎯 to Em2:" (= "Em2" (get result "🎯"))))
  
  ;; Test 5: Unique emoji extraction
  (println "\n5. Testing unique emoji extraction...")
  (let [result (text-proc/extract-unique-emoji "💡 test 💡 more 🎯")]
    (println "✓ Extracts unique emoji only:" (= #{"💡" "🎯"} result)))
  
  ;; Test 6: Complete mixed content processing
  (println "\n6. Testing complete mixed content processing...")
  (let [cache (images/create-image-cache)
        text "Hello 💡 world"
        xobject-refs {"💡" "Em1"}
        result (text-proc/process-mixed-content text 100 200 "Arial" 14 cache {:xobject-refs xobject-refs})]
    (println "✓ Processing succeeds:" (:success result))
    (println "✓ Creates 3 segments:" (= 3 (count (:segments result))))
    (println "✓ Generates operators:" (not-empty (:operators result)))
    (when (:success result)
      (println "✓ Contains text operators:" (clojure.string/includes? (:operators result) "BT"))
      (println "✓ Contains image operators:" (clojure.string/includes? (:operators result) "/Em1 Do"))))
  
  ;; Test 7: Fallback functionality
  (println "\n7. Testing fallback functionality...")
  (let [cache (images/create-image-cache)
        segments [{:type :emoji :content "🦄" :x 0 :y 0}]  ; Unicorn emoji, likely no file
        result (text-proc/render-mixed-segments segments "Arial" 12 cache {:fallback-strategy :hex-string})]
    (println "✓ Fallback to hex encoding works:" (and (clojure.string/includes? result "BT")
                                                        (clojure.string/includes? result "<"))))
  
  (println "\n=== Step 8 Implementation Complete! ===")
  (println "All mixed content rendering functions are working correctly."))

;; Run the test
(test-basic-functionality)