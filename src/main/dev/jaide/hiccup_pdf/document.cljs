(ns dev.jaide.hiccup-pdf.document
  "PDF document generation functionality for complete PDF files with pages."
  (:require [dev.jaide.hiccup-pdf.validation :as v]
            [clojure.string :as str]
            [dev.jaide.hiccup-pdf.images :as images]
))

(declare document->pdf page->content-stream collect-page-images)

(defn hiccup-document->pdf
  "Implementation function for generating complete PDF documents from hiccup with emoji support.

  Takes a hiccup document vector with :document root element containing :page elements
  and returns a complete PDF document as a string.

  Args:
    hiccup-document: Hiccup vector with [:document attrs & pages] structure
    options: Optional map with emoji configuration

  Returns:
    Complete PDF document as string

  Example:
    (hiccup-document->pdf
      [:document {:title \"My Doc\"}
       [:page {} [:rect {:x 10 :y 10 :width 100 :height 50}]]]
      {:enable-emoji-images true :image-cache cache})"
  [hiccup-document & [options]]
  ;; Basic validation - must be a vector with :document as first element
  (when-not (vector? hiccup-document)
    (throw (js/Error. "Document must be a hiccup vector")))

  (when (empty? hiccup-document)
    (throw (js/Error. "Document vector cannot be empty")))

  (let [opts (merge {:enable-emoji-images false} options)
        validated-structure (v/validate-hiccup-structure hiccup-document)
        [tag attributes & _pages] validated-structure]

    ;; Validate element type - must be :document
    (when-not (= tag :document)
      (throw (js/Error. (str "Root element must be :document, got: " tag))))

    ;; Validate element type with general validator
    (v/validate-element-type tag)

    ;; Validate and apply document attribute defaults
    (let [validated-attributes (v/validate-document-attributes attributes)]

      ;; Process all pages in the document
      (if (empty? _pages)
        ;; Document with no pages - create a minimal PDF
        (document->pdf [] validated-attributes opts)
        
        ;; Process each page with emoji support
        (let [pages-data (mapv (fn [page-element]
                                 (when-not (vector? page-element)
                                   (throw (js/Error. "Page must be a hiccup vector")))
                                 (let [[page-tag page-attributes & page-content] page-element]
                                   (when-not (= page-tag :page)
                                     (throw (js/Error. (str "Expected :page element, got: " page-tag))))
                                   ;; Process page content into content stream with emoji support
                                   (page->content-stream page-attributes page-content validated-attributes opts)))
                               _pages)]
          ;; Generate complete PDF document with emoji support
          (document->pdf pages-data validated-attributes opts))))))

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
   (let [[_top-margin _ _bottom-margin _] margins]
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
                :rect (if (and (:y attributes) (:height attributes))
                        ;; For rectangles, y is top-left in web coords, but bottom-left in PDF
                        ;; PDF y = page-height - web-y - height
                        (assoc attributes :y (- (web-to-pdf-y (:y attributes) page-height margins) (:height attributes)))
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
  "Processes page content into a content stream with coordinate transformation and emoji support.

  Args:
    page-attributes: Page-specific attributes (width, height, margins)
    page-content: Vector of hiccup elements representing page content
    document-defaults: Document defaults for inheritance
    options: Optional map with emoji configuration

  Returns:
    Map with page data: {:width :height :margins :content-stream :metadata :emoji-used}"
  [page-attributes page-content document-defaults & [options]]
  (let [opts (merge {:enable-emoji-images false} options)
        ;; Validate and merge page attributes with document defaults
        validated-page-attrs (v/validate-page-attributes page-attributes document-defaults)
        page-width (:width validated-page-attrs)
        page-height (:height validated-page-attrs)
        page-margins (:margins validated-page-attrs [0 0 0 0])

        ;; Extract emoji from this page's content if emoji images enabled
        page-emoji (if (:enable-emoji-images opts)
                     (collect-page-images page-content)
                     #{})

        ;; Transform coordinates from web-style to PDF-style
        transformed-content (transform-coordinates-for-page page-content page-height page-margins)

        ;; Generate content stream using actual PDF operators with emoji support
        content-stream (if (:enable-emoji-images opts)
                         ;; Use emoji-enabled processing
                         (let [;; Create image cache and XObject references for this page's emoji
                               _image-cache (:image-cache opts)
                               xobject-refs {}
                               ;; Generate PDF operators with emoji image support
                               element-ops (map (fn [element]
                                                  ;; Dynamically call hiccup->pdf-ops from core namespace
                                                  ((resolve 'dev.jaide.hiccup-pdf.core/hiccup->pdf-ops)
                                                   element (merge opts {:xobject-refs xobject-refs})))
                                                transformed-content)]
                           (str/join "\n" element-ops))
                         ;; Use standard processing without emoji images
                         (let [element-ops (map (fn [element]
                                                  ;; Dynamically call hiccup->pdf-ops from core namespace
                                                  ((resolve 'dev.jaide.hiccup-pdf.core/hiccup->pdf-ops) element opts))
                                                transformed-content)]
                           (str/join "\n" element-ops)))]

    {:width page-width
     :height page-height
     :margins page-margins
     :content-stream content-stream
     :emoji-used page-emoji  ; Track emoji used on this page
     :metadata {:element-count (count page-content)
                :has-transforms (boolean (some #(and (vector? %) (= :g (first %)) (:transforms (second %))) page-content))
                :coordinate-system "pdf"
                :emoji-count (count page-emoji)}}))

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

;; Emoji Image Resource Management

(defn collect-page-images
  "Scans page content for emoji images and returns collection.
  
  Recursively scans hiccup page content for :text elements and extracts
  unique emoji characters that would need image resources.
  
  Args:
    page-content: Vector of hiccup elements
    
  Returns:
    Set of emoji character strings found in text content"
  [page-content]
  (letfn [(extract-emoji [element]
            (if-not (vector? element)
              #{}
              (let [[tag _attributes & children] element]
                (cond-> #{}
                  ;; Extract emoji from text element content
                  (= tag :text)
                  (into #{})
                  
                  ;; Recursively process children
                  (seq children)
                  (into (mapcat extract-emoji children))))))]
    (into #{} (mapcat extract-emoji page-content))))

(defn generate-image-resources
  "Creates PDF resource dictionary entries for images.
  
  Generates the XObject section of a PDF resource dictionary
  with references to emoji image objects.
  
  Args:
    image-refs: Map from emoji characters to object numbers
                e.g., {\"💡\" 10, \"🎯\" 11}
    xobject-names: Map from emoji characters to XObject names  
                   e.g., {\"💡\" \"Em1\", \"🎯\" \"Em2\"}
    
  Returns:
    String with PDF XObject resource dictionary entries"
  [image-refs xobject-names]
  (if (empty? image-refs)
    ""
    (let [entries (map (fn [[emoji-char obj-num]]
                         (let [xobj-name (get xobject-names emoji-char)]
                           (when xobj-name
                             (str "/" xobj-name " " obj-num " 0 R"))))
                       image-refs)
          valid-entries (filter some? entries)]
      (if (empty? valid-entries)
        ""
        (str "/XObject <<\n"
             (str/join "\n" valid-entries) "\n"
             ">>")))))

(defn update-page-resources
  "Merges image resources with existing page resources (fonts, etc.).
  
  Updates the PDF page resource dictionary to include both font
  and XObject (image) resources.
  
  Args:
    font-dict: String containing font resource dictionary
    image-dict: String containing XObject resource dictionary
    
  Returns:
    String with complete resource dictionary or empty string if no resources"
  [font-dict image-dict]
  (let [has-fonts (not (str/blank? font-dict))
        has-images (not (str/blank? image-dict))]
    (cond
      (and has-fonts has-images)
      (str "/Resources <<\n" font-dict "\n" image-dict "\n>>")
      
      has-fonts
      (str "/Resources <<\n" font-dict "\n>>")
      
      has-images  
      (str "/Resources <<\n" image-dict "\n>>")
      
      :else
      "")))

(defn embed-images-in-document
  "Integrates image objects into complete PDF document.
  
  Processes emoji images for a document, generates XObjects,
  and updates object numbering and references.
  
  Args:
    unique-emoji: Set of unique emoji characters in document
    image-cache: Image cache atom for loading emoji images
    starting-object-number: Starting object number for image objects
    options: Map with emoji configuration options
    
  Returns:
    Map with image processing results:
    {:image-objects [pdf-object-strings]
     :image-refs {emoji-char object-number}
     :xobject-names {emoji-char xobject-name}
     :next-object-number number
     :success boolean
     :errors [error-strings]}"
  [unique-emoji image-cache starting-object-number & [options]]
  (let [opts (merge {:fallback-strategy :hex-string :logging? false} options)]
    (try
      (if (empty? unique-emoji)
        ;; No emoji to process
        {:image-objects []
         :image-refs {}
         :xobject-names {}
         :next-object-number starting-object-number
         :success true
         :errors []}
        
        ;; Process each unique emoji
        (let [emoji-list (vec unique-emoji)
              xobject-names (into {} (map-indexed (fn [idx emoji-char]
                                                    [emoji-char (str "Em" (+ idx 1))])
                                                  emoji-list))
              
              ;; Load images and generate XObjects
              results (loop [remaining-emoji emoji-list
                           current-obj-num starting-object-number
                           image-objects []
                           image-refs {}
                           errors []]
                        
                        (if (empty? remaining-emoji)
                          {:image-objects image-objects
                           :image-refs image-refs  
                           :next-object-number current-obj-num
                           :errors errors}
                          
                          (let [emoji-char (first remaining-emoji)
                                rest-emoji (rest remaining-emoji)
                                xobj-name (get xobject-names emoji-char)
                                
                                ;; Attempt to load image
                                image-result (images/emoji-image-with-fallback 
                                              image-cache emoji-char opts)]
                            
                            (if (and (:success image-result) (= :image (:type image-result)))
                              ;; Successfully loaded image - generate XObject
                              (let [xobj-result (images/generate-image-xobject image-result xobj-name current-obj-num)]
                                (if (:success xobj-result)
                                  ;; XObject generated successfully
                                  (recur rest-emoji
                                         (inc current-obj-num)
                                         (conj image-objects (:pdf-object xobj-result))
                                         (assoc image-refs emoji-char current-obj-num)
                                         errors)
                                  ;; XObject generation failed
                                  (recur rest-emoji
                                         current-obj-num
                                         image-objects
                                         image-refs
                                         (conj errors (str "XObject generation failed for " emoji-char ": " (:error xobj-result))))))
                              
                              ;; Image loading failed or fallback used - skip XObject generation
                              (recur rest-emoji
                                     current-obj-num
                                     image-objects
                                     image-refs
                                     (if (:success image-result)
                                       errors  ; Fallback used, not an error
                                       (conj errors (str "Image loading failed for " emoji-char ": " (:error image-result)))))))))
              
              final-result (assoc results :xobject-names xobject-names :success true)]
          final-result))
      
      (catch js/Error e
        {:image-objects []
         :image-refs {}
         :xobject-names {}
         :next-object-number starting-object-number
         :success false
         :errors [(str "Image embedding failed: " (.-message e))]}))))

(defn scan-document-for-emoji
  "Scans entire document for unique emoji across all pages.
  
  Recursively processes document structure to find all unique
  emoji characters that will need image resources.
  
  Args:
    document-vector: Hiccup document vector
    
  Returns:
    Set of unique emoji character strings"
  [document-vector]
  (if-not (and (vector? document-vector) (= :document (first document-vector)))
    #{}
    (let [[_ _attributes & pages] document-vector]
      ;; Extract emoji from all pages
      (into #{} (mapcat (fn [page]
                          (if (and (vector? page) (= :page (first page)))
                            (let [[_ _page-attrs & page-content] page]
                              (collect-page-images page-content))
                            #{}))
                        pages)))))

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
  (let [stream-content (str content-stream "\n")  ; Include the newline in the length calculation
        stream-length (count stream-content)]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Length " stream-length "\n"
         ">>\n"
         "stream\n"
         stream-content
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
    image-refs: Optional map of emoji characters to object references for images
    xobject-names: Optional map of emoji characters to XObject names
    
  Returns:
    String containing PDF page object"
  [object-number page-data parent-pages-ref content-stream-ref font-refs & [image-refs xobject-names]]
  (let [{:keys [width height margins]} page-data
        [_top _right bottom left] (or margins [0 0 0 0])
        ;; MediaBox defines the page boundaries
        media-box (str "[" left " " bottom " " width " " height "]")
        ;; Create font resource dictionary
        font-dict (if (empty? font-refs)
                    ""
                    (str "/Font <<\n"
                         (str/join "\n" (map (fn [[font-name ref]]
                                               (str "/" font-name " " ref " 0 R"))
                                             font-refs))
                         "\n>>"))
        ;; Create image resource dictionary
        image-dict (generate-image-resources (or image-refs {}) (or xobject-names {}))
        ;; Combine resources
        resource-dict (update-page-resources font-dict image-dict)]
    (str object-number " 0 obj\n"
         "<<\n"
         "/Type /Page\n"
         "/Parent " parent-pages-ref " 0 R\n"
         "/MediaBox " media-box "\n"
         (when (not (str/blank? resource-dict))
           (str resource-dict "\n"))
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

(defn generate-pdf-header
  "Generates the PDF header.
  
  Returns:
    String containing PDF header"
  []
  "%PDF-1.4")

(defn calculate-byte-offsets
  "Calculates byte offsets for PDF objects for xref table.
  
  Args:
    objects: Vector of PDF object strings
    
  Returns:
    Vector of byte offsets"
  [objects]
  (let [header-length (+ (count (generate-pdf-header)) 1)] ; +1 for newline
    (loop [objects objects
           offsets []
           current-offset header-length]
      (if (empty? objects)
        offsets
        (let [obj (first objects)
              obj-length (+ (count obj) 1)] ; +1 for newline
          (recur (rest objects)
                 (conj offsets current-offset)
                 (+ current-offset obj-length)))))))

(defn generate-xref-table
  "Generates the cross-reference table.
  
  Args:
    object-count: Total number of objects (including object 0)
    byte-offsets: Vector of byte offsets for each object
    
  Returns:
    String containing xref table"
  [object-count byte-offsets]
  (let [;; Object 0 is always free with generation 65535
        obj-0-entry "0000000000 65535 f \n"
        ;; Generate entries for each object
        object-entries (map-indexed
                        (fn [_idx offset]
                          (let [padded-offset (str (apply str (repeat (- 10 (count (str offset))) "0")) offset)]
                            (str padded-offset " 00000 n \n")))
                        byte-offsets)]
    (str "xref\n"
         "0 " object-count "\n"
         obj-0-entry
         (str/join "" object-entries))))

(defn generate-trailer
  "Generates the PDF trailer.
  
  Args:
    object-count: Total number of objects
    catalog-ref: Reference to catalog object
    info-ref: Reference to info object (optional)
    xref-offset: Byte offset of xref table
    
  Returns:
    String containing trailer"
  [object-count catalog-ref info-ref xref-offset]
  (str "trailer\n"
       "<<\n"
       "/Size " object-count "\n"
       "/Root " catalog-ref " 0 R\n"
       (when info-ref (str "/Info " info-ref " 0 R\n"))
       ">>\n"
       "startxref\n"
       xref-offset "\n"
       "%%EOF"))

(defn document->pdf
  "Generates a complete PDF document from processed page data with emoji image support.
  
  Args:
    pages-data: Vector of page data maps from page->content-stream
    document-attributes: Document metadata
    options: Optional map with configuration including emoji support
    
  Returns:
    Complete PDF document as string"
  [pages-data document-attributes & [options]]
  (let [opts (merge {:enable-emoji-images false} options)
        ;; Extract all fonts from content streams by parsing the content
        extract-fonts-from-content-stream (fn [content-stream]
                                            (let [font-matches (re-seq #"/(\w+)\s+\d+\s+Tf" content-stream)]
                                              (set (map second font-matches))))
        all-fonts (into #{} (mapcat #(extract-fonts-from-content-stream (:content-stream %)) pages-data))
        
        ;; Extract unique emoji from all pages if emoji images enabled
        unique-emoji (if (:enable-emoji-images opts)
                       (into #{} (mapcat :emoji-used pages-data))
                       #{})
        
        ;; Generate emoji image objects if needed
        image-result (if (and (:enable-emoji-images opts) (seq unique-emoji))
                       (let [image-cache (:image-cache opts)
                             starting-obj-num (+ 2 (count all-fonts))]
                         (embed-images-in-document unique-emoji image-cache starting-obj-num opts))
                       {:image-objects []
                        :image-refs {}
                        :xobject-names {}
                        :next-object-number (+ 2 (count all-fonts))
                        :success true
                        :errors []})
        
        ;; Object numbering strategy with emoji images:
        ;; 1: Catalog
        ;; 2+: Font objects (one per unique font)
        ;; N+: Image objects (emoji XObjects, if any)
        ;; M+: Content stream objects (one per page)  
        ;; P+: Page objects (one per page)
        ;; Last: Pages collection object
        ;; Last+1: Info object (if metadata provided)
        
        font-list (vec all-fonts)
        font-count (count font-list)
        _image-count (count (:image-objects image-result))
        page-count (count pages-data)
        
        ;; Calculate object numbers
        catalog-ref 1
        first-font-ref 2
        _first-image-ref (+ first-font-ref font-count)
        first-content-ref (:next-object-number image-result)
        first-page-ref (+ first-content-ref page-count)
        pages-collection-ref (+ first-page-ref page-count)
        info-ref (if (seq document-attributes) (+ pages-collection-ref 1) nil)
        total-objects (if info-ref (+ pages-collection-ref 1) pages-collection-ref)
        
        ;; Generate font objects and create font reference map
        font-refs (into {} (map-indexed
                           (fn [idx font-name]
                             [font-name (+ first-font-ref idx)])
                           font-list))
        font-objects (map-indexed
                      (fn [idx font-name]
                        (generate-font-resource-object (+ first-font-ref idx) font-name))
                      font-list)
        
        ;; Get image objects from embedding process
        image-objects (:image-objects image-result)
        image-refs (:image-refs image-result)
        xobject-names (:xobject-names image-result)
        
        ;; Generate content stream objects
        content-objects (map-indexed
                         (fn [idx page-data]
                           (generate-content-stream-object 
                            (+ first-content-ref idx)
                            (:content-stream page-data)))
                         pages-data)
        
        ;; Generate page objects with image resources
        page-objects (map-indexed
                      (fn [idx page-data]
                        (let [page-emoji (:emoji-used page-data #{})
                              page-image-refs (select-keys image-refs page-emoji)
                              page-xobject-names (select-keys xobject-names page-emoji)]
                          (generate-page-object
                           (+ first-page-ref idx)
                           page-data
                           pages-collection-ref
                           (+ first-content-ref idx)
                           font-refs
                           page-image-refs
                           page-xobject-names)))
                      pages-data)
        
        ;; Generate pages collection object
        page-refs (map #(+ first-page-ref %) (range page-count))
        pages-object (generate-pages-object pages-collection-ref page-refs)
        
        ;; Generate catalog object
        catalog-object (generate-catalog-object catalog-ref pages-collection-ref)
        
        ;; Generate info object if metadata provided
        info-object (when info-ref
                      (generate-info-object info-ref document-attributes))
        
        ;; Assemble all objects in order
        all-objects (concat [catalog-object]
                           font-objects
                           image-objects  ; Include emoji image objects
                           content-objects
                           page-objects
                           [pages-object]
                           (when info-object [info-object]))
        
        ;; Calculate byte offsets
        byte-offsets (calculate-byte-offsets all-objects)
        
        ;; Generate PDF components
        header (generate-pdf-header)
        objects-section (str/join "\n" all-objects)
        xref-offset (+ (count header) 1 (count objects-section) 1) ; +1 for newlines
        xref-table (generate-xref-table (+ total-objects 1) byte-offsets) ; +1 for object 0
        trailer (generate-trailer (+ total-objects 1) catalog-ref info-ref xref-offset)]
    
    ;; Assemble final PDF
    (str header "\n"
         objects-section "\n"
         xref-table
         trailer)))

