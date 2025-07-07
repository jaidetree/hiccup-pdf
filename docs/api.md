# Hiccup-PDF API Reference

A ClojureScript library for converting hiccup vectors into PDF operators for vector graphics.

## Installation

Add to your `nbb.edn`:

```clojure
{:deps {hiccup-pdf {:local/root "."}}}
```

## See Also

- **[Document Generation Guide](document-guide.md)** - Comprehensive guide for PDF document creation with examples and best practices
- **[Examples](examples.md)** - Real-world examples including business documents and technical documentation

## Quick Start

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

;; Simple rectangle
(hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}])
;; => "1 0 0 rg\n10 20 100 50 re\nf"

;; Circle with stroke
(hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "#0000ff" :stroke-width 2}])

;; Emoji elements (requires image cache)
(require '[hiccup-pdf.images :as images])
(let [cache (images/create-image-cache)]
  (hiccup->pdf-ops [:emoji {:code :smile :size 24 :x 100 :y 200}] {:image-cache cache}))

;; Complex document with emoji
(hiccup->pdf-ops [:g {}
                  [:rect {:x 0 :y 0 :width 200 :height 100 :fill "#f0f0f0"}]
                  [:emoji {:code :star :size 20 :x 10 :y 15}]
                  [:text {:x 40 :y 25 :font "Arial" :size 16} "Premium Service"]]
                 {:image-cache cache})
```

## API Functions

### `hiccup->pdf-ops`

**Signature:** `(hiccup->pdf-ops hiccup-vector)` `(hiccup->pdf-ops hiccup-vector options)`

Transforms hiccup vectors into PDF vector primitives represented as raw PDF operators.

**Parameters:**
- `hiccup-vector` - A hiccup vector representing PDF primitives
- `options` - Optional hash-map parameter (reserved for future use)

**Returns:** String of PDF operators ready for PDF content streams

**Throws:** `ValidationError` if hiccup structure or element attributes are invalid

### `hiccup->pdf-document`

**Signature:** `(hiccup->pdf-document hiccup-document)`

Generates complete PDF documents with pages from hiccup structure.

**Parameters:**
- `hiccup-document` - A hiccup vector with `:document` root element containing `:page` elements

**Returns:** Complete PDF document as string ready for writing to file

**Throws:** `ValidationError` if document structure or attributes are invalid

**Example:**
```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-document]])

;; Simple document
(hiccup->pdf-document
  [:document {:title "Business Report"}
   [:page {}
    [:text {:x 100 :y 100 :font "Arial" :size 20} "Hello World!"]]])

;; Multi-page document with different page sizes
(hiccup->pdf-document
  [:document {:title "Mixed Format" :width 612 :height 792}
   [:page {}  ; Letter size
    [:rect {:x 50 :y 50 :width 100 :height 100 :fill "#0000ff"}]]
   [:page {:width 842 :height 595}  ; A4 landscape
    [:circle {:cx 400 :cy 300 :r 50 :fill "#ff0000"}]]])

;; Document with emoji elements
(let [cache (images/create-image-cache)]
  (hiccup->pdf-document
    [:document {:title "Status Report" :author "Team Lead"}
     [:page {}
      [:emoji {:code :star :size 24 :x 50 :y 100}]
      [:text {:x 80 :y 100 :font "Arial" :size 18} "Project Status"]
      [:emoji {:code :thumbsup :size 16 :x 50 :y 140}]
      [:text {:x 75 :y 140 :font "Arial" :size 12} "All tasks completed"]]]
    {:image-cache cache}))
