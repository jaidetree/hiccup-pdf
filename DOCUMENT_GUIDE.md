# Document Generation Guide

This guide provides comprehensive information about generating complete PDF documents using `hiccup->pdf-document`.

## Table of Contents

1. [Overview](#overview)
2. [Document Structure](#document-structure)
3. [Page Management](#page-management)
4. [Coordinate System](#coordinate-system)
5. [System Fonts](#system-fonts)
6. [Inheritance Patterns](#inheritance-patterns)
7. [Best Practices](#best-practices)
8. [Common Patterns](#common-patterns)

## Overview

The `hiccup->pdf-document` function generates complete, standalone PDF documents that can be written directly to files. Unlike `hiccup->pdf-ops` which generates content streams for embedding in existing PDFs, this function creates the entire PDF structure including:

- PDF header and metadata
- Document catalog and page tree
- Font resources and references
- Cross-reference table and trailer
- Proper PDF object numbering and byte offsets

## Document Structure

### Required Structure

All documents must follow this structure:

```clojure
[:document document-attributes
 [:page page-attributes & page-content]
 [:page page-attributes & page-content]
 ...]
```

### Document Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `:title` | string | none | Document title (appears in PDF metadata) |
| `:author` | string | none | Document author |
| `:subject` | string | none | Document subject |
| `:keywords` | string | none | Document keywords (comma-separated) |
| `:creator` | string | "hiccup-pdf" | Creating application |
| `:producer` | string | "hiccup-pdf" | Producing application |
| `:width` | number | 612 | Default page width in points |
| `:height` | number | 792 | Default page height in points |
| `:margins` | vector | [0 0 0 0] | Default margins [top right bottom left] |

### Page Attributes

| Attribute | Type | Inherited | Description |
|-----------|------|-----------|-------------|
| `:width` | number | ✓ | Page width in points |
| `:height` | number | ✓ | Page height in points |
| `:margins` | vector | ✓ | Page margins [top right bottom left] |

## Page Management

### Standard Page Sizes

Common page sizes in PDF points (72 points = 1 inch):

```clojure
;; Letter (8.5" × 11")
{:width 612 :height 792}

;; A4 (210mm × 297mm)
{:width 595 :height 842}

;; Legal (8.5" × 14")
{:width 612 :height 1008}

;; Tabloid/Ledger (11" × 17")
{:width 792 :height 1224}

;; Custom business card (3.5" × 2")
{:width 252 :height 144}
```

### Orientation Examples

```clojure
;; Portrait orientation (default)
[:page {:width 612 :height 792} ...]

;; Landscape orientation (swap dimensions)
[:page {:width 792 :height 612} ...]

;; Square format
[:page {:width 612 :height 612} ...]
```

### Multiple Page Sizes

```clojure
[:document {:title "Mixed Format Document"}
 ;; Letter portrait
 [:page {:width 612 :height 792}
  [:text {:x 50 :y 50 :font "Arial" :size 16} "Letter Page"]]
 
 ;; A4 landscape
 [:page {:width 842 :height 595}
  [:text {:x 50 :y 50 :font "Arial" :size 16} "A4 Landscape"]]
 
 ;; Custom size
 [:page {:width 400 :height 600}
  [:text {:x 50 :y 50 :font "Arial" :size 16} "Custom Page"]]]
```

## Coordinate System

### Web-Style Input Coordinates

The library uses web-style coordinates for input consistency:

- **Origin**: Top-left corner (0, 0)
- **X-axis**: Increases rightward →
- **Y-axis**: Increases downward ↓

### Automatic PDF Conversion

The library automatically converts to PDF coordinates internally:

- **Origin**: Bottom-left corner (0, 0) 
- **X-axis**: Increases rightward →
- **Y-axis**: Increases upward ↑

### Coordinate Examples

```clojure
;; Web coordinates (input)
[:text {:x 100 :y 100 :font "Arial" :size 12} "Hello"]

;; Automatically converted to PDF coordinates based on page height
;; For 792pt page: y=100 becomes y=692 (792-100)
```

### Working with Margins

```clojure
;; Document with margins
[:document {:margins [72 72 72 72]}  ; 1 inch margins
 [:page {}
  ;; Content positioned relative to page, not margins
  ;; This text will be 72pt from top edge
  [:text {:x 72 :y 72 :font "Arial" :size 12} "Margined content"]]]
```

## System Fonts

### Supported Font Names

The library maps common font names to PDF standard fonts:

| Input Font Name | PDF Standard Font | Style |
|-----------------|-------------------|-------|
| "Arial" | Helvetica | Sans-serif |
| "Helvetica" | Helvetica | Sans-serif |
| "Times" | Times-Roman | Serif |
| "Times New Roman" | Times-Roman | Serif |
| "Courier" | Courier | Monospace |
| (unknown) | Helvetica | Default fallback |

### Font Usage Examples

```clojure
;; Different fonts in same document
[:page {}
 [:text {:x 50 :y 50 :font "Arial" :size 16} "Sans-serif heading"]
 [:text {:x 50 :y 80 :font "Times" :size 12} "Serif body text"]
 [:text {:x 50 :y 110 :font "Courier" :size 10} "Monospace code"]]
```

### Font Sizing

Font sizes are specified in points:

```clojure
;; Common font sizes
[:text {:font "Arial" :size 8} "Small footnote"]      ; 8pt
[:text {:font "Arial" :size 10} "Small body"]         ; 10pt  
[:text {:font "Arial" :size 12} "Normal body"]        ; 12pt
[:text {:font "Arial" :size 14} "Large body"]         ; 14pt
[:text {:font "Arial" :size 16} "Small heading"]      ; 16pt
[:text {:font "Arial" :size 20} "Medium heading"]     ; 20pt
[:text {:font "Arial" :size 24} "Large heading"]      ; 24pt
[:text {:font "Arial" :size 36} "Title"]              ; 36pt
```

## Inheritance Patterns

### Basic Inheritance

```clojure
[:document {:width 612 :height 792 :margins [50 50 50 50]}
 ;; Page inherits all document attributes
 [:page {}
  [:text {:x 0 :y 0 :font "Arial" :size 12} "Uses inherited settings"]]
 
 ;; Page overrides specific attributes
 [:page {:width 842}  ; Override width, inherit height and margins
  [:text {:x 0 :y 0 :font "Arial" :size 12} "Custom width page"]]]
```

### Selective Inheritance

```clojure
[:document {:width 612 :height 792 :margins [72 72 72 72] :title "Report"}
 [:page {}]                              ; Inherits: width=612, height=792, margins=[72,72,72,72]
 [:page {:width 842}]                    ; Inherits: height=792, margins=[72,72,72,72]
 [:page {:margins [0 0 0 0]}]            ; Inherits: width=612, height=792
 [:page {:width 400 :height 600}]       ; Inherits: margins=[72,72,72,72]
 [:page {:width 842 :height 595 :margins [0 0 0 0]}]]  ; Overrides all
```

### Template Pattern

```clojure
;; Document as template
(defn create-report [title pages-data]
  [:document {:title title
              :width 612 :height 792
              :margins [72 72 72 72]
              :author "Reports Team"}
   ;; All pages inherit consistent formatting
   (map (fn [page-data]
          [:page {}
           [:text {:x 0 :y 0 :font "Arial" :size 16} (:title page-data)]
           [:text {:x 0 :y 30 :font "Arial" :size 12} (:content page-data)]])
        pages-data)])
```

## Best Practices

### Document Structure

1. **Always specify metadata**: Include title, author, and subject for professional documents
2. **Use consistent page sizes**: Avoid mixing page sizes unless necessary  
3. **Plan your layout**: Consider margins and content areas before positioning elements
4. **Group related content**: Use semantic grouping for complex layouts

```clojure
;; Good: Consistent structure
[:document {:title "Professional Report" :author "Team Lead"}
 [:page {:margins [72 72 72 72]}
  ;; Header
  [:text {:x 0 :y 0 :font "Arial" :size 20} "Report Title"]
  ;; Body
  [:text {:x 0 :y 50 :font "Arial" :size 12} "Content..."]
  ;; Footer
  [:text {:x 0 :y 720 :font "Arial" :size 8} "Page 1"]]]
```

### Performance Considerations

1. **Minimize font variety**: Each unique font creates additional PDF objects
2. **Batch similar elements**: Group similar operations for better performance
3. **Reasonable page counts**: Very large documents may impact memory usage

### Error Prevention

1. **Validate dimensions**: Ensure positive width/height values
2. **Check coordinate bounds**: Position elements within page boundaries
3. **Test inheritance**: Verify page attributes inherit correctly

## Common Patterns

### Multi-Column Layout

```clojure
[:page {:width 612 :height 792 :margins [72 72 72 72]}
 ;; Left column (content area: 468x648, split into 2 columns of 224 each)
 [:text {:x 0 :y 0 :font "Arial" :size 12} "Left column content"]
 [:text {:x 0 :y 20 :font "Arial" :size 12} "More left content"]
 
 ;; Right column (starts at x=244, giving 20pt gutter)
 [:text {:x 244 :y 0 :font "Arial" :size 12} "Right column content"]
 [:text {:x 244 :y 20 :font "Arial" :size 12} "More right content"]]
```

### Header/Footer Pattern

```clojure
[:page {}
 ;; Header
 [:rect {:x 0 :y 0 :width 612 :height 50 :fill "#f0f0f0"}]
 [:text {:x 20 :y 25 :font "Arial" :size 14} "Document Header"]
 
 ;; Content area (y: 50 to 742)
 [:text {:x 20 :y 70 :font "Arial" :size 12} "Main content starts here"]
 
 ;; Footer
 [:rect {:x 0 :y 742 :width 612 :height 50 :fill "#f0f0f0"}]
 [:text {:x 20 :y 767 :font "Arial" :size 10} "Footer content"]]
```

### Table Layout

```clojure
[:page {}
 ;; Table header
 [:rect {:x 50 :y 100 :width 500 :height 30 :fill "#e6e6e6" :stroke "black"}]
 [:text {:x 60 :y 120 :font "Arial" :size 12} "Column 1"]
 [:text {:x 200 :y 120 :font "Arial" :size 12} "Column 2"]
 [:text {:x 340 :y 120 :font "Arial" :size 12} "Column 3"]
 
 ;; Table rows
 [:rect {:x 50 :y 130 :width 500 :height 25 :stroke "black"}]
 [:text {:x 60 :y 150 :font "Arial" :size 11} "Row 1, Col 1"]
 [:text {:x 200 :y 150 :font "Arial" :size 11} "Row 1, Col 2"]
 [:text {:x 340 :y 150 :font "Arial" :size 11} "Row 1, Col 3"]]
```

### Form Layout

```clojure
[:page {}
 ;; Form title
 [:text {:x 50 :y 50 :font "Arial" :size 18} "Application Form"]
 
 ;; Form fields
 [:text {:x 50 :y 100 :font "Arial" :size 12} "Name:"]
 [:rect {:x 120 :y 90 :width 200 :height 20 :stroke "black"}]
 
 [:text {:x 50 :y 140 :font "Arial" :size 12} "Email:"]
 [:rect {:x 120 :y 130 :width 200 :height 20 :stroke "black"}]
 
 [:text {:x 50 :y 180 :font "Arial" :size 12} "Phone:"]
 [:rect {:x 120 :y 170 :width 200 :height 20 :stroke "black"}]]
```

### Chart/Graph Integration

```clojure
[:page {}
 ;; Chart title
 [:text {:x 200 :y 50 :font "Arial" :size 16} "Sales Performance"]
 
 ;; Chart area
 [:rect {:x 100 :y 100 :width 400 :height 300 :stroke "black" :stroke-width 2}]
 
 ;; Data bars (simplified bar chart)
 [:rect {:x 120 :y 350 :width 30 :height 40 :fill "blue"}]
 [:rect {:x 170 :y 330 :width 30 :height 60 :fill "blue"}]
 [:rect {:x 220 :y 320 :width 30 :height 70 :fill "blue"}]
 [:rect {:x 270 :y 300 :width 30 :height 90 :fill "blue"}]
 
 ;; Labels
 [:text {:x 125 :y 420 :font "Arial" :size 10} "Q1"]
 [:text {:x 175 :y 420 :font "Arial" :size 10} "Q2"]
 [:text {:x 225 :y 420 :font "Arial" :size 10} "Q3"]
 [:text {:x 275 :y 420 :font "Arial" :size 10} "Q4"]]
```

This guide provides the foundation for creating professional PDF documents using the hiccup-pdf library. Combine these patterns with the comprehensive element support to create complex, well-structured documents for any use case.