# Hiccup-PDF Project Todos

## Phase 1: Foundation & Setup

### Step 1: Project Structure & Dependencies
- [ ] Create deps.edn with ClojureScript dependencies
- [ ] Create basic directory structure (src/, test/)
- [ ] Create package.json for node-babashka compatibility
- [ ] Create basic README.md with project description
- [ ] Create .gitignore file
- [ ] Verify project structure is compatible with node-babashka environment

### Step 2: Core Namespace & Function Skeleton
- [ ] Create core namespace `hiccup-pdf.core`
- [ ] Implement main function `hiccup->pdf-ops` with signature `(hiccup->pdf-ops hiccup-vector & [options])`
- [ ] Add basic docstring explaining the function's purpose
- [ ] Create placeholder implementation that returns empty string
- [ ] Add basic namespace requires and declarations
- [ ] Create simple smoke test that verifies the function exists
- [ ] Ensure smoke test can be called without errors
- [ ] Verify test passes with placeholder implementation

### Step 3: Basic Test Infrastructure
- [ ] Create test namespace `hiccup-pdf.core-test`
- [ ] Set up basic test functions using cljs.test
- [ ] Create test runner script that can be executed with node-babashka
- [ ] Add basic tests for the main function (existence, basic call patterns)
- [ ] Ensure tests can run and pass with current placeholder implementation
- [ ] Verify test infrastructure is working properly

## Phase 2: Validation Foundation

### Step 4: Valhalla Integration Setup
- [ ] Add valhalla dependency to deps.edn
- [ ] Create validation namespace `hiccup-pdf.validation`
- [ ] Set up basic validation infrastructure
- [ ] Create simple validation schemas for hiccup structure
- [ ] Add tests to verify validation setup is working
- [ ] Document requirements if valhalla needs updates for nbb compatibility
- [ ] Implement basic validation fallback if valhalla isn't compatible

### Step 5: Basic Hiccup Structure Validation
- [ ] Implement validation that input is a vector
- [ ] Implement validation that first element is a keyword (element type)
- [ ] Implement validation that second element is a map (attributes)
- [ ] Validate basic hiccup structure without element-specific rules
- [ ] Add comprehensive tests for structure validation
- [ ] Ensure validation errors are clear and descriptive
- [ ] Test error messages for malformed hiccup structures

## Phase 3: Simple Element Implementation

### Step 6: Rectangle Element Implementation
- [ ] Add validation rules for :rect elements (x, y, width, height required)
- [ ] Implement basic rectangle PDF operator generation
- [ ] Support only required attributes first (x, y, width, height)
- [ ] Generate correct PDF operators for rectangle drawing
- [ ] Add comprehensive unit tests for rectangle transformation
- [ ] Test both valid inputs and validation errors
- [ ] Verify PDF operator output format is correct

### Step 7: Rectangle Styling Support
- [ ] Add support for :fill attribute (color strings)
- [ ] Add support for :stroke attribute (color strings)
- [ ] Add support for :stroke-width attribute
- [ ] Implement proper PDF operators for fill and stroke
- [ ] Add tests for all styling combinations
- [ ] Ensure validation catches invalid color values
- [ ] Test edge cases for styling attributes

### Step 8: Line Element Implementation
- [ ] Add validation rules for :line elements (x1, y1, x2, y2 required)
- [ ] Implement line PDF operator generation
- [ ] Support stroke and stroke-width attributes
- [ ] Generate correct PDF operators for line drawing
- [ ] Add comprehensive unit tests for line transformation
- [ ] Test validation for required and optional attributes
- [ ] Verify line PDF operators follow established patterns

## Phase 4: Circular and Path Elements

### Step 9: Circle Element Implementation
- [ ] Add validation rules for :circle elements (cx, cy, r required)
- [ ] Implement circle PDF operator generation using curve approximation
- [ ] Support fill, stroke, and stroke-width attributes
- [ ] Generate correct PDF operators for circle drawing
- [ ] Add comprehensive unit tests for circle transformation
- [ ] Test edge cases like zero radius
- [ ] Verify curve approximation produces accurate circles

### Step 10: Path Element Implementation
- [ ] Add validation rules for :path elements (d attribute required)
- [ ] Implement basic path PDF operator generation
- [ ] Support SVG-style path data parsing
- [ ] Support fill, stroke, and stroke-width attributes
- [ ] Generate correct PDF operators for path drawing
- [ ] Add comprehensive unit tests for path transformation
- [ ] Test various path commands (M, L, C, Z, etc.)
- [ ] Handle complex path data edge cases

## Phase 5: Text and Font Support

### Step 11: Basic Text Element Implementation
- [ ] Add validation rules for :text elements (x, y, font, size required)
- [ ] Implement text PDF operator generation (BT/ET blocks)
- [ ] Support font name and size attributes
- [ ] Generate correct PDF operators for text drawing
- [ ] Add comprehensive unit tests for text transformation
- [ ] Test font name validation and text positioning
- [ ] Verify text PDF operators are properly formatted

### Step 12: Text Styling and Emoji Support
- [ ] Add support for :fill attribute (text color)
- [ ] Implement emoji character support in text content
- [ ] Test emoji rendering with various fonts
- [ ] Add tests for text styling combinations
- [ ] Ensure proper PDF text operator generation
- [ ] Test edge cases like empty text, special characters
- [ ] Verify emoji characters are properly encoded in PDF

