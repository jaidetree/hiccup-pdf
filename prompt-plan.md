# Hiccup-PDF Implementation Plan

## Overview
This document provides a step-by-step implementation plan for the hiccup-pdf library, broken down into small, iterative chunks that build on each other. Each step is designed to be implemented safely with strong testing while moving the project forward incrementally.

## Phase 1: Foundation & Setup

### Step 1: Project Structure & Dependencies ✅ COMPLETED
**Goal**: Set up the basic project structure and dependencies

```
Set up the initial ClojureScript project structure for node-babashka. Create:
- deps.edn with ClojureScript dependencies
- Basic directory structure (src/, test/)
- Package.json for node-babashka compatibility
- Basic README.md with project description
- .gitignore file

The project should be named "hiccup-pdf" and set up for ClojureScript development in node-babashka environment. Include basic ClojureScript dependencies but don't add any external libraries yet.
```

**Status**: Created nbb.edn with paths configuration, src/ and test/ directories, README.md, and .gitignore file. Project structure is ready for ClojureScript development with node-babashka.

### Step 2: Core Namespace & Function Skeleton ✅ COMPLETED
**Goal**: Create the main namespace and function signature

```
Create the core namespace `hiccup-pdf.core` with:
- Main function `hiccup->pdf-ops` with signature `(hiccup->pdf-ops hiccup-vector & [options])`
- Basic docstring explaining the function's purpose
- Placeholder implementation that returns empty string
- Basic namespace requires and declarations

Add a simple smoke test that verifies the function exists and can be called without errors. The test should pass with the placeholder implementation.
```

**Status**: Created hiccup-pdf.core namespace with main function, comprehensive docstring, placeholder implementation, and smoke tests. Updated nbb.edn to include test path. All tests passing.

### Step 3: Basic Test Infrastructure ✅ COMPLETED
**Goal**: Set up testing framework and basic test structure

```
Set up a simple test runner and basic test infrastructure:
- Create test namespace `hiccup-pdf.core-test`
- Set up basic test functions using cljs.test
- Create test runner script that can be executed with node-babashka
- Add basic tests for the main function (existence, basic call patterns)
- Ensure tests can run and pass with current placeholder implementation

Focus on getting the test infrastructure working before adding complex logic.
```

**Status**: Created comprehensive test infrastructure with test_runner.cljs, expanded test suite with 3 test functions covering basic function calls and signature variations, added npm test script. All tests pass (3 tests, 12 assertions).

## Phase 2: Validation Foundation

### Step 4: Valhalla Integration Setup ✅ COMPLETED
**Goal**: Integrate valhalla validation library

```
Integrate the valhalla validation library into the project:
- Add valhalla dependency to deps.edn
- Create validation namespace `hiccup-pdf.validation`
- Set up basic validation infrastructure
- Create simple validation schemas for hiccup structure
- Add tests to verify validation setup is working

If valhalla needs updates for nbb compatibility, document the requirements but use basic validation for now.
```

**Status**: Integrated valhalla validation library with nbb.edn dependency, created hiccup-pdf.validation namespace with basic validation functions, set up validation infrastructure using v/chain for hiccup structure validation, added comprehensive tests. All tests passing (6 tests, 22 assertions).

### Step 5: Basic Hiccup Structure Validation ✅ COMPLETED
**Goal**: Implement basic hiccup vector validation

```
Implement basic validation for hiccup vector structure:
- Validate that input is a vector
- Validate that first element is a keyword (element type)
- Validate that second element is a map (attributes)
- Validate basic hiccup structure without element-specific rules
- Add comprehensive tests for structure validation
- Ensure validation errors are clear and descriptive

This step focuses on the hiccup structure itself, not the PDF-specific elements.
```

**Status**: Implemented comprehensive hiccup structure validation with v/chain validators for vector structure, element type keywords, and attributes maps. Added validation functions for element types and attributes with clear error messages. All validation tests passing with comprehensive test coverage.

## Phase 3: Simple Element Implementation

### Step 6: Rectangle Element Implementation ✅ COMPLETED
**Goal**: Implement the first concrete PDF primitive

