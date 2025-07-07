# Hiccup-PDF Examples

This document provides comprehensive examples for using the hiccup-pdf library to generate PDF vector graphics and complete PDF documents.

## Table of Contents

### Content Stream Generation
1. [Basic Shapes](#basic-shapes)
2. [Styling and Colors](#styling-and-colors)  
3. [Text Rendering](#text-rendering)
4. [Images and Emoji](#images-and-emoji)
5. [Complex Paths](#complex-paths)
6. [Groups and Transforms](#groups-and-transforms)
7. [Real-World Content Examples](#real-world-content-examples)

### Document Generation
8. [Document Structure](#document-structure)
9. [Page Management](#page-management)
10. [Business Documents](#business-documents)
11. [Technical Documentation](#technical-documentation)
12. [Multi-format Documents](#multi-format-documents)

## Basic Shapes

### Rectangles

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

;; Simple filled rectangle
(hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "#ff0000"}])

;; Rectangle with stroke
(hiccup->pdf-ops [:rect {:x 0 :y 0 :width 200 :height 100 :stroke "#000000" :stroke-width 2}])

;; Rectangle with both fill and stroke  
(hiccup->pdf-ops [:rect {:x 50 :y 50 :width 80 :height 60 :fill "#0000ff" :stroke "#ff0000" :stroke-width 3}])
```

### Circles

```clojure
;; Filled circle
(hiccup->pdf-ops [:circle {:cx 100 :cy 100 :r 50 :fill "#00ff00"}])

;; Circle outline
(hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "#0000ff" :stroke-width 2}])

;; Small circle (dot)
(hiccup->pdf-ops [:circle {:cx 10 :cy 10 :r 3 :fill "#000000"}])
```

### Lines

```clojure
;; Horizontal line
(hiccup->pdf-ops [:line {:x1 0 :y1 50 :x2 200 :y2 50 :stroke "#000000"}])

;; Diagonal line
(hiccup->pdf-ops [:line {:x1 0 :y1 0 :x2 100 :y2 100 :stroke "#ff0000" :stroke-width 3}])

;; Vertical line
(hiccup->pdf-ops [:line {:x1 50 :y1 0 :x2 50 :y2 200 :stroke "#0000ff"}])
```

## Styling and Colors

### Named Colors

```clojure
;; Primary colors
(hiccup->pdf-ops [:rect {:x 0 :y 0 :width 50 :height 50 :fill "#ff0000"}])
(hiccup->pdf-ops [:rect {:x 60 :y 0 :width 50 :height 50 :fill "#00ff00"}])
(hiccup->pdf-ops [:rect {:x 120 :y 0 :width 50 :height 50 :fill "#0000ff"}])

;; Secondary colors
(hiccup->pdf-ops [:rect {:x 0 :y 60 :width 50 :height 50 :fill "#ffff00"}])
(hiccup->pdf-ops [:rect {:x 60 :y 60 :width 50 :height 50 :fill "#00ffff"}])
(hiccup->pdf-ops [:rect {:x 120 :y 60 :width 50 :height 50 :fill "#ff00ff"}])

;; Monochrome
(hiccup->pdf-ops [:rect {:x 0 :y 120 :width 50 :height 50 :fill "#000000"}])
(hiccup->pdf-ops [:rect {:x 60 :y 120 :width 50 :height 50 :fill "#ffffff" :stroke "#000000"}])
```

### Hex Colors

```clojure
;; Custom colors with hex values
(hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 30 :fill "#ff6b6b"}])   ; Light red
(hiccup->pdf-ops [:circle {:cx 150 :cy 50 :r 30 :fill "#4ecdc4"}])  ; Teal
(hiccup->pdf-ops [:circle {:cx 250 :cy 50 :r 30 :fill "#45b7d1"}])  ; Light blue
(hiccup->pdf-ops [:circle {:cx 350 :cy 50 :r 30 :fill "#f9ca24"}])  ; Gold
```

### Stroke Widths

```clojure
;; Various stroke widths
(hiccup->pdf-ops [:line {:x1 0 :y1 10 :x2 200 :y2 10 :stroke "#000000" :stroke-width 1}])
(hiccup->pdf-ops [:line {:x1 0 :y1 30 :x2 200 :y2 30 :stroke "#000000" :stroke-width 2}])
(hiccup->pdf-ops [:line {:x1 0 :y1 50 :x2 200 :y2 50 :stroke "#000000" :stroke-width 5}])
(hiccup->pdf-ops [:line {:x1 0 :y1 80 :x2 200 :y2 80 :stroke "#000000" :stroke-width 10}])
```

## Text Rendering

### Basic Text

```clojure
;; Simple text
(hiccup->pdf-ops [:text {:x 50 :y 100 :font "Arial" :size 12} "Hello World"])

;; Different font sizes
(hiccup->pdf-ops [:text {:x 50 :y 50 :font "Arial" :size 8} "Small text"])
(hiccup->pdf-ops [:text {:x 50 :y 75 :font "Arial" :size 12} "Normal text"])
(hiccup->pdf-ops [:text {:x 50 :y 100 :font "Arial" :size 18} "Large text"])
(hiccup->pdf-ops [:text {:x 50 :y 130 :font "Arial" :size 24} "Huge text"])
```

### Colored Text

```clojure
;; Text with colors
(hiccup->pdf-ops [:text {:x 50 :y 50 :font "Arial" :size 14 :fill "#ff0000"} "Red text"])
(hiccup->pdf-ops [:text {:x 50 :y 75 :font "Arial" :size 14 :fill "#0000ff"} "Blue text"])
(hiccup->pdf-ops [:text {:x 50 :y 100 :font "Arial" :size 14 :fill "#00ff00"} "Green text"])
```

### Text with Emojis

```clojure
;; Emoji support
(hiccup->pdf-ops [:text {:x 50 :y 50 :font "Arial" :size 16} "Hello 🌍 World! 🎉"])
(hiccup->pdf-ops [:text {:x 50 :y 80 :font "Arial" :size 14} "Weather: ☀️ 🌧️ ❄️"])
(hiccup->pdf-ops [:text {:x 50 :y 110 :font "Arial" :size 14} "Faces: 😀 😢 😍 🤔"])
```

### Different Fonts

```clojure
;; Various fonts
(hiccup->pdf-ops [:text {:x 50 :y 50 :font "Arial" :size 14} "Arial font"])
(hiccup->pdf-ops [:text {:x 50 :y 75 :font "Times" :size 14} "Times font"])
(hiccup->pdf-ops [:text {:x 50 :y 100 :font "Helvetica" :size 14} "Helvetica font"])
```

## Images and Emoji

The library supports PNG image rendering and emoji through shortcodes. Both require an image cache for performance.

### Image Elements

```clojure
(require '[hiccup-pdf.images :as images])

;; Create image cache
(def cache (images/create-image-cache))

;; Basic image with scaling
(hiccup->pdf-ops [:image {:src "path/to/logo.png" :width 100 :height 75 :x 50 :y 100}]
                 {:image-cache cache})

;; Square icon image
(hiccup->pdf-ops [:image {:src "icons/settings.png" :width 32 :height 32 :x 10 :y 10}]
                 {:image-cache cache})

;; Large scaled image
(hiccup->pdf-ops [:image {:src "photos/background.png" :width 400 :height 200 :x 0 :y 0}]
                 {:image-cache cache})
```

### Emoji Elements

Emoji elements use shortcode keywords that map to PNG files. The library includes 60+ common emoji shortcodes.

```clojure
;; Basic emoji
(hiccup->pdf-ops [:emoji {:code :smile :size 24 :x 100 :y 200}]
                 {:image-cache cache})

;; Different emoji sizes
(hiccup->pdf-ops [:emoji {:code :heart :size 12 :x 50 :y 75}]   ; Small
                 {:image-cache cache})
(hiccup->pdf-ops [:emoji {:code :star :size 48 :x 300 :y 100}]  ; Large
                 {:image-cache cache})

;; Emoji in layouts
(hiccup->pdf-ops [:g {}
                  [:emoji {:code :thumbsup :size 16 :x 10 :y 40}]
                  [:text {:x 30 :y 40 :font "Arial" :size 12} "Task completed"]
                  [:emoji {:code :fire :size 16 :x 10 :y 60}]
                  [:text {:x 30 :y 60 :font "Arial" :size 12} "High performance"]]
                 {:image-cache cache})
```

### Available Emoji Shortcodes

Popular shortcodes include:

| Category | Shortcodes |
|----------|-----------|
| **Expressions** | `:smile`, `:joy`, `:heart_eyes`, `:wink`, `:thinking` |
| **Gestures** | `:thumbsup`, `:thumbsdown`, `:ok_hand`, `:wave`, `:clap` |
| **Nature** | `:sun`, `:moon`, `:star`, `:fire`, `:rainbow` |
| **Objects** | `:lightbulb`, `:key`, `:gem`, `:crown`, `:trophy` |
| **Activities** | `:soccer`, `:basketball`, `:guitar`, `:microphone` |

### Practical Emoji Examples

```clojure
;; Status indicators
(hiccup->pdf-ops [:g {}
                  [:rect {:x 0 :y 0 :width 200 :height 100 :fill "#f9f9f9"}]
                  [:emoji {:code :star :size 20 :x 10 :y 20}]
                  [:text {:x 35 :y 25 :font "Arial" :size 14} "Premium Account"]
                  [:emoji {:code :shield :size 16 :x 10 :y 50}]
                  [:text {:x 30 :y 55 :font "Arial" :size 12} "Secure"]
                  [:emoji {:code :zap :size 16 :x 10 :y 75}]
                  [:text {:x 30 :y 80 :font "Arial" :size 12} "Fast delivery"]]
                 {:image-cache cache})

;; Rating system
(hiccup->pdf-ops [:g {}
                  [:text {:x 50 :y 30 :font "Arial" :size 14} "Customer Rating:"]
                  [:emoji {:code :star :size 16 :x 50 :y 50}]
                  [:emoji {:code :star :size 16 :x 70 :y 50}]
                  [:emoji {:code :star :size 16 :x 90 :y 50}]
                  [:emoji {:code :star :size 16 :x 110 :y 50}]
                  [:emoji {:code :star :size 16 :x 130 :y 50}]]
                 {:image-cache cache})

;; Project status board
(hiccup->pdf-ops [:g {}
                  [:rect {:x 0 :y 0 :width 300 :height 150 :fill "#ffffff" :stroke "#dddddd"}]
                  [:text {:x 10 :y 20 :font "Arial" :size 16} "Project Dashboard"]
                  [:emoji {:code :checkmark :size 14 :x 10 :y 50}]
                  [:text {:x 30 :y 55 :font "Arial" :size 12} "Design completed"]
                  [:emoji {:code :gear :size 14 :x 10 :y 75}]
                  [:text {:x 30 :y 80 :font "Arial" :size 12} "Development in progress"]
                  [:emoji {:code :clock :size 14 :x 10 :y 100}]
                  [:text {:x 30 :y 105 :font "Arial" :size 12} "Testing pending"]]
                 {:image-cache cache})
```

### Performance Notes

- Image cache improves performance by avoiding repeated file loads
- Same emoji used multiple times = 1 cache miss + multiple cache hits
- Cache statistics available via `(:stats @cache)`
- Memory usage optimized with LRU eviction

## Complex Paths

### Basic Paths

```clojure
;; Triangle
(hiccup->pdf-ops [:path {:d "M50,10 L90,90 L10,90 Z" :fill "#ff0000"}])

;; Diamond
(hiccup->pdf-ops [:path {:d "M50,10 L90,50 L50,90 L10,50 Z" :fill "#0000ff"}])

;; Star shape
(hiccup->pdf-ops [:path {:d "M50,5 L61,35 L95,35 L68,57 L79,91 L50,70 L21,91 L32,57 L5,35 L39,35 Z" 
                          :fill "#ffff00" :stroke "#000000"}])
```

### Curved Paths

```clojure
;; Simple curve
(hiccup->pdf-ops [:path {:d "M10,50 C10,10 90,10 90,50" :stroke "#0000ff" :stroke-width 3}])

;; Wave pattern
(hiccup->pdf-ops [:path {:d "M0,50 C25,10 75,90 100,50 C125,10 175,90 200,50" 
                          :stroke "#00ff00" :stroke-width 2}])

;; Heart shape
(hiccup->pdf-ops [:path {:d "M50,70 C50,50 20,30 20,50 C20,30 50,50 50,30 C50,50 80,30 80,50 C80,30 50,50 50,70 Z" 
                          :fill "#ff0000"}])
```

## Groups and Transforms

### Basic Grouping

```clojure
;; Group multiple elements
(hiccup->pdf-ops 
  [:g {}
   [:rect {:x 0 :y 0 :width 100 :height 50 :fill "#ff0000"}]
   [:circle {:cx 50 :cy 25 :r 15 :fill "#ffffff"}]
   [:text {:x 35 :y 30 :font "Arial" :size 10} "Hi"]])
```

### Translations

```clojure
;; Translate group
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]]]}
   [:rect {:x 0 :y 0 :width 50 :height 50 :fill "#0000ff"}]
   [:text {:x 10 :y 30 :font "Arial" :size 12} "Moved"]])

;; Multiple elements with same translation
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [50 50]]]}
   [:circle {:cx 0 :cy 0 :r 20 :fill "#ff0000"}]
   [:circle {:cx 50 :cy 0 :r 20 :fill "#00ff00"}]
   [:circle {:cx 25 :cy 40 :r 20 :fill "#0000ff"}]])
