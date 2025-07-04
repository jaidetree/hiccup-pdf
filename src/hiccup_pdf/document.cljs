(ns hiccup-pdf.document
  "PDF document generation functionality for complete PDF files with pages."
  (:require [hiccup-pdf.validation :as v]
            [clojure.string :as str]))

(defn hiccup-document->pdf
  "Implementation function for generating complete PDF documents from hiccup.

  Takes a hiccup document vector with :document root element containing :page elements
  and returns a complete PDF document as a string.

  Args:
    hiccup-document: Hiccup vector with [:document attrs & pages] structure

  Returns:
    Complete PDF document as string

  Example:
    (hiccup-document->pdf
      [:document {:title \"My Doc\"}
       [:page {} [:rect {:x 10 :y 10 :width 100 :height 50}]]])"
  [hiccup-document]
  ;; Basic validation - must be a vector with :document as first element
  (when-not (vector? hiccup-document)
    (throw (js/Error. "Document must be a hiccup vector")))

  (when (empty? hiccup-document)
    (throw (js/Error. "Document vector cannot be empty")))

  (let [validated-structure (v/validate-hiccup-structure hiccup-document)
        [tag attributes & _pages] validated-structure]

    ;; Validate element type - must be :document
    (when-not (= tag :document)
      (throw (js/Error. (str "Root element must be :document, got: " tag))))

    ;; Validate element type with general validator
    (v/validate-element-type tag)

    ;; Validate and apply document attribute defaults
    (let [validated-attributes (v/validate-document-attributes attributes)]

      ;; For now, return a placeholder with validated attributes
      ;; This will be implemented in subsequent steps
      (str "PDF document placeholder for: "
           (:title validated-attributes "Untitled Document")
           " (width: " (:width validated-attributes)
           ", height: " (:height validated-attributes) ")"))))

(defn web-to-pdf-y
  "Converts web-style Y coordinate to PDF-style Y coordinate.

  Web coordinates: (0,0) at top-left, Y increases downward
  PDF coordinates: (0,0) at bottom-left, Y increases upward

  Args:
    web-y: Y coordinate in web-style coordinates
    page-height: Height of the page in PDF points
    margins: Optional margins vector [top right bottom left]

  Returns:
    Y coordinate in PDF-style coordinates"
  ([web-y page-height]
   (web-to-pdf-y web-y page-height [0 0 0 0]))
  ([web-y page-height margins]
   (let [[top-margin _ bottom-margin _] margins]
     ;; In web coordinates: y=0 is at top, increases downward
     ;; In PDF coordinates: y=0 is at bottom, increases upward
     ;; With margins: content area is between top-margin and (page-height - bottom-margin)
     ;; Web y=0 should map to PDF y=(page-height - top-margin)
     ;; Web y=top-margin should map to PDF y=(page-height - top-margin)
     ;; Web y=(page-height - bottom-margin) should map to PDF y=bottom-margin
     (- page-height web-y))))

(defn transform-element-coordinates
  "Transforms coordinates for a single element from web-style to PDF-style.

  Args:
    element: Hiccup element vector [tag attributes & children]
    page-height: Height of the page in PDF points
    margins: Margins vector [top right bottom left]

  Returns:
    Element with transformed coordinates"
  [element page-height margins]
  (if-not (vector? element)
    element
    (let [[tag attributes & children] element]
      (if-not (keyword? tag)
        element
        (let [transformed-attributes
              (case tag
                :rect (if (:y attributes)
                        (assoc attributes :y (web-to-pdf-y (:y attributes) page-height margins))
                        attributes)
                :circle (if (:cy attributes)
                          (assoc attributes :cy (web-to-pdf-y (:cy attributes) page-height margins))
                          attributes)
                :line (cond-> attributes
                        (:y1 attributes) (assoc :y1 (web-to-pdf-y (:y1 attributes) page-height margins))
                        (:y2 attributes) (assoc :y2 (web-to-pdf-y (:y2 attributes) page-height margins)))
                :text (if (:y attributes)
                        (assoc attributes :y (web-to-pdf-y (:y attributes) page-height margins))
                        attributes)
                :path attributes  ; Path coordinates are handled within the path data
                :g (if (:transforms attributes)
                     (let [transformed-transforms
                           (mapv (fn [transform]
                                   (if (and (vector? transform) (= :translate (first transform)))
                                     (let [[_ [tx ty]] transform]
                                       [:translate [tx (if ty (web-to-pdf-y ty page-height margins) 0)]])
                                     transform))
                                 (:transforms attributes))]
                       (assoc attributes :transforms transformed-transforms))
                     attributes)
                :document attributes  ; Document elements don't have coordinates
                :page attributes      ; Page elements don't have coordinates
                attributes)           ; Unknown elements pass through unchanged

              ;; Recursively transform children
              transformed-children (mapv #(transform-element-coordinates % page-height margins) children)]

          (into [tag transformed-attributes] transformed-children))))))

(defn transform-coordinates-for-page
  "Transforms all coordinates in page content from web-style to PDF-style.

  Args:
    page-content: Vector of hiccup elements representing page content
    page-height: Height of the page in PDF points
    margins: Margins vector [top right bottom left]

  Returns:
    Vector of hiccup elements with transformed coordinates"
  [page-content page-height margins]
  (mapv #(transform-element-coordinates % page-height margins) page-content))

(defn page->content-stream
  "Processes page content into a content stream with coordinate transformation.

  Args:
    page-attributes: Page-specific attributes (width, height, margins)
    page-content: Vector of hiccup elements representing page content
    document-defaults: Document defaults for inheritance

  Returns:
    Map with page data: {:width :height :margins :content-stream :metadata}"
  [page-attributes page-content document-defaults]
  (let [;; Validate and merge page attributes with document defaults
        validated-page-attrs (v/validate-page-attributes page-attributes document-defaults)
        page-width (:width validated-page-attrs)
        page-height (:height validated-page-attrs)
        page-margins (:margins validated-page-attrs [0 0 0 0])

        ;; Transform coordinates from web-style to PDF-style
        transformed-content (transform-coordinates-for-page page-content page-height page-margins)

        ;; Generate content stream using existing content stream generation
        ;; We need to call hiccup->pdf-ops for each element and combine results
        content-stream (str/join "\n"
                                 (map (fn [element]
                                        ;; Use require to access hiccup->pdf-ops function
                                        (let [core-ns (find-ns 'hiccup-pdf.core)
                                              hiccup->pdf-ops-fn (ns-resolve core-ns 'hiccup->pdf-ops)]
                                          (if hiccup->pdf-ops-fn
                                            (hiccup->pdf-ops-fn element)
                                            (throw (js/Error. "Could not resolve hiccup->pdf-ops function")))))
                                      transformed-content))]

    {:width page-width
     :height page-height
     :margins page-margins
     :content-stream content-stream
     :metadata {:element-count (count page-content)
                :has-transforms (boolean (some #(and (vector? %) (= :g (first %)) (:transforms (second %))) page-content))
                :coordinate-system "pdf"}}))

(defn extract-fonts-from-content
  "Extracts unique font names from page content for font resource dictionary.
  
  Args:
    page-content: Vector of hiccup elements
    
  Returns:
    Set of font names used in the content"
  [page-content]
  (letfn [(extract-fonts [element]
            (if-not (vector? element)
              #{}
              (let [[tag attributes & children] element]
                (cond-> #{}
                  (and (= tag :text) (:font attributes))
                  (conj (:font attributes))
                  
                  (seq children)
                  (into (mapcat extract-fonts children))))))]
    (into #{} (mapcat extract-fonts page-content))))

(defn generate-font-resource-object
  "Generates a font resource object for system fonts.
  
  Args:
    object-number: PDF object number
    font-name: Name of the font (e.g., 'Arial', 'Times-Roman')
    
  Returns:
    String containing PDF font object"
  [object-number font-name]
  (let [;; Map common font names to PDF standard font names
        pdf-font-name (case font-name
                        "Arial" "Helvetica"
                        "Times" "Times-Roman"
                        "Courier" "Courier"
                        "Times New Roman" "Times-Roman"
                        "Helvetica" "Helvetica"
                        ;; Default to Helvetica for unknown fonts
                        "Helvetica")]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Type /Font\n"
         "/Subtype /Type1\n"
         "/BaseFont /" pdf-font-name "\n"
         ">>\n"
         "endobj")))

(defn generate-content-stream-object
  "Generates a content stream object with proper length calculation.
  
  Args:
    object-number: PDF object number
    content-stream: String containing PDF operators
    
  Returns:
    String containing PDF content stream object"
  [object-number content-stream]
  (let [stream-length (count content-stream)]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Length " stream-length "\n"
         ">>\n"
         "stream\n"
         content-stream "\n"
         "endstream\n"
         "endobj")))

(defn generate-page-object
  "Generates a page object with MediaBox and resource references.
  
  Args:
    object-number: PDF object number
    page-data: Map with :width, :height, :content-stream, etc.
    parent-pages-ref: Reference to parent pages object
    content-stream-ref: Reference to content stream object
    font-refs: Map of font names to object references
    
  Returns:
    String containing PDF page object"
  [object-number page-data parent-pages-ref content-stream-ref font-refs]
  (let [{:keys [width height margins]} page-data
        [top right bottom left] (or margins [0 0 0 0])
        ;; MediaBox defines the page boundaries
        media-box (str "[" left " " bottom " " width " " height "]")
        ;; Create font resource dictionary
        font-dict (if (empty? font-refs)
                    ""
                    (str "/Font <<\n"
                         (str/join "\n" (map (fn [[font-name ref]]
                                               (str "/" font-name " " ref " 0 R"))
                                             font-refs))
                         "\n>>"))]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Type /Page\n"
         "/Parent " parent-pages-ref " 0 R\n"
         "/MediaBox " media-box "\n"
         (when (not (str/blank? font-dict))
           (str "/Resources <<\n" font-dict "\n>>\n"))
         "/Contents " content-stream-ref " 0 R\n"
         ">>\n"
         "endobj")))

(defn generate-pages-object
  "Generates a pages collection object.
  
  Args:
    object-number: PDF object number
    page-refs: Vector of page object references
    
  Returns:
    String containing PDF pages object"
  [object-number page-refs]
  (let [page-count (count page-refs)
        kids-array (str "[" (str/join " " (map #(str % " 0 R") page-refs)) "]")]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Type /Pages\n"
         "/Kids " kids-array "\n"
         "/Count " page-count "\n"
         ">>\n"
         "endobj")))

(defn generate-catalog-object
  "Generates the PDF catalog (root) object.
  
  Args:
    object-number: PDF object number  
    pages-ref: Reference to pages object
    
  Returns:
    String containing PDF catalog object"
  [object-number pages-ref]
  (str object-number " 0 obj\n"
       "<<\n"
       "/Type /Catalog\n"
       "/Pages " pages-ref " 0 R\n"
       ">>\n"
       "endobj"))

(defn generate-info-object
  "Generates the PDF info object with document metadata.
  
  Args:
    object-number: PDF object number
    document-attributes: Map with document metadata
    
  Returns:
    String containing PDF info object"
  [object-number document-attributes]
  (let [{:keys [title author subject keywords creator producer]} document-attributes
        ;; Helper to format PDF string with proper escaping
        format-pdf-string (fn [s] (when s (str "(" s ")")))]
    (str object-number " 0 obj\n"
         "<<\n"
         (when title (str "/Title " (format-pdf-string title) "\n"))
         (when author (str "/Author " (format-pdf-string author) "\n"))
         (when subject (str "/Subject " (format-pdf-string subject) "\n"))
         (when keywords (str "/Keywords " (format-pdf-string keywords) "\n"))
         (when creator (str "/Creator " (format-pdf-string creator) "\n"))
         (when producer (str "/Producer " (format-pdf-string producer) "\n"))
         ">>\n"
         "endobj")))

