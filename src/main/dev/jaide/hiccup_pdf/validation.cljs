(ns dev.jaide.hiccup-pdf.validation
  "Validation namespace for hiccup-pdf library using valhalla."
  (:require [dev.jaide.valhalla.core :as v]
            [clojure.string :as str]))

(defn validation-error
  "Creates a descriptive validation error with context.
  
  Args:
    element-type: The type of element being validated (e.g., :rect, :circle)
    attribute: The attribute name that failed validation (optional)
    message: The specific error message
    value: The invalid value (optional)
    
  Returns:
    Error object with detailed context"
  ([element-type message]
   (validation-error element-type nil message nil))
  ([element-type attribute message]
   (validation-error element-type attribute message nil))
  ([element-type attribute message value]
   (let [context (if attribute
                   (str "in " (name element-type) " element, attribute '" (name attribute) "'")
                   (str "in " (name element-type) " element"))
         full-message (if value
                        (str message " " context ". Got: " (pr-str value))
                        (str message " " context))]
     (js/Error. full-message))))

(defn wrap-validation
  "Wraps a validation function to provide better error context.
  
  Args:
    element-type: The type of element being validated
    validator-fn: The validation function to wrap
    
  Returns:
    Enhanced validator function with error context"
  [element-type validator-fn]
  (fn [data]
    (try
      (validator-fn data)
      (catch js/Error e
        (let [message (str "Validation failed " (.-message e))]
          (throw (validation-error element-type message)))))))