```

### Rotations

```clojure
;; Rotate rectangle
(hiccup->pdf-ops 
  [:g {:transforms [[:rotate 45]]}
   [:rect {:x 0 :y 0 :width 60 :height 20 :fill "#00ff00"}]])

;; Rotate around translated center
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]] [:rotate 30]]}
   [:rect {:x -25 :y -10 :width 50 :height 20 :fill "#ff0000"}]
   [:text {:x -15 :y 5 :font "Arial" :size 10} "Rotated"]])
```

### Scaling

```clojure
;; Scale up
(hiccup->pdf-ops 
  [:g {:transforms [[:scale [2 2]]]}
   [:circle {:cx 25 :cy 25 :r 15 :fill "#0000ff"}]])

;; Non-uniform scaling
(hiccup->pdf-ops 
  [:g {:transforms [[:scale [3 1]]]}
   [:rect {:x 0 :y 0 :width 20 :height 40 :fill "#00ff00"}]])

;; Scale down
(hiccup->pdf-ops 
  [:g {:transforms [[:scale [0.5 0.5]]]}
   [:text {:x 0 :y 0 :font "Arial" :size 24} "Tiny"]])
```

### Combined Transforms

```clojure
;; Translate, rotate, and scale
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]] [:rotate 45] [:scale [1.5 1.5]]]}
   [:rect {:x -20 :y -20 :width 40 :height 40 :fill "#ff0000"}]
   [:circle {:cx 0 :cy 0 :r 10 :fill "#ffffff"}]])
