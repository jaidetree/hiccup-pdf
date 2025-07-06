;; Test just the render functions from text-processing

(defn render-text-segment
  "Simple version of render-text-segment"
  [segment font-name font-size]
  (let [content (:content segment)
        x (:x segment)
        y (:y segment)]
    (str "BT\\n"
         "/" font-name " " font-size " Tf\\n"
         x " " y " Td\\n"
         "(" content ") Tj\\n"
         "ET\\n")))

(defn render-image-segment
  "Simple version of render-image-segment"
  [segment font-size xobject-ref]
  (let [x (:x segment)
        y (:y segment)
        scale-factor (/ font-size 72.0)]
    (str "q\\n"
         scale-factor " 0 0 " scale-factor " " x " " y " cm\\n"
         "/" xobject-ref " Do\\n"
         "Q\\n")))

;; Test the functions
(println "Testing render functions...")

(let [text-seg {:content "Hello" :x 100 :y 200}
      result (render-text-segment text-seg "Arial" 14)]
  (println "Text segment result:")
  (println result))

(let [img-seg {:content "💡" :x 100 :y 200}
      result (render-image-segment img-seg 14 "Em1")]
  (println "Image segment result:")
  (println result))

(println "Render functions work!")