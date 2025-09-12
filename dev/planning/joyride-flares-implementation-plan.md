# Joyride Flares Implementation Plan

## Overview

This plan outlines the implementation of Flares for Joyride - a powerful feature for creating custom WebView panels and sidebar views directly from ClojureScript evaluation results. Flares enable users to display HTML content, web pages, data visualizations, and custom UI components within VS Code through both tagged literals and function calls.

## Requirements

### Functional Requirements
- **Tagged Literal Support**: Process `#joyride/flare {...}` literals returned from evaluations
- **Function API**: Provide `(joyride.core/flare! {...})` function for direct invocation
- **Hiccup Support**: Accept both HTML strings and Hiccup data structures, using Replicant for rendering
- **Calva Compatibility**: Full compatibility with Calva's Flare specification for easy migration
- **Display Options**: Support both separate panels and sidebar views
- **Panel Management**: Key-based panel reuse and lifecycle management
- **Return Values**: Return handles to created webviews for programmatic control

### Non-Functional Requirements
- **Performance**: Minimal impact on evaluation pipeline
- **Reliability**: Robust error handling and resource cleanup
- **Extensibility**: Architecture supports future flare types (charts, images, etc.)
- **User Experience**: Seamless integration with existing Joyride workflows

## Implementation Steps

### Phase 1: Core Infrastructure

#### Step 1.1: Create Flare Namespace
**File**: `src/joyride/flare.cljs`

```clojure
(ns joyride.flare
  "Joyride Flares - WebView panel and sidebar view creation"
  (:require
   ["vscode" :as vscode]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [replicant.string :as replicant]))

;; Core flare processing functions
;; WebView management utilities
;; Tagged literal processor
;; Hiccup rendering with Replicant
;; Public API functions
```

**Key Components**:
- **WebView Registry**: Track panels by key for reuse
- **Sidebar Provider**: WebView provider for sidebar views
- **Content Generators**: HTML and iframe content creation with Hiccup support
- **Hiccup Renderer**: Replicant integration for Hiccup-to-HTML conversion

#### Step 1.2: WebView Management System
**Location**: `src/joyride/flare.cljs`

```clojure
;; Panel registry for key-based reuse
(defonce !flare-panels (atom {}))
(defonce !flare-sidebar-views (atom {}))

;; WebView creation with proper lifecycle management
(defn create-webview-panel! [options]
  ;; Create or reuse panel based on :key
  ;; Set up proper disposal handling
  ;; Return panel handle)

(defn create-sidebar-view! [options]
  ;; Create sidebar view
  ;; Register with provider
  ;; Return view handle)
```

#### Step 1.3: Content Processing
**Location**: `src/joyride/flare.cljs`

```clojure
(defn process-flare-request! [flare-data]
  ;; Choose panel vs sidebar
  ;; Generate content (HTML vs Hiccup vs iframe)
  ;; Create or update webview
  ;; Return appropriate handle)

(defn render-content [content-data title]
  ;; Handle different content types:
  ;; - HTML string: use as-is
  ;; - Hiccup data: render with Replicant
  ;; - URL: create iframe wrapper
  ;; Add proper VS Code styling)

(defn hiccup->html [hiccup-data]
  ;; Use Replicant to render Hiccup to HTML string
  (replicant/render hiccup-data))

(defn generate-iframe-content [url]
  ;; Create iframe wrapper following Calva's approach)
```

### Phase 2: Tagged Literal Integration

#### Step 2.1: SCI Tagged Literal Processor
**File**: `src/joyride/flare/tagged_literal.cljs`

```clojure
(ns joyride.flare.tagged-literal
  (:require
   [joyride.flare :as flare]
   [clojure.edn :as edn]))

(defn joyride-flare-reader [flare-data]
  "Tagged literal reader for #joyride/flare"
  (flare/process-flare-request! flare-data))
```

#### Step 2.2: Evaluation Result Processing
**Update**: `src/joyride/lm/evaluation.cljs` and `src/joyride/sci.cljs`

```clojure
;; In evaluation.cljs
(defn process-evaluation-result [result]
  ;; Scan result for tagged literals
  ;; Process any flares found
  ;; Return processed result)

;; In sci.cljs - add to SCI context
;; Add tagged literal reader to SCI configuration
```

### Phase 3: Function API

#### Step 3.1: Public API Functions
**Update**: Add to `joyride-code` in `src/joyride/sci.cljs`

```clojure
;; Add to joyride-code map
'flare! (sci/copy-var flare/flare! joyride-ns)
```

**Implementation** in `src/joyride/flare.cljs`:

```clojure
(defn flare!
  "Create a WebView panel or sidebar view with the given options.

   Options:
   - :html - HTML content string OR Hiccup data structure
   - :url - URL to display in iframe
   - :title - Panel/view title (default: 'WebView')
   - :key - Identifier for reusing panels
   - :reload - Force reload even if content unchanged (default: false)
   - :reveal - Show/focus the panel (default: true)
   - :column - VS Code ViewColumn (default: vscode.ViewColumn.Beside)
   - :opts - WebView options (default: {:enableScripts true})
   - :sidebar-panel? - Display in sidebar vs separate panel (default: false)

   Content Examples:
   - HTML string: {:html "<h1>Hello</h1>"}
   - Hiccup data: {:html [:div [:h1 "Hello"] [:p "World"]]}
   - URL: {:url "https://example.com"}

   Returns:
   - {:panel <webview-panel> :type :panel} for panels
   - {:view <webview-view> :type :sidebar} for sidebar views"
  [options]
  (flare/process-flare-request! options))
```

