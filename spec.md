# Hiccup-PDF Library Specification

## Overview
A ClojureScript library for node-babashka that transforms hiccup vectors into PDF vector primitives represented as raw PDF operators and coordinates.

## Core Requirements

### Primary Functions

#### Content Stream Generation
- **Function Name**: `hiccup->pdf-ops`
- **Signature**: `(hiccup->pdf-ops hiccup-vector & [options])`
- **Input**: Hiccup vector representing PDF primitives
- **Output**: String of PDF operators ready for insertion into PDF content streams
- **Options**: Hash-map parameter (ignored for now, reserved for future use)

#### Document Generation
- **Function Name**: `hiccup->pdf-document`
- **Signature**: `(hiccup->pdf-document hiccup-document)`
- **Input**: Hiccup vector with `:document` root element containing `:page` elements
- **Output**: Complete PDF document as string/bytes
- **Purpose**: Generates complete PDF files with proper document structure, metadata, and multiple pages

### Supported PDF Primitives

#### Document Structure Elements
1. **Document** (`:document`)
   - Root element for PDF document generation
   - Attributes: `:title`, `:author`, `:subject`, `:keywords`, `:creator`, `:producer`, `:width`, `:height`, `:margins`
   - Content: Must contain one or more `:page` elements

2. **Page** (`:page`)
   - Represents a single page within a PDF document
   - Attributes: `:width`, `:height`, `:margins` (inherits from document if not specified)
   - Content: Any combination of PDF primitive elements
   - Coordinate system: Web-style (top-left origin), automatically converted to PDF coordinates

#### PDF Primitive Elements
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
   - Attributes: `:transforms` (vector of transform operations)
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

#### Content Stream Examples
```clojure
;; Rectangle
[:rect {:x 10 :y 20 :width 100 :height 50 :fill "red"}]

;; Circle
[:circle {:cx 50 :cy 50 :r 25 :stroke "blue" :stroke-width 2}]

;; Text with emoji
[:text {:x 10 :y 100 :font "Arial" :size 12} "Hello 👋"]

;; Group with transforms
[:g {:transforms [[:translate [10 10]] [:rotate 45]]}
  [:rect {:x 0 :y 0 :width 50 :height 50}]]
```

#### Document Examples
```clojure
;; Simple document with multiple pages
(hiccup->pdf-document
  [:document {:title "Business Report" 
              :author "Jane Smith"
              :width 612 :height 792
              :margins [72 72 72 72]}
   
   ;; Cover page
   [:page {}
    [:text {:x 234 :y 300 :font "Arial" :size 24} "📊 Annual Report"]
    [:text {:x 234 :y 250 :font "Arial" :size 16} "2024"]]
   
   ;; Content page
   [:page {}
    [:text {:x 0 :y 0 :font "Arial" :size 18} "Executive Summary"]
    [:rect {:x 0 :y 50 :width 468 :height 200 :fill "#f0f0f0" :stroke "black"}]]
   
   ;; Landscape page
   [:page {:width 792 :height 612}
    [:text {:x 100 :y 100 :font "Arial" :size 16} "🗺️ Wide Format Data"]]])
```

### Transform Structure
Transforms are represented as vectors of operation pairs within the `:transforms` attribute:
```clojure
;; Single transform
[:g {:transforms [[:translate [50 50]]]}
 [:rect {:x 0 :y 0 :width 100 :height 50}]]

;; Multiple transforms (applied left-to-right)
[:g {:transforms [[:translate [100 100]] 
                  [:rotate 45] 
                  [:scale [1.5 1.5]]]}
 [:circle {:cx 0 :cy 0 :r 25}]]
```

#### Transform Operations
- `[:translate [x y]]` - Move by x, y points
- `[:rotate degrees]` - Rotate by degrees (positive = clockwise)  
- `[:scale [sx sy]]` - Scale by sx (horizontal) and sy (vertical) factors

### PDF Operator Output

#### Content Stream Output
Raw PDF operators as strings for content streams:
```
"10 20 100 50 re
f
BT
/Arial 12 Tf
50 700 Td
(Hello 👋) Tj
ET"
```

