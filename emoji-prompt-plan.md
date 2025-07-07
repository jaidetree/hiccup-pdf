# Emoji System Refactoring Implementation Plan

Based on the comprehensive specification in `emoji-spec.md`, I'll create a detailed blueprint broken down into small, safe, iterative steps that build on each other.

## High-Level Blueprint Analysis

The project requires:
1. **New `:image` element** (currently doesn't exist)
2. **New `:emoji` element** that transforms to `:image`
3. **Shortcode system** using `emojis.edn`
4. **Image caching** with atom-based storage
5. **Complete removal** of complex Unicode text parsing
6. **Migration** of existing functionality

## Step-by-Step Implementation Plan

### Phase 1: Foundation (Steps 1-4)
Core infrastructure without breaking existing functionality.

### Phase 2: Image Element (Steps 5-8) 
New `:image` element implementation with validation and rendering.

### Phase 3: Emoji Element (Steps 9-12)
New `:emoji` element that transforms to `:image` elements.

### Phase 4: Integration & Migration (Steps 13-16)
Connect everything together and remove old system.

---

## Detailed Implementation Prompts

### Step 1: Create Image Validation Infrastructure

```
Create comprehensive validation for a new `:image` element in the validation namespace. The `:image` element should support these required attributes: `:src` (non-empty string), `:width` (positive number), `:height` (positive number), `:x` (number), `:y` (number). Add a `validate-image-attributes` function that follows the same pattern as existing validation functions (like `validate-rect-attributes`). Update the main `validate-element` function to recognize `:image` as a valid element type. Create comprehensive unit tests covering all validation scenarios including missing attributes, invalid types, and edge cases. The validation should use the existing valhalla validation pattern for consistency. Do not implement any rendering functionality yet - this step is validation only.
```

### Step 2: Implement Basic Image Loading Infrastructure

```
Create a new namespace `hiccup-pdf.images` with basic PNG file loading functionality. Implement `load-image-file` function that takes a file path and returns a Node.js Buffer containing PNG data, with proper error handling for missing files. Add `get-png-dimensions` function that extracts width/height from PNG headers (assume 72x72 for Noto emoji as fallback). Create `validate-png-data` function for basic PNG file validation. Add comprehensive unit tests for all functions using mock files or test fixtures. This step focuses only on file loading and validation - no caching or PDF generation yet. Follow the existing code patterns and error handling strategies from the current codebase.
```

### Step 3: Create Image Caching System

```
Extend the `hiccup-pdf.images` namespace with an atom-based LRU image cache. Implement `create-image-cache` function that returns an atom with structure `{:items {} :order [] :config {:max-size 50 :max-memory-mb 10} :stats {:hits 0 :misses 0}}`. Add `cache-get`, `cache-put`, and `cache-clear` functions with proper LRU eviction. Implement `estimate-image-memory` for memory usage tracking. Add `load-image-cached` function that combines file loading with caching. Create comprehensive unit tests covering cache hits/misses, LRU eviction, memory limits, and edge cases. The cache should be efficient and thread-safe using ClojureScript atom operations. Do not integrate with PDF generation yet - focus on cache functionality and performance.
```

### Step 4: Implement Image to PDF Operators

```
Create the `image->pdf-ops` function in the core namespace that generates PDF operators for `:image` elements. The function should load images using the caching system, generate proper PDF XObject references, and create transformation matrices for positioning and scaling. Use PDF operators: `q` (save state), `cm` (transformation matrix), `/XObjectName Do` (draw), `Q` (restore state). Add the `:image` case to the `element->pdf-ops` dispatcher. Create comprehensive unit tests that verify correct PDF operator generation, proper transformation matrices, and integration with the caching system. Mock the image loading for tests to avoid file dependencies. This step completes the `:image` element implementation and should integrate seamlessly with existing element rendering.
```

### Step 5: Create Shortcode Configuration System

```
Create `emojis/emojis.edn` file with 50 common emoji shortcodes mapping to PNG filenames following the specification format. Implement `load-emoji-shortcodes` function that reads and parses the EDN file. Create `validate-shortcode` function that checks if a shortcode exists in the loaded mappings. Add `resolve-shortcode-to-path` function that converts shortcode keywords to full image file paths. Include comprehensive error handling for missing files, invalid EDN syntax, and file I/O errors. Create unit tests covering shortcode loading, validation, and path resolution. The shortcode system should be loaded lazily and cached for performance. Follow ClojureScript EDN parsing best practices and maintain consistency with existing configuration patterns.
```

### Step 6: Implement Emoji Element Validation

```
Add `:emoji` element validation to the validation namespace. Create `validate-emoji-attributes` function that validates required attributes: `:code` (shortcode keyword), `:size` (positive number), `:x` (number), `:y` (number). The `:code` attribute should be validated against loaded shortcode mappings. Update the main `validate-element` function to recognize `:emoji` as a valid element type. Create comprehensive unit tests covering valid shortcodes, invalid shortcodes, missing attributes, and edge cases. Add clear error messages that list available shortcodes when validation fails. This validation should integrate with the shortcode system from the previous step and follow existing validation patterns for consistency.
```

### Step 7: Implement Emoji to Image Transformation

```
Create the `emoji->pdf-ops` function in the core namespace that transforms `:emoji` elements to `:image` elements and delegates to image rendering. The transformation should: resolve shortcode to file path, convert `:size` to `:width` and `:height`, preserve `:x` and `:y` coordinates, and call `image->pdf-ops` with the transformed element. Add the `:emoji` case to the `element->pdf-ops` dispatcher. Create comprehensive unit tests covering shortcode resolution, attribute transformation, and integration with image rendering. Test error scenarios like invalid shortcodes and ensure proper error propagation. This step should reuse all existing `:image` functionality while providing the ergonomic `:emoji` interface.
```

### Step 8: Create Integration Test Suite

```
Create comprehensive integration tests that verify the complete `:emoji` and `:image` element pipeline. Test scenarios should include: single emoji elements, multiple emoji with different shortcodes, emoji mixed with other elements (text, rectangles, etc.), nested groups containing emoji, complex documents with many emoji, and error handling for missing files and invalid shortcodes. Add performance tests for caching efficiency and memory usage. Create test fixtures with actual PNG files for end-to-end testing. Verify that generated PDF operators are correct and that the existing element functionality (text, rectangles, circles, etc.) remains unaffected. This step ensures the new emoji system works correctly in real-world scenarios.
```

### Step 9: Update API Documentation

```
Update the API documentation (API.md) to include complete documentation for `:image` and `:emoji` elements. Document all attributes, provide usage examples, and show expected PDF operator output. Add the new elements to the supported elements table and create detailed examples showing emoji in various contexts (business documents, technical diagrams, etc.). Update the main README.md to highlight the new emoji capabilities. Create usage examples in EXAMPLES.md demonstrating practical emoji use cases. Ensure documentation is consistent with existing patterns and includes performance considerations and error handling guidance.
```

### Step 10: Plan Migration Strategy

```
Create a comprehensive migration plan document that outlines how to transition from the old Unicode text parsing system to the new explicit `:emoji` elements. Document the breaking changes, provide before/after code examples, and create migration utilities if needed. Identify all files that need to be modified or removed (text_processing.cljs, emoji.cljs parsing functions, etc.). Create a checklist for safely removing the old system while preserving compatibility. Document performance improvements and new capabilities. This step prepares for the final migration phase and ensures no functionality is lost during the transition.
```

### Step 11: Remove Legacy Text Processing

```
Safely remove the legacy emoji parsing system from text processing. Remove Unicode emoji detection from text elements, delete complex parsing functions in emoji.cljs that are no longer needed, remove text_processing.cljs functions related to emoji segmentation, and update text element rendering to handle only plain text. Preserve all non-emoji text functionality and ensure existing text elements continue to work correctly. Update all existing tests to remove emoji parsing expectations. Create tests to verify that text elements now render emoji characters as regular text (fallback behavior). This step simplifies the codebase significantly while maintaining backward compatibility for plain text.
```

### Step 12: Final Integration and Testing

```
Perform final integration testing and cleanup. Update all example files to use the new `:emoji` element syntax instead of embedded emoji in text. Run the complete test suite to ensure no regressions. Test the end-to-end workflow: run `npx nbb ./triangle_with_save.cljs`, then `./scripts/pdf2svg triangle_test.pdf` to verify PDF generation works correctly. Update any remaining documentation inconsistencies. Verify performance meets the specification requirements (< 5ms per emoji cached, < 100MB memory for 200 cached images). Create a final verification report documenting successful implementation of all specification requirements. This step ensures the project is complete, tested, and ready for production use.
```

---

## Implementation Notes

### Key Principles
- **Incremental Progress**: Each step builds on previous steps without big complexity jumps
- **Strong Testing**: Every step includes comprehensive unit and integration tests
- **No Orphaned Code**: All code is integrated and functional at each step
- **Backward Compatibility**: Existing functionality preserved until migration phase

### Dependencies Between Steps
- Steps 1-3: Can be done in parallel (validation, loading, caching)
- Step 4: Requires steps 1-3 (completes `:image` element)
- Steps 5-6: Can be done in parallel (shortcodes and validation)
- Step 7: Requires steps 4-6 (completes `:emoji` element)
- Steps 8-9: Require step 7 (testing and documentation)
- Steps 10-12: Sequential migration and cleanup

### Success Criteria for Each Step
1. All tests pass
2. No regressions in existing functionality
3. Code follows established patterns
4. Documentation is updated
5. Integration points are tested

This plan provides 12 well-scoped prompts that will systematically implement the emoji system refactoring while maintaining code quality and project stability throughout the process.