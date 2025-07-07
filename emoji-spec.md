# Emoji Element Specification

## Overview

This specification defines a new `:emoji` hiccup element that renders emoji as PNG images in PDF documents. This replaces the current Unicode text parsing approach with an explicit, reliable element-based system.

## Core Philosophy

**Explicit over Implicit**: Users explicitly declare emoji elements rather than relying on automatic Unicode parsing within text strings. This provides predictable behavior, better performance, and eliminates complex edge cases.

## Step 1: Fundamental Design Decisions ✅

### 1.1 Element Structure
```clojure
[:emoji {:code :lightbulb :size 14 :x 100 :y 200}]
```

**Key Design Choices:**
- `:code` attribute uses shortcode keywords (e.g., `:lightbulb`, `:thumbs_up`)
- Shortcodes map to specific PNG filenames in the emoji directory
- Standard `:x`, `:y`, `:size` attributes for positioning and scaling
- No text content - the shortcode fully defines the emoji

### 1.2 Implementation Layer
```clojure
;; :emoji elements internally become :image elements
[:emoji {:code :lightbulb :size 14 :x 100 :y 200}]
;; ↓ transforms to ↓
[:image {:src "emojis/noto-72/emoji_u1f4a1.png" 
         :width 14 :height 14 :x 100 :y 200}]
```

**Decision**: Transform to `:image` - reuses existing image rendering pipeline once `:image` element is implemented.

---

## Step 2: Shortcode System Design ✅

### 2.1 Shortcode Storage: emojis.edn
```clojure
;; File: emojis/emojis.edn
{;; Basic emoji
 :lightbulb "noto-72/emoji_u1f4a1.png"
 :target "noto-72/emoji_u1f3af.png" 
 :warning "noto-72/emoji_u26a0.png"
 :check_mark "noto-72/emoji_u2705.png"
 :check_mark_button "noto-72/emoji_u2705.png"  ; Alias to same file
 
 ;; Skin tone variants
 :thumbs_up "noto-72/emoji_u1f44d.png"              ; Default (yellow)
 :thumbs_up_tone_1 "noto-72/emoji_u1f44d_1f3fb.png" ; Light skin
 :thumbs_up_tone_2 "noto-72/emoji_u1f44d_1f3fc.png" ; Medium-light skin
 :thumbs_up_tone_3 "noto-72/emoji_u1f44d_1f3fd.png" ; Medium skin
 :thumbs_up_tone_4 "noto-72/emoji_u1f44d_1f3fe.png" ; Medium-dark skin
 :thumbs_up_tone_5 "noto-72/emoji_u1f44d_1f3ff.png" ; Dark skin
 
 ;; Alternative names for same emoji
 :plus_one "noto-72/emoji_u1f44d.png"               ; Alias for thumbs_up
 :like "noto-72/emoji_u1f44d.png"                   ; Another alias
 
 ;; Composite emoji (ZWJ sequences)
 :man_technologist "noto-72/emoji_u1f468_200d_1f4bb.png"
 :woman_technologist "noto-72/emoji_u1f469_200d_1f4bb.png"}
```

### 2.2 Loading Strategy
```clojure
;; Load shortcode mappings at startup or on first use
(def emoji-shortcodes 
  (atom (clojure.edn/read-string (slurp "emojis/emojis.edn"))))
```

**Benefits of .edn approach:**
- Easy to edit and maintain
- Allows multiple shortcodes mapping to same PNG file
- Can be version controlled
- Simple to validate and parse
- Supports comments for documentation

---

## Step 3: Image Element Implementation (Prerequisite) ✅

Since no `:image` element exists in the current API, we must implement it first.

### 3.1 Image Element Design
```clojure
[:image {:src "path/to/image.png" :width 50 :height 50 :x 100 :y 200}]
```

**Required Attributes:**
- `:src` (string) - Relative path from project root
- `:width` (number) - Width in PDF points
- `:height` (number) - Height in PDF points  
- `:x` (number) - X coordinate
- `:y` (number) - Y coordinate

### 3.2 Image Caching Strategy
```clojure
;; Global image cache atom
(def image-cache 
  (atom {:images {}    ; path -> {buffer, width, height, ...}
         :stats {:hits 0 :misses 0 :loads 0}}))

;; Cache structure
{:images {"emojis/noto-72/emoji_u1f4a1.png" 
          {:buffer #<Buffer ...>
           :width 72
           :height 72
           :loaded-at 1234567890}}
 :stats {:hits 15 :misses 3 :loads 3}}
```