```
Implement rectangle element transformation:
- Add validation rules for :rect elements (x, y, width, height required)
- Implement basic rectangle PDF operator generation
- Support only required attributes first (x, y, width, height)
- Generate correct PDF operators for rectangle drawing
- Add comprehensive unit tests for rectangle transformation
- Test both valid inputs and validation errors

Focus on getting one element working correctly before moving to others.
```

**Status**: Implemented complete rectangle element support with validate-rect-attributes function for required attributes (x, y, width, height), rect->pdf-ops function generating correct PDF operators ("x y width height re\nf"), element->pdf-ops dispatcher, and comprehensive unit tests for both valid inputs and validation errors. All tests passing (8 tests, 32 assertions). Code passes linting with no warnings.

### Step 7: Rectangle Styling Support ✅ COMPLETED
**Goal**: Add styling support to rectangles

```
Extend rectangle implementation to support optional styling:
- Add support for :fill attribute (color strings)
- Add support for :stroke attribute (color strings)
- Add support for :stroke-width attribute
- Implement proper PDF operators for fill and stroke
- Add tests for all styling combinations
- Ensure validation catches invalid color values

This step adds complexity to the rectangle implementation while keeping it focused.
```

**Status**: Implemented complete rectangle styling support with validate-color function supporting named colors (red, green, blue, black, white, yellow, cyan, magenta) and hex colors (#rrggbb format), color->pdf-color conversion function, enhanced rect->pdf-ops with proper PDF operators (rg/RG for fill/stroke colors, w for stroke width, f/S/B for fill/stroke/both), and comprehensive test coverage for all styling combinations. All tests passing (11 tests, 49 assertions). Code passes linting with no warnings.

### Step 8: Line Element Implementation ✅ COMPLETED
**Goal**: Implement line primitive

```
Implement line element transformation:
- Add validation rules for :line elements (x1, y1, x2, y2 required)
- Implement line PDF operator generation
- Support stroke and stroke-width attributes
- Generate correct PDF operators for line drawing
- Add comprehensive unit tests for line transformation
- Test validation for required and optional attributes

Build on the patterns established with rectangles.
```

**Status**: Implemented complete line element support with validate-line-attributes function for required coordinates (x1, y1, x2, y2) and optional styling (stroke, stroke-width), line->pdf-ops function generating correct PDF operators ("x1 y1 m\nx2 y2 l\nS" with optional stroke width and color), integrated with element->pdf-ops dispatcher, and comprehensive unit tests for both valid inputs and validation errors. All tests passing (14 tests, 65 assertions). Code passes linting with no warnings.

## Phase 4: Circular and Path Elements

### Step 9: Circle Element Implementation
**Goal**: Implement circle primitive

```
Implement circle element transformation:
- Add validation rules for :circle elements (cx, cy, r required)
- Implement circle PDF operator generation using curve approximation
- Support fill, stroke, and stroke-width attributes
- Generate correct PDF operators for circle drawing
- Add comprehensive unit tests for circle transformation
- Test edge cases like zero radius

Circles require more complex PDF operators than rectangles and lines.
```

### Step 10: Path Element Implementation
**Goal**: Implement path primitive

```
Implement path element transformation:
- Add validation rules for :path elements (d attribute required)
- Implement basic path PDF operator generation
- Support SVG-style path data parsing
- Support fill, stroke, and stroke-width attributes
- Generate correct PDF operators for path drawing
- Add comprehensive unit tests for path transformation
- Test various path commands (M, L, C, Z, etc.)

This is the most complex primitive element.
```

## Phase 5: Text and Font Support

### Step 11: Basic Text Element Implementation
**Goal**: Implement text primitive without emoji

```
Implement basic text element transformation:
- Add validation rules for :text elements (x, y, font, size required)
- Implement text PDF operator generation (BT/ET blocks)
- Support font name and size attributes
- Generate correct PDF operators for text drawing
- Add comprehensive unit tests for text transformation
- Test font name validation and text positioning

Start with simple text before adding emoji support.
```

### Step 12: Text Styling and Emoji Support
**Goal**: Add text styling and emoji support

```
Extend text implementation with styling and emoji:
- Add support for :fill attribute (text color)
- Implement emoji character support in text content
- Test emoji rendering with various fonts
- Add tests for text styling combinations
- Ensure proper PDF text operator generation
- Test edge cases like empty text, special characters

This step adds the required emoji functionality.
```

## Phase 6: Graphics State and Transforms

### Step 13: Basic Group Element Implementation
**Goal**: Implement group element without transforms

```
Implement basic group element support:
- Add validation rules for :g elements
- Implement graphics state save/restore (q/Q operators)
- Support nested hiccup elements within groups
- Generate correct PDF operators for grouping
- Add comprehensive unit tests for group transformation
- Test nested group structures

Focus on grouping without transforms first.
```

### Step 14: Transform Implementation
**Goal**: Implement structured transforms

```
Implement transform support for groups:
- Add validation for transform vectors [[:translate [x y]] [:rotate degrees]]
- Implement PDF transformation matrix generation
- Support translate, rotate, and scale transforms
- Apply transforms before rendering child elements
- Add comprehensive unit tests for transform combinations
- Test transform composition and nesting

This is complex - build incrementally.
```

### Step 15: Nested Groups and Complex Transforms
**Goal**: Complete group and transform functionality

```
Complete group and transform implementation:
- Ensure proper graphics state nesting
- Test complex transform combinations
- Verify coordinate system transformations work correctly
- Add integration tests for deeply nested groups
- Test transform inheritance and isolation
- Ensure proper PDF operator ordering

This step completes the graphics state management.
```

## Phase 7: Integration and Error Handling

### Step 16: Comprehensive Error Handling
**Goal**: Implement robust error handling

```
Implement comprehensive error handling:
- Add detailed error messages for all validation failures
- Implement error context (element type, attribute name)
- Add proper error types and error data
- Test error handling for all element types
- Ensure errors are thrown immediately (incremental processing)
- Add error handling integration tests

Focus on making errors clear and actionable.
```

### Step 17: Integration Tests
**Goal**: Add comprehensive integration tests

```
Create comprehensive integration tests:
- Test complex hiccup documents with multiple elements
- Test nested groups with transforms
- Test mixed element types in single documents
- Test error conditions in complex documents
- Verify proper PDF operator generation for complex cases
- Test performance with larger documents

This step ensures everything works together correctly.
```

## Phase 8: Documentation and Polish

### Step 18: API Documentation
**Goal**: Complete API documentation

```
Create comprehensive API documentation:
- Add detailed docstrings to all public functions
- Include usage examples in docstrings
- Document all element types and their attributes
- Add examples of PDF operator output
- Create element reference documentation
- Set up documentation generation from code comments

This step makes the library usable by others.
```

### Step 19: Performance and Optimization
**Goal**: Optimize performance and memory usage

```
Optimize library performance:
- Profile memory usage during processing
- Optimize string concatenation for PDF operators
- Ensure incremental processing works efficiently
- Add performance tests for large documents
- Optimize validation overhead
- Document performance characteristics

Focus on the performance requirements from the spec.
```

### Step 20: Final Integration and Polish
**Goal**: Complete the library implementation

```
Complete final integration and polish:
- Ensure all elements work together seamlessly
- Add final integration tests
- Verify all spec requirements are met
- Test emoji support thoroughly
- Ensure proper error handling throughout
- Add final documentation and examples
- Prepare for release

This step ties everything together and ensures completeness.
```

## Testing Strategy for Each Step

Each step should include:
1. **Unit Tests**: Test the specific functionality being added
2. **Integration Tests**: Test how the new functionality works with existing code
3. **Error Tests**: Test error conditions and validation
4. **Example Tests**: Test with real-world examples

## Success Criteria

Each step is complete when:
1. All tests pass
2. New functionality works as specified
3. No regressions in existing functionality
4. Code follows established patterns
5. Documentation is updated
6. Error handling is comprehensive

## Dependencies Between Steps

- Steps 1-3 must be completed sequentially
- Steps 4-5 can be done in parallel after step 3
- Steps 6-12 can be done in parallel after step 5
- Steps 13-15 require steps 6-12 to be complete
- Steps 16-20 require all previous steps

This plan ensures incremental progress with strong testing at each stage, building complexity gradually while maintaining working functionality throughout the development process.