## Phase 6: Graphics State and Transforms

### Step 13: Basic Group Element Implementation
- [ ] Add validation rules for :g elements
- [ ] Implement graphics state save/restore (q/Q operators)
- [ ] Support nested hiccup elements within groups
- [ ] Generate correct PDF operators for grouping
- [ ] Add comprehensive unit tests for group transformation
- [ ] Test nested group structures
- [ ] Verify proper graphics state management

### Step 14: Transform Implementation
- [ ] Add validation for transform vectors [[:translate [x y]] [:rotate degrees]]
- [ ] Implement PDF transformation matrix generation
- [ ] Support translate, rotate, and scale transforms
- [ ] Apply transforms before rendering child elements
- [ ] Add comprehensive unit tests for transform combinations
- [ ] Test transform composition and nesting
- [ ] Verify transformation matrices are calculated correctly

### Step 15: Nested Groups and Complex Transforms
- [ ] Ensure proper graphics state nesting
- [ ] Test complex transform combinations
- [ ] Verify coordinate system transformations work correctly
- [ ] Add integration tests for deeply nested groups
- [ ] Test transform inheritance and isolation
- [ ] Ensure proper PDF operator ordering
- [ ] Verify complex nested structures work properly

## Phase 7: Integration and Error Handling

### Step 16: Comprehensive Error Handling
- [ ] Add detailed error messages for all validation failures
- [ ] Implement error context (element type, attribute name)
- [ ] Add proper error types and error data
- [ ] Test error handling for all element types
- [ ] Ensure errors are thrown immediately (incremental processing)
- [ ] Add error handling integration tests
- [ ] Verify error messages are clear and actionable

### Step 17: Integration Tests
- [ ] Test complex hiccup documents with multiple elements
- [ ] Test nested groups with transforms
- [ ] Test mixed element types in single documents
- [ ] Test error conditions in complex documents
- [ ] Verify proper PDF operator generation for complex cases
- [ ] Test performance with larger documents
- [ ] Ensure all elements work together seamlessly

## Phase 8: Documentation and Polish

### Step 18: API Documentation
- [ ] Add detailed docstrings to all public functions
- [ ] Include usage examples in docstrings
- [ ] Document all element types and their attributes
- [ ] Add examples of PDF operator output
- [ ] Create element reference documentation
- [ ] Set up documentation generation from code comments
- [ ] Verify documentation is comprehensive and accurate

### Step 19: Performance and Optimization
- [ ] Profile memory usage during processing
- [ ] Optimize string concatenation for PDF operators
- [ ] Ensure incremental processing works efficiently
- [ ] Add performance tests for large documents
- [ ] Optimize validation overhead
- [ ] Document performance characteristics
- [ ] Verify performance meets spec requirements

### Step 20: Final Integration and Polish
- [ ] Ensure all elements work together seamlessly
- [ ] Add final integration tests
- [ ] Verify all spec requirements are met
- [ ] Test emoji support thoroughly
- [ ] Ensure proper error handling throughout
- [ ] Add final documentation and examples
- [ ] Prepare for release
- [ ] Verify project is ready for production use

## Testing Requirements (for each step)

### Unit Tests
- [ ] Test the specific functionality being added
- [ ] Test error conditions and validation
- [ ] Test edge cases and boundary conditions
- [ ] Verify correct PDF operator generation

### Integration Tests
- [ ] Test how new functionality works with existing code
- [ ] Test real-world usage scenarios
- [ ] Verify no regressions in existing functionality

### Error Tests
- [ ] Test all error conditions
- [ ] Verify error messages are clear and helpful
- [ ] Test validation for all input types

### Example Tests
- [ ] Test with real-world hiccup examples
- [ ] Test complex document structures
- [ ] Test performance with realistic data

## Success Criteria (for each step)

- [ ] All tests pass
- [ ] New functionality works as specified
- [ ] No regressions in existing functionality
- [ ] Code follows established patterns
- [ ] Documentation is updated
- [ ] Error handling is comprehensive
- [ ] Performance requirements are met

## Dependencies and Ordering

### Sequential Dependencies
- Steps 1-3 must be completed sequentially
- Steps 4-5 can be done in parallel after step 3
- Steps 6-12 can be done in parallel after step 5
- Steps 13-15 require steps 6-12 to be complete
- Steps 16-20 require all previous steps

### Parallel Work Opportunities
- Rectangle, line, circle, path, and text elements can be developed in parallel
- Unit tests and integration tests can be developed in parallel
- Documentation can be developed alongside implementation

## Quality Assurance

### Code Quality
- [ ] Follow ClojureScript best practices
- [ ] Maintain consistent code style
- [ ] Use appropriate data structures
- [ ] Minimize memory allocation
- [ ] Ensure efficient string operations

### Test Quality
- [ ] Achieve comprehensive test coverage
- [ ] Test all error conditions
- [ ] Test edge cases and boundary conditions
- [ ] Test performance characteristics
- [ ] Verify PDF operator correctness

### Documentation Quality
- [ ] Clear and accurate API documentation
- [ ] Comprehensive usage examples
- [ ] Element reference documentation
- [ ] Error handling documentation
- [ ] Performance characteristics documentation