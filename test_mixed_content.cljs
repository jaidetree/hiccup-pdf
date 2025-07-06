(require '[clojure.string :as str]
         '[dev.jaide.hiccup-pdf.text-processing :as text-proc]
         '[dev.jaide.hiccup-pdf.images :as images])

(println "Testing mixed content rendering...")

;; Test basic text segment rendering
(let [segment {:content "Hello" :x 100 :y 200}
      result (text-proc/render-text-segment segment "Arial" 14)]
  (println "Text segment result:")
  (println result)
  (println "Contains BT?" (str/includes? result "BT"))
  (println "Contains ET?" (str/includes? result "ET")))

;; Test basic image segment rendering  
(let [segment {:content "💡" :x 100 :y 200}
      result (text-proc/render-image-segment segment 14 "Em1")]
  (println "\nImage segment result:")
  (println result)
  (println "Contains q?" (clojure.string/includes? result "q"))
  (println "Contains Q?" (clojure.string/includes? result "Q")))

;; Test position calculation
(let [segments [{:type :text :content "Hi "}
                {:type :emoji :content "💡"}
                {:type :text :content " ok"}]
      result (text-proc/calculate-segment-positions segments 0 0 12)]
  (println "\nPosition calculation:")
  (println "Number of segments:" (count result))
  (println "First segment x:" (:x (first result)))
  (println "Second segment x:" (:x (second result)))
  (println "Third segment x:" (:x (nth result 2))))

;; Test XObject reference mapping
(let [result (text-proc/create-xobject-reference-map "Hello 💡 world 🎯")]
  (println "\nXObject references:")
  (println result))

;; Test unique emoji extraction
(let [result (text-proc/extract-unique-emoji "Hello 💡 world 🎯 more 💡")]
  (println "\nUnique emoji:")
  (println result))

;; Test complete mixed content processing
(let [cache (images/create-image-cache)
      text "Hello 💡 world"
      xobject-refs {"💡" "Em1"}
      result (text-proc/process-mixed-content text 100 200 "Arial" 14 cache {:xobject-refs xobject-refs})]
  (println "\nComplete processing:")
  (println "Success:" (:success result))
  (println "Segment count:" (count (:segments result)))
  (println "Operators length:" (count (:operators result)))
  (when (:success result)
    (println "First 200 chars of operators:")
    (println (subs (:operators result) 0 (min 200 (count (:operators result)))))))

(println "\nAll tests completed!")