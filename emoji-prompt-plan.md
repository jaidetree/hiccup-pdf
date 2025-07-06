# Emoji PNG Support Implementation Blueprint

## Detailed Step-by-Step Blueprint

Based on the emoji specification, here's a comprehensive implementation plan broken down into manageable, testable increments:

### Phase 1: Foundation & Unicode Processing
1. **Unicode Detection & Processing** - Build core emoji detection and codepoint extraction
2. **Filename Mapping** - Convert Unicode to Noto emoji filenames
3. **Text Segmentation** - Split text into emoji and non-emoji segments

### Phase 2: Image Management
4. **File System Integration** - Load PNG files from local directory
5. **Image Caching** - Implement memory cache for loaded images
6. **Error Handling** - Graceful fallback when images missing

### Phase 3: PDF Integration
7. **PDF Image Objects** - Generate XObject streams for PNG images
8. **Mixed Content Rendering** - Combine text and image PDF operators
9. **Resource Management** - Update page resources with image references

### Phase 4: Integration & Configuration
10. **API Integration** - Wire into existing text processing pipeline
11. **Configuration System** - Add emoji options and feature flags
12. **Document-Level Support** - Integrate with full document generation

## Iterative Implementation Chunks

### First Iteration: Core Components (Steps 1-3)
- Unicode processing foundations
- Basic emoji detection
- Text segmentation logic

### Second Iteration: Image Handling (Steps 4-6)
- File system operations
- Caching infrastructure
- Error handling patterns

### Third Iteration: PDF Generation (Steps 7-9)
- PDF image object creation
- Operator generation
- Resource dictionary updates

### Fourth Iteration: Integration (Steps 10-12)
- API wiring
- Configuration options
- End-to-end functionality

## Small Step Breakdown

Each step is sized to be:
- **Testable**: Has clear inputs/outputs and success criteria
- **Incremental**: Builds on previous steps without big jumps
- **Isolated**: Can be developed and tested independently
- **Integrated**: Gets wired into the existing system immediately

---

# Implementation Prompts

## Step 1: Unicode Detection & Processing ✅ COMPLETED

### Context
We're implementing emoji image support for a hiccup-pdf library. The first step is building the foundation for Unicode emoji detection and processing. This step focuses on correctly identifying emoji characters in text and extracting their Unicode codepoints, handling complex cases like surrogate pairs.

### Prompt 1A: Basic Unicode Processing ✅ COMPLETED

```
Create a new ClojureScript namespace `hiccup-pdf.emoji` that handles Unicode emoji processing. Implement the following core functions with comprehensive test coverage:

1. `extract-emoji-codepoints` - Takes an emoji character string and returns vector of Unicode codepoints, handling surrogate pairs correctly
2. `emoji?` - Predicate function that determines if a character is an emoji using Unicode ranges
3. `surrogate-pair?` - Helper function to detect UTF-16 surrogate pairs

Requirements:
- Handle basic emoji like 💡 (U+1F4A1) and 🎯 (U+1F3AF)
- Correctly process surrogate pairs (high surrogate 0xD800-0xDBFF, low surrogate 0xDC00-0xDFFF)
- Support emoji in ranges: 0x1F300-0x1F9FF, 0x2600-0x26FF, 0x2700-0x27BF
- Return codepoints as decimal integers, not hex
- Include comprehensive unit tests covering edge cases

Example expected behavior:
- "💡" -> [127825] (0x1F4A1 in decimal)
- "🎯" -> [127919] (0x1F3AF in decimal)
- "a" -> [] (not an emoji)

Use ClojureScript's JavaScript interop for character code extraction. Create test file `hiccup-pdf.emoji-test` with at least 10 test cases covering various emoji types and edge cases.
```

### Prompt 1B: Text Emoji Detection ✅ COMPLETED

