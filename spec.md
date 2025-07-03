# Hiccup-PDF Library Specification

## Overview
A ClojureScript library for node-babashka that transforms hiccup vectors into PDF vector primitives represented as raw PDF operators and coordinates.

## Core Requirements

### Primary Function
- **Function Name**: `hiccup->pdf-ops`
- **Signature**: `(hiccup->pdf-ops hiccup-vector & [options])`
- **Input**: Hiccup vector representing PDF primitives
- **Output**: String of PDF operators ready for insertion into PDF content streams
- **Options**: Hash-map parameter (ignored for now, reserved for future use)

### Supported PDF Primitives

#### Required Elements
1. **Rectangle** (`:rect`)
   - Attributes: `:x`, `:y`, `:width`, `:height`
   - Optional: `:fill`, `:stroke`, `:stroke-width`

2. **Circle** (`:circle`)
   - Attributes: `:cx`, `:cy`, `:r`
   - Optional: `:fill`, `:stroke`, `:stroke-width`

3. **Line** (`:line`)
   - Attributes: `:x1`, `:y1`, `:x2`, `:y2`
   - Optional: `:stroke`, `:stroke-width`

4. **Text** (`:text`)
   - Attributes: `:x`, `:y`, `:font`, `:size`
   - Content: String content as hiccup body
   - Optional: `:fill`

5. **Path** (`:path`)
   - Attributes: `:d` (path data string)
   - Optional: `:fill`, `:stroke`, `:stroke-width`

6. **Group** (`:g`)
   - Attributes: `:transform` (structured transform vector)
   - Content: Nested hiccup elements

#### Required Special Feature
- **Emoji Support**: Text elements must support emoji characters
- **Font Requirement**: Library references system font names; font management handled by consuming application

## Data Format Specifications

### Hiccup Vector Format
```clojure
[:element-type {:attribute "value"} content]
```

### Examples
```clojure
;; Rectangle
[:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]

;; Circle
[:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}]

;; Text with emoji
[:text {:x 10 :y 100 :font "Arial" :size 12} "Hello 👋"]

;; Group with transforms
[:g {:transform [[:translate [10 10]] [:rotate 45]]}
  [:rect {:x 0 :y 0 :width 50 :height 50}]]
```

### Transform Structure
Transforms are represented as vectors of operation pairs:
```clojure
[[:translate [x y]]
 [:rotate degrees]
 [:scale [sx sy]]]
```

### PDF Operator Output
Raw PDF operators as strings, example:
```
"10 20 100 50 re
f
BT
/Arial 12 Tf
50 700 Td
(Hello 👋) Tj
ET"
```

## Architecture Decisions

### Coordinate System
- **Input**: Web-style coordinates (top-left origin, y increases downward)
- **Units**: PDF points (1/72 inch)
- **Conversion**: Library handles automatic conversion to PDF coordinate system
- **Note**: Coordinate conversion depends on consuming application providing page dimensions

### Graphics State Management
- **Grouping**: Support `:g` elements for logical grouping
- **State Stack**: Use PDF `q` (save) and `Q` (restore) operators
- **Transforms**: Apply to coordinate system before rendering child elements
- **Nesting**: Support nested groups with proper state management

### Memory Management
- **Processing**: Incremental validation and processing
- **Efficiency**: Avoid storing entire document structure in memory
- **Streaming**: Process elements as encountered

## Error Handling Strategy

### Validation Approach
- **Timing**: Incremental validation during processing
- **Library**: Use valhalla validation library (compatible with node-babashka)
- **Strictness**: Strict validation - all attributes must be correct
- **Behavior**: Throw errors immediately on invalid elements or attributes

### Error Types
1. **Unknown Elements**: Throw error for unsupported hiccup elements
2. **Invalid Attributes**: Throw error for:
   - Missing required attributes
   - Invalid attribute values
   - Unknown attributes
3. **Malformed Structure**: Throw error for invalid hiccup structure

### Error Messages
- Clear, descriptive error messages
- Include element type and problematic attribute
- Provide context about expected format

## Testing Strategy

### Unit Tests
- **Scope**: Individual element type transformations
- **Coverage**: Each primitive element (rect, circle, line, text, path)
- **Validation**: Test both valid inputs and error conditions
- **Output**: Verify correct PDF operator generation

### Integration Tests
- **Scope**: Complete hiccup documents
- **Features**: Nested groups, transforms, multiple elements
- **Scenarios**: Complex document structures
- **State Management**: Verify proper graphics state handling

### Test Framework
- Simple test runner suitable for ClojureScript/node-babashka
- Fast execution for development workflow
- Clear test output and failure reporting

## Documentation Requirements

### Code Documentation
- **Format**: Generated from code comments/docstrings
- **Tool**: ClojureScript-compatible documentation generator

### Required Documentation
1. **API Documentation**
   - Function signatures
   - Parameter descriptions
   - Return value specifications
   - Error conditions

2. **Usage Examples**
   - Basic element usage
   - Complex nested structures
   - Transform examples
   - Error handling examples

3. **Element Reference**
   - Complete list of supported elements
   - Required and optional attributes
   - Attribute value formats
   - PDF operator output examples

## Technical Implementation Notes

### Dependencies
- valhalla validation library (may require updates for nbb compatibility)
- ClojureScript standard library
- node-babashka runtime

### Performance Considerations
- Incremental processing for large documents
- Efficient string concatenation for PDF operators
- Minimal memory allocation during transformation

### Future Extensibility
- Options parameter reserved for future features
- Modular design for adding new primitives
- Plugin architecture consideration for custom elements

## Validation Rules

### Element-Specific Rules
```clojure
;; Rectangle
{:x number? :y number? :width pos? :height pos?}

;; Circle  
{:cx number? :cy number? :r pos?}

;; Line
{:x1 number? :y1 number? :x2 number? :y2 number?}

;; Text
{:x number? :y number? :font string? :size pos?}

;; Path
{:d string?}

;; Group
{:transform vector?} ;; optional
```

### Common Optional Attributes
- `:fill` - color string
- `:stroke` - color string  
- `:stroke-width` - positive number

## Success Criteria
1. All primitive elements transform correctly to PDF operators
2. Nested groups with transforms work properly
3. Emoji text rendering supported
4. Comprehensive test coverage
5. Complete API documentation
6. Error handling provides clear feedback
7. Performance suitable for typical document sizes