(require '[hiccup-pdf.core :refer [hiccup->pdf-document]])

(println "Generating half-letter horizontal page with green triangle in red square...")

;; Half-letter horizontal page dimensions
;; Standard letter is 612x792 points, so half-letter horizontal is 612x396 points
(let [page-width 612
      page-height 396
      
      ;; Calculate center of page
      center-x (/ page-width 2)   ; 306
      center-y (/ page-height 2)  ; 198
      
      ;; Red square dimensions (let's make it 200x200 points)
      square-size 200
      square-x (- center-x (/ square-size 2))  ; 206
      square-y (- center-y (/ square-size 2))  ; 98
      
      ;; Green triangle - equilateral triangle centered in the square
      ;; Triangle size (height from center to vertex)
      triangle-radius 60
      ;; Calculate triangle vertices (equilateral triangle pointing up)
      ;; Top vertex
      top-x center-x
      top-y (- center-y triangle-radius)
      ;; Bottom left vertex  
      bottom-left-x (- center-x (* triangle-radius 0.866))  ; cos(30°) ≈ 0.866
      bottom-left-y (+ center-y (/ triangle-radius 2))
      ;; Bottom right vertex
      bottom-right-x (+ center-x (* triangle-radius 0.866))
      bottom-right-y (+ center-y (/ triangle-radius 2))
      
      ;; Create the PDF document
      pdf (hiccup->pdf-document
           [:document {:title "Green Triangle in Red Square"
                       :author "hiccup-pdf test"
                       :width page-width
                       :height page-height}
            [:page {}
             ;; Red square in center of page
             [:rect {:x square-x 
                     :y square-y 
                     :width square-size 
                     :height square-size 
                     :fill "red"}]
             
             ;; Green triangle in center of square (using path for triangle)
             [:path {:d (str "M " top-x " " top-y 
                            " L " bottom-left-x " " bottom-left-y
                            " L " bottom-right-x " " bottom-right-y
                            " Z")
                     :fill "green"}]
             
             ;; Optional: Add some labels for verification
             [:text {:x 50 :y 50 :font "Arial" :size 12 :fill "black"} 
                    (str "Half-letter horizontal: " page-width "x" page-height " pts")]
             [:text {:x 50 :y 70 :font "Arial" :size 10 :fill "black"} 
                    (str "Square center: (" center-x ", " center-y ")")]
             [:text {:x 50 :y 90 :font "Arial" :size 10 :fill "black"} 
                    (str "Square: " square-size "x" square-size " at (" square-x ", " square-y ")")]]])]

  ;; Display results
  (println "PDF generated successfully!")
  (println "Document size:" (count pdf) "characters")
  (println "Page dimensions:" page-width "x" page-height "points")
  (println "Square position:" square-x "," square-y "with size" square-size "x" square-size)
  (println "Triangle vertices:")
  (println "  Top:" top-x "," top-y)
  (println "  Bottom-left:" bottom-left-x "," bottom-left-y)  
  (println "  Bottom-right:" bottom-right-x "," bottom-right-y)
  
  ;; Verify PDF structure
  (println "\nPDF verification:")
  (println "- Starts with PDF header:" (.startsWith pdf "%PDF-1.4"))
  (println "- Contains red fill:" (boolean (re-find #"1 0 0 rg" pdf)))
  (println "- Contains green fill:" (boolean (re-find #"0 1 0 rg" pdf)))
  (println "- Contains path data:" (boolean (re-find #"[ML].*Z" pdf)))
  (println "- Ends with EOF:" (.endsWith pdf "%%EOF"))
  
  ;; Save instructions
  (println "\nTo save this PDF to a file, you could use:")
  (println "(require '[\"fs\" :as fs])")
  (println "(fs/writeFileSync \"triangle_test.pdf\" pdf)")
  
  ;; Return the PDF for inspection if needed
  pdf)