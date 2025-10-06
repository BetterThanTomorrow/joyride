# Joyride Evaluation Tool - Agent Guide

Critically important contexts for agents to effectively use Joyride's evaluation capabilities for testing, exploring, and automating VS Code.

## What is Joyride Evaluation?

Joyride provides an AI agent tool that allows the agent to execute ClojureScript code directly in VS Code's runtime environment. This enables real-time testing, inspection, and automation of the editor. This enables Interactive Programming (aka REPL Driven Development).

## Core Evaluation Capabilities

### Code Execution Environment
- **Runtime**: Small Clojure Interpreter (SCI) with ClojureScript
- **Context**: Full VS Code Extension Host environment
- **APIs Available**: Complete VS Code Extension API + APIS exposed by any active extension
- **Execution Mode**: Synchronous and asynchronous support

### Async Operation Handling
The evaluation tool has an `awaitResult` parameter for handling async operations:

- **`awaitResult: false` (default)**: Returns immediately, suitable for
  * synchronous operations, returns the value of resulting from the evaluation
  * fire-and-forget async evaluations, returns a serialized representation of the promise)
- **`awaitResult: true`**: Waits for async operations to complete before returning results, returns the resolved value of the promise

**When to use `awaitResult: true`:**
- Any operation returning a promise/thenable where you need the actual resolved value for further processing, e.g.
  - User input dialogs where you need the response (`showInputBox`, `showQuickPick`)
  - Async file operations where you need the results (`findFiles`, `readFile`)
  - Extension API calls
  - Information messages with buttons where you need to know which was clicked
  - Etcetera

**When to use `awaitResult: false` (default):**
- When you want non-blocking behavior
- Synchronous operations
- Fire-and-forget async operations like information messages. Here it is crucial to not block on the message being dismissed, because the user may not even see the message.
- Side-effect async operations where you don't need the return value

**Example with awaitResult:**
```clojure
;; This will wait for user input and return the actual result
(require '["vscode" :as vscode])
(vscode/window.showInputBox #js {:prompt "Enter your name:"})
;; Use awaitResult: true to get the actual input value
```

### Examples of What You Can Do
1. **Test code snippets** before suggesting them to users
2. **Inspect VS Code state** (open files, workspace, configuration)
3. **Execute automation scripts** (file operations, UI interactions)
4. **Validate approaches** by running real code
5. **Explore VS Code APIs** interactively

## Essential APIs for Agents

