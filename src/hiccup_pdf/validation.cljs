(ns hiccup-pdf.validation
  "Validation namespace for hiccup-pdf library using valhalla."
  (:require [dev.jaide.valhalla.core :as v]))

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
  (let [supported-types #{:rect :circle :line :text :path :g}
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
  "Validates that a color value is a valid color string.
  
  Args:
    color: The color string to validate
    
  Returns:
    The validated color string if valid
    
  Throws:
    Validation error if color is invalid"
  [color]
  (let [valid-color-names #{"red" "green" "blue" "black" "white" "yellow" "cyan" "magenta"}
        schema (v/chain
                 (v/string)
                 (v/assert #(or (some? (re-find #"^#[0-9a-fA-F]{6}$" %))
                                (contains? valid-color-names %))))]
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
        valid-color-names #{"red" "green" "blue" "black" "white" "yellow" "cyan" "magenta"}
        color-validator (v/chain
                          (v/string)
                          (v/assert #(or (some? (re-find #"^#[0-9a-fA-F]{6}$" %))
                                         (contains? valid-color-names %))))
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
        valid-color-names #{"red" "green" "blue" "black" "white" "yellow" "cyan" "magenta"}
        color-validator (v/chain
                          (v/string)
                          (v/assert #(or (some? (re-find #"^#[0-9a-fA-F]{6}$" %))
                                         (contains? valid-color-names %))))
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
        valid-color-names #{"red" "green" "blue" "black" "white" "yellow" "cyan" "magenta"}
        color-validator (v/chain
                          (v/string)
                          (v/assert #(or (some? (re-find #"^#[0-9a-fA-F]{6}$" %))
                                         (contains? valid-color-names %))))
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
                                        (v/assert #(pos? (count %))))})
        valid-color-names #{"red" "green" "blue" "black" "white" "yellow" "cyan" "magenta"}
        color-validator (v/chain
                          (v/string)
                          (v/assert #(or (some? (re-find #"^#[0-9a-fA-F]{6}$" %))
                                         (contains? valid-color-names %))))
        optional-schema (v/record {:fill (v/nilable color-validator)
                                   :stroke (v/nilable color-validator)
                                   :stroke-width (v/nilable (v/number))})]
    (v/parse required-schema attributes)
    (v/parse optional-schema attributes)
    attributes))
