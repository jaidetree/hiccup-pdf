# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a ClojureScript library for node-babashka (nbb) that transforms hiccup vectors into PDF vector primitives represented as raw PDF operators and coordinates. The library converts web-style hiccup markup into PDF content streams. The project uses `nbb.edn` for configuration and dependency management.

## Development Environment

The project uses Nix flakes for development environment setup:

```bash
# Enter development shell with all dependencies
nix develop
```

The development environment includes:

- `clj-kondo` for linting
- `clojure` CLI tools
- `clojure-lsp` for language server support
- `nodejs_22` for nbb runtime
- `temurin-jre-bin-17` for Java runtime

## Core Architecture

### Primary Function

- **Main API**: `hiccup->pdf-ops` function
- **Input**: Hiccup vectors representing PDF primitives
- **Output**: String of PDF operators for PDF content streams
- **Runtime**: node-babashka (nbb) with ClojureScript

### Supported PDF Primitives

1. **Rectangle** (`:rect`) - basic shapes with fill/stroke
2. **Circle** (`:circle`) - circular shapes
3. **Line** (`:line`) - straight lines
4. **Text** (`:text`) - text with emoji support
5. **Path** (`:path`) - complex vector paths
6. **Group** (`:g`) - transformable containers with nested elements

### Coordinate System

- Input uses web-style coordinates (top-left origin, y increases downward)
- Library handles automatic conversion to PDF coordinate system
- Units in PDF points (1/72 inch)

### Graphics State Management

- Uses PDF `q` (save) and `Q` (restore) operators for state stack
- Supports nested groups with proper transform inheritance
- Transform operations: translate, rotate, scale

## Development Commands

### Testing

```bash
# Run tests (when implemented)
nbb test
```

### Linting

```bash
# Lint ClojureScript code
clj-kondo --lint src/
```

### Development

```bash
# Start nbb REPL for interactive development
nbb repl
```

## Error Handling Strategy

- Uses valhalla validation library for strict input validation
- Incremental validation during processing
- Throws errors immediately on invalid elements or attributes
- Clear, descriptive error messages with context

## Implementation Notes

### Dependencies

- `nbb` (node-babashka) runtime with `nbb.edn` configuration
- valhalla validation library (may need nbb compatibility updates)
- ClojureScript standard library

### Performance Considerations

- Incremental processing for large documents
- Efficient string concatenation for PDF operators
- Minimal memory allocation during transformation
- Streaming processing of elements

### Future Extensibility

- Options parameter reserved for future features
- Modular design for adding new primitives
- Plugin architecture consideration for custom elements

## Development Setup Notes

### Node Babashka Configuration

- Node babashka should already be installed through package.json and available through npx

### Runtime Environment

- Nbb already provides a clojurescript runtime through node