# Flare Hiccup File Support

## Overview

Add support for hiccup files (`.hiccup`, `.clj`, `.cljs`, `.cljc`, `.bb`) to the `:file` option in Joyride Flares. This extends the current HTML file support to include native hiccup data structures.

## Current State

The `:file` option currently supports:
- HTML files (`.html`, `.htm`) - reads content via `fs/readFileSync` and sets as webview HTML
- File path normalization via `normalize-file-option`

## Proposed Enhancement

### File Type Detection

Use file extension to determine content type:

```clojure
(defn detect-file-type [file-path]
  (let [extension (-> file-path str (.toLowerCase) (.substring (.lastIndexOf file-path ".")))]
    (cond
      (contains? #{".clj" ".cljs" ".cljc" ".bb" ".hiccup"} extension) :hiccup
      (contains? #{".html" ".htm"} extension) :html
      :else :html))) ; Default to HTML, let parser handle errors
```

### Content Processing

Enhance `render-content` in `panel.cljs`:

```clojure
(:file flare-options)
(let [^js file-uri (:file flare-options)
      file-path (.-fsPath file-uri)
      file-type (detect-file-type file-path)
      file-content (fs/readFileSync file-path "utf8")]
  (case file-type
    :hiccup (render-hiccup (read-string file-content))  ; Parse hiccup from file
    :html file-content))                                ; Use HTML as-is
```

## Benefits

1. **Native Hiccup Support**: Write UI in `.hiccup` files using Clojure data structures
2. **Clojure File Reuse**: Use existing `.cljs` files containing hiccup definitions
3. **Consistent API**: Same `:file` option works for both HTML and hiccup content
4. **Editor Support**: Full Clojure editing experience for UI definitions

## Example Usage

### Hiccup File (`ui.hiccup`)
```clojure
[:div {:style {:padding "20px"}}
 [:h1 "Welcome to Joyride Flares"]
 [:p "This content is loaded from a hiccup file"]
 [:button {:onclick "alert('Hello from hiccup!')"} "Click me"]]
```

### Using the Hiccup File
```clojure
(flare! {:file "ui.hiccup"
         :title "Hiccup File Demo"
         :key :hiccup-demo})
```

## Implementation Notes

- File extension detection is case-insensitive
- Default to HTML format for unknown extensions
- Hiccup parsing uses standard `read-string`
- Error handling should provide clear messages for invalid hiccup syntax
- Maintains backward compatibility with existing HTML file usage

## Dependencies

- `clojure.edn` or `cljs.reader` for parsing hiccup files
- Existing `replicant.string` for hiccup→HTML rendering

## Testing

Create test files:
- `test.hiccup` with various hiccup structures
- `test.cljs` with hiccup definitions
- Verify error handling for malformed hiccup
- Ensure HTML files continue working unchanged