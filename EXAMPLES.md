# Hiccup-PDF Examples

This document provides comprehensive examples for using the hiccup-pdf library to generate PDF vector graphics.

## Table of Contents

1. [Basic Shapes](#basic-shapes)
2. [Styling and Colors](#styling-and-colors)  
3. [Text Rendering](#text-rendering)
4. [Complex Paths](#complex-paths)
5. [Groups and Transforms](#groups-and-transforms)
6. [Real-World Examples](#real-world-examples)

## Basic Shapes

### Rectangles

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

;; Simple filled rectangle
(hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}])

;; Rectangle with stroke
(hiccup->pdf-ops [:rect {:x 0 :y 0 :width 200 :height 100 :stroke "black" :stroke-width 2}])

;; Rectangle with both fill and stroke  
(hiccup->pdf-ops [:rect {:x 50 :y 50 :width 80 :height 60 :fill "blue" :stroke "red" :stroke-width 3}])
```

### Circles

```clojure
;; Filled circle
(hiccup->pdf-ops [:circle {:cx 100 :cy 100 :r 50 :fill "green"}])

;; Circle outline
(hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}])

;; Small circle (dot)
(hiccup->pdf-ops [:circle {:cx 10 :cy 10 :r 3 :fill "black"}])
```

### Lines

```clojure
;; Horizontal line
(hiccup->pdf-ops [:line {:x1 0 :y1 50 :x2 200 :y2 50 :stroke "black"}])

;; Diagonal line
(hiccup->pdf-ops [:line {:x1 0 :y1 0 :x2 100 :y2 100 :stroke "red" :stroke-width 3}])

;; Vertical line
(hiccup->pdf-ops [:line {:x1 50 :y1 0 :x2 50 :y2 200 :stroke "blue"}])
```

## Styling and Colors

### Named Colors

```clojure
;; Primary colors
(hiccup->pdf-ops [:rect {:x 0 :y 0 :width 50 :height 50 :fill "red"}])
(hiccup->pdf-ops [:rect {:x 60 :y 0 :width 50 :height 50 :fill "green"}])
(hiccup->pdf-ops [:rect {:x 120 :y 0 :width 50 :height 50 :fill "blue"}])

;; Secondary colors
(hiccup->pdf-ops [:rect {:x 0 :y 60 :width 50 :height 50 :fill "yellow"}])
(hiccup->pdf-ops [:rect {:x 60 :y 60 :width 50 :height 50 :fill "cyan"}])
(hiccup->pdf-ops [:rect {:x 120 :y 60 :width 50 :height 50 :fill "magenta"}])

;; Monochrome
(hiccup->pdf-ops [:rect {:x 0 :y 120 :width 50 :height 50 :fill "black"}])
(hiccup->pdf-ops [:rect {:x 60 :y 120 :width 50 :height 50 :fill "white" :stroke "black"}])
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
(hiccup->pdf-ops [:line {:x1 0 :y1 10 :x2 200 :y2 10 :stroke "black" :stroke-width 1}])
(hiccup->pdf-ops [:line {:x1 0 :y1 30 :x2 200 :y2 30 :stroke "black" :stroke-width 2}])
(hiccup->pdf-ops [:line {:x1 0 :y1 50 :x2 200 :y2 50 :stroke "black" :stroke-width 5}])
(hiccup->pdf-ops [:line {:x1 0 :y1 80 :x2 200 :y2 80 :stroke "black" :stroke-width 10}])
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
(hiccup->pdf-ops [:text {:x 50 :y 50 :font "Arial" :size 14 :fill "red"} "Red text"])
(hiccup->pdf-ops [:text {:x 50 :y 75 :font "Arial" :size 14 :fill "blue"} "Blue text"])
(hiccup->pdf-ops [:text {:x 50 :y 100 :font "Arial" :size 14 :fill "green"} "Green text"])
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

## Complex Paths

### Basic Paths

```clojure
;; Triangle
(hiccup->pdf-ops [:path {:d "M50,10 L90,90 L10,90 Z" :fill "red"}])

;; Diamond
(hiccup->pdf-ops [:path {:d "M50,10 L90,50 L50,90 L10,50 Z" :fill "blue"}])

;; Star shape
(hiccup->pdf-ops [:path {:d "M50,5 L61,35 L95,35 L68,57 L79,91 L50,70 L21,91 L32,57 L5,35 L39,35 Z" 
                          :fill "yellow" :stroke "black"}])
```

### Curved Paths

```clojure
;; Simple curve
(hiccup->pdf-ops [:path {:d "M10,50 C10,10 90,10 90,50" :stroke "blue" :stroke-width 3}])

;; Wave pattern
(hiccup->pdf-ops [:path {:d "M0,50 C25,10 75,90 100,50 C125,10 175,90 200,50" 
                          :stroke "green" :stroke-width 2}])

;; Heart shape
(hiccup->pdf-ops [:path {:d "M50,70 C50,50 20,30 20,50 C20,30 50,50 50,30 C50,50 80,30 80,50 C80,30 50,50 50,70 Z" 
                          :fill "red"}])
```

## Groups and Transforms

### Basic Grouping

```clojure
;; Group multiple elements
(hiccup->pdf-ops 
  [:g {}
   [:rect {:x 0 :y 0 :width 100 :height 50 :fill "red"}]
   [:circle {:cx 50 :cy 25 :r 15 :fill "white"}]
   [:text {:x 35 :y 30 :font "Arial" :size 10} "Hi"]])
```

### Translations

```clojure
;; Translate group
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]]]}
   [:rect {:x 0 :y 0 :width 50 :height 50 :fill "blue"}]
   [:text {:x 10 :y 30 :font "Arial" :size 12} "Moved"]])

;; Multiple elements with same translation
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [50 50]]]}
   [:circle {:cx 0 :cy 0 :r 20 :fill "red"}]
   [:circle {:cx 50 :cy 0 :r 20 :fill "green"}]
   [:circle {:cx 25 :cy 40 :r 20 :fill "blue"}]])
```

### Rotations

```clojure
;; Rotate rectangle
(hiccup->pdf-ops 
  [:g {:transforms [[:rotate 45]]}
   [:rect {:x 0 :y 0 :width 60 :height 20 :fill "green"}]])

;; Rotate around translated center
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]] [:rotate 30]]}
   [:rect {:x -25 :y -10 :width 50 :height 20 :fill "red"}]
   [:text {:x -15 :y 5 :font "Arial" :size 10} "Rotated"]])
```

### Scaling

```clojure
;; Scale up
(hiccup->pdf-ops 
  [:g {:transforms [[:scale [2 2]]]}
   [:circle {:cx 25 :cy 25 :r 15 :fill "blue"}]])

;; Non-uniform scaling
(hiccup->pdf-ops 
  [:g {:transforms [[:scale [3 1]]]}
   [:rect {:x 0 :y 0 :width 20 :height 40 :fill "green"}]])

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
   [:rect {:x -20 :y -20 :width 40 :height 40 :fill "red"}]
   [:circle {:cx 0 :cy 0 :r 10 :fill "white"}]])
```

### Nested Groups

```clojure
;; Nested transformations
(hiccup->pdf-ops 
  [:g {:transforms [[:translate [100 100]]]}
   [:rect {:x -30 :y -30 :width 60 :height 60 :fill "blue"}]
   [:g {:transforms [[:rotate 45]]}
    [:rect {:x -15 :y -15 :width 30 :height 30 :fill "red"}]
    [:g {:transforms [[:scale [0.5 0.5]]]}
     [:circle {:cx 0 :cy 0 :r 10 :fill "white"}]]]])
```

## Real-World Examples

### Business Card

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Background
   [:rect {:x 0 :y 0 :width 350 :height 200 :fill "white" :stroke "black" :stroke-width 1}]
   
   ;; Company logo area
   [:g {:transforms [[:translate [20 20]]]}
    [:circle {:cx 0 :cy 0 :r 15 :fill "blue"}]
    [:text {:x 25 :y 5 :font "Arial" :size 18 :fill "blue"} "TechCorp"]]
   
   ;; Contact information
   [:g {:transforms [[:translate [20 80]]]}
    [:text {:x 0 :y 0 :font "Arial" :size 14 :fill "black"} "John Smith"]
    [:text {:x 0 :y 20 :font "Arial" :size 12 :fill "black"} "Senior Developer"] 
    [:text {:x 0 :y 40 :font "Arial" :size 10 :fill "black"} "john.smith@techcorp.com"]
    [:text {:x 0 :y 55 :font "Arial" :size 10 :fill "black"} "+1 (555) 123-4567"]]
   
   ;; Decorative element
   [:g {:transforms [[:translate [250 50]]]}
    [:path {:d "M0,0 L50,25 L0,50 L10,25 Z" :fill "blue"}]]])