```
Extend the `hiccup-pdf.emoji` namespace with text processing functions that can detect and locate emoji within text strings:

1. `detect-emoji-in-text` - Scans text and returns vector of maps with emoji info: {:char "💡" :start-index 5 :end-index 7 :codepoints [127825]}
2. `contains-emoji?` - Simple predicate to check if text contains any emoji
3. `split-by-emoji` - Splits text into alternating segments of regular text and emoji

Requirements:
- Handle mixed text like "Hello 💡 world 🎯!"
- Correctly track character indices (accounting for surrogate pairs taking 2 char positions)
- Preserve exact character boundaries
- Handle edge cases: empty strings, emoji-only text, no emoji text
- Performance: should handle text up to 1000 characters efficiently

Build comprehensive tests covering:
- Empty strings and null inputs
- Text with no emoji
- Text with only emoji
- Mixed content with multiple emoji
- Emoji at start, middle, and end positions
- Adjacent emoji characters

Use the functions from Step 1A as building blocks. All tests should pass and demonstrate correct boundary detection.
```

## Step 2: Filename Mapping ✅ COMPLETED

### Context
Now that we can detect and extract emoji codepoints, we need to map them to the corresponding Noto emoji PNG filenames in the local directory structure.

### Prompt 2: Unicode to Filename Conversion ✅ COMPLETED

```
Extend the `hiccup-pdf.emoji` namespace with filename mapping functionality for Noto emoji files:

1. `unicode-to-filename` - Converts codepoint(s) to Noto emoji filename format
2. `build-emoji-file-map` - Creates a mapping from common emoji codepoints to their filenames
3. `emoji-filename` - Main function that takes emoji character and returns corresponding PNG filename

Requirements:
- Follow Noto emoji naming convention: "emoji_u{codepoint}.png" (lowercase hex)
- Handle single codepoints: 127825 -> "emoji_u1f4a1.png" 
- Support the existing emoji in the local directory: 💡🎯⚠️✅•
- Include fallback handling for unmapped emoji
- Validate that generated filenames match actual files in emojis/noto-72/

Create a predefined map for the known emoji:
- 💡 (127825/0x1F4A1) -> "emoji_u1f4a1.png"
- 🎯 (127919/0x1F3AF) -> "emoji_u1f3af.png" 
- ⚠️ (9888/0x26A0) -> "emoji_u26a0.png"
- ✅ (9989/0x2705) -> "emoji_u2705.png"
- • (8226/0x2022) -> "emoji_u2022.png"

Build tests that:
- Verify correct filename generation for known emoji
- Test fallback behavior for unknown emoji
- Validate filename format compliance
- Check round-trip consistency (emoji -> filename -> should be findable)

Include integration test that checks if generated filenames actually exist in the emojis/noto-72/ directory.
```

## Step 3: Text Segmentation ✅ COMPLETED

### Context
With emoji detection and filename mapping complete, we need to segment text into alternating text and emoji parts for mixed content rendering.

### Prompt 3: Text Segmentation for Mixed Content ✅ COMPLETED

```
Create a new namespace `hiccup-pdf.text-processing` that handles segmentation of text with emoji for mixed content rendering:

1. `segment-text` - Takes text string and returns vector of segment maps: {:type :text/:emoji :content "..." :start-idx N :end-idx N}
2. `prepare-mixed-content` - Processes segments and adds rendering metadata like widths and positions
3. `validate-segments` - Ensures segmentation is complete and non-overlapping

Requirements:
- Segment text into consecutive chunks of pure text or single emoji
- Preserve all characters - no content should be lost in segmentation
- Each segment should know its original position in the source text
- Handle edge cases: empty strings, emoji-only, text-only
- Segments should be in source text order

Expected segmentation examples:
- "Hello 💡 world" -> [{:type :text :content "Hello " :start-idx 0 :end-idx 6}
                       {:type :emoji :content "💡" :start-idx 6 :end-idx 8}  
                       {:type :text :content " world" :start-idx 8 :end-idx 14}]

Build comprehensive tests:
- Empty and null inputs
- Pure text (no emoji)
- Pure emoji (multiple consecutive)
- Mixed content with various patterns
- Validation that segments cover entire input without gaps/overlaps
- Performance test with longer texts (500+ characters)

Use the emoji detection functions from previous steps. All segments should reconstruct the original text when concatenated.
```

## Step 4: File System Integration ✅ COMPLETED

### Context
Now we need to load the actual PNG image files from the local emoji directory, handling file system operations and basic error cases.

### Prompt 4: PNG Image Loading ✅ COMPLETED

