(require '[dev.jaide.hiccup-pdf.core :refer [hiccup->pdf-document]]
         '["fs" :as fs])

(println "=== Triangle Test with File Save ===")

;; Generate the PDF
(let [pdf (hiccup->pdf-document
           [:document {:title "Green Triangle in Red Square Test"
                       :author "hiccup-pdf library"
                       :subject "Geometric shapes test"}
            ;; Half-letter horizontal page (612x396 points)
            [:page {:width 612 :height 396}

             ;; Red square (200x200) centered on page
             ;; Page center: (306, 198), square should be at (206, 98) in web coordinates
             ;; After transform: y = 396 - 98 = 298, but we want center at 198
             ;; So input y should be: 396 - 198 - 100 = 98... wait, let me recalculate
             ;; For center at y=198 in PDF coords, top edge should be at y=298 in PDF
             ;; So in web coords: y = 396 - 298 = 98 ✓ This should be right...
             ;; Let me try a different approach - center at 198 web coords
             [:rect {:x 206 :y 98 :width 200 :height 200 :fill "#ff0000"}]

             ;; Green equilateral triangle centered in square  
             ;; Triangle center should be at (306, 198) in web coordinates
             ;; Triangle vertices: top at (306, 138), bottom at y=228
             [:path {:d "M 306 138 L 254.04 228 L 357.96 228 Z"
                     :fill "#00ff00"}]

             ;; Title and info
             [:text {:x 50 :y 50 :font "Arial" :size 16 :fill "#000000"}
              "Green Triangle in Red Square"]
             [:text {:x 50 :y 80 :font "Arial" :size 12 :fill "#000000"}
              "Half-letter horizontal (612×396 pts)"]
             [:text {:x 50 :y 100 :font "Arial" :size 10 :fill "#000000"}
              "Generated by hiccup-pdf library"]]])

      filename "triangle_test.pdf"]

  ;; Save to file
  (fs/writeFileSync filename pdf)

  ;; Report results
  (println "✅ PDF generated and saved!")
  (println "📄 File:" filename)
  (println "📏 Size:" (count pdf) "characters")
  (println "🎨 Content: Green triangle inside red square on half-letter page")

  ;; Verify file was created
  (let [file-stats (fs/statSync filename)]
    (println "💾 File size:" (.-size file-stats) "bytes"))

  (println "\n🔍 PDF Structure Verification:")
  (println "- PDF version:" (boolean (re-find #"%PDF-1\.4" pdf)))
  (println "- Document title:" (boolean (re-find #"Green Triangle" pdf)))
  (println "- Page MediaBox:" (boolean (re-find #"MediaBox.*612.*396" pdf)))
  (println "- Red rectangle:" (boolean (re-find #"1 0 0 rg.*re.*f" pdf)))
  (println "- Green triangle:" (boolean (re-find #"0 1 0 rg.*M.*L.*Z" pdf)))
  (println "- Text content:" (boolean (re-find #"hiccup-pdf" pdf)))
  (println "- Valid ending:" (.endsWith pdf "%%EOF"))

  (println "\n📖 You can now:")
  (println "1. Open" filename "in any PDF viewer")
  (println "2. Verify the green triangle is centered in the red square")
  (println "3. Check that it's on a half-letter horizontal page"))