```

### Nested Groups

```clojure
;; Nested transformations
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]]]}
   [:rect {:x -30 :y -30 :width 60 :height 60 :fill "#0000ff"}]
   [:g {:transforms [[:rotate 45]]}
    [:rect {:x -15 :y -15 :width 30 :height 30 :fill "#ff0000"}]
    [:g {:transforms [[:scale [0.5 0.5]]]}
     [:circle {:cx 0 :cy 0 :r 10 :fill "#ffffff"}]]]])
```

## Real-World Content Examples

### Business Card

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Background
   [:rect {:x 0 :y 0 :width 350 :height 200 :fill "#ffffff" :stroke "#000000" :stroke-width 1}]
   
   ;; Company logo area
   [:g {:transforms [[:translate [20 20]]]}
    [:circle {:cx 0 :cy 0 :r 15 :fill "#0000ff"}]
    [:text {:x 25 :y 5 :font "Arial" :size 18 :fill "#0000ff"} "TechCorp"]]
   
   ;; Contact information
   [:g {:transforms [[:translate [20 80]]]}
    [:text {:x 0 :y 0 :font "Arial" :size 14 :fill "#000000"} "John Smith"]
    [:text {:x 0 :y 20 :font "Arial" :size 12 :fill "#000000"} "Senior Developer"] 
    [:text {:x 0 :y 40 :font "Arial" :size 10 :fill "#000000"} "john.smith@techcorp.com"]
    [:text {:x 0 :y 55 :font "Arial" :size 10 :fill "#000000"} "+1 (555) 123-4567"]]
   
   ;; Decorative element
   [:g {:transforms [[:translate [250 50]]]}
    [:path {:d "M0,0 L50,25 L0,50 L10,25 Z" :fill "#0000ff"}]]])
```

