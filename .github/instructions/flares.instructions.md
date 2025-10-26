---
description: 'Best practices and effective repl use when working on the Joyride Flares feature'
applyTo: 'src/joyride/flare.cljs,src/joyride/flare/**'
---

# Joyride Flares Development

You are an expert VS Code extension, ClojureScript interactive developer working on the Joyride extension and specifically the Joyride Flares feature.

## Essential Reading
1. **API documentation**: `./doc/flares.md`
2. **Working examples**: `./examples/.joyride/src/flares_examples.cljs` - Comprehensive examples showing all flare patterns including message handling

**Critical**: When working with Flares, always start by reading and copying patterns from the examples file. Don't create message handling from scratch - the examples file contains tested, working code for all common patterns including bidirectional messaging.

## Interactive programming (a.k.a. REPL-Driven Development)

### Direct CLJS REPL Access
Use the Backseat Driver CLJS REPL (`clojure_evaluate_code` with `replSessionKey: "cljs"`) to develop and test the flares implementation:

```clojure
(in-ns 'joyride.flare)
(require '[joyride.db :as db] :reload)

;; Create test flare
(flare!+ {:key :test :html [:h1 "Test"] :title "Development Test"})

;; Inspect internal state
(:flares @db/!app-db)
(keys (:flares @db/!app-db))
(get (:flares @db/!app-db) :test)
```

**Why direct access?**: You're testing the extension implementation itself, not the user-facing API. Direct CLJS REPL access provides:
- Full access to internal namespaces and implementation details
- No SCI context overhead or quoting complexity
- Direct state inspection without serialization
- Ability to test internal helper functions

### When to Use `jsci/eval-string`
Only when testing the **user-facing Joyride API** as it appears in user scripts. Must be evaluated from `user` namespace to avoid circular dependencies:

```clojure
;; Evaluate from user namespace, not joyride.flare
(in-ns 'user)
(require '[joyride.sci :as jsci] :reload)
(jsci/eval-string "(require '[joyride.flare :as flare])
                   (flare/flare!+ {:html [:h1 \"User API Test\"]})")
```

Use this to verify:
- API exports are correct in SCI context
- User-facing function signatures work
- Documentation examples are valid

## State Inspection Powers

### App-DB Exploration
The REPL provides extensive introspection capabilities. Almost anything can be inspected:

```clojure
;; View all active flares
(:flares @db/!app-db)

;; Check sidebar state
(:flare-sidebar-views @db/!app-db)

;; Inspect specific flare structure (handle circular refs)
(-> @db/!app-db :flares :test (dissoc :view))

;; Check if view is disposed
(let [flare-data (get (:flares @db/!app-db) :test)
      view (:view flare-data)]
  (.-disposed view))

;; List all flare keys
(keys (:flares @db/!app-db))

;; Check message handler presence
(-> @db/!app-db :flares :test :message-handler boolean)
```

### View Object Inspection
Extract and inspect view objects to access their properties and content:

```clojure
;; Extract panel view
(def panel-view (-> @db/!app-db :flares :my-panel :view))

;; Panel properties
{:title (.-title panel-view)
 :view-type (.-viewType panel-view)
 :view-column (.-viewColumn panel-view)
 :disposed (.-disposed panel-view)
 :visible (.-visible panel-view)
 :active (.-active panel-view)}

;; Extract sidebar view (different type)
(def sidebar-view (-> @db/!app-db :flares :sidebar-1 :view))

;; Sidebar properties
{:title (.-title sidebar-view)
 :description (.-description sidebar-view)
 :visible (.-visible sidebar-view)}
```

### Webview Content Inspection
Access the webview to inspect HTML content and configuration:

```clojure
;; Get webview from panel or sidebar view
(def webview (.-webview view))

;; Inspect webview content and settings
{:html (.-html webview)
 :csp-source (.-cspSource webview)
 :options (js->clj (.-options webview) :keywordize-keys true)}

;; Verify content updates
(.-html (.-webview panel-view))
;; => "<div><h1>Updated Content</h1>..."
```

### Icon Path Inspection
Check icon paths for panels:

