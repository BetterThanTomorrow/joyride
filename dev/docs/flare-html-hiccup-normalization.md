# Flare HTML to Hiccup Normalization

## Overview

Implement internal normalization of HTML content to hiccup data structures in Joyride Flares. This creates a unified processing pipeline where all content (HTML strings, hiccup data, HTML files, hiccup files) flows through a common hiccup-based processing system.

## Current State

The `:html` option currently supports:
- HTML strings - passed directly to webview
- Hiccup data structures - rendered via Replicant to HTML

Processing is inconsistent between content types.

## Proposed Enhancement

### Unified Processing Pipeline

All content types convert to hiccup internally:

```
HTML String → parse to hiccup → process → render to HTML
Hiccup Data → process → render to HTML
HTML File → read → parse to hiccup → process → render to HTML
Hiccup File → read → parse hiccup → process → render to HTML
```

### HTML→Hiccup Conversion

Adapt Calva's `html2hiccup.cljs` implementation:

```clojure
;; Simplified version adapted for Replicant
(defn html->hiccup [html-string]
  ;; Use posthtml-parser to create AST
  ;; Transform to hiccup data structure
  ;; Return vector of hiccup forms
  )
```

Key adaptations needed:
- Remove zprint formatting (we render back to HTML)
- Simplify attribute normalization for Replicant compatibility
- Focus on structural conversion rather than pretty-printing

### Enhanced render-content

```clojure
(defn render-content [flare-options]
  (let [hiccup-data (cond
                      (:file flare-options) (file->hiccup flare-options)
                      (:url flare-options) (generate-iframe-hiccup (:url flare-options))
                      (:html flare-options) (html-content->hiccup (:html flare-options))
                      :else (throw (ex-info "Missing content" {})))]
    ;; Process hiccup (file path conversion, etc.)
    (let [processed-hiccup (process-hiccup hiccup-data webview)]
      ;; Render back to HTML
      (render-hiccup processed-hiccup))))

(defn html-content->hiccup [html-content]
  (if (vector? html-content)
    html-content                    ; Already hiccup
    (html->hiccup html-content)))   ; Parse HTML string
```

## Benefits

1. **Consistent Processing**: All content flows through same pipeline
2. **Unified Enhancements**: Features like file path conversion work for all content types
3. **Simplified Logic**: Single code path for processing
4. **Future-Proof**: Easy to add new content transformations

## Implementation Details

### Dependencies

Add to project dependencies:
```clojure
["posthtml-parser" "^0.11.0"]  ; HTML parsing (from Calva approach)
```

### HTML Parsing Options

Configure posthtml-parser for Replicant compatibility:
```javascript
{
  recognizeNoValueAttribute: true,  // Handle boolean attributes
  lowerCaseAttributeNames: false,   // Preserve attribute casing for React/DOM compatibility
}
```

### Replicant Compatibility

Ensure hiccup output is compatible with Replicant's expectations:
- Use kebab-case for attribute names where appropriate
- Handle special cases (viewBox, baseProfile for SVG)
- Preserve CSS class and style attribute formats

## Example Transformations

### HTML String Input
```clojure
;; Input
{:html "<div class='container'><h1>Hello</h1></div>"}

;; Internal hiccup (after parsing)
[:div {:class "container"} [:h1 "Hello"]]

;; Output HTML (after processing and rendering)
"<div class=\"container\"><h1>Hello</h1></div>"
```

### Mixed Content
```clojure
;; Both work the same way internally
{:html [:div [:h1 "Hello"]]}           ; Direct hiccup
{:html "<div><h1>Hello</h1></div>"}    ; HTML string → parsed to hiccup
```

## Error Handling

- Malformed HTML: Provide clear parsing error messages
- Invalid hiccup: Standard Clojure reader errors
- Fallback: If parsing fails, pass content through unchanged

## Performance Considerations

- HTML→hiccup parsing adds overhead for HTML strings
- Consider caching parsed results for static content
- Profile performance impact and optimize if needed

## Testing Strategy

1. **Round-trip testing**: HTML → hiccup → HTML should preserve meaning
2. **Complex HTML**: Forms, tables, SVG, embedded CSS/JS
3. **Edge cases**: Comments, CDATA, malformed HTML
4. **Compatibility**: Ensure Replicant rendering works correctly