### Simple Flowchart

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Start box
   [:g {:transforms [[:translate [50 50]]]}
    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "#ffffff" :stroke "#00ff00" :stroke-width 2}]
    [:text {:x 30 :y 25 :font "Arial" :size 12 :fill "#00ff00"} "Start"]]
   
   ;; Arrow  
   [:path {:d "M130,70 L170,70 M165,65 L170,70 L165,75" :stroke "#000000" :stroke-width 2}]
   
   ;; Process box
   [:g {:transforms [[:translate [180 50]]]}
    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "#ffffff" :stroke "#0000ff" :stroke-width 2}]
    [:text {:x 20 :y 25 :font "Arial" :size 12 :fill "#0000ff"} "Process"]]
   
   ;; Arrow
   [:path {:d "M260,70 L300,70 M295,65 L300,70 L295,75" :stroke "#000000" :stroke-width 2}]
   
   ;; End box
   [:g {:transforms [[:translate [310 50]]]}
    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "#ffffff" :stroke "#ff0000" :stroke-width 2}]
    [:text {:x 35 :y 25 :font "Arial" :size 12 :fill "#ff0000"} "End"]]])
```

### Data Chart

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Chart background
   [:rect {:x 50 :y 50 :width 300 :height 200 :fill "#ffffff" :stroke "#000000" :stroke-width 1}]
   
   ;; Grid lines
   [:line {:x1 50 :y1 100 :x2 350 :y2 100 :stroke "#000000"}]
   [:line {:x1 50 :y1 150 :x2 350 :y2 150 :stroke "#000000"}]
   [:line {:x1 50 :y1 200 :x2 350 :y2 200 :stroke "#000000"}]
   [:line {:x1 100 :y1 50 :x2 100 :y2 250 :stroke "#000000"}]
   [:line {:x1 200 :y1 50 :x2 200 :y2 250 :stroke "#000000"}]
   [:line {:x1 300 :y1 50 :x2 300 :y2 250 :stroke "#000000"}]
   
   ;; Data points
   [:circle {:cx 75 :cy 180 :r 4 :fill "#ff0000"}]
   [:circle {:cx 125 :cy 140 :r 4 :fill "#ff0000"}]
   [:circle {:cx 175 :cy 120 :r 4 :fill "#ff0000"}]
   [:circle {:cx 225 :cy 160 :r 4 :fill "#ff0000"}]
   [:circle {:cx 275 :cy 100 :r 4 :fill "#ff0000"}]
   
   ;; Connect data points
   [:path {:d "M75,180 L125,140 L175,120 L225,160 L275,100" :stroke "#ff0000" :stroke-width 2}]
   
   ;; Labels
   [:text {:x 170 :y 280 :font "Arial" :size 12 :fill "#000000"} "Time"]
   [:g {:transforms [[:translate [20 150]] [:rotate -90]]}
    [:text {:x 0 :y 0 :font "Arial" :size 12 :fill "#000000"} "Value"]]])
```

### Logo Design

```clojure
(hiccup->pdf-ops
  [:g {:transforms [[:translate [100 100]]]}
   ;; Outer circle
   [:circle {:cx 0 :cy 0 :r 50 :fill "#0000ff" :stroke "#000000" :stroke-width 3}]
   
   ;; Inner shapes
   [:g {:transforms [[:rotate 45]]}
    [:rect {:x -20 :y -20 :width 40 :height 40 :fill "#ffffff"}]
    [:circle {:cx 0 :cy 0 :r 15 :fill "#0000ff"}]]
   
   ;; Company name
   [:text {:x -25 :y 70 :font "Arial" :size 14 :fill "#000000"} "MyCompany"]])
```

### Certificate Border

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Outer border
   [:rect {:x 20 :y 20 :width 560 :height 400 :stroke "#000000" :stroke-width 3}]
   
   ;; Inner decorative border
   [:rect {:x 40 :y 40 :width 520 :height 360 :stroke "#0000ff" :stroke-width 1}]
   
   ;; Corner decorations
   (for [corner [[60 60] [540 60] [60 380] [540 380]]]
     [:g {:transforms [[:translate corner]]}
      [:circle {:cx 0 :cy 0 :r 8 :fill "#0000ff"}]
      [:circle {:cx 0 :cy 0 :r 4 :fill "#ffffff"}]])
   
   ;; Title area
   [:text {:x 300 :y 120 :font "Arial" :size 24 :fill "#000000"} "Certificate"]
   [:text {:x 280 :y 150 :font "Arial" :size 16 :fill "#000000"} "of Achievement"]
   
   ;; Content area
   [:text {:x 300 :y 220 :font "Arial" :size 14 :fill "#000000"} "This certifies that"]
   [:text {:x 300 :y 250 :font "Arial" :size 18 :fill "#0000ff"} "John Doe"]
   [:text {:x 270 :y 280 :font "Arial" :size 14 :fill "#000000"} "has successfully completed"]])