### Phase 4: VS Code Integration

#### Step 4.1: Extension Registration
**Update**: `src/joyride/extension.cljs`

```clojure
;; Add to activate function
(require '[joyride.flare :as flare])

;; Register sidebar provider
(let [flare-disposables (flare/register-providers! extension-context)]
  (doseq [disposable flare-disposables]
    (swap! db/!app-db update :disposables conj disposable)
    (.push (.-subscriptions extension-context) disposable)))
```

#### Step 4.2: Package.json Updates
**Update**: `package.json`

```json
{
  "contributes": {
    "views": {
      "joyride": [
        {
          "type": "webview",
          "id": "joyride.flare",
          "name": "Flare",
          "when": "joyride.flareViewVisible"
        }
      ]
    },
    "commands": [
      {
        "command": "joyride.flare.focus",
        "title": "Focus Flare View",
        "category": "Joyride"
      }
    ]
  }
}
```

#### Step 4.3: Sidebar Provider Implementation
**File**: `src/joyride/flare/sidebar_provider.cljs`

```clojure
(ns joyride.flare.sidebar-provider
  (:require ["vscode" :as vscode]))

(defn create-flare-webview-provider [extension-context]
  "Create WebView provider for sidebar flare views"
  ;; Implement vscode.WebviewViewProvider interface
  ;; Handle view resolution and updates
  ;; Manage content lifecycle)
```

### Phase 5: Advanced Features

#### Step 5.1: Custom Tab Icons
**Investigation**: Determine if VS Code API supports custom webview panel icons
**Implementation**: Add icon support if available

#### Step 5.2: Enhanced Error Handling
**File**: `src/joyride/flare/error_handling.cljs`

```clojure
(ns joyride.flare.error-handling
  "Error handling for flare requests")

(defn validate-flare-options [options]
  "Validate flare options structure and return normalized options"
  ;; Check required fields exist
  ;; Validate option types
  ;; Provide helpful error messages for structural issues)
```

#### Step 5.3: WebView Content Generation
**Implementation**: Follow Calva's approach for content handling

```clojure
(defn generate-webview-html [content]
  "Generate WebView HTML content following Calva's approach"
  ;; Direct HTML pass-through like Calva
  ;; Rely on VS Code's WebView security model
  ;; No content validation - trust the platform)
```

#### Step 5.4: Hiccup Integration with Replicant
**File**: `src/joyride/flare/hiccup.cljs`

```clojure
(ns joyride.flare.hiccup
  "Hiccup rendering support using Replicant"
  (:require
   [replicant.string :as replicant]
   [clojure.string :as str]))

(defn hiccup? [data]
  "Check if data is a Hiccup structure (vector starting with keyword)"
  (and (vector? data)
       (keyword? (first data))))

(defn render-hiccup [hiccup-data]
  "Render Hiccup data structure to HTML string using Replicant"
  (try
    ;; Use Replicant's string rendering for server-side HTML generation
    ;; This is the correct API for Hiccup-to-HTML conversion
    (replicant/render hiccup-data)
    (catch js/Error e
      (throw (ex-info "Failed to render Hiccup data"
                      {:hiccup hiccup-data
                       :error (.-message e)})))))
```

**Integration**: Add Replicant dependency to project and integrate with content rendering pipeline.

## Testing Strategy

### Unit Tests
**File**: `test/joyride/flare_test.cljs`

```clojure
(ns joyride.flare-test
  (:require
   [cljs.test :refer [deftest testing is]]
   [joyride.flare :as flare]))

(deftest flare-validation-test
  (testing "Valid flare options"
    ;; Test valid HTML content
    ;; Test valid Hiccup data structures
    ;; Test valid URL content
    ;; Test all option combinations)

  (testing "Invalid flare options"
    ;; Test missing required fields
    ;; Test invalid option types
    ;; Test malformed data structures))

(deftest hiccup-rendering-test
  (testing "Hiccup to HTML conversion"
    ;; Test simple elements
    ;; Test nested structures
    ;; Test attributes and classes
    ;; Test edge cases and errors))

(deftest webview-management-test
  (testing "Panel creation and reuse"
    ;; Test key-based reuse
    ;; Test disposal handling
    ;; Test multiple panels))
```

### Integration Tests
**File**: `test/joyride/flare_integration_test.cljs`

```clojure
(deftest tagged-literal-integration-test
  (testing "Tagged literal processing"
    ;; Test evaluation result scanning
    ;; Test flare creation from tagged literals
    ;; Test return value handling))

(deftest function-api-integration-test
  (testing "Function API"
    ;; Test direct function calls
    ;; Test SCI context integration
    ;; Test return value handling))
```