```

## API Comparison

| Function | Purpose | Input | Output | Use Case |
|----------|---------|-------|--------|-----------|
| `hiccup->pdf-ops` | Content stream generation | Primitive elements | PDF operators | Embedding in existing PDFs, custom layouts |
| `hiccup->pdf-document` | Complete document generation | Document structure | Complete PDF | Standalone documents, file output |

## Document Structure

The `hiccup->pdf-document` function requires a specific document structure:

### Document Element (`:document`)

The root element must be `:document` with optional attributes:

**Attributes:**
- `:title` - Document title (string)
- `:author` - Document author (string)  
- `:subject` - Document subject (string)
- `:keywords` - Document keywords (string)
- `:creator` - Creating application (defaults to "hiccup-pdf")
- `:producer` - Producing application (defaults to "hiccup-pdf")
- `:width` - Default page width in points (defaults to 612)
- `:height` - Default page height in points (defaults to 792)
- `:margins` - Default margins `[top right bottom left]` (defaults to `[0 0 0 0]`)

### Page Element (`:page`)

Pages inherit document defaults but can override dimensions and margins:

**Attributes:**
- `:width` - Page width in points (inherits from document)
- `:height` - Page height in points (inherits from document)  
- `:margins` - Page margins `[top right bottom left]` (inherits from document)

**Example:**
```clojure
[:document {:title "My Document" :width 612 :height 792}
 [:page {}]                           ; Uses document defaults
 [:page {:width 842 :height 595}]     ; A4 landscape  
 [:page {:margins [50 50 50 50]}]     ; Custom margins
]
```

## Page Size Reference

Common page sizes in PDF points (1 point = 1/72 inch):

| Format | Width | Height | Orientation |
|--------|-------|--------|--------------|
| Letter | 612 | 792 | Portrait |
| Letter | 792 | 612 | Landscape |
| A4 | 595 | 842 | Portrait |
| A4 | 842 | 595 | Landscape |
| Legal | 612 | 1008 | Portrait |
| Tabloid | 792 | 1224 | Portrait |

## Coordinate System

Both functions use **web-style coordinates** for input:
- Origin (0,0) at top-left
- X increases rightward  
- Y increases downward

The library automatically converts to PDF coordinate system (origin at bottom-left) internally.

## Inheritance Behavior

Pages inherit attributes from their parent document:

```clojure
[:document {:width 612 :height 792 :margins [72 72 72 72]}
 [:page {}]                     ; Inherits: width=612, height=792, margins=[72,72,72,72]
 [:page {:width 842}]           ; Inherits: height=792, margins=[72,72,72,72], overrides width=842
 [:page {:margins [0 0 0 0]}]]  ; Inherits: width=612, height=792, overrides margins=[0,0,0,0]
```

## Supported Elements

### Rectangle (`:rect`)

Draws filled and/or stroked rectangles.

**Required Attributes:**
- `:x` (number) - X coordinate of top-left corner
- `:y` (number) - Y coordinate of top-left corner  
- `:width` (number) - Width of rectangle
- `:height` (number) - Height of rectangle

**Optional Attributes:**
- `:fill` (color) - Fill color
- `:stroke` (color) - Stroke color
- `:stroke-width` (number) - Stroke width

**Examples:**

```clojure
;; Basic filled rectangle
[:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}]

;; Rectangle with stroke
[:rect {:x 0 :y 0 :width 50 :height 30 :stroke "#000000" :stroke-width 2}]

;; Rectangle with both fill and stroke
[:rect {:x 25 :y 25 :width 75 :height 75 :fill "#ff0000" :stroke "#0000ff" :stroke-width 1.5}]
```

**PDF Output:**
```
;; Fill only: "1 0 0 rg\n10 20 100 50 re\nf"
;; Stroke only: "2 w\n0 0 0 RG\n0 0 50 30 re\nS"  
;; Both: "1.5 w\n1 0 0 rg\n0 0 1 RG\n25 25 75 75 re\nB"
```

### Circle (`:circle`)

Draws circles using Bézier curve approximation.

**Required Attributes:**
- `:cx` (number) - X coordinate of center
- `:cy` (number) - Y coordinate of center
- `:r` (number) - Radius (>= 0)

**Optional Attributes:**
- `:fill` (color) - Fill color
- `:stroke` (color) - Stroke color
- `:stroke-width` (number) - Stroke width

**Examples:**

```clojure
;; Basic filled circle
[:circle {:cx 50 :cy 50 :r 25 :fill "#00ff00"}]

;; Circle with stroke
[:circle {:cx 100 :cy 100 :r 30 :stroke "#ff0000" :stroke-width 3}]

;; Zero radius circle (point)
[:circle {:cx 10 :cy 10 :r 0 :fill "#000000"}]
```

### Line (`:line`)

Draws straight lines.

**Required Attributes:**
- `:x1` (number) - Start X coordinate
- `:y1` (number) - Start Y coordinate
- `:x2` (number) - End X coordinate
- `:y2` (number) - End Y coordinate

**Optional Attributes:**
- `:stroke` (color) - Stroke color (defaults to black)
- `:stroke-width` (number) - Stroke width

**Examples:**

```clojure
;; Basic line
[:line {:x1 0 :y1 0 :x2 100 :y2 100}]

