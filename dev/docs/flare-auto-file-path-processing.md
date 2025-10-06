# Flare Auto File Path Processing

## Overview

Implement automatic file path processing in Joyride Flares to convert file-like URIs in hiccup attributes to webview-compatible URIs. This enables seamless local resource loading without requiring users to understand webview URI conversion.

## Current State

Users must manually handle file paths for local resources:
- No automatic conversion to webview URIs
- Users need to understand VS Code's webview security model

## Proposed Enhancement

### Automatic File Path Detection

Detect and convert file-like paths in URI attributes:

```clojure
(def uri-attributes
  #{:src :href :action :data :content :poster :background})

(defn looks-like-file-path? [path-string]
  (and (string? path-string)
       (not (some #(str/starts-with? path-string %)
                  ["http://" "https://" "data:" "#" "javascript:"]))
       (or (str/starts-with? path-string "./")
           (str/starts-with? path-string "/")
           (re-matches #"^[A-Z]:\\\\" path-string)  ; Windows drive
           (str/starts-with? path-string "file://")
           (re-matches #".*\.[a-zA-Z0-9]+$" path-string))))  ; Has extension
```

### File Path Conversion

Convert detected file paths using VS Code APIs:

```clojure
(defn path-to-webview-uri [path-string webview]
  (let [file-uri (cond
                   (str/starts-with? path-string "file://")
                   (vscode/Uri.parse path-string)

                   (str/starts-with? path-string "/")
                   (vscode/Uri.file path-string)

                   (str/starts-with? path-string "./")
                   (let [workspace-uri (.-uri (first vscode/workspace.workspaceFolders))]
                     (vscode/Uri.joinPath workspace-uri (subs path-string 2)))

                   :else  ; Workspace-relative
                   (let [workspace-uri (.-uri (first vscode/workspace.workspaceFolders))]
                     (vscode/Uri.joinPath workspace-uri path-string)))]
    (.asWebviewUri webview file-uri)))
```

### Hiccup Processing

Walk hiccup structure and convert file paths:

```clojure
(defn process-file-paths [hiccup-data webview]
  (clojure.walk/postwalk
    (fn [form]
      (if (and (map? form) (some uri-attributes (keys form)))
        (reduce-kv
          (fn [m k v]
            (if (and (uri-attributes k) (looks-like-file-path? v))
              (assoc m k (str (path-to-webview-uri v webview)))
              (assoc m k v)))
          {}
          form)
        form))
    hiccup-data))
```

## Integration Points

### In render-content Pipeline

```clojure
(defn render-content [flare-options webview]
  (let [hiccup-data (content->hiccup flare-options)
        processed-hiccup (process-file-paths hiccup-data webview)]
    (render-hiccup processed-hiccup)))
```

### webview Access

Ensure webview reference is available in processing pipeline:
- Pass webview to `update-view-content!`
- Thread through `render-content` calls
- Handle both panel and sidebar webviews

## Supported Path Formats

### Relative Paths
```clojure
[:img {:src "./assets/logo.png"}]        ; → workspace/assets/logo.png
[:img {:src "assets/logo.png"}]          ; → workspace/assets/logo.png
```

### Absolute Paths
```clojure
[:img {:src "/Users/me/images/logo.png"}]    ; → file:///Users/me/images/logo.png
[:img {:src "C:\\images\\logo.png"}]         ; → file:///C:/images/logo.png (Windows)
```

### File URIs
```clojure
[:link {:href "file:///path/to/style.css"}]  ; → webview URI
```

### URLs (Unchanged)
```clojure
[:img {:src "https://example.com/logo.png"}]     ; ← No conversion
[:a {:href "mailto:user@example.com"}]           ; ← No conversion
[:a {:href "#section"}]                          ; ← No conversion
```

## Error Handling

### Invalid Paths
- Log warnings for unresolvable paths
- Continue processing other attributes
- Let webview handle final URI validation

### No Workspace
- Absolute paths still work via `vscode.Uri.file()`
- Relative paths throw descriptive errors
- Provide fallback suggestions

### Security Restrictions
- Respect VS Code's `localResourceRoots` configuration
- Let webview security model handle blocked resources
- Provide clear error messages for blocked content

## Performance Considerations

- File path detection uses regex - optimize patterns
- URI conversion involves VS Code API calls - consider caching
- Large hiccup structures - profile walking performance

## User Experience

### Transparent Operation
```clojure
;; User writes natural file paths
(flare! {:html [:div
                [:img {:src "./logo.png"}]
                [:link {:rel "stylesheet" :href "style.css"}]
                [:script {:src "/abs/path/script.js"}]]
         :title "Auto File Paths"})

;; System automatically converts to webview URIs
;; No special configuration or understanding required
```

### Debug Support
- Option to log path conversions: `:debug-file-paths? true`
- Clear error messages for conversion failures
- Preserve original paths in error messages

## Benefits

1. **Zero Learning Curve**: Use standard file path conventions
2. **VS Code Native**: Leverages built-in security and path resolution
3. **Transparent**: No special schemes or configuration needed
4. **Robust**: Handles various path formats automatically
5. **Maintainable**: Less custom code, VS Code handles complexity

## Testing Requirements

### Path Format Coverage
- Relative paths: `./`, workspace-relative
- Absolute paths: Unix `/`, Windows `C:\`
- File URIs: `file://`
- Mixed content with URLs and paths

### Edge Cases
- Non-existent files
- Paths outside workspace
- Special characters in paths
- Unicode path names

### Integration Testing
- HTML files with file paths
- Hiccup files with file paths
- Mixed hiccup with various path types
- Error scenarios and fallbacks