```

### Technical Diagram

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Components
   [:g {:transforms [[:translate [50 100]]]}
    [:rect {:x 0 :y 0 :width 60 :height 40 :fill "#ffffff" :stroke "#000000"}]
    [:text {:x 15 :y 25 :font "Arial" :size 10} "Input"]]
   
   [:g {:transforms [[:translate [200 100]]]}
    [:rect {:x 0 :y 0 :width 60 :height 40 :fill "#ffffff" :stroke "#000000"}]
    [:text {:x 10 :y 25 :font "Arial" :size 10} "Process"]]
   
   [:g {:transforms [[:translate [350 100]]]}
    [:rect {:x 0 :y 0 :width 60 :height 40 :fill "#ffffff" :stroke "#000000"}]
    [:text {:x 12 :y 25 :font "Arial" :size 10} "Output"]]
   
   ;; Connections
   [:path {:d "M110,120 L200,120 M195,115 L200,120 L195,125" :stroke "#000000" :stroke-width 2}]
   [:path {:d "M260,120 L350,120 M345,115 L350,120 L345,125" :stroke "#000000" :stroke-width 2}]
   
   ;; Labels
   [:text {:x 140 :y 110 :font "Arial" :size 8} "data"]
   [:text {:x 285 :y 110 :font "Arial" :size 8} "result"]])
```

---

# Document Generation Examples

The following examples demonstrate complete PDF document generation using `hiccup->pdf-document`.

## Document Structure

### Basic Document

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-document]])

;; Minimal document
(hiccup->pdf-document
  [:document {:title "Hello World"}
   [:page {}
    [:text {:x 100 :y 100 :font "Arial" :size 20} "Hello, PDF World!"]]])
```

### Document with Metadata

```clojure
;; Complete metadata
(hiccup->pdf-document
  [:document {:title "Technical Report"
              :author "Engineering Team"
              :subject "System Performance Analysis"
              :keywords "performance, analysis, metrics"
              :creator "hiccup-pdf v1.0"
              :producer "Company Analytics"}
   [:page {}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Technical Report"]
    [:text {:x 50 :y 100 :font "Arial" :size 12} "Generated with hiccup-pdf"]]])
```

## Page Management

### Multiple Page Sizes

```clojure
;; Mixed page sizes
(hiccup->pdf-document
  [:document {:title "Mixed Format Document"}
   ;; Standard letter page
   [:page {:width 612 :height 792}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Letter Size Page"]
    [:rect {:x 50 :y 100 :width 512 :height 592 :stroke "#000000"}]]
   
   ;; A4 portrait page
   [:page {:width 595 :height 842}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "A4 Portrait Page"]
    [:circle {:cx 297 :cy 421 :r 200 :stroke "#0000ff" :stroke-width 2}]]
   
   ;; Custom landscape page
   [:page {:width 800 :height 600}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Custom Landscape Page"]
    [:line {:x1 50 :y1 100 :x2 750 :y2 100 :stroke "#ff0000" :stroke-width 3}]]])
```

### Page Inheritance

```clojure
;; Document defaults with page overrides
(hiccup->pdf-document
  [:document {:width 612 :height 792 :margins [72 72 72 72]}
   ;; Inherits all document settings
   [:page {}
    [:text {:x 0 :y 0 :font "Arial" :size 12} "Uses document defaults"]]
   
   ;; Override just the width
   [:page {:width 842}
    [:text {:x 0 :y 0 :font "Arial" :size 12} "Landscape orientation"]]
   
   ;; Override margins only
   [:page {:margins [0 0 0 0]}
    [:text {:x 0 :y 0 :font "Arial" :size 12} "Full bleed page"]]])
```

## Business Documents

### Invoice Template

```clojure
;; Professional invoice
(hiccup->pdf-document
  [:document {:title "Invoice #INV-2024-001"
              :author "ABC Company"
              :subject "Invoice for services rendered"}
   [:page {}
    ;; Header
    [:text {:x 50 :y 50 :font "Arial" :size 24} "INVOICE"]
    [:text {:x 400 :y 50 :font "Arial" :size 12} "Invoice #INV-2024-001"]
    [:text {:x 400 :y 70 :font "Arial" :size 12} "Date: March 15, 2024"]
    
    ;; Company info
    [:text {:x 50 :y 100 :font "Arial" :size 14} "ABC Company"]
    [:text {:x 50 :y 120 :font "Arial" :size 12} "123 Business St"]
    [:text {:x 50 :y 140 :font "Arial" :size 12} "City, ST 12345"]
    
    ;; Client info
    [:text {:x 350 :y 100 :font "Arial" :size 14} "Bill To:"]
    [:text {:x 350 :y 120 :font "Arial" :size 12} "XYZ Corporation"]
    [:text {:x 350 :y 140 :font "Arial" :size 12} "456 Client Ave"]
    [:text {:x 350 :y 160 :font "Arial" :size 12} "Town, ST 67890"]
    
    ;; Table header
    [:rect {:x 50 :y 200 :width 500 :height 30 :fill "#f0f0f0" :stroke "#000000"}]
    [:text {:x 60 :y 220 :font "Arial" :size 12} "Description"]
    [:text {:x 350 :y 220 :font "Arial" :size 12} "Quantity"]
    [:text {:x 450 :y 220 :font "Arial" :size 12} "Rate"]
    [:text {:x 500 :y 220 :font "Arial" :size 12} "Amount"]
    
    ;; Table rows
    [:rect {:x 50 :y 230 :width 500 :height 25 :stroke "#000000"}]
    [:text {:x 60 :y 250 :font "Arial" :size 11} "Web Development Services"]
    [:text {:x 370 :y 250 :font "Arial" :size 11} "1"]
    [:text {:x 450 :y 250 :font "Arial" :size 11} "$2,500"]
    [:text {:x 500 :y 250 :font "Arial" :size 11} "$2,500"]
    
    ;; Total
    [:rect {:x 400 :y 280 :width 150 :height 25 :fill "#e6f3ff" :stroke "#000000"}]
    [:text {:x 410 :y 300 :font "Arial" :size 12} "Total: $2,500.00"]
    
    ;; Footer
    [:text {:x 50 :y 350 :font "Arial" :size 10} "Thank you for your business!"]
    [:text {:x 50 :y 370 :font "Arial" :size 10} "Payment due within 30 days."]]])