;; Styled line
[:line {:x1 50 :y1 20 :x2 150 :y2 80 :stroke "#ff0000" :stroke-width 2}]
```

### Text (`:text`)

Renders text with font support and emoji compatibility.

**Required Attributes:**
- `:x` (number) - X coordinate
- `:y` (number) - Y coordinate
- `:font` (string) - Font name (non-empty)
- `:size` (number) - Font size (> 0)

**Optional Attributes:**
- `:fill` (color) - Text color (defaults to black)

**Content:** String as third element of hiccup vector

**Examples:**

```clojure
;; Basic text
[:text {:x 100 :y 200 :font "Arial" :size 12} "Hello World"]

;; Styled text
[:text {:x 50 :y 100 :font "Times" :size 18 :fill "#0000ff"} "Blue Text"]

;; Text with emoji
[:text {:x 10 :y 50 :font "Arial" :size 14} "Hello 🌍 World! 🎉"]

;; Empty text
[:text {:x 0 :y 0 :font "Arial" :size 10} ""]
```

### Path (`:path`)

Draws complex vector paths using SVG-style path data.

**Required Attributes:**
- `:d` (string) - SVG-style path data (non-empty)

**Optional Attributes:**
- `:fill` (color) - Fill color
- `:stroke` (color) - Stroke color
- `:stroke-width` (number) - Stroke width

**Supported Path Commands:**
- `M x,y` / `m x,y` - Move to
- `L x,y` / `l x,y` - Line to
- `C x1,y1 x2,y2 x,y` / `c x1,y1 x2,y2 x,y` - Cubic Bézier curve
- `Z` / `z` - Close path

**Examples:**

```clojure
;; Simple path
[:path {:d "M10,10 L50,50 L10,90 Z" :fill "#ffff00"}]

;; Curved path
[:path {:d "M20,20 C40,10 60,30 80,20" :stroke "#00ff00" :stroke-width 2}]

;; Complex path with both fill and stroke
[:path {:d "M100,100 L150,100 L125,150 Z" :fill "#ff0000" :stroke "#000000"}]
```

### Group (`:g`)

Groups elements with optional transforms and graphics state isolation.

**Optional Attributes:**
- `:transforms` (vector) - Vector of transform operations

**Content:** Child hiccup elements

**Transform Operations:**
- `[:translate [x y]]` - Translate by x, y
- `[:rotate degrees]` - Rotate by degrees
- `[:scale [sx sy]]` - Scale by sx, sy factors

**Examples:**

```clojure
;; Basic group
[:g {} 
 [:rect {:x 0 :y 0 :width 50 :height 50 :fill "#ff0000"}]
 [:circle {:cx 25 :cy 25 :r 10 :fill "#0000ff"}]]

;; Group with transforms
[:g {:transforms [[:translate [100 100]] [:rotate 45]]}
 [:rect {:x 0 :y 0 :width 30 :height 30 :fill "#00ff00"}]]

;; Nested groups
[:g {:transforms [[:translate [50 50]]]}
 [:rect {:x 0 :y 0 :width 20 :height 20 :fill "#ff0000"}]
 [:g {:transforms [[:rotate 45]]}
  [:circle {:cx 0 :cy 0 :r 10 :fill "#0000ff"}]]]
```

### Image (`:image`)

Renders images from PNG files with scaling and positioning support.

**Required Attributes:**
- `:src` (string) - Path to PNG image file
- `:width` (number) - Display width in points (> 0)
- `:height` (number) - Display height in points (> 0)
- `:x` (number) - X coordinate for positioning
- `:y` (number) - Y coordinate for positioning

**Options Configuration:**
- `:image-cache` - Image cache atom (required for image processing)

**Examples:**

```clojure
;; Basic image with scaling
[:image {:src "path/to/image.png" :width 100 :height 75 :x 50 :y 100}]

;; Square image (common for icons)
[:image {:src "icons/logo.png" :width 72 :height 72 :x 10 :y 10}]