### 3.3 Image Loading Pipeline
```clojure
(defn load-image-cached [file-path]
  "Load image from cache or disk, update cache"
  (if-let [cached (get-in @image-cache [:images file-path])]
    (do
      (swap! image-cache update-in [:stats :hits] inc)
      cached)
    (do
      (swap! image-cache update-in [:stats :misses] inc)
      (let [loaded (load-image-from-disk file-path)]
        (swap! image-cache assoc-in [:images file-path] loaded)
        (swap! image-cache update-in [:stats :loads] inc)
        loaded))))
```

---

## Step 4: Validation and Error Handling ✅

### 4.1 Emoji Element Validation
```clojure
(defn validate-emoji-attributes [attrs]
  (let [errors []
        shortcodes @emoji-shortcodes]
    
    ;; Required :code attribute
    (when-not (:code attrs)
      (conj errors ":code attribute is required"))
    
    ;; Valid shortcode exists
    (when (and (:code attrs) (not (contains? shortcodes (:code attrs))))
      (conj errors (str "Unknown emoji code: " (:code attrs) 
                       ". Available codes: " (take 10 (keys shortcodes)) "...")))
    
    ;; Required positioning attributes
    (when-not (and (:x attrs) (number? (:x attrs)))
      (conj errors ":x attribute must be a number"))
    
    (when-not (and (:y attrs) (number? (:y attrs)))
      (conj errors ":y attribute must be a number"))
    
    ;; Required size attribute
    (when-not (and (:size attrs) (number? (:size attrs)) (pos? (:size attrs)))
      (conj errors ":size attribute must be a positive number"))
    
    {:valid? (empty? errors) :errors errors}))
```

### 4.2 Error Strategy: Fail Fast
- **Validation errors**: Throw immediately during element processing
- **File not found**: Throw immediately when PNG file missing
- **No fallback modes**: Keep system simple and predictable

---

## Step 5: Integration with Existing System ✅

### 5.1 Element Dispatcher Integration
```clojure
;; In validation.cljs - Add emoji validation
(defn validate-element [element]
  (case (first element)
    :rect (validate-rect-attributes (second element))
    :circle (validate-circle-attributes (second element))
    :text (validate-text-attributes (second element))
    :image (validate-image-attributes (second element))  ; NEW
    :emoji (validate-emoji-attributes (second element))  ; NEW
    ;; ... other elements
    ))

;; In core.cljs - Add emoji processing  
(defn element->pdf-ops [element]
  (case (first element)
    :rect (rect->pdf-ops element)
    :circle (circle->pdf-ops element)
    :text (text->pdf-ops element)
    :image (image->pdf-ops element)  ; NEW
    :emoji (emoji->pdf-ops element)  ; NEW
    ;; ... other elements
    ))
```

### 5.2 Emoji to Image Transformation
```clojure
(defn emoji->pdf-ops [element]
  "Transform emoji element to image element and delegate"
  (let [[_ attrs] element
        shortcode (:code attrs)
        shortcodes @emoji-shortcodes
        relative-path (get shortcodes shortcode)
        full-path (str "emojis/" relative-path)
        image-attrs (-> attrs
                       (dissoc :code)
                       (assoc :src full-path)
                       (assoc :width (:size attrs))
                       (assoc :height (:size attrs)))]
    ;; Delegate to image element processing
    (image->pdf-ops [:image image-attrs])))
```

---

## Step 6: Image Element Implementation Details ✅

### 6.1 Image Validation
```clojure
(defn validate-image-attributes [attrs]
  (let [errors []]
    ;; Required attributes
    (when-not (and (:src attrs) (string? (:src attrs)) (not-empty (:src attrs)))
      (conj errors ":src must be a non-empty string"))
    
    (when-not (and (:width attrs) (number? (:width attrs)) (pos? (:width attrs)))
      (conj errors ":width must be a positive number"))
    
    (when-not (and (:height attrs) (number? (:height attrs)) (pos? (:height attrs)))
      (conj errors ":height must be a positive number"))
    
    (when-not (and (:x attrs) (number? (:x attrs)))
      (conj errors ":x must be a number"))
    
    (when-not (and (:y attrs) (number? (:y attrs)))
      (conj errors ":y must be a number"))
    
    {:valid? (empty? errors) :errors errors}))
```

