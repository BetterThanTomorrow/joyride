# Joyride Evaluation Tool - Agent Guide

This document provides essential context for Language Learning Models (LLMs) to effectively use Joyride's evaluation capabilities for testing, exploring, and automating VS Code.

## What is Joyride Evaluation?

Joyride provides a Language Model Tool that allows agents to execute ClojureScript code directly in VS Code's runtime environment. This enables real-time testing, inspection, and automation of the editor.

## Core Evaluation Capabilities

### Code Execution Environment
- **Runtime**: Small Clojure Interpreter (SCI) with ClojureScript
- **Context**: Full VS Code Extension Host environment
- **APIs Available**: Complete VS Code API + Extension APIs
- **Execution Mode**: Synchronous and asynchronous support

### What You Can Do
1. **Test code snippets** before suggesting them to users
2. **Inspect VS Code state** (open files, workspace, configuration)
3. **Execute automation scripts** (file operations, UI interactions)
4. **Validate approaches** by running real code
5. **Explore VS Code APIs** interactively

## Essential APIs for Agents

### VS Code API Access
```clojure
["vscode" :as vscode]

;; Common inspection patterns
(-> vscode/window.activeTextEditor .-document .-fileName)
(-> vscode/workspace.workspaceFolders first .-uri .-fsPath)
(vscode/workspace.getConfiguration "editor")
```

### Joyride Core API
```clojure
[joyride.core :as joyride]

;; Essential functions for agents:
joyride/*file*                    ; Current file path
(joyride/extension-context)       ; VS Code extension context
(joyride/output-channel)          ; Joyride's output channel
joyride/user-joyride-dir          ; User joyride directory
```

### Extension APIs
```clojure
;; Better approach - get extension and check if active
(when-let [ext (vscode/extensions.getExtension "ms-python.python")]
  (when (.-isActive ext)
    (let [python-api (.-exports ext)]
      ;; Access Python environments
      (-> python-api .-environments .-known count)
      ;; Check if Jupyter is available
      (boolean (.-jupyter python-api)))))
```

## Available Libraries
- `clojure.core`, `clojure.set`, `clojure.string`, `clojure.walk`, `clojure.data`, `clojure.edn`
- `clojure.zip` - Tree manipulation
- `promesa.core` - Promise handling (partial support)
- `rewrite-clj` - Code manipulation

## Common Evaluation Patterns

### Environment Inspection
```clojure
;; Check what's currently open
(when-let [editor vscode/window.activeTextEditor]
  {:file (.-fileName (.-document editor))
   :language (.-languageId (.-document editor))
   :line-count (.-lineCount (.-document editor))})

;; Check workspace
(map #(.-fsPath (.-uri %)) vscode/workspace.workspaceFolders)

;; Check configuration
(-> (vscode/workspace.getConfiguration "editor")
    (.get "fontSize"))
```

### Testing File Operations
```clojure
;; Safe file reading
(when-let [folders vscode/workspace.workspaceFolders]
  (let [root-path (-> folders first .-uri .-fsPath)]
    ;; Test if file exists before operations
    ))
```

### Promise Handling
```clojure
[promesa.core :as p]

;; Always use p/let for async operations
(p/let [files (vscode/workspace.findFiles "**/*.cljs")]
  (count files))
```

### Error Handling
```clojure
;; Wrap risky operations
(try
  (some-operation)
  (catch js/Error e
    {:error (.-message e)}))
```

## Validation Strategies

### Before Suggesting Code
1. **Test the approach** with minimal code
2. **Verify API availability** (check if extensions are loaded)
3. **Validate file paths** and permissions
4. **Check workspace state** (folders, files, configuration)

### Code Testing Pattern
```clojure
;; 1. Test API availability
(when vscode/window.activeTextEditor
  ;; 2. Test core operation
  (let [doc (.-document vscode/window.activeTextEditor)]
    ;; 3. Validate result
    {:success true :file (.-fileName doc)}))
```

## Safe Exploration Guidelines

### Read-Only Operations (Always Safe)
- Inspecting workspace folders
- Reading file contents
- Checking configuration
- Querying editor state
- Exploring available APIs

### Write Operations (Use Carefully)
- File modifications
- Configuration changes
- UI state changes
- Installing event handlers

### Best Practices for Agents
1. **Start with inspection** before suggesting modifications
2. **Test incrementally** - small steps first
3. **Validate assumptions** about workspace state
4. **Handle errors gracefully** in suggestions
5. **Check extension availability** before using extension APIs

## JavaScript Interop for Agents

### Object Inspection
```clojure
;; Get all properties of a JS object
(joyride.core/js-properties some-object)

;; Convert JS to Clojure for inspection
(js->clj some-js-object :keywordize-keys true)
```

### Creating JS Objects
```clojure
;; Instead of deftype (not available in SCI)
#js {:method (fn [x y] ...)}
```

## Common Inspection Patterns

### Workspace Analysis
```clojure
;; Get workspace overview
{:folders (map #(.-fsPath (.-uri %)) vscode/workspace.workspaceFolders)
 :active-file (when-let [editor vscode/window.activeTextEditor]
                (.-fileName (.-document editor)))
 :language (when-let [editor vscode/window.activeTextEditor]
             (.-languageId (.-document editor)))}
```

### Extension Availability Check
```clojure
;; Check if extension is available before using
(when-let [ext (vscode/extensions.getExtension "ms-python.python")]
  (when (.-isActive ext)
    (let [python-api (.-exports ext)]
      ;; Get Python environment info
      {:python-envs (-> python-api .-environments .-known count)
       :jupyter-available (boolean (.-jupyter python-api))})))
```

### Configuration Inspection
```clojure
;; Get current editor configuration
(let [config (vscode/workspace.getConfiguration "editor")]
  {:font-size (.get config "fontSize")
   :tab-size (.get config "tabSize")
   :word-wrap (.get config "wordWrap")})
```

## Error Recovery Patterns

### Graceful Failure
```clojure
(try
  (risky-operation)
  {:success true :result result}
  (catch js/Error e
    {:success false :error (.-message e) :suggestion "Try alternative approach"}))
```

### Validation Before Action
```clojure
;; Always check preconditions
(cond
  (not vscode/window.activeTextEditor)
  {:error "No active editor"}

  (not (seq vscode/workspace.workspaceFolders))
  {:error "No workspace open"}

  :else
  {:ready true})
```

## Agent Workflow Recommendations

1. **Explore First**: Use evaluation to understand the current VS Code state
2. **Test Incrementally**: Build up complex operations from simple, tested parts
3. **Validate Continuously**: Check that each step works before proceeding
4. **Provide Context**: Include inspection results when suggesting solutions
5. **Handle Edge Cases**: Test with empty workspaces, no active files, etc.

This evaluation tool enables you to provide tested, working solutions rather than theoretical code suggestions.