;; Scaled emoji image
[:image {:src "emojis/noto-72/emoji_u1f600.png" :width 24 :height 24 :x 200 :y 300}]
```

**PDF Output:**
```
;; Basic structure: "q\n{scale_x} 0 0 {scale_y} {x} {y} cm\n/{XObjectRef} Do\nQ"
;; Example: "q\n1.389 0 0 1.042 50 100 cm\n/Im1 Do\nQ"
```

**Notes:**
- Requires image cache for performance: `{:image-cache (images/create-image-cache)}`
- Automatically scales images to fit specified dimensions
- Generates unique XObject references for PDF embedding
- Supports PNG format with transparency

### Emoji (`:emoji`)

Renders emoji using shortcode keywords that map to PNG image files. Provides an ergonomic interface for common emoji usage.

**Required Attributes:**
- `:code` (keyword) - Shortcode keyword (e.g., `:smile`, `:heart`, `:star`)
- `:size` (number) - Size in points (> 0, used for both width and height)
- `:x` (number) - X coordinate for positioning  
- `:y` (number) - Y coordinate for positioning

**Options Configuration:**
- `:image-cache` - Image cache atom (required for emoji processing)

**Available Shortcodes:**

| Shortcode | Emoji | Description |
|-----------|-------|-------------|
| `:smile` | 😀 | Grinning face |
| `:joy` | 😂 | Face with tears of joy |
| `:heart` | ❤️ | Red heart |
| `:star` | ⭐ | Star |
| `:thumbsup` | 👍 | Thumbs up |
| `:fire` | 🔥 | Fire |
| `:lightbulb` | 💡 | Light bulb |
| `:thinking` | 🤔 | Thinking face |
| `:warning` | ⚠️ | Warning sign |
| ... | | (60+ shortcodes available) |

**Examples:**

```clojure
;; Basic emoji
[:emoji {:code :smile :size 24 :x 100 :y 200}]

;; Small emoji for inline use
[:emoji {:code :heart :size 12 :x 50 :y 75}]

;; Large decorative emoji
[:emoji {:code :star :size 48 :x 300 :y 100}]

;; Multiple emoji in layout
[:g {}
 [:emoji {:code :thumbsup :size 16 :x 10 :y 40}]
 [:text {:x 30 :y 40 :font "Arial" :size 12} "Task completed"]
 [:emoji {:code :star :size 20 :x 200 :y 35}]]
```

**PDF Output:**
```
;; Transforms to image element internally:
;; [:emoji {:code :smile :size 24 :x 100 :y 200}]
;; becomes: [:image {:src "emojis/noto-72/emoji_u1f600.png" :width 24 :height 24 :x 100 :y 200}]
;; Output: "q\n0.333 0 0 0.333 100 200 cm\n/Em1 Do\nQ"
```

**Notes:**
- Square aspect ratio (size applies to both width and height)
- Shortcodes resolve to Noto emoji PNG files
- Automatically cached for performance
- Validation ensures shortcode exists
- Error messages list available shortcodes when validation fails
- Delegates to image rendering pipeline for PDF generation

## Colors

### Hex Colors

6-digit hex color format: `"#rrggbb"`

Examples:
- `"#ff0000"` - Red
- `"#00ff00"` - Green  
- `"#0000ff"` - Blue
- `"#ffffff"` - White
- `"#000000"` - Black

## Transforms

Transform operations are applied to groups and affect all child elements.

### Translate

Moves elements by specified offset.

**Format:** `[:translate [x y]]`

**Example:**
```clojure
[:g {:transforms [[:translate [50 100]]]}
 [:rect {:x 0 :y 0 :width 20 :height 20}]] ; Rendered at (50, 100)
```

### Rotate

Rotates elements by specified degrees around origin.

**Format:** `[:rotate degrees]`

**Example:**
```clojure
[:g {:transforms [[:rotate 45]]}
 [:rect {:x 10 :y 10 :width 20 :height 20}]] ; Rotated 45 degrees
```

### Scale

Scales elements by specified factors.

**Format:** `[:scale [sx sy]]`

**Example:**
```clojure
[:g {:transforms [[:scale [2 1.5]]]}
 [:circle {:cx 0 :cy 0 :r 10}]] ; Scaled 2x horizontally, 1.5x vertically
```

### Transform Composition

Multiple transforms are applied in order (left to right):

```clojure
[:g {:transforms [[:translate [50 50]] [:rotate 45] [:scale [2 2]]]}
 [:rect {:x 0 :y 0 :width 10 :height 10}]]
;; 1. Translate by (50, 50)
;; 2. Rotate by 45 degrees  
;; 3. Scale by 2x
```

## PDF Operator Output

The library generates standard PDF content stream operators:

### Graphics State
- `q` - Save graphics state
- `Q` - Restore graphics state
- `cm` - Concatenate transformation matrix

### Colors  
- `rg` - Set RGB fill color
- `RG` - Set RGB stroke color

### Paths
- `re` - Rectangle path
- `m` - Move to
- `l` - Line to  
- `c` - Cubic Bézier curve
- `h` - Close path

### Paint Operations
- `f` - Fill
- `S` - Stroke
- `B` - Fill and stroke

### Text
- `BT` - Begin text object
- `ET` - End text object
- `Tf` - Set font and size
- `Td` - Move text position
- `Tj` - Show text string

### Line Attributes
- `w` - Set line width

## Error Handling

The library uses comprehensive validation with the valhalla library:

### ValidationError

Thrown when input validation fails:

```clojure
;; Missing required attributes
(hiccup->pdf-ops [:rect {}])
;; => ValidationError: x: Expected number, got nil