### 6.2 Image PDF Operator Generation
```clojure
(defn image->pdf-ops [element]
  "Generate PDF operators for image element"
  (let [[_ attrs] element
        {:keys [src width height x y]} attrs
        
        ;; Load image from cache or disk
        image-data (load-image-cached src)
        
        ;; Generate PDF XObject (simplified - actual implementation more complex)
        xobject-ref (str "Im" (hash src))
        scale-x (/ width (:width image-data))
        scale-y (/ height (:height image-data))]
    
    ;; PDF operators for drawing image
    (str "q\n"                                    ; Save graphics state
         scale-x " 0 0 " scale-y " " x " " y " cm\n" ; Transform matrix
         "/" xobject-ref " Do\n"                  ; Draw XObject
         "Q\n")))                                 ; Restore graphics state
```

### 6.3 Image File Loading
```clojure
(defn load-image-from-disk [file-path]
  "Load PNG file and extract dimensions"
  (try
    (let [buffer (js/require "fs").readFileSync file-path)]
      ;; Extract PNG dimensions from header (simplified)
      {:buffer buffer
       :width 72   ; Assume 72x72 for Noto emoji
       :height 72
       :format "PNG"})
    (catch js/Error e
      (throw (js/Error. (str "Failed to load image: " file-path " - " (.-message e)))))))
```

---

## Step 7: Implementation Plan ✅

### Phase 1: Foundation (Priority 1)
1. **Implement `:image` element**
   - Add validation for image attributes
   - Add image->pdf-ops function
   - Add image caching system
   - Add basic PNG loading

2. **Create emoji shortcode system**
   - Create emojis/emojis.edn with ~50 common emoji
   - Add shortcode loading functionality
   - Add emoji shortcode validation

### Phase 2: Emoji Element (Priority 1)  
3. **Implement `:emoji` element**
   - Add validation for emoji attributes
   - Add emoji->pdf-ops transformation function
   - Integrate with element dispatcher
   - Add comprehensive error handling

### Phase 3: Testing (Priority 1)
4. **Create test suite**
   - Unit tests for all functions
   - Integration tests with real PDF generation
   - Error handling tests
   - Performance tests

### Phase 4: Migration (Priority 2)
5. **Remove text parsing system**
   - Remove all Unicode parsing code
   - Remove text-processing namespace  
   - Remove emoji detection from text elements
   - Update examples to use new syntax

---

## Step 8: File Organization ✅

### 8.1 New Files Required
```
src/main/dev/jaide/hiccup_pdf/
├── images.cljs                    # NEW - Image element implementation
└── emoji_shortcodes.cljs          # NEW - Emoji shortcode management

emojis/
├── emojis.edn                     # NEW - Shortcode mappings
└── noto-72/                       # EXISTING - PNG files
    ├── emoji_u1f4a1.png
    └── ...

src/test/dev/jaide/hiccup_pdf/
├── images_test.cljs               # NEW - Image element tests  
└── emoji_test.cljs                # UPDATED - Remove text parsing tests
```

### 8.2 Modified Files
```
src/main/dev/jaide/hiccup_pdf/
├── validation.cljs                # ADD image/emoji validation
└── core.cljs                      # ADD image/emoji to dispatcher

src/test/dev/jaide/hiccup_pdf/
├── core_test.cljs                 # UPDATE examples to use :emoji elements
└── text_processing_test.cljs      # REMOVE (delete entire file)
```

### 8.3 Removed Files
```
src/main/dev/jaide/hiccup_pdf/
├── text_processing.cljs           # DELETE - No longer needed
├── emoji.cljs                     # DELETE - Replace with simpler version
└── images.cljs                    # DELETE - Replace with new implementation
```

---

## Step 9: Performance Requirements ✅

### 9.1 Performance Targets
- **Image Loading**: < 5ms per emoji (cached)
- **Memory Usage**: < 100MB for 200 cached emoji images  
- **Cache Hit Rate**: > 90% for typical documents
- **Element Processing**: < 1ms per emoji element
- **Startup Time**: < 50ms to load emojis.edn

### 9.2 Optimization Strategies
```clojure
;; Lazy loading of shortcode mappings
(defonce emoji-shortcodes
  (delay (clojure.edn/read-string (slurp "emojis/emojis.edn"))))

;; LRU cache with size limits
(defn evict-oldest-images [cache max-size]
  "Remove oldest images when cache exceeds max-size")

;; Pre-loading common emoji
(def common-emoji #{:thumbs_up :check_mark :warning :lightbulb})
(defn preload-common-emoji []
  "Pre-load frequently used emoji on startup")
```

