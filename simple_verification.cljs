(require '[hiccup-pdf.core :refer [hiccup->pdf-document hiccup->pdf-ops]])

(println "Simple Verification Test")

;; Test content stream generation
(let [ops (hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}])]
  (println "Content stream:" (count ops) "characters"))

;; Test document generation
(let [doc (hiccup->pdf-document
           [:document {:title "Test"}
            [:page {}
             [:text {:x 100 :y 100 :font "Arial" :size 16} "Hello PDF!"]]])]
  (println "Document size:" (count doc) "characters")
  (println "Valid PDF:" (and (.startsWith doc "%PDF-1.4") (.endsWith doc "%%EOF"))))

(println "Verification complete!")