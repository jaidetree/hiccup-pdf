# Hiccup-PDF API Reference

A ClojureScript library for converting hiccup vectors into PDF operators for vector graphics.

## Installation

Add to your `nbb.edn`:

```clojure
{:deps {hiccup-pdf {:local/root "."}}}
```

## Quick Start

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

;; Simple rectangle
(hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}])
;; => "1 0 0 rg\n10 20 100 50 re\nf"

;; Circle with stroke
(hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}])

;; Text with emoji support
(hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 🌍!"])
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
[:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]

;; Rectangle with stroke
[:rect {:x 0 :y 0 :width 50 :height 30 :stroke "black" :stroke-width 2}]

;; Rectangle with both fill and stroke
[:rect {:x 25 :y 25 :width 75 :height 75 :fill "#ff0000" :stroke "blue" :stroke-width 1.5}]
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
[:circle {:cx 50 :cy 50 :r 25 :fill "green"}]

;; Circle with stroke
[:circle {:cx 100 :cy 100 :r 30 :stroke "red" :stroke-width 3}]

;; Zero radius circle (point)
[:circle {:cx 10 :cy 10 :r 0 :fill "black"}]
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
[:line {:x1 50 :y1 20 :x2 150 :y2 80 :stroke "red" :stroke-width 2}]
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
[:text {:x 50 :y 100 :font "Times" :size 18 :fill "blue"} "Blue Text"]

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
[:path {:d "M10,10 L50,50 L10,90 Z" :fill "yellow"}]

;; Curved path
[:path {:d "M20,20 C40,10 60,30 80,20" :stroke "green" :stroke-width 2}]

;; Complex path with both fill and stroke
[:path {:d "M100,100 L150,100 L125,150 Z" :fill "red" :stroke "black"}]
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
 [:rect {:x 0 :y 0 :width 50 :height 50 :fill "red"}]
 [:circle {:cx 25 :cy 25 :r 10 :fill "blue"}]]

;; Group with transforms
[:g {:transforms [[:translate [100 100]] [:rotate 45]]}
 [:rect {:x 0 :y 0 :width 30 :height 30 :fill "green"}]]

;; Nested groups
[:g {:transforms [[:translate [50 50]]]}
 [:rect {:x 0 :y 0 :width 20 :height 20 :fill "red"}]
 [:g {:transforms [[:rotate 45]]}
  [:circle {:cx 0 :cy 0 :r 10 :fill "blue"}]]]
```

## Colors

### Named Colors

Supported named colors:
- `"red"` - RGB(255, 0, 0)
- `"green"` - RGB(0, 255, 0)  
- `"blue"` - RGB(0, 0, 255)
- `"black"` - RGB(0, 0, 0)
- `"white"` - RGB(255, 255, 255)
- `"yellow"` - RGB(255, 255, 0)
- `"cyan"` - RGB(0, 255, 255)
- `"magenta"` - RGB(255, 0, 255)

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

## Usage Patterns

### Simple Graphics

```clojure
(hiccup->pdf-ops 
  [:g {}
   [:rect {:x 10 :y 10 :width 100 :height 50 :fill "red"}]
   [:circle {:cx 60 :cy 35 :r 15 :fill "blue"}]
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
    [:rect {:x 0 :y 0 :width 200 :height 100 :stroke "black"}]
    [:text {:x 10 :y 20 :font "Arial" :size 12} "Content goes here"]]])
```

### Data Visualization

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Chart background
   [:rect {:x 50 :y 50 :width 300 :height 200 :fill "white" :stroke "black"}]
   
   ;; Data points
   (for [i (range 5)]
     [:circle {:cx (+ 75 (* i 60)) 
               :cy (+ 100 (* i 20)) 
               :r 5 
               :fill "red"}])])
```