---

## Step 10: Initial Emoji Set (50 Common Emoji) ✅

### 10.1 Curated Emoji List
```clojure
;; emojis/emojis.edn - Initial 50 emoji
{;; Reactions & Gestures
 :thumbs_up "noto-72/emoji_u1f44d.png"
 :thumbs_down "noto-72/emoji_u1f44e.png" 
 :clapping_hands "noto-72/emoji_u1f44f.png"
 :ok_hand "noto-72/emoji_u1f44c.png"
 :waving_hand "noto-72/emoji_u1f44b.png"
 
 ;; Faces & Emotions
 :grinning_face "noto-72/emoji_u1f600.png"
 :smiling_face "noto-72/emoji_u263a.png" 
 :winking_face "noto-72/emoji_u1f609.png"
 :thinking_face "noto-72/emoji_u1f914.png"
 :confused_face "noto-72/emoji_u1f615.png"
 
 ;; Status & Symbols  
 :check_mark "noto-72/emoji_u2705.png"
 :cross_mark "noto-72/emoji_u274c.png"
 :warning "noto-72/emoji_u26a0.png"
 :information "noto-72/emoji_u2139.png"
 :question_mark "noto-72/emoji_u2753.png"
 
 ;; Objects & Tools
 :lightbulb "noto-72/emoji_u1f4a1.png"
 :target "noto-72/emoji_u1f3af.png"
 :gear "noto-72/emoji_u2699.png"
 :hammer "noto-72/emoji_u1f528.png"
 :wrench "noto-72/emoji_u1f527.png"
 
 ;; Technology
 :computer "noto-72/emoji_u1f4bb.png"
 :mobile_phone "noto-72/emoji_u1f4f1.png"
 :robot "noto-72/emoji_u1f916.png"
 :rocket "noto-72/emoji_u1f680.png"
 :satellite "noto-72/emoji_u1f4e1.png"
 
 ;; Business & Money
 :money_bag "noto-72/emoji_u1f4b0.png"
 :chart_increasing "noto-72/emoji_u1f4c8.png"
 :chart_decreasing "noto-72/emoji_u1f4c9.png"
 :briefcase "noto-72/emoji_u1f4bc.png"
 :bank "noto-72/emoji_u1f3e6.png"
 
 ;; Communication
 :envelope "noto-72/emoji_u2709.png"
 :telephone "noto-72/emoji_u260e.png"
 :bell "noto-72/emoji_u1f514.png"
 :loudspeaker "noto-72/emoji_u1f4e2.png"
 :speech_balloon "noto-72/emoji_u1f4ac.png"
 
 ;; Time & Calendar
 :clock "noto-72/emoji_u1f55b.png"
 :calendar "noto-72/emoji_u1f4c5.png"
 :hourglass "noto-72/emoji_u231b.png"
 :stopwatch "noto-72/emoji_u23f1.png"
 :alarm_clock "noto-72/emoji_u23f0.png"
 
 ;; Transportation  
 :car "noto-72/emoji_u1f697.png"
 :airplane "noto-72/emoji_u2708.png"
 :ship "noto-72/emoji_u1f6a2.png"
 :bicycle "noto-72/emoji_u1f6b2.png"
 :bus "noto-72/emoji_u1f68c.png"
 
 ;; Nature
 :sun "noto-72/emoji_u2600.png"
 :cloud "noto-72/emoji_u2601.png"
 :fire "noto-72/emoji_u1f525.png"
 :water_drop "noto-72/emoji_u1f4a7.png"
 :earth "noto-72/emoji_u1f30d.png"}
```

---

## Step 11: Testing Strategy ✅