```

### Simple Flowchart

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Start box
   [:g {:transforms [[:translate [50 50]]]}
    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "white" :stroke "green" :stroke-width 2}]
    [:text {:x 30 :y 25 :font "Arial" :size 12 :fill "green"} "Start"]]
   
   ;; Arrow  
   [:path {:d "M130,70 L170,70 M165,65 L170,70 L165,75" :stroke "black" :stroke-width 2}]
   
   ;; Process box
   [:g {:transforms [[:translate [180 50]]]}
    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "white" :stroke "blue" :stroke-width 2}]
    [:text {:x 20 :y 25 :font "Arial" :size 12 :fill "blue"} "Process"]]
   
   ;; Arrow
   [:path {:d "M260,70 L300,70 M295,65 L300,70 L295,75" :stroke "black" :stroke-width 2}]
   
   ;; End box
   [:g {:transforms [[:translate [310 50]]]}
    [:rect {:x 0 :y 0 :width 80 :height 40 :fill "white" :stroke "red" :stroke-width 2}]
    [:text {:x 35 :y 25 :font "Arial" :size 12 :fill "red"} "End"]]])
```

### Data Chart

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Chart background
   [:rect {:x 50 :y 50 :width 300 :height 200 :fill "white" :stroke "black" :stroke-width 1}]
   
   ;; Grid lines
   [:line {:x1 50 :y1 100 :x2 350 :y2 100 :stroke "black"}]
   [:line {:x1 50 :y1 150 :x2 350 :y2 150 :stroke "black"}]
   [:line {:x1 50 :y1 200 :x2 350 :y2 200 :stroke "black"}]
   [:line {:x1 100 :y1 50 :x2 100 :y2 250 :stroke "black"}]
   [:line {:x1 200 :y1 50 :x2 200 :y2 250 :stroke "black"}]
   [:line {:x1 300 :y1 50 :x2 300 :y2 250 :stroke "black"}]
   
   ;; Data points
   [:circle {:cx 75 :cy 180 :r 4 :fill "red"}]
   [:circle {:cx 125 :cy 140 :r 4 :fill "red"}]
   [:circle {:cx 175 :cy 120 :r 4 :fill "red"}]
   [:circle {:cx 225 :cy 160 :r 4 :fill "red"}]
   [:circle {:cx 275 :cy 100 :r 4 :fill "red"}]
   
   ;; Connect data points
   [:path {:d "M75,180 L125,140 L175,120 L225,160 L275,100" :stroke "red" :stroke-width 2}]
   
   ;; Labels
   [:text {:x 170 :y 280 :font "Arial" :size 12 :fill "black"} "Time"]
   [:g {:transforms [[:translate [20 150]] [:rotate -90]]}
    [:text {:x 0 :y 0 :font "Arial" :size 12 :fill "black"} "Value"]]])