```clojure
;; Icon path structure
(.-iconPath view)
;; => #js {:light #object [Ta file:///.../light.svg],
;;         :dark #object [Ta file:///.../dark.svg]}
```

### Handling Circular References
VS Code API objects contain circular references. Use `dissoc` or `select-keys` before inspection:

```clojure
;; ❌ May cause issues
(:flares @db/!app-db)

;; ✅ Safe inspection
(update-vals (:flares @db/!app-db) #(dissoc % :view))
(update-vals (:flares @db/!app-db) #(select-keys % [:message-handler]))
```

### VS Code API Object Inspection
Inspecting VS Code objects may trigger warnings about proposed APIs. This is expected and safe during development:

```clojure
;; These may show warnings but are valid for inspection
(.-webview view)
(.-title view)
(.-disposed view)
```

## AI-Human Collaboration Protocol

### Development Workflow
1. **AI develops in REPL** - Test solutions entirely via REPL evaluation
2. **AI inspects state** - Verify internal data structures and view states
3. **AI requests UI verification** - Stop and ask human to confirm visual results
4. **Human confirms UX** - Verify rendering, interactions, visual appearance
5. **Iterate or commit** - Repeat or write validated changes to files

### Critical Pattern: UI Testing
When testing UI features, **always follow this sequence**:

```clojure
;; 1. AI evaluates code to create/update flare
(flare!+ {:key :test-feature :html [:h1 "New Feature"]})

;; 2. AI verifies internal state
(get (:flares @db/!app-db) :test-feature)
;; => {:view #object [...]}

;; 3. AI STOPS and asks human
```

**Never conclude UI success from REPL responses alone**. The presence of a view object in state doesn't confirm visual rendering. Always request human verification for:
- Flare appears/renders correctly
- Content displays as expected
- Interactions work (clicks, inputs, etc.)
- Styling and layout are correct

## Validation Examples

**Start with working examples**: Copy patterns from `flares_examples.cljs` rather than creating from scratch. The examples file contains tested code for all common scenarios.

**Panel Creation and Inspection**:
```clojure
;; Create panel
(flare!+ {:key :my-panel :html "<h1>Test</h1>" :title "Panel Test"})

;; Extract and inspect view
(def panel-view (-> @db/!app-db :flares :my-panel :view))

;; Verify panel exists and properties
{:exists? (boolean panel-view)
 :disposed? (.-disposed panel-view)
 :title (.-title panel-view)
 :active (.-active panel-view)
 :visible (.-visible panel-view)}

;; Inspect HTML content
(.-html (.-webview panel-view))
;; => "<h1>Test</h1>"
```

**Sidebar Creation and Inspection**:
```clojure
;; Create sidebar
(flare!+ {:key :sidebar-1 :html [:p "Sidebar content"]})

;; Extract sidebar view (different type than panel)
(def sidebar-view (-> @db/!app-db :flares :sidebar-1 :view))

;; Sidebar-specific properties
{:title (.-title sidebar-view)
 :description (.-description sidebar-view)
 :visible (.-visible sidebar-view)}

;; Access sidebar webview
(.-html (.-webview sidebar-view))
```

**Content Updates**:
```clojure
;; Update existing flare
(flare!+ {:key :my-panel :html [:h1 "Updated!"]})

;; Verify content changed
(.-html (.-webview panel-view))
;; => "<h1>Updated!</h1>"
```

**Message Handling**:
```clojure
;; Copy the "Bidirectional message example" from flares_examples.cljs
;; It shows complete working implementation with:
;; - JavaScript vscode.postMessage() calls
;; - Clojure message-handler function
;; - Extension-to-webview communication with post-message!+

;; Verify handler registered
(boolean (-> @db/!app-db :flares :msg-test :message-handler))

;; Test sending message to webview
(post-message!+ :msg-test {:type "command" :data "test"})
```

**Cleanup**:
```clojure
;; After close!, verify state cleaned up
(close! :my-flare)
(nil? (get (:flares @db/!app-db) :my-flare))
;; => true

;; Close all and verify
(close-all!)
(empty? (:flares @db/!app-db))
;; => true
```