### 11.1 Unit Test Coverage
```clojure
;; Test emoji element validation
(deftest test-emoji-validation
  (testing "Valid emoji element"
    (is (:valid? (validate-emoji-attributes 
                  {:code :lightbulb :size 14 :x 100 :y 200}))))
  
  (testing "Missing code attribute"
    (is (not (:valid? (validate-emoji-attributes {:size 14 :x 100 :y 200})))))
  
  (testing "Invalid shortcode"
    (is (not (:valid? (validate-emoji-attributes 
                       {:code :nonexistent :size 14 :x 100 :y 200}))))))

;; Test emoji to image transformation
(deftest test-emoji-transformation
  (testing "Emoji transforms to image element"
    (let [result (emoji->pdf-ops [:emoji {:code :lightbulb :size 14 :x 100 :y 200}])]
      (is (string? result))
      (is (clojure.string/includes? result "Do"))))) ; Should contain XObject reference

;; Test image caching
(deftest test-image-caching
  (testing "Image caching works correctly"
    (reset! image-cache {:images {} :stats {:hits 0 :misses 0 :loads 0}})
    (load-image-cached "emojis/noto-72/emoji_u1f4a1.png")
    (is (= 1 (get-in @image-cache [:stats :loads])))
    (load-image-cached "emojis/noto-72/emoji_u1f4a1.png") ; Second load
    (is (= 1 (get-in @image-cache [:stats :hits])))))
```

### 11.2 Integration Tests
```clojure
(deftest test-emoji-integration
  (testing "Complete emoji rendering pipeline"
    (let [doc [:g {}
               [:text {:x 100 :y 200 :font "Arial" :size 14} "Status: "]
               [:emoji {:code :check_mark :size 14 :x 150 :y 200}]]
          result (hiccup->pdf-ops doc)]
      (is (string? result))
      (is (clojure.string/includes? result "BT")) ; Text rendering
      (is (clojure.string/includes? result "Do")) ; Image rendering
      (is (clojure.string/includes? result "q")))))  ; Graphics state management
```

---

## Step 12: Migration and Deployment ✅

### 12.1 Migration Steps
1. **Implement new system** (Phases 1-3 above)
2. **Update all examples** to use `:emoji` elements instead of text parsing
3. **Remove old system** (text-processing, emoji parsing, etc.)
4. **Update documentation** to reflect new API

### 12.2 Breaking Changes Documentation
```markdown
## Breaking Changes in v2.0

### Emoji Handling
- **REMOVED**: Automatic emoji detection in `:text` elements
- **ADDED**: Explicit `:emoji` element with shortcode system
- **MIGRATION**: Replace emoji characters in text with separate `:emoji` elements

### Before (v1.x):
```clojure
[:text {:x 100 :y 200 :font "Arial" :size 14} "Status: ✅ Complete"]
```

### After (v2.x):
```clojure
[:g {}
 [:text {:x 100 :y 200 :font "Arial" :size 14} "Status: "]
 [:emoji {:code :check_mark :size 14 :x 150 :y 200}]
 [:text {:x 164 :y 200 :font "Arial" :size 14} " Complete"]]
```

### 12.3 Documentation Updates Required
- Update README.md with new `:emoji` and `:image` elements
- Update API.md with complete element reference
- Update examples.md with new syntax
- Create migration guide for v1.x users

---

## Success Criteria ✅

### Functional Requirements
- [ ] `:emoji` element renders PNG images correctly in PDF
- [ ] All 50 initial emoji shortcodes work reliably
- [ ] Image caching reduces file I/O for repeated emoji
- [ ] Validation provides clear error messages
- [ ] Integration with existing elements works seamlessly

### Performance Requirements  
- [ ] < 5ms per emoji rendering (cached)
- [ ] < 100MB memory usage for 200 cached images
- [ ] > 90% cache hit rate in typical usage
- [ ] < 50ms startup time for loading emojis.edn

### Quality Requirements
- [ ] 100% test coverage for new functionality
- [ ] Zero regressions in existing element behavior
- [ ] All examples updated to new syntax
- [ ] Complete removal of text parsing complexity

### Documentation Requirements
- [ ] Complete API documentation for `:emoji` and `:image` elements
- [ ] Migration guide for users upgrading from text parsing
- [ ] Updated examples demonstrating new capabilities
- [ ] Clear error message reference guide

---

## Conclusion

This specification provides a complete roadmap for replacing the complex Unicode text parsing system with a simple, reliable, explicit `:emoji` element approach. The new system will be:

1. **More Reliable**: No complex Unicode parsing edge cases
2. **Better Performance**: Efficient image caching and direct file mapping
3. **Easier Maintenance**: Simple .edn configuration and explicit element structure
4. **More Flexible**: Easy to add new emoji and aliases
5. **Predictable**: Users know exactly what will render as images

The implementation can proceed step-by-step with clear success criteria at each phase.