```
Create a new namespace `hiccup-pdf.images` for handling PNG image file operations:

1. `load-png-file` - Loads PNG file from emojis/noto-72/ directory, returns Buffer or nil
2. `emoji-file-exists?` - Checks if emoji PNG file exists for given emoji character
3. `load-emoji-image` - Main function that loads emoji image with error handling
4. `get-image-dimensions` - Extracts width/height from PNG file (should be 72x72 for Noto)

Requirements:
- Use Node.js fs module for file operations (require ["node:fs" :as fs])
- Handle file path construction using path.join for cross-platform compatibility  
- Graceful error handling for missing files, permission errors, corrupted files
- Return consistent data structure: {:buffer Buffer :width 72 :height 72 :success true}
- Add validation that loaded images are valid PNG format

Implementation details:
- Base directory: "emojis/noto-72/"
- Use synchronous file operations for now (async in later steps)
- PNG dimension reading can be basic (assume 72x72, verify if possible)
- Comprehensive error handling with descriptive error messages

Build tests:
- Loading existing emoji files (💡, 🎯, ⚠️, ✅, •)
- Handling missing files gracefully
- Invalid file paths and permissions
- Corrupted/invalid PNG data simulation
- Performance test loading multiple files
- Cross-platform path handling

Include setup/teardown for test files and mock scenarios for error conditions.
```

## Step 5: Image Caching System ✅ COMPLETED

### Context
Loading images from disk is expensive, so we need a caching system to store loaded images in memory during document generation.

### Prompt 5: Image Caching Infrastructure ✅ COMPLETED