(defn validate-hiccup-structure
  "Validates that the input has basic hiccup structure.
  
  Args:
    hiccup: The hiccup vector to validate
    
  Returns:
    The validated hiccup vector if valid
    
  Throws:
    Validation error if hiccup structure is invalid"
  [hiccup]
  (let [schema (v/chain 
                 (v/assert vector?)
                 (v/assert #(>= (count %) 2))
                 (v/assert #(keyword? (first %)))
                 (v/assert #(map? (second %))))]
    (v/parse schema hiccup)))

(defn validate-element-type
  "Validates that the element type is supported.
  
  Args:
    element-type: The keyword representing the element type
    
  Returns:
    The validated element type if valid
    
  Throws:
    Validation error if element type is not supported"
  [element-type]
  (let [supported-types #{:rect :circle :line :text :path :g :document :page}
        schema (v/enum supported-types)]
    (v/parse schema element-type)))

(defn validate-attributes
  "Validates that attributes is a map.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if attributes is not a map"
  [attributes]
  (let [schema (v/assert map?)]
    (v/parse schema attributes)))

(defn validate-color
  "Validates that a color value is a valid hex color string.
  
  Args:
    color: The color string to validate (hex format like #ff0000)
    
  Returns:
    The validated color string if valid
    
  Throws:
    Validation error if color is invalid"
  [color]
  (let [schema (v/chain
                 (v/string)
                 (v/regex "^#[0-9a-fA-F]{6}$"))]
    (v/parse schema color)))

(defn validate-rect-attributes
  "Validates that attributes contains required rectangle attributes.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if required attributes are missing or invalid"
  [attributes]
  (let [required-schema (v/record {:x (v/number)
                                   :y (v/number)
                                   :width (v/number)
                                   :height (v/number)})
        color-validator (v/chain
                          (v/string)
                          (v/regex "^#[0-9a-fA-F]{6}$"))
        optional-schema (v/record {:fill (v/nilable color-validator)
                                   :stroke (v/nilable color-validator)
                                   :stroke-width (v/nilable (v/number))})]
    (v/parse required-schema attributes)
    (v/parse optional-schema attributes)
    attributes))

(defn validate-line-attributes
  "Validates that attributes contains required line attributes.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if required attributes are missing or invalid"
  [attributes]
  (let [required-schema (v/record {:x1 (v/number)
                                   :y1 (v/number)
                                   :x2 (v/number)
                                   :y2 (v/number)})
        color-validator (v/chain
                          (v/string)
                          (v/regex "^#[0-9a-fA-F]{6}$"))
        optional-schema (v/record {:stroke (v/nilable color-validator)
                                   :stroke-width (v/nilable (v/number))})]
    (v/parse required-schema attributes)
    (v/parse optional-schema attributes)
    attributes))

(defn validate-circle-attributes
  "Validates that attributes contains required circle attributes.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if required attributes are missing or invalid"
  [attributes]
  (let [required-schema (v/record {:cx (v/number)
                                   :cy (v/number)
                                   :r (v/chain (v/number) (v/assert #(>= % 0)))})
        color-validator (v/chain
                          (v/string)
                          (v/regex "^#[0-9a-fA-F]{6}$"))
        optional-schema (v/record {:fill (v/nilable color-validator)
                                   :stroke (v/nilable color-validator)
                                   :stroke-width (v/nilable (v/number))})]
    (v/parse required-schema attributes)
    (v/parse optional-schema attributes)
    attributes))

(defn validate-path-attributes
  "Validates that attributes contains required path attributes.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if required attributes are missing or invalid"
  [attributes]
  (let [required-schema (v/record {:d (v/chain 
                                        (v/string)
                                        (v/assert #(not (str/blank? %))))})
        color-validator (v/chain
                          (v/string)
                          (v/regex "^#[0-9a-fA-F]{6}$"))
        optional-schema (v/record {:fill (v/nilable color-validator)
                                   :stroke (v/nilable color-validator)
                                   :stroke-width (v/nilable (v/number))})]
    (v/parse required-schema attributes)
    (v/parse optional-schema attributes)
    attributes))

(defn validate-text-attributes
  "Validates that attributes contains required text attributes.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if required attributes are missing or invalid"
  [attributes]
  (let [required-schema (v/record {:x (v/number)
                                   :y (v/number)
                                   :font (v/chain 
                                           (v/string)
                                           (v/assert #(not (str/blank? %))))
                                   :size (v/chain (v/number) (v/assert #(> % 0)))})
        color-validator (v/chain
                          (v/string)
                          (v/regex "^#[0-9a-fA-F]{6}$"))
        optional-schema (v/record {:fill (v/nilable color-validator)})]
    (v/parse required-schema attributes)
    (v/parse optional-schema attributes)
    attributes))

(defn validate-transform
  "Validates a single transform operation.
  
  Args:
    transform: Vector containing transform operation [type args]
    
  Returns:
    The validated transform if valid
    
  Throws:
    Validation error if transform is invalid"
  [transform]
  (let [schema (v/chain
                 (v/assert vector?)
                 (v/assert #(= 2 (count %)))
                 (v/assert #(keyword? (first %))))]
    (v/parse schema transform)
    (let [[type args] transform]
      (case type
        :translate (do
                     (v/parse (v/chain (v/assert vector?) (v/assert #(= 2 (count %))) 
                                       (v/assert #(every? number? %))) args)
                     transform)
        :rotate (do
                  (v/parse (v/number) args)
                  transform)
        :scale (do
                 (v/parse (v/chain (v/assert vector?) (v/assert #(= 2 (count %)))
                                   (v/assert #(every? number? %))) args)
                 transform)
        (throw (js/Error. (str "Unsupported transform type: " type)))))))

(defn validate-transforms
  "Validates a vector of transform operations.
  
  Args:
    transforms: Vector of transform operations
    
  Returns:
    The validated transforms vector if valid
    
  Throws:
    Validation error if transforms are invalid"
  [transforms]
  (let [schema (v/chain (v/assert vector?))]
    (v/parse schema transforms)
    (mapv validate-transform transforms)))

(defn validate-group-attributes
  "Validates that attributes contains valid group attributes.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map if valid
    
  Throws:
    Validation error if attributes are invalid"
  [attributes]
  (let [;; Group elements can have optional transform attributes
        schema (v/record {:transforms (v/nilable (v/chain (v/assert vector?) 
                                                           (v/assert #(every? vector? %))))})]
    (v/parse schema attributes)
    ;; Validate transforms if present
    (when (:transforms attributes)
      (validate-transforms (:transforms attributes)))
    attributes))

(defn validate-document-attributes
  "Validates that attributes contains valid document attributes with defaults.
  
  Args:
    attributes: The attributes map to validate
    
  Returns:
    The validated attributes map with defaults applied
    
  Throws:
    Validation error if attributes are invalid"
  [attributes]
  (let [;; Define default values
        defaults {:width 612              ; Letter width default
                  :height 792             ; Letter height default
                  :margins [0 0 0 0]      ; No margins default
                  :creator "hiccup-pdf"   ; Library identifier
                  :producer "hiccup-pdf"} ; Producer identifier
        
        ;; Validation schemas
        string-schema (v/chain (v/string) (v/assert #(not (str/blank? %))))
        positive-number-schema (v/chain (v/number) (v/assert #(pos? %)))
        margins-schema (v/chain 
                         (v/assert vector?)
                         (v/assert #(= 4 (count %)))
                         (v/assert #(every? number? %)))
        
        optional-schema (v/record {:title (v/nilable string-schema)
                                   :author (v/nilable string-schema)
                                   :subject (v/nilable string-schema)
                                   :keywords (v/nilable string-schema)
                                   :creator (v/nilable string-schema)
                                   :producer (v/nilable string-schema)
                                   :width (v/nilable positive-number-schema)
                                   :height (v/nilable positive-number-schema)
                                   :margins (v/nilable margins-schema)})
        
        ;; Merge with defaults
        merged-attributes (merge defaults attributes)]
    
    ;; Validate the merged attributes
    (v/parse optional-schema merged-attributes)
    merged-attributes))

(defn validate-page-attributes
  "Validates that attributes contains valid page attributes with inheritance from document defaults.
  
  Args:
    attributes: The page attributes map to validate
    document-defaults: Document attributes to inherit from (optional)
    
  Returns:
    The validated page attributes map with inheritance applied
    
  Throws:
    Validation error if attributes are invalid"
  ([attributes]
   (validate-page-attributes attributes {}))
  ([attributes document-defaults]
   (let [;; Extract inheritable attributes from document defaults
         inheritable-keys [:width :height :margins]
         inherited-values (select-keys document-defaults inheritable-keys)
         
         ;; Validation schemas
         positive-number-schema (v/chain (v/number) (v/assert #(pos? %)))
         margins-schema (v/chain 
                          (v/assert vector?)
                          (v/assert #(= 4 (count %)))
                          (v/assert #(every? number? %)))
         
         optional-schema (v/record {:width (v/nilable positive-number-schema)
                                    :height (v/nilable positive-number-schema)
                                    :margins (v/nilable margins-schema)})
         
         ;; Merge inheritance with page attributes (page attributes override document)
         merged-attributes (merge inherited-values attributes)]
     
     ;; Validate the merged attributes
     (v/parse optional-schema merged-attributes)
     merged-attributes)))