;; Invalid attribute types  
(hiccup->pdf-ops [:circle {:cx "invalid" :cy 50 :r 25}])
;; => ValidationError: cx: Expected number, got "invalid"

;; Invalid hiccup structure
(hiccup->pdf-ops "not-a-vector")
;; => ValidationError: Assert failed, got "not-a-vector"
```

### Immediate Error Throwing

Validation occurs immediately during processing - errors are thrown as soon as invalid elements are encountered, ensuring incremental processing safety.

## Performance Characteristics

- **Memory Efficient**: Incremental string concatenation
- **Fast Processing**: Direct PDF operator generation
- **Scalable**: Handles large documents with thousands of elements
- **Deep Nesting**: Supports deeply nested groups (tested to 20+ levels)
- **Image Caching**: LRU cache with configurable memory limits (default 10MB, 50 items)
- **Emoji Performance**: < 5ms per cached emoji, < 100ms for cache miss
- **Cache Efficiency**: 95%+ hit rate for repeated emoji usage

## Image and Emoji Performance Optimization

### Cache Configuration

```clojure
(require '[hiccup-pdf.images :as images])

;; Create cache with custom settings
(def cache (images/create-image-cache {:max-size 100 :max-memory-mb 20}))

;; Use cache across multiple operations
(hiccup->pdf-ops [:emoji {:code :smile :size 24 :x 10 :y 10}] {:image-cache cache})
(hiccup->pdf-ops [:emoji {:code :heart :size 20 :x 40 :y 40}] {:image-cache cache})
```

### Performance Best Practices

1. **Reuse Cache**: Create one cache instance and reuse it across multiple operations
2. **Preload Common Emoji**: Load frequently used emoji early to warm the cache
3. **Monitor Cache Stats**: Check `(:stats @cache)` for hit/miss ratios
4. **Size Optimization**: Use consistent emoji sizes to maximize cache efficiency
5. **Memory Management**: Configure cache limits based on your memory constraints

### Error Handling for Images

```clojure
;; Always provide image cache for emoji/image elements
(try
  (hiccup->pdf-ops [:emoji {:code :smile :size 24 :x 10 :y 10}] {:image-cache cache})
  (catch js/Error e
    (if (re-find #"Image cache is required" (.-message e))
      ;; Handle missing cache
      (println "Please provide image cache for emoji rendering")
      ;; Handle other errors (validation, file not found, etc.)
      (println "Emoji rendering error:" (.-message e)))))

;; Validate shortcodes before use
(require '[hiccup-pdf.images :as images])
(if (images/validate-shortcode :custom-emoji)
  [:emoji {:code :custom-emoji :size 16 :x 10 :y 10}]
  [:text {:x 10 :y 10 :font "Arial" :size 12} "❓"]) ; Fallback
```

## Usage Patterns

### Simple Graphics

```clojure
(hiccup->pdf-ops 
  [:g {}
   [:rect {:x 10 :y 10 :width 100 :height 50 :fill "#ff0000"}]
   [:circle {:cx 60 :cy 35 :r 15 :fill "#0000ff"}]
   [:text {:x 20 :y 30 :font "Arial" :size 12} "Hello"]])
```

### Complex Layouts

```clojure
(hiccup->pdf-ops
  [:g {:transforms [[:translate [50 50]]]}
   ;; Header
   [:rect {:x 0 :y 0 :width 200 :height 30 :fill "#f0f0f0"}]
   [:text {:x 10 :y 20 :font "Arial" :size 14} "Document Title"]
   
   ;; Content area
   [:g {:transforms [[:translate [0 40]]]}
    [:rect {:x 0 :y 0 :width 200 :height 100 :stroke "#000000"}]
    [:text {:x 10 :y 20 :font "Arial" :size 12} "Content goes here"]]])
```

### Data Visualization

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Chart background
   [:rect {:x 50 :y 50 :width 300 :height 200 :fill "#ffffff" :stroke "#000000"}]
   
   ;; Data points
   (for [i (range 5)]
     [:circle {:cx (+ 75 (* i 60)) 
               :cy (+ 100 (* i 20)) 
               :r 5 
               :fill "#ff0000"}])])
```