### Manual Testing Scenarios
1. **Basic HTML Display**: Simple HTML content in both panel and sidebar
2. **Hiccup Rendering**: Various Hiccup structures with nested elements and attributes
3. **URL Display**: Web page loading with iframe
4. **Panel Reuse**: Multiple calls with same key
5. **Multiple Panels**: Several panels with different keys
6. **Content Type Mixing**: HTML strings and Hiccup data in different panels
7. **Error Cases**: Invalid HTML, malformed Hiccup, network failures, missing options
8. **Performance**: Large HTML content, complex Hiccup structures, multiple simultaneous panels

## Considerations

### Architectural Considerations

#### **Separation of Concerns**
- **Flare Core**: Pure flare processing logic
- **VS Code Integration**: WebView creation and management
- **SCI Integration**: Tagged literal and function registration
- **Error Handling**: Graceful handling of rendering failures

#### **Extension Point Design**
The architecture supports future extensions:
- **Chart Flares**: `#joyride/chart` for data visualization
- **Image Flares**: `#joyride/image` for image display
- **Custom Flares**: Plugin system for user-defined flare types

#### **Resource Management**
- **Memory**: Proper disposal of webviews and event listeners
- **Performance**: Efficient panel reuse and content caching
- **Security**: CSP headers and content sanitization

### Technical Considerations

#### **WebView Lifecycle**
- Panels are automatically disposed when closed by user
- Sidebar views persist across VS Code sessions
- Key-based registry allows programmatic cleanup

#### **Content Security**
- VS Code WebView security model provides built-in protection
- No additional content validation needed (following Calva's approach)
- Trust VS Code's CSP and sandboxing capabilities

#### **Error Resilience**
- Invalid flare data doesn't crash evaluation
- WebView creation failures are gracefully handled
- Network errors for URL content are managed

### Integration Considerations

#### **SCI Context Updates**
- Tagged literal readers must be registered in SCI
- Function symbols added to joyride.core namespace
- No breaking changes to existing SCI configuration

#### **Evaluation Pipeline**
- Result processing happens after successful evaluation
- Tagged literals are detected via string scanning
- Performance impact is minimized through efficient scanning

#### **VS Code API Usage**
- WebView panels use standard VS Code APIs
- Sidebar integration follows VS Code patterns
- Extension points align with VS Code extension model

### Future Extensibility

#### **Multiple Flare Types**
The architecture readily supports additional flare types:

```clojure
;; Current HTML/Hiccup flares
#joyride/flare {:html "<h1>Hello</h1>"}
#joyride/flare {:html [:div [:h1 "Hello"] [:p "World"]]}

;; Future flare types
#joyride/chart {:type :bar :data [...] :title "Sales Chart"}
#joyride/image {:src "data:image/png;base64,..." :title "Diagram"}
#joyride/table {:data [...] :columns [...] :title "Data Table"}
```#### **Plugin System**
```clojure
;; Register custom flare handlers
(flare/register-handler! :custom-type custom-handler-fn)
```

#### **Integration APIs**
```clojure
;; Programmatic flare control
(flare/update-content! panel-handle new-content)
(flare/close-panel! panel-handle)
(flare/list-active-panels)
```

## Risk Mitigation

### **Performance Risks**
- **Mitigation**: Efficient panel reuse, content caching, lazy loading
- **Monitoring**: Track panel count and memory usage

### **Security Risks**
- **Mitigation**: Trust VS Code's WebView security model (following Calva's approach)
- **Testing**: Basic functionality testing, integration testing

### **Integration Risks**
- **Mitigation**: Incremental integration, feature flags, rollback plan
- **Testing**: Comprehensive integration testing, user acceptance testing

### **Maintenance Risks**
- **Mitigation**: Clear documentation, modular architecture, automated testing
- **Planning**: Regular code reviews, dependency updates, performance monitoring

## Success Criteria

### **Functional Success**
- [ ] Tagged literals `#joyride/flare` work in all evaluation contexts
- [ ] Function API `(flare! {...})` creates panels/sidebar views correctly
- [ ] Both HTML strings and Hiccup data structures render properly
- [ ] Replicant integration provides robust Hiccup-to-HTML conversion
- [ ] All Calva Flare options are supported and compatible
- [ ] Panel reuse via `:key` parameter works reliably
- [ ] Both HTML and URL content display properly
- [ ] Return values provide useful webview handles

### **Quality Success**
- [ ] No performance degradation in evaluation pipeline
- [ ] Robust error handling with helpful messages
- [ ] Memory leaks prevented through proper disposal
- [ ] Security relies on VS Code's WebView model (like Calva)
- [ ] Test coverage >90% for flare-related code

### **User Experience Success**
- [ ] Intuitive API that feels natural in Joyride workflows
- [ ] Documentation with clear examples and use cases
- [ ] Smooth integration with existing Joyride features
- [ ] Responsive UI with proper VS Code theming
- [ ] Minimal learning curve for users familiar with Calva Flares

This implementation plan provides a comprehensive roadmap for bringing Flares to Joyride while maintaining compatibility with Calva and ensuring robust, extensible architecture for future enhancements.