```

### Logo Design

```clojure
(hiccup->pdf-ops
  [:g {:transforms [[:translate [100 100]]]}
   ;; Outer circle
   [:circle {:cx 0 :cy 0 :r 50 :fill "blue" :stroke "black" :stroke-width 3}]
   
   ;; Inner shapes
   [:g {:transforms [[:rotate 45]]}
    [:rect {:x -20 :y -20 :width 40 :height 40 :fill "white"}]
    [:circle {:cx 0 :cy 0 :r 15 :fill "blue"}]]
   
   ;; Company name
   [:text {:x -25 :y 70 :font "Arial" :size 14 :fill "black"} "MyCompany"]])
```

### Certificate Border

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Outer border
   [:rect {:x 20 :y 20 :width 560 :height 400 :stroke "black" :stroke-width 3}]
   
   ;; Inner decorative border
   [:rect {:x 40 :y 40 :width 520 :height 360 :stroke "blue" :stroke-width 1}]
   
   ;; Corner decorations
   (for [corner [[60 60] [540 60] [60 380] [540 380]]]
     [:g {:transforms [[:translate corner]]}
      [:circle {:cx 0 :cy 0 :r 8 :fill "blue"}]
      [:circle {:cx 0 :cy 0 :r 4 :fill "white"}]])
   
   ;; Title area
   [:text {:x 300 :y 120 :font "Arial" :size 24 :fill "black"} "Certificate"]
   [:text {:x 280 :y 150 :font "Arial" :size 16 :fill "black"} "of Achievement"]
   
   ;; Content area
   [:text {:x 300 :y 220 :font "Arial" :size 14 :fill "black"} "This certifies that"]
   [:text {:x 300 :y 250 :font "Arial" :size 18 :fill "blue"} "John Doe"]
   [:text {:x 270 :y 280 :font "Arial" :size 14 :fill "black"} "has successfully completed"]])
```

### Technical Diagram

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Components
   [:g {:transforms [[:translate [50 100]]]}
    [:rect {:x 0 :y 0 :width 60 :height 40 :fill "white" :stroke "black"}]
    [:text {:x 15 :y 25 :font "Arial" :size 10} "Input"]]
   
   [:g {:transforms [[:translate [200 100]]]}
    [:rect {:x 0 :y 0 :width 60 :height 40 :fill "white" :stroke "black"}]
    [:text {:x 10 :y 25 :font "Arial" :size 10} "Process"]]
   
   [:g {:transforms [[:translate [350 100]]]}
    [:rect {:x 0 :y 0 :width 60 :height 40 :fill "white" :stroke "black"}]
    [:text {:x 12 :y 25 :font "Arial" :size 10} "Output"]]
   
   ;; Connections
   [:path {:d "M110,120 L200,120 M195,115 L200,120 L195,125" :stroke "black" :stroke-width 2}]
   [:path {:d "M260,120 L350,120 M345,115 L350,120 L345,125" :stroke "black" :stroke-width 2}]
   
   ;; Labels
   [:text {:x 140 :y 110 :font "Arial" :size 8} "data"]
   [:text {:x 285 :y 110 :font "Arial" :size 8} "result"]])
```

These examples demonstrate the versatility and power of the hiccup-pdf library for creating a wide range of PDF vector graphics, from simple shapes to complex technical diagrams and layouts.