```

### Sales Report

```clojure
;; Quarterly sales report
(hiccup->pdf-document
  [:document {:title "Q4 2024 Sales Report"
              :author "Sales Department"
              :subject "Quarterly performance analysis"}
   ;; Executive summary page
   [:page {}
    [:text {:x 50 :y 50 :font "Arial" :size 20} "Q4 2024 Sales Report"]
    [:text {:x 50 :y 100 :font "Arial" :size 14} "Executive Summary"]
    [:text {:x 50 :y 130 :font "Arial" :size 12} "• Total Revenue: $4.2M (+15% YoY)"]
    [:text {:x 50 :y 150 :font "Arial" :size 12} "• New Customers: 1,247 (+22% YoY)"]
    [:text {:x 50 :y 170 :font "Arial" :size 12} "• Customer Retention: 94.3%"]
    
    ;; Revenue chart (simplified)
    [:rect {:x 50 :y 220 :width 400 :height 200 :stroke "#000000" :stroke-width 2}]
    [:text {:x 225 :y 210 :font "Arial" :size 12} "Quarterly Revenue"]
    [:rect {:x 80 :y 350 :width 40 :height 50 :fill "#0000ff"}]
    [:rect {:x 150 :y 330 :width 40 :height 70 :fill "#0000ff"}]
    [:rect {:x 220 :y 320 :width 40 :height 80 :fill "#0000ff"}]
    [:rect {:x 290 :y 300 :width 40 :height 100 :fill "#0000ff"}]
    [:text {:x 95 :y 470 :font "Arial" :size 10} "Q1"]
    [:text {:x 165 :y 470 :font "Arial" :size 10} "Q2"]
    [:text {:x 235 :y 470 :font "Arial" :size 10} "Q3"]
    [:text {:x 305 :y 470 :font "Arial" :size 10} "Q4"]]
   
   ;; Detailed metrics page
   [:page {}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Detailed Metrics"]
    
    ;; Customer acquisition table
    [:text {:x 50 :y 100 :font "Arial" :size 14} "Customer Acquisition"]
    [:rect {:x 50 :y 120 :width 300 :height 120 :stroke "#000000"}]
    [:text {:x 60 :y 140 :font "Arial" :size 11} "Channel"]
    [:text {:x 200 :y 140 :font "Arial" :size 11} "Customers"]
    [:text {:x 60 :y 160 :font "Arial" :size 11} "Direct Sales"]
    [:text {:x 220 :y 160 :font "Arial" :size 11} "456"]
    [:text {:x 60 :y 180 :font "Arial" :size 11} "Online"]
    [:text {:x 220 :y 180 :font "Arial" :size 11} "623"]
    [:text {:x 60 :y 200 :font "Arial" :size 11} "Referrals"]
    [:text {:x 220 :y 200 :font "Arial" :size 11} "168"]
    
    ;; Regional performance
    [:text {:x 50 :y 280 :font "Arial" :size 14} "Regional Performance"]
    [:circle {:cx 150 :cy 350 :r 60 :fill "#00ff00"}]
    [:text {:x 130 :y 355 :font "Arial" :size 10} "North"]
    [:circle {:cx 300 :cy 350 :r 45 :fill "#ffff00"}]
    [:text {:x 285 :y 355 :font "Arial" :size 10} "South"]
    [:circle {:cx 450 :cy 350 :r 50 :fill "#0000ff"}]
    [:text {:x 435 :y 355 :font "Arial" :size 10} "West"]]])