**Important for agents**: To load namespaces/files into the REPL, instead of `load-file` (which isn't implemented in SCI) use the Joyride async version: `joyride.core/load-file`.

### VS Code API Access
```clojure
(require '["vscode" :as vscode])

;; Common inspection patterns
vscode/window.activeTextEditor.document.fileName ;; may bomb, but in the repl it may be fine
(some-> vscode/window.activeTextEditor .-document .-fileName) ;; nil safe access
(vscode/workspace.getConfiguration "editor")
```

### Joyride Core API
```clojure
(require '[joyride.core :as joyride])

;; Essential functions for agents:
joyride/*file*                    ; Current file path
(joyride/extension-context)       ; VS Code extension context
(joyride/output-channel)          ; Joyride's output channel
joyride/user-joyride-dir          ; User joyride directory
joyride/slurp                     ; Similar to Clojure `slurp`, but is async. Accepts absolute or relative (to workspace) path. Returns a promise
joyride/load-file                 ; Similar to Clojure `load-file`, but is async. Accepts absolute or relative (to workspace) path. Returns a promise
```

### VS Code Extension API
```clojure
(require '["vscode" :as vscode])

;; Get extension and check if active
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

## Joyride Flares - WebView Creation

Joyride Flares provide a convenient way to create WebView panels and sidebar views.

### Basic Usage
```clojure
(require '[joyride.flare :as flare])

;; Create a flare with Hiccup
(flare/flare!+ {:html [:h1 "Hello World!"]
                :title "My Flare"
                :key "example"})

;; Create sidebar flare (slots 1-5 available)
(flare/flare!+ {:html [:div [:h2 "Sidebar"] [:p "Content"]]
                :key :sidebar-1})

;; Load from file (HTML or EDN with Hiccup)
(flare/flare!+ {:file "assets/my-view.html"
                :key "my-view"})

;; Display external URL
(flare/flare!+ {:url "https://example.com"
                :title "External Site"})
```

**Note**: `flare!+` returns a promise, use `awaitResult: true`.

### Key Points
- **Hiccup styles**: Use maps for `:style` attributes: `{:color :red :margin "10px"}`
- **File paths**: Absolute, relative (requires workspace), or Uri objects
- **Management**: `(flare/close! key)`, `(flare/ls)`, `(flare/close-all!)`
- **Bidirectional messaging**: Use `:message-handler` and `post-message!+`

**Full documentation**: [API docs](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md#joyrideflare)

**Comprehensive examples**: [flares_examples.cljs](https://github.com/BetterThanTomorrow/joyride/blob/master/examples/.joyride/src/flares_examples.cljs)

## Common Evaluation Patterns

### Environment Inspection
```clojure
(require '["vscode" :as vscode])

;; Check what's currently open
(when-let [editor vscode/window.activeTextEditor]
  {:file (some-> editor .-document .-fileName)
   :language (some-> editor .-document .-languageId)
   :line-count (some-> editor .-document .-lineCount)})

;; Check workspace
(map #(.-fsPath (.-uri %)) vscode/workspace.workspaceFolders)

;; Check configuration
(-> (vscode/workspace.getConfiguration "editor")
    (.get "fontSize"))
```

### Testing File Operations
```clojure
(require '["vscode" :as vscode])

;; Safe file reading
(when-let [folders vscode/workspace.workspaceFolders]
  (let [root-path (some-> folders first .-uri .-fsPath)]
    ;; Test if file exists before operations
    ))
```

### Promise Handling
```clojure
(require '[promesa.core :as p])
(require '["vscode" :as vscode])

;; For testing async operations in evaluation tool, use this pattern:
;; Pattern for unwrapping async results when exploring, (use awaitResult: true)
(vscode/workspace.findFiles "**/*.cljs")

;; If you want to define the file vector in the REPL for later use, (still use awaitResult: true):
(p/let [files (vscode/workspace.findFiles "**/*.cljs")]
  (def found-files files))
;; Now `found-files` is defined in the namespace in the REPL
found-files ;=> the vector of file objects

;; Another example of the same thing: Test user interactions (use awaitResult: true)
(p/let [input (vscode/window.showInputBox #js {:prompt "Enter value:"})]
  (def user-input input))
;; Now `user-input` contains the result

;; File operations with joyride.core/slurp (use awaitResult: true)
(p/let [content (joyride.core/slurp "some/file/in/the/workspace.csv")]
  (def file-content content) ; if you want to use/inspect `content` later in the session
  ; Do something with the content
  )

;; Loading namespaces/files with joyride.core/load-file (use awaitResult: true)
(joyride.core/load-file "src/my_namespace.cljs")
;; File is now loaded and available

;; Fire-and-forget: Show message and continue (awaitResult: false)
(p/let [files (vscode/workspace.findFiles "**/*.cljs")]
  (vscode/window.showInformationMessage (str "Found " (count files) " files")))
;; (Technically this continues, i.e. unblocks you, and then the message is displayed async when the promise resolves.)

;; Wait for user response: Get which button was clicked (awaitResult: true)
(p/let [files (vscode/workspace.findFiles "**/*.cljs")
        choice (vscode/window.showInformationMessage
                 (str "Found " (count files) " files")
                 "Open First" "Cancel")]
  (when (= choice "Open First")
    ;; Do something with first file
    ))
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

## Exploration Guidelines

### Best Practices for Agents
1. **If possible: Start with REPL exploration** before suggesting modifications
2. **Test incrementally** - small steps first
3. **Validate assumptions** in the REPL
4. **Handle errors gracefully** in suggestions
5. **Check extension availability** before using extension APIs

## JavaScript Interop for Agents

### Node access

```clojure
(ns ...
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ...))

(path/join segment-1 segment-2)

(fs/existsSync dir-path)
```


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
(require '["vscode" :as vscode])

;; Get workspace overview
{:folders (map #(.-fsPath (.-uri %)) vscode/workspace.workspaceFolders)
 :active-file (some-> vscode/window.activeTextEditor .-document .-fileName)
 :language (some-> vscode/window.activeTextEditor .-document .-languageId)}
```

### Extension Availability Check
```clojure
(require '["vscode" :as vscode])

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
(require '["vscode" :as vscode])

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
(require '["vscode" :as vscode])

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
   * Show the user what you are evaluating by placing it in a code block in the chat before the evaluation tool use.
2. **Test Incrementally**: Build up complex operations from simple, tested parts
3. **Validate Continuously**: Check that each step works before proceeding
4. **Provide Context**: Include inspection results when suggesting solutions
5. **Handle Edge Cases**: Test with empty workspaces, no active files, etc.

## Important Notes for Agents

- **Promise Results**: Use `awaitResult: true` when you need the actual async result.
- **awaitResult Usage**: Set `awaitResult: true` for user interactions, file operations, and any async calls where you need the resolved value. Use `awaitResult: false` (default) asynchronous operations where you are _not_ interested in the result, to avoid blocking.
- **Error Handling**: JS property access that doesn't exist returns `nil` rather than throwing errors in many cases.
- **Extension Timing**: Always check if extensions are loaded and active before accessing their APIs.

The evaluation tool enables you to provide tested, working solutions rather than theoretical code suggestions. It is the ultimate reality check.
