# hiccup-pdf

A ClojureScript library for transforming hiccup vectors into PDF vector primitives represented as raw PDF operators and coordinates.

## Overview

This library converts web-style hiccup markup into PDF content streams, providing a functional approach to PDF generation from structured data.

## Usage

```clojure
(require '[hiccup-pdf.core :refer [hiccup->pdf-ops]])

(hiccup->pdf-ops [:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}])
```

## Development

This project uses node-babashka (nbb) for ClojureScript development.

```bash
# Start development REPL
nbb repl

# Run tests
nbb test
```

## License

GPL-3.0-or-later