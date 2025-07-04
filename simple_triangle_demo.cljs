(require '[hiccup-pdf.core :refer [hiccup->pdf-document]])

;; Simple demo: Green triangle in red square on half-letter horizontal page
(def triangle-pdf
  (hiccup->pdf-document
    [:document {:title "Triangle Demo"}
     ;; Half-letter horizontal: 612 x 396 points (standard letter height ÷ 2)
     [:page {:width 612 :height 396}
      
      ;; Red square (200x200) centered at (306, 198)
      ;; Top-left corner at (206, 98)
      [:rect {:x 206 :y 98 :width 200 :height 200 :fill "red"}]
      
      ;; Green equilateral triangle centered in the square
      ;; Using path to create triangle pointing upward
      [:path {:d "M 306 138 L 254 228 L 358 228 Z" :fill "green"}]]]))

;; Display results
(println "Triangle PDF demo generated!")
(println "Document size:" (count triangle-pdf) "characters")
(println "Valid PDF format:" 
         (and (.startsWith triangle-pdf "%PDF-1.4") 
              (.endsWith triangle-pdf "%%EOF")))

;; Show the coordinate calculations
(println "\nGeometry:")
(println "• Page: 612×396 pts (half-letter horizontal)")
(println "• Page center: (306, 198)")  
(println "• Red square: 200×200 at (206, 98)")
(println "• Green triangle: equilateral, centered in square")
(println "  - Top vertex: (306, 138)")
(println "  - Left vertex: (254, 228)")  
(println "  - Right vertex: (358, 228)")

triangle-pdf