#### Document Output
Complete PDF document structure for `hiccup->pdf-document`:
```
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>
endobj
4 0 obj
<< /Length 45 >>
stream
10 20 100 50 re
f
endstream
endobj
xref
0 5
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000207 00000 n 
trailer
<< /Size 5 /Root 1 0 R >>
startxref
252
%%EOF
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

#### Document Structure Elements
```clojure
;; Document
{:title string?           ;; optional - document title
 :author string?          ;; optional - document author
 :subject string?         ;; optional - document subject
 :keywords string?        ;; optional - document keywords
 :creator string?         ;; optional - creating application (defaults to "hiccup-pdf")
 :producer string?        ;; optional - producing application (defaults to "hiccup-pdf")
 :width pos?              ;; optional - default page width in points (defaults to 612)
 :height pos?             ;; optional - default page height in points (defaults to 792)
 :margins vector?}        ;; optional - default margins [left bottom right top] (defaults to [0 0 0 0])

;; Page
{:width pos?              ;; optional - page width in points (inherits from document)
 :height pos?             ;; optional - page height in points (inherits from document)
 :margins vector?}        ;; optional - page margins [left bottom right top] (inherits from document)
```

#### PDF Primitive Elements
```clojure
;; Rectangle
{:x number? :y number? :width pos? :height pos?}

;; Circle  
{:cx number? :cy number? :r (and number? #(>= % 0))}

;; Line
{:x1 number? :y1 number? :x2 number? :y2 number?}

;; Text
{:x number? :y number? :font string? :size pos?}

;; Path
{:d string?}

;; Group
{:transforms vector?} ;; optional - vector of transform operations
```

### Common Optional Attributes
- `:fill` - color string (named colors: "red", "green", "blue", "black", "white", "yellow", "cyan", "magenta" or hex: "#rrggbb")
- `:stroke` - color string  
- `:stroke-width` - positive number

### Page Size Reference

#### Common Page Dimensions (width × height in points)
- **Letter**: 612 × 792 (US Letter, 8.5" × 11")
- **Legal**: 612 × 1008 (US Legal, 8.5" × 14")
- **A4**: 595 × 842 (ISO A4, 210mm × 297mm)
- **A3**: 842 × 1191 (ISO A3, 297mm × 420mm)
- **Tabloid**: 792 × 1224 (US Tabloid, 11" × 17")
- **Ledger**: 1224 × 792 (US Ledger, 17" × 11")

#### Landscape Orientation
For landscape pages, swap the width and height values:
```clojure
;; Portrait A4
[:page {:width 595 :height 842} ...]

;; Landscape A4  
[:page {:width 842 :height 595} ...]
```

#### Units
- All dimensions are in PDF points (1/72 inch)
- To convert: inches × 72 = points
- Common margins: 72 points = 1 inch, 36 points = 0.5 inch

### Document Inheritance
Page elements inherit default values from their parent document:
```clojure
[:document {:width 612 :height 792 :margins [72 72 72 72]}
 [:page {}                           ;; Uses 612×792 with 72pt margins
  ...]
 [:page {:margins [50 50 50 50]}     ;; Uses 612×792 with custom margins
  ...]
 [:page {:width 842 :height 595}     ;; Custom size, inherits margins
  ...]]
```

## Success Criteria

### Content Stream Generation (`hiccup->pdf-ops`)
1. All primitive elements transform correctly to PDF operators
2. Nested groups with transforms work properly
3. Emoji text rendering supported
4. Graphics state management with proper q/Q operators
5. Transform matrix calculations work correctly
6. Color system supports named and hex colors with caching

### Document Generation (`hiccup->pdf-document`)
1. Complete PDF document structure generated correctly
2. Document metadata embedded properly (title, author, etc.)
3. Multiple pages with different sizes and orientations supported
4. Page inheritance from document defaults works correctly
5. Coordinate system automatically converted from web to PDF
6. Proper page object and content stream generation
7. Font resources managed correctly across pages

### General Requirements
1. Comprehensive test coverage for all functionality
2. Complete API documentation with examples
3. Error handling provides clear feedback for validation failures
4. Performance suitable for typical document sizes
5. Emoji support works in both content streams and documents
6. Memory efficiency with incremental processing
7. Clear separation between content stream and document generation APIs