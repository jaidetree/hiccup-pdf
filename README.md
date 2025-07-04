# hiccup-pdf

A ClojureScript library for transforming hiccup vectors into PDF vector primitives represented as raw PDF operators and coordinates.

[![Tests](https://img.shields.io/badge/tests-passing-brightgreen)](#testing)
[![ClojureScript](https://img.shields.io/badge/ClojureScript-1.11+-blue)](#)
[![nbb](https://img.shields.io/badge/nbb-compatible-orange)](#)

## Overview

This library converts web-style hiccup markup into PDF content streams, providing a functional approach to PDF generation from structured data. It supports all major PDF drawing primitives including rectangles, circles, lines, text with emoji support, SVG-style paths, and grouped elements with transforms.

## Features

- ✅ **Complete PDF Primitives**: Rectangles, circles, lines, text, paths, and groups
- ✅ **Transform Support**: Translate, rotate, and scale operations with matrix composition
- ✅ **Styling**: Fill colors, stroke colors, stroke widths with named and hex color support  
- ✅ **Text Rendering**: Font support with emoji compatibility and proper PDF text escaping
- ✅ **SVG Path Data**: Full support for SVG-style path commands (M, L, C, Z)
- ✅ **Graphics State Management**: Proper PDF graphics state save/restore with nested groups
- ✅ **Document Generation**: Complete PDF documents with metadata, multiple pages, and coordinate transformation
- ✅ **Page Management**: Multiple page sizes, orientations, and inheritance from document defaults
- ✅ **Web-style Coordinates**: Automatic conversion from web coordinates to PDF coordinate system
- ✅ **Comprehensive Validation**: Immediate error detection with detailed error messages
- ✅ **Performance Optimized**: Efficient string concatenation and memory usage

## Quick Start

### Content Stream Generation

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

;; Simple rectangle
(hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}])
;; => "1 0 0 rg\n10 20 100 50 re\nf"

;; Circle with stroke
(hiccup->pdf-ops [:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}])

;; Text with emoji
(hiccup->pdf-ops [:text {:x 100 :y 200 :font "Arial" :size 14} "Hello 🌍!"])

;; Complex group with transforms
(hiccup->pdf-ops [:g {:transforms [[:translate [50 50]] [:rotate 45]]}
                  [:rect {:x 0 :y 0 :width 30 :height 30 :fill "green"}]
                  [:circle {:cx 0 :y 0 :r 15 :fill "white"}]])
```

### Complete PDF Document Generation

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-document]])

;; Simple business report
(hiccup->pdf-document
  [:document {:title "Q4 Sales Report" :author "Sales Team"}
   [:page {}
    [:text {:x 100 :y 100 :font "Arial" :size 24} "Q4 Sales Report"]
    [:text {:x 100 :y 150 :font "Arial" :size 12} "Total Revenue: $1,234,567"]
    [:rect {:x 100 :y 200 :width 400 :height 200 :fill "#e6f3ff" :stroke "blue"}]]])

;; Multi-page document with different layouts
(hiccup->pdf-document
  [:document {:title "Mixed Format Document" :width 612 :height 792}
   ;; Letter size page
   [:page {}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "Letter Size Page"]]
   ;; A4 landscape page
   [:page {:width 842 :height 595}
    [:text {:x 50 :y 50 :font "Arial" :size 16} "A4 Landscape Page"]]])
```

## API Overview

The library provides two main functions:

| Function | Purpose | Use Case |
|----------|---------|----------|
| `hiccup->pdf-ops` | Generate PDF content streams | Embedding in existing PDFs, custom layouts |
| `hiccup->pdf-document` | Generate complete PDF documents | Standalone documents, file output |

## Documentation

- **[API Reference](API.md)** - Complete API documentation for both content streams and documents
- **[Document Generation Guide](DOCUMENT_GUIDE.md)** - Comprehensive guide for PDF document creation
- **[Examples](EXAMPLES.md)** - Comprehensive examples from basic shapes to complex layouts
- **[Development Guide](CLAUDE.md)** - Development environment setup and architecture notes

## Supported Elements

| Element | Description | Required Attributes | Optional Attributes |
|---------|-------------|-------------------|-------------------|
| `:rect` | Rectangle | `:x`, `:y`, `:width`, `:height` | `:fill`, `:stroke`, `:stroke-width` |
| `:circle` | Circle | `:cx`, `:cy`, `:r` | `:fill`, `:stroke`, `:stroke-width` |
| `:line` | Line | `:x1`, `:y1`, `:x2`, `:y2` | `:stroke`, `:stroke-width` |
| `:text` | Text | `:x`, `:y`, `:font`, `:size` | `:fill` |
| `:path` | SVG Path | `:d` | `:fill`, `:stroke`, `:stroke-width` |
| `:g` | Group | None | `:transforms` |

## Installation

Add to your `nbb.edn`:

```clojure
{:deps {hiccup-pdf {:local/root "."}}}
```

## Examples

### Business Card

```clojure
(hiccup->pdf-ops
  [:g {}
   [:rect {:x 0 :y 0 :width 350 :height 200 :fill "white" :stroke "black"}]
   [:g {:transforms [[:translate [20 20]]]}
    [:circle {:cx 0 :cy 0 :r 15 :fill "blue"}]
    [:text {:x 25 :y 5 :font "Arial" :size 18 :fill "blue"} "TechCorp"]]
   [:g {:transforms [[:translate [20 80]]]}
    [:text {:x 0 :y 0 :font "Arial" :size 14} "John Smith"]
    [:text {:x 0 :y 20 :font "Arial" :size 12} "Senior Developer"]]])
```

### Data Visualization

```clojure
(hiccup->pdf-ops
  [:g {}
   ;; Chart background
   [:rect {:x 50 :y 50 :width 300 :height 200 :fill "white" :stroke "black"}]
   ;; Data points
   (for [i (range 5)]
     [:circle {:cx (+ 75 (* i 60)) :cy (+ 100 (* i 20)) :r 5 :fill "red"}])])
```

## Development

This project uses node-babashka (nbb) for ClojureScript development with Nix flakes for environment management.

```bash
# Enter development environment
nix develop

# Start development REPL
nbb repl

# Run tests
npx nbb test_runner.cljs

# Lint code  
clj-kondo --lint src/
```

## Testing

The library includes comprehensive test coverage:

- **Unit Tests**: All element types and validation functions
- **Integration Tests**: Complex documents and real-world scenarios  
- **Error Handling Tests**: Validation and error condition coverage
- **Performance Tests**: Large documents and deep nesting

```bash
# Run all tests
npx nbb test_runner.cljs

# Tests include:
# - 41 test functions
# - 398 assertions  
# - 100% pass rate
```

## License

GPL-3.0-or-later