```
Extend the `hiccup-pdf.images` namespace with a caching system for loaded emoji images:

1. `create-image-cache` - Creates new cache instance with configurable size limits
2. `cache-get` - Retrieves cached image or nil if not found
3. `cache-put` - Stores image in cache with LRU eviction
4. `cache-clear` - Clears cache contents
5. `load-emoji-image-cached` - Main function combining file loading with caching

Requirements:
- Implement LRU (Least Recently Used) cache eviction
- Configurable cache size (default 50 images)
- Memory usage tracking (estimate ~3-5KB per 72x72 PNG)
- Cache key format: emoji character string (e.g. "💡")
- Thread-safe operations using Clojure atoms
- Cache statistics: hits, misses, evictions

Cache data structure suggestion:
```clojure
{:items {"💡" {:buffer #<Buffer> :width 72 :height 72 :timestamp 1234567890}}
 :order ["💡" "🎯" ...]  ; LRU order, most recent last
 :config {:max-size 50 :max-memory-mb 10}
 :stats {:hits 0 :misses 0 :evictions 0}}
```

Build comprehensive tests:
- Basic cache operations (put/get/clear)
- LRU eviction when cache exceeds size limit
- Cache hit/miss statistics tracking
- Memory usage estimation accuracy
- Concurrent access simulation
- Cache performance vs direct file loading

Include integration test showing 10x+ performance improvement for cached emoji vs file system access.
```

## Step 6: Error Handling & Fallback ✅ COMPLETED

### Context
When emoji images can't be loaded, we need graceful fallback strategies that maintain document generation without breaking.

### Prompt 6: Error Handling and Fallback Strategies ✅ COMPLETED

```
Extend the `hiccup-pdf.images` namespace with comprehensive error handling and fallback mechanisms:

1. `handle-image-error` - Centralized error handling with configurable fallback strategies
2. `fallback-to-hex` - Falls back to existing hex string encoding for emoji
3. `fallback-to-placeholder` - Creates placeholder text like "[emoji]"
4. `validate-image-data` - Validates loaded PNG data integrity
5. `emoji-image-with-fallback` - Main function that attempts image loading with graceful fallback

Requirements:
- Support multiple fallback strategies: :hex-string, :placeholder, :skip, :error
- Integrate with existing hex string encoding from `hiccup-pdf.core`
- Log warnings for missing files but don't break document generation
- Validate PNG file integrity (basic checks: file size, PNG header)
- Performance: fallback should be as fast as normal text rendering

Fallback strategy behavior:
- `:hex-string` - Use existing hex encoding (e.g. <3DA1>)
- `:placeholder` - Render text like "[💡]" 
- `:skip` - Render empty string (skip emoji entirely)
- `:error` - Throw exception (for debugging)

Error scenarios to handle:
- File not found (missing PNG)
- Permission denied
- Corrupted PNG data
- Invalid emoji character
- Cache memory limits exceeded
- File system errors

Build tests covering:
- Each fallback strategy behavior
- Error logging without breaking execution
- Performance comparison: fallback vs normal rendering
- Integration with existing hex string encoding
- Graceful degradation under various error conditions
- Memory cleanup after errors

Ensure fallback maintains document generation performance and doesn't introduce breaking changes.
```

## Step 7: PDF Image Objects ✅ COMPLETED

### Context
Now we need to generate proper PDF XObject streams for PNG images that can be embedded in PDF documents.

### Prompt 7: PDF Image Object Generation ✅ COMPLETED

```
Create PDF image object generation functions in the `hiccup-pdf.images` namespace:

1. `png-to-pdf-object` - Converts PNG buffer to PDF XObject stream
2. `generate-image-xobject` - Creates complete PDF image object with headers
3. `create-resource-reference` - Generates unique image references (Em1, Em2, etc.)
4. `calculate-image-transform` - Computes scaling and positioning for font size matching

Requirements:
- Generate valid PDF XObject syntax for PNG images
- Use DCTDecode filter for PNG compression (or FlateDecode if needed)
- Handle 72x72 source images scaling to match font sizes (8px to 72px)
- Create unique object references for each image
- Proper PDF object numbering and cross-references

PDF XObject format:
```
N 0 obj
<<
/Type /XObject
/Subtype /Image
/Width 72
/Height 72
/ColorSpace /DeviceRGB
/BitsPerComponent 8
/Filter /DCTDecode
/Length <byte-length>
>>
stream
<PNG binary data>
endstream
endobj
```

Implementation details:
- Convert PNG Buffer to base64 or raw bytes for PDF stream
- Calculate proper scaling factors for different font sizes
- Handle baseline alignment (emoji should sit on text baseline)
- Generate sequential object numbers starting from high range (1000+)

Build tests:
- Valid PDF object syntax generation
- Correct length calculations for PNG data
- Proper scaling calculations for various font sizes (8, 12, 16, 24, 72)
- Unique reference generation for multiple images
- Baseline alignment calculations
- Integration with existing PDF generation patterns from hiccup-pdf.document

Validate generated objects can be parsed by PDF viewers (basic syntax validation).
```

## Step 8: Mixed Content Rendering ✅ COMPLETED

### Context
We need to combine text and image segments into proper PDF operators that position emoji images inline with text.

### Prompt 8: Mixed Content PDF Operators ✅ COMPLETED

```
Extend the `hiccup-pdf.text-processing` namespace with PDF operator generation for mixed text and emoji content:

1. `render-text-segment` - Generates PDF text operators for text segments
2. `render-image-segment` - Generates PDF image operators for emoji segments  
3. `render-mixed-segments` - Combines text and image segments into complete PDF operator string
4. `calculate-segment-positions` - Computes x,y positions for each segment with proper spacing

Requirements:
- Generate proper PDF operators: BT/ET blocks for text, q/cm/Do/Q for images
- Handle horizontal positioning: advance x position after each segment
- Baseline alignment: images aligned with text baseline
- Font size scaling: emoji images scale to match text font size
- Proper spacing between text and emoji segments

PDF operator examples:
```
BT
/Arial 14 Tf
100 200 Td
(Hello ) Tj
ET
q
14 0 0 14 142 200 cm
/Em1 Do
Q
BT
/Arial 14 Tf
156 200 Td
( world) Tj
ET
```

Implementation details:
- Use existing text rendering from hiccup-pdf.core as base
- Calculate text width to position next segment
- Apply baseline offset for image alignment (typically -0.2 * font-size)
- Handle font and color state management across segments
- Integrate with PDF image objects from Step 7

Build tests:
- Simple mixed content: "Hello 💡"
- Multiple emoji: "Status: ✅ Target: 🎯"
- Edge cases: emoji-only, text-only, adjacent emoji
- Various font sizes (8, 12, 16, 24)
- Baseline alignment verification
- Position calculation accuracy
- PDF operator syntax validation

Include integration test generating complete PDF with mixed content and validating with pdf2svg.
```

## Step 9: Resource Management ✅ COMPLETED

### Context
PDF documents need resource dictionaries that reference all XObject images used in pages. We need to update the document generation to include emoji image resources.

### Prompt 9: PDF Resource Dictionary Management ✅ COMPLETED

```
Extend the `hiccup-pdf.document` namespace with emoji image resource management:

1. `collect-page-images` - Scans page content for emoji images and returns collection
2. `generate-image-resources` - Creates PDF resource dictionary entries for images
3. `update-page-resources` - Merges image resources with existing page resources (fonts, etc.)
4. `embed-images-in-document` - Integrates image objects into complete PDF document

Requirements:
- Scan hiccup page content for emoji text elements
- Extract unique emoji images needed for each page
- Generate proper PDF resource dictionary syntax
- Integrate with existing font resource management
- Handle object numbering and cross-references

Resource dictionary format:
```
/Resources <<
  /Font <<
    /Arial 3 0 R
    /Times 4 0 R
  >>
  /XObject <<
    /Em1 10 0 R
    /Em2 11 0 R
  >>
>>
```

Implementation details:
- Recursively scan hiccup page content for :text elements
- Extract unique emoji from text content using previous emoji detection functions
- Maintain object number sequence for images
- Update existing page resource generation
- Handle multiple pages with different emoji sets

Build tests:
- Single page with multiple emoji
- Multiple pages with overlapping emoji sets
- Empty pages (no emoji)
- Complex nested hiccup structures
- Resource dictionary syntax validation
- Object numbering consistency
- Integration with existing font resources

Include end-to-end test: generate complete PDF with emoji, validate structure with pdf parser, convert to SVG to verify emoji are embedded correctly.
```

## Step 10: API Integration ✅ COMPLETED

### Context
Now we integrate emoji image support into the existing hiccup-pdf API, making it available through the main text processing functions.

### Prompt 10: API Integration and Text Processing Enhancement ✅ COMPLETED

```
Enhance the existing `hiccup-pdf.core` namespace to support emoji images through the main API functions:

1. Update `encode-pdf-text` to support emoji image mode
2. Enhance `text->pdf-ops` with emoji image rendering option
3. Add emoji support to `element->pdf-ops` dispatcher
4. Create `process-text-with-emoji-images` function for mixed content

Requirements:
- Maintain backward compatibility: existing hex string encoding remains default
- Add optional emoji image mode through options parameter
- Integrate with all previous emoji processing functions
- Handle both content stream generation and document generation paths
- Preserve existing function signatures and behavior

Enhanced function signatures:
```clojure
(defn text->pdf-ops
  "Enhanced with emoji image support"
  [attributes content & [options]]
  ;; options: {:enable-emoji-images true :emoji-config {...}}
  )

(defn encode-pdf-text  
  "Enhanced with emoji image fallback"
  [text-content & [options]]
  ;; Falls back to hex encoding when images disabled/unavailable
  )
```

Implementation details:
- Check `:enable-emoji-images` flag in options
- Route to emoji image processing when enabled
- Fallback to existing hex encoding when disabled or on error
- Pass through emoji configuration (cache size, fallback strategy, etc.)
- Maintain performance: no overhead when emoji images disabled

Build tests:
- Backward compatibility: existing code works unchanged
- Emoji image mode: generates proper mixed content operators
- Fallback behavior: graceful degradation to hex encoding
- Configuration passing: emoji options propagated correctly
- Performance: no regression when emoji images disabled
- Integration: works with both hiccup->pdf-ops and hiccup->pdf-document

Include migration test: existing examples should work identically with new code.
```

## Step 11: Configuration System

### Context
We need a comprehensive configuration system for emoji image features, allowing users to customize behavior while maintaining sensible defaults.

### Prompt 11: Configuration System and Options

```
Create a configuration system for emoji image features in `hiccup-pdf.emoji` namespace:

1. `default-emoji-config` - Default configuration map with sensible defaults
2. `validate-emoji-config` - Validates user configuration options
3. `merge-emoji-config` - Merges user config with defaults
4. `emoji-config-enabled?` - Checks if emoji images are enabled in config

Requirements:
- Comprehensive configuration options covering all emoji image features
- Validation of user input with helpful error messages
- Sensible defaults that work out of the box
- Easy enable/disable of entire emoji image system
- Integration with existing hiccup-pdf options patterns

Configuration structure:
```clojure
(def default-emoji-config
  {:enable-emoji-images false           ; Feature flag
   :emoji-directory "emojis/noto-72/"   ; PNG file location
   :fallback-strategy :hex-string       ; :hex-string, :placeholder, :skip, :error
   :cache-size 50                       ; Max cached images
   :cache-memory-mb 10                  ; Memory limit
   :baseline-adjust 0.2                 ; Baseline offset ratio
   :min-font-size 8                     ; Min size for image emoji
   :max-font-size 72                    ; Max size for image emoji
   :spacing-adjust 1.0                  ; Horizontal spacing multiplier
   :debug false})                       ; Debug logging
```

API integration examples:
```clojure
(hiccup->pdf-ops [:text {:x 100 :y 100 :font "Arial" :size 14} "Hello 💡"]
                 {:emoji {:enable-emoji-images true}})

(hiccup->pdf-document document 
                      {:emoji {:enable-emoji-images true 
                               :fallback-strategy :placeholder}})
```

Build tests:
- Default configuration validity
- Configuration validation with invalid inputs
- Configuration merging behavior
- Feature enable/disable functionality
- Integration with main API functions
- Error handling for invalid config options

Include documentation examples showing common configuration patterns and use cases.
```

## Step 12: Document-Level Integration

### Context
Final step: integrate emoji image support into complete PDF document generation, handling multi-page documents and resource management.

### Prompt 12: Complete Document Integration

```
Complete the emoji image integration by enhancing `hiccup-pdf.document` namespace for full document generation support:

1. Update `document->pdf` to handle emoji image resources
2. Enhance `page->content-stream` with emoji image processing  
3. Modify object numbering to accommodate image objects
4. Update `hiccup-document->pdf` with emoji configuration support

Requirements:
- Full document generation with emoji images across multiple pages
- Proper object numbering and cross-references for image objects
- Resource dictionary management for complex documents
- Page-level emoji image optimization (shared resources)
- Integration with existing document metadata and page management

Implementation details:
- Pre-scan entire document for unique emoji images
- Generate image objects before page objects
- Update page resource dictionaries with image references
- Handle object numbering sequence with images
- Maintain existing document structure and metadata

Document generation flow:
1. Parse hiccup document structure
2. Scan all pages for emoji usage
3. Load and cache unique emoji images
4. Generate PDF image objects
5. Process pages with emoji-aware content streams
6. Generate final PDF with proper resource references

Build comprehensive tests:
- Single page document with emoji
- Multi-page documents with shared emoji
- Documents with no emoji (no regression)
- Large documents with many unique emoji
- Error handling during document generation
- Resource sharing efficiency
- Object numbering consistency

Include end-to-end integration tests:
- Generate complete PDF documents with emoji
- Validate PDF structure and syntax
- Convert to SVG and verify emoji rendering
- Test with various PDF viewers
- Performance benchmarking vs hex string encoding

Final validation:
- All existing tests pass
- New emoji image functionality works correctly  
- Performance impact is within acceptable limits (<15% overhead)
- Fallback behavior maintains document generation reliability
- API remains backward compatible
```

---

# Summary

This implementation plan provides 12 carefully structured steps that build emoji PNG support incrementally:

**Phase 1 (Steps 1-3)**: Unicode processing foundations
**Phase 2 (Steps 4-6)**: Image loading and error handling  
**Phase 3 (Steps 7-9)**: PDF generation and resource management
**Phase 4 (Steps 10-12)**: API integration and document support

Each step:
- Has clear, testable requirements
- Builds on previous steps without large complexity jumps
- Includes comprehensive test coverage
- Integrates immediately with existing code
- Maintains backward compatibility

The prompts are designed for a code-generation LLM to implement test-driven development with strong incremental progress and no orphaned code.