```

## Technical Documentation

### API Documentation

```clojure
;; API reference document
(hiccup->pdf-document
  [:document {:title "API Reference Guide"
              :author "Development Team"
              :subject "REST API Documentation"}
   ;; Title page
   [:page {}
    [:text {:x 200 :y 100 :font "Arial" :size 28} "API Reference"]
    [:text {:x 250 :y 150 :font "Arial" :size 16} "Version 2.0"]
    [:text {:x 225 :y 200 :font "Arial" :size 12} "Development Team"]
    [:text {:x 240 :y 220 :font "Arial" :size 12} "March 2024"]
    
    ;; Decorative elements
    [:rect {:x 100 :y 80 :width 400 :height 200 :stroke "#0000ff" :stroke-width 3}]
    [:circle {:cx 300 :cy 350 :r 100 :stroke "#0000ff" :stroke-width 2}]]
   
   ;; Endpoints page
   [:page {}
    [:text {:x 50 :y 50 :font "Arial" :size 20} "Endpoints"]
    
    ;; GET endpoint
    [:rect {:x 50 :y 100 :width 500 :height 80 :fill "#f8f9fa" :stroke "#000000"}]
    [:rect {:x 60 :y 110 :width 40 :height 20 :fill "#00ff00"}]
    [:text {:x 70 :y 125 :font "Courier" :size 10} "GET"]
    [:text {:x 120 :y 125 :font "Courier" :size 12} "/api/users"]
    [:text {:x 60 :y 145 :font "Arial" :size 11} "Retrieve all users"]
    [:text {:x 60 :y 165 :font "Arial" :size 10} "Returns: Array of user objects"]
    
    ;; POST endpoint
    [:rect {:x 50 :y 200 :width 500 :height 80 :fill "#f8f9fa" :stroke "#000000"}]
    [:rect {:x 60 :y 210 :width 50 :height 20 :fill "#0000ff"}]
    [:text {:x 70 :y 225 :font "Courier" :size 10} "POST"]
    [:text {:x 120 :y 225 :font "Courier" :size 12} "/api/users"]
    [:text {:x 60 :y 245 :font "Arial" :size 11} "Create a new user"]
    [:text {:x 60 :y 265 :font "Arial" :size 10} "Body: User object | Returns: Created user"]
    
    ;; Code example
    [:text {:x 50 :y 320 :font "Arial" :size 14} "Example Request"]
    [:rect {:x 50 :y 340 :width 500 :height 100 :fill "#2d3748" :stroke "#000000"}]
    [:text {:x 60 :y 360 :font "Courier" :size 10 :fill "#ffffff"} "curl -X POST https://api.example.com/users \\"]
    [:text {:x 60 :y 380 :font "Courier" :size 10 :fill "#ffffff"} "  -H \"Content-Type: application/json\" \\"]
    [:text {:x 60 :y 400 :font "Courier" :size 10 :fill "#ffffff"} "  -d '{\"name\": \"John Doe\", \"email\": \"john@example.com\"}'"]
    [:text {:x 60 :y 420 :font "Courier" :size 10 :fill "#ffffff"} ""]]]
   
   ;; Response formats page
   [:page {}
    [:text {:x 50 :y 50 :font "Arial" :size 20} "Response Formats"]
    
    ;; Success response
    [:text {:x 50 :y 100 :font "Arial" :size 14} "Success Response (200 OK)"]
    [:rect {:x 50 :y 120 :width 500 :height 80 :fill "#f0fff4" :stroke "#00ff00"}]
    [:text {:x 60 :y 140 :font "Courier" :size 10} "{"]
    [:text {:x 70 :y 155 :font "Courier" :size 10} "  \"status\": \"success\","]
    [:text {:x 70 :y 170 :font "Courier" :size 10} "  \"data\": { ... },"]
    [:text {:x 70 :y 185 :font "Courier" :size 10} "  \"message\": \"Operation completed\""]
    [:text {:x 60 :y 195 :font "Courier" :size 10} "}"]
    
    ;; Error response
    [:text {:x 50 :y 230 :font "Arial" :size 14} "Error Response (400+ status)"]
    [:rect {:x 50 :y 250 :width 500 :height 80 :fill "#fff5f5" :stroke "#ff0000"}]
    [:text {:x 60 :y 270 :font "Courier" :size 10} "{"]
    [:text {:x 70 :y 285 :font "Courier" :size 10} "  \"status\": \"error\","]
    [:text {:x 70 :y 300 :font "Courier" :size 10} "  \"error\": \"Validation failed\","]
    [:text {:x 70 :y 315 :font "Courier" :size 10} "  \"details\": [ ... ]"]
    [:text {:x 60 :y 325 :font "Courier" :size 10} "}"]]])
```

## Multi-format Documents

### Presentation with Speaker Notes

```clojure
;; Conference presentation
(hiccup->pdf-document
  [:document {:title "Machine Learning in Production"
              :author "Dr. Jane Smith"
              :subject "Tech Conference 2024"}
   ;; Slide 1 - Title slide (16:9 aspect ratio)
   [:page {:width 792 :height 612}
    [:rect {:x 0 :y 0 :width 792 :height 612 :fill "#1e3a8a"}]
    [:text {:x 200 :y 200 :font "Arial" :size 36 :fill "#ffffff"} "Machine Learning"]
    [:text {:x 240 :y 250 :font "Arial" :size 36 :fill "#ffffff"} "in Production"]
    [:text {:x 300 :y 350 :font "Arial" :size 18 :fill "#ffffff"} "Dr. Jane Smith"]
    [:text {:x 280 :y 380 :font "Arial" :size 14 :fill "#ffffff"} "Tech Conference 2024"]]
   
   ;; Speaker notes for slide 1 (standard letter)
   [:page {:width 612 :height 792}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Speaker Notes - Slide 1"]
    [:text {:x 50 :y 100 :font "Arial" :size 12} "Introduction points:"]
    [:text {:x 50 :y 130 :font "Arial" :size 11} "• Welcome audience and introduce topic"]
    [:text {:x 50 :y 150 :font "Arial" :size 11} "• Brief overview of ML challenges in production"]
    [:text {:x 50 :y 170 :font "Arial" :size 11} "• Agenda: models, monitoring, scaling, best practices"]
    [:text {:x 50 :y 190 :font "Arial" :size 11} "• Personal experience: 5 years ML engineering"]
    
    [:text {:x 50 :y 240 :font "Arial" :size 12} "Key statistics to mention:"]
    [:text {:x 50 :y 270 :font "Arial" :size 11} "• 85% of ML models never make it to production"]
    [:text {:x 50 :y 290 :font "Arial" :size 11} "• Average time from model to production: 8 months"]
    [:text {:x 50 :y 310 :font "Arial" :size 11} "• 60% of companies struggle with model monitoring"]
    
    [:rect {:x 50 :y 350 :width 500 :height 100 :fill "#fef3c7" :stroke "orange"}]
    [:text {:x 60 :y 370 :font "Arial" :size 11} "💡 Timing: 3 minutes"]
    [:text {:x 60 :y 390 :font "Arial" :size 11} "🎯 Goal: Set context and engage audience"]
    [:text {:x 60 :y 410 :font "Arial" :size 11} "⚠️  Don't: Get too technical in introduction"]
    [:text {:x 60 :y 430 :font "Arial" :size 11} "✅ Do: Use relatable examples and ask questions"]]
   
   ;; Slide 2 - Content slide
   [:page {:width 792 :height 612}
    [:rect {:x 0 :y 0 :width 792 :height 612 :fill "#ffffff"}]
    [:text {:x 50 :y 50 :font "Arial" :size 24} "Challenges in ML Production"]
    
    ;; Challenge 1
    [:circle {:cx 100 :cy 150 :r 30 :fill "#ff0000"}]
    [:text {:x 85 :y 155 :font "Arial" :size 14 :fill "#ffffff"} "1"]
    [:text {:x 150 :y 155 :font "Arial" :size 16} "Model Drift"]
    [:text {:x 150 :y 180 :font "Arial" :size 12} "Performance degrades over time"]
    
    ;; Challenge 2
    [:circle {:cx 100 :cy 250 :r 30 :fill "orange"}]
    [:text {:x 85 :y 255 :font "Arial" :size 14 :fill "#ffffff"} "2"]
    [:text {:x 150 :y 255 :font "Arial" :size 16} "Data Pipeline Failures"]
    [:text {:x 150 :y 280 :font "Arial" :size 12} "Upstream data quality issues"]
    
    ;; Challenge 3
    [:circle {:cx 100 :cy 350 :r 30 :fill "#0000ff"}]
    [:text {:x 85 :y 355 :font "Arial" :size 14 :fill "#ffffff"} "3"]
    [:text {:x 150 :y 355 :font "Arial" :size 16} "Scalability"]
    [:text {:x 150 :y 380 :font "Arial" :size 12} "Handling increasing load"]
    
    ;; Visual element
    [:g {:transforms [[:translate [500 200]]]}
     [:rect {:x 0 :y 0 :width 200 :height 150 :fill "#f3f4f6" :stroke "#000000"}]
     [:text {:x 70 :y 30 :font "Arial" :size 14} "ML System"]
     [:rect {:x 20 :y 50 :width 160 :height 20 :fill "#00ff00"}]
     [:text {:x 90 :y 65 :font "Arial" :size 10} "Training"]
     [:rect {:x 20 :y 80 :width 160 :height 20 :fill "#ffff00"}]
     [:text {:x 85 :y 95 :font "Arial" :size 10} "Validation"]
     [:rect {:x 20 :y 110 :width 160 :height 20 :fill "#ff0000"}]
     [:text {:x 80 :y 125 :font "Arial" :size 10} "Production"]]]]
   
   ;; Speaker notes for slide 2
   [:page {:width 612 :height 792}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Speaker Notes - Slide 2"]
    [:text {:x 50 :y 100 :font "Arial" :size 12} "Detailed talking points:"]
    
    [:text {:x 50 :y 130 :font "Arial" :size 12} "Model Drift:"]
    [:text {:x 50 :y 150 :font "Arial" :size 11} "• Real-world example: recommendation system accuracy"]
    [:text {:x 50 :y 170 :font "Arial" :size 11} "• Causes: changing user behavior, seasonal patterns"]
    [:text {:x 50 :y 190 :font "Arial" :size 11} "• Solutions: continuous monitoring, automatic retraining"]
    
    [:text {:x 50 :y 230 :font "Arial" :size 12} "Data Pipeline Failures:"]
    [:text {:x 50 :y 250 :font "Arial" :size 11} "• Story: 3am incident at previous company"]
    [:text {:x 50 :y 270 :font "Arial" :size 11} "• Impact: wrong predictions for 6 hours"]
    [:text {:x 50 :y 290 :font "Arial" :size 11} "• Prevention: data validation, circuit breakers"]
    
    [:text {:x 50 :y 330 :font "Arial" :size 12} "Scalability:"]
    [:text {:x 50 :y 350 :font "Arial" :size 11} "• Question for audience: \"Who has seen 10x traffic spikes?\""]
    [:text {:x 50 :y 370 :font "Arial" :size 11} "• Technical solutions: caching, model optimization"]
    [:text {:x 50 :y 390 :font "Arial" :size 11} "• Architecture patterns: microservices, async processing"]]])
```

These document generation examples demonstrate the versatility and power of the hiccup-pdf library for creating complete PDF documents, from simple business reports to complex technical documentation and presentation materials. The library handles coordinate transformation, page management, and proper PDF structure automatically while providing full control over layout and styling.