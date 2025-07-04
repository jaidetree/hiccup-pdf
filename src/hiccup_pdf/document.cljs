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

