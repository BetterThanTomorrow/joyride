---
description: 'Essential patterns and workflows for developing the Joyride VS Code extension, including REPL environments, testing patterns, and development host interactions.'
applyTo: '*,**'
---

# Joyride Development Memory

Essential development workflows and REPL patterns for building and testing the Joyride VS Code extension.

## REPL Environment Architecture

Joyride development involves three distinct REPL environments that serve different purposes:

### 1. Backseat Driver CLJS REPL
- **Purpose**: The primary development environment for creating, inspecting, and bringing the new version of Joyride into existence. Building and hot-reloading the new Joyride extension into the Extension Development Host
- **Access**: Use `clojure_evaluate_code` tool with `replSessionKey: "cljs"`
- **Capabilities**: Can access and evaluate ClojureScript code that runs in the development extension host
- **Key Pattern**: Use this for developing, testing, and debugging new Joyride features and API changes

### 2. Local Joyride REPL
- **Purpose**: Running Joyride scripts in the current VS Code window (installed extension)
- **Access**: Use `joyride_evaluate_code` tool
- **Capabilities**: Limited to the installed/stable Joyride extension features
- **Key Pattern**: Use for VS Code automation tasks and user interaction during development

### 3. Dev Extension Host Joyride REPL
- **Purpose**: The REPL environment for testing new (under development) Joyride API and capabilities running in the Extension Development Host
- **Access**: Via Backseat Driver CLJS REPL using `joyride.sci/eval-string`
- **Capabilities**: Full access to new features, APIs, and changes under development
- **Key Pattern**: This is where you test new implementation before it's released

## Testing New Joyride Features

### Verification Pattern
Always test new Joyride features in the dev extension host to verify they work with the new implementation:

```clojure
;; Use CLJS REPL (replSessionKey: "cljs") for dev extension host access
(require '[joyride.sci :as jsci])

;; Evaluate Joyride code in the dev extension host
(jsci/eval-string "(require '[joyride.flare :as flare])
                   (flare/flare! {:html [:h1 \"🔥 Test from Dev Host\"]
                                  :title \"Feature Test\"
                                  :key :test-feature})")
```

### Why This Works
- The Backseat Driver CLJS REPL has access to the development extension host's Joyride environment
- `joyride.sci/eval-string` executes code within the dev extension host's SCI context
- This bypasses the local (installed) Joyride extension and tests the new code directly

## Development Host vs Local Extension

### Feature Availability Test
If a feature works in the dev extension host but not locally, it confirms:
- ✅ New implementation is working correctly
- ✅ Shadow CLJS hot-reload is functioning
- ✅ Extension Development Host has the updated code
- ❌ Local VS Code window still has old/stable extension

### Extension Configuration Changes
When modifying `package.json` (views, commands, contexts):
- Extension Development Host requires restart to pick up package.json changes
- Use VS Code's "Reload Window" command in the Extension Development Host
- Local VS Code window is unaffected and maintains old configuration

## Hot-Reload Development Flow

### Effective Pattern
1. **Edit ClojureScript source** - Shadow CLJS automatically hot-reloads into dev extension host
2. **Test via Backseat Driver REPL** - Use CLJS REPL to verify functionality
3. **Iterate rapidly** - Changes appear immediately without manual compilation
4. **Restart extension host** only when package.json changes are made

### REPL Session Selection
- **CLJS REPL** (`"cljs"`): For developing and testing new Joyride features, accessing dev extension host
- **CLJ REPL** (`"clj"`): For Shadow CLJS build system and tooling tasks
- **Local Joyride** (`joyride_evaluate_code`): For user interaction and current window automation (runs in CLJS REPL context)

## API Testing Strategy

### New API Validation
When implementing new Joyride APIs (like the sidebar-N keys):

```clojure
;; Test in dev extension host first
(jsci/eval-string "(require '[joyride.flare :as flare])
                   (flare/flare! {:html [:div \"Testing new API\"]
                                  :key :sidebar-1})")
```

### Testing Protocol for UI Features

**Critical Pattern**: When testing UI features that create visual elements, always follow this protocol:

1. **Execute the test code** in the dev extension host REPL
2. **Stop and ask the human** to verify the visual results
3. **Never conclude success** based solely on REPL return values
4. **Wait for human confirmation** before proceeding

**Why This Matters**:
- REPL responses show internal state, not user-visible results
- UI elements may be created but not rendered correctly
- Visual validation requires human inspection
- Premature conclusions skip critical verification steps

**Example Protocol**:
```clojure
;; Execute test
(jsci/eval-string "...test code...")
;; Result: {:sidebar #object [...]}

;; STOP HERE - Ask: "Can you confirm you see the expected UI element in the Extension Development Host?"
;; WAIT for human verification before continuing
```

### Feature Validation
Verify that new API implementations work as expected in the development environment before release.

## Structural Editing for Clojure Development

Use Clojure structural editing tools instead of generic text editing to maintain code integrity and bracket balance.

### Essential Pattern for Function Refactoring
When cleaning up or refactoring Clojure functions, use `replace_top_level_form` instead of generic text replacement:

```clojure
;; Use structural editing tools to replace entire function definitions
(replace_top_level_form
  {:filePath "src/my/namespace.cljs"
   :line 42
   :targetLineText "(defn my-function"
   :newForm "(defn my-function..."})
```

### Benefits of Structural Editing
- **Bracket integrity**: Automatically maintains proper bracket balance
- **Safe refactoring**: Replaces complete forms rather than partial text
- **Clojure-aware**: Understands Clojure syntax and structure
- **Error prevention**: Reduces syntax errors during complex edits

## Race Condition Debugging in VS Code Extensions

When debugging timing issues between VS Code API callbacks and extension logic, use systematic REPL-driven investigation.

### When Context vs Webview Creation Timing
**Problem Pattern**: VS Code webview creation depends on `when` contexts, creating potential race conditions:

1. Extension calls VS Code API to create webview
2. VS Code checks `when` context condition
3. If context is `false`, webview creation is deferred
4. If context becomes `true` later, webview creation happens asynchronously
5. Extension logic may assume webview exists before VS Code completes creation

### Debugging Strategy
**Use REPL to investigate state**:
```clojure
;; Check current app state
(select-keys @db/!app-db [:flare-sidebar-views :flare-sidebars])

;; Verify when context state
(:contexts @when-contexts/!db)

;; Test individual components
(sidebar/ensure-sidebar-view! slot-number)
```

### Solution Pattern: Context-First Approach
**Set when contexts BEFORE attempting to use dependent resources**:

```clojure
;; ❌ Wrong - race condition possible
(let [view (ensure-view! slot)]
  (set-context! slot true)  ; Too late - view creation already attempted
  (use-view view))

;; ✅ Correct - context-first approach
(set-context! slot true)    ; Enable VS Code resource creation
(let [view (ensure-view! slot)]  ; Now view can be created
  (use-view view))
```

This prevents race conditions where VS Code APIs depend on context state for resource creation.