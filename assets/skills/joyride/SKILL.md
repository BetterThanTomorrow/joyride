---
name: joyride
description: >-
  Joyride core — REPL evaluation, async patterns, VS Code API access,
  Flares, JS interop, and available libraries. Use when: working with
  Joyride evaluation, writing ClojureScript in any Joyride context,
  creating Flares/WebViews, exploring VS Code APIs via the REPL, or
  using joyride_evaluate_code / joyride_request_human_input tools.
---

# Joyride — Core Skill

> **Reference files**: This document links to `references/*.cljs` files. Resolve them relative to this file's path using `read_file`.

Joyride makes VS Code hackable in user space — the Emacs/ELisp model for VS Code. A ClojureScript scripting runtime powered by SCI (Small Clojure Interpreter), giving users full Extension Host access with live REPL interaction.

## Tools

### `joyride_evaluate_code`

Execute ClojureScript in VS Code's Extension Host. **"Use the REPL" means this tool.**

**Parameters:**
- `code` — ClojureScript to evaluate
- `who` — stable kebab-case slug identifying you as an evaluator (e.g., `joyride-helper`)
- `namespace` — target namespace (defaults to `user`). When showing code to the user, prepend `(in-ns 'namespace)` in the code block
- `awaitResult` — whether to wait for async resolution

**`awaitResult` decision:**
- `true` — when you need the resolved value: user input dialogs, file operations, extension API calls, messages with buttons
- `false` (default) — synchronous operations, fire-and-forget async (e.g., simple information messages). **Never** use `true` for sync code — it hangs indefinitely

### `joyride_request_human_input`

Ask the human for input or guidance. Give context in chat first, then use this tool.

## Interactive Programming Workflow

1. **Explore first** — evaluate subexpressions to understand current state
2. **Test incrementally** — build solutions from small verified pieces
3. **Validate continuously** — check that each step works before proceeding
4. **Evaluate subexpressions, not println** — direct evaluation gives actual data; println gives string representations
5. **Show visual results** — use information messages, Flares, or markdown previews to demonstrate what you build
6. **Only update files when asked** — prefer REPL exploration

## Joyride Core API

```clojure
(require '[joyride.core :as joyride])

joyride/*file*                    ; Current file path
(joyride/invoked-script)          ; Script being run (nil in REPL)
(joyride/extension-context)       ; VS Code extension context
(joyride/output-channel)          ; Joyride's output channel
joyride/user-joyride-dir          ; User joyride directory path
joyride/slurp                     ; Async. Accepts absolute or relative (to workspace) path
joyride/load-file                 ; Async. Use instead of `load-file` (not implemented in SCI)
(joyride/js-properties obj)       ; Get all properties of a JS object
```

## VS Code API Access

```clojure
(require '["vscode" :as vscode])

(vscode/window.showInformationMessage "Hello!")
(vscode/commands.executeCommand "workbench.action.files.save")
(some-> vscode/window.activeTextEditor .-document .-fileName)
(vscode/workspace.getConfiguration "editor")
```

### Extension APIs

```clojure
;; Check and use other extensions safely
(when-let [ext (vscode/extensions.getExtension "publisher.extension-name")]
  (when (.-isActive ext)
    (let [api (.-exports ext)]
      ;; Use extension API
      )))
```

## Async Patterns

```clojure
(require '[promesa.core :as p])

;; Use awaitResult: true for all of these:

;; User input
(p/let [input (vscode/window.showInputBox #js {:prompt "Enter value:"})]
  (when input
    (vscode/window.showInformationMessage (str "You entered: " input))))

;; Capture async result for later REPL use
(p/let [files (vscode/workspace.findFiles "**/*.cljs")]
  (def found-files files))
;; Now `found-files` is available in the namespace

;; File reading
(p/let [content (joyride.core/slurp "some/file.csv")]
  (def file-content content))

;; Loading namespace files
(joyride.core/load-file "src/my_namespace.cljs")
```

## Flares (WebViews)

Flares create WebView panels and sidebar views with Hiccup syntax.

```clojure
(require '[joyride.flare :as flare])

;; Panel with Hiccup (awaitResult: true)
(flare/flare!+ {:html [:h1 "Hello World!"]
                :title "My Flare"
                :key "example"})

;; Sidebar (slots 1-5 available)
(flare/flare!+ {:html [:div [:h2 "Sidebar"]]
                :key :sidebar-1})

;; From file (HTML or EDN with Hiccup)
(flare/flare!+ {:file "assets/my-view.html"
                :key "my-view"})

;; External URL
(flare/flare!+ {:url "https://example.com"
                :title "External Site"})
```

- **Styles**: use maps — `{:color :red :margin "10px"}`
- **Management**: `(flare/close! key)`, `(flare/ls)`, `(flare/close-all!)`
- **Messaging**: use `:message-handler` and `post-message!+` for bidirectional communication

For comprehensive examples including SVG, Scittle+Replicant, animations, and bidirectional messaging: see [references/flares_examples.cljs](references/flares_examples.cljs).

## JS Interop

```clojure
;; Node.js access
(require '["fs" :as fs] '["path" :as path])
(path/join segment-1 segment-2)
(fs/existsSync dir-path)

;; Object inspection
(joyride.core/js-properties some-object)
(js->clj some-js-object :keywordize-keys true)

;; Creating JS objects (deftype not available in SCI)
#js {:method (fn [x y] ...)}
```

## Scripts vs Source Files

- **Scripts** (`scripts/`) — runnable from `Joyride: Run User Script` / `Run Workspace Script` menus
- **Source files** (`src/`) — library functions callable from keyboard shortcuts and other scripts via `joyride.runCode` command

### Classpath resolution order

1. `<workspace-root>/.joyride/src`
2. `<workspace-root>/.joyride/scripts`
3. `<user-home>/.config/joyride/src`
4. `<user-home>/.config/joyride/scripts`

Workspace files take precedence over User files.

## Available Libraries

`clojure.core`, `clojure.set`, `clojure.string`, `clojure.walk`, `clojure.data`, `clojure.edn`, `clojure.zip`, `promesa.core` (partial), `rewrite-clj`

## Error Handling

```clojure
(try
  (some-operation)
  (catch js/Error e
    {:error (.-message e)}))

;; Async error handling
(p/catch
  (risky-async-operation)
  (fn [error]
    (vscode/window.showErrorMessage (str "Error: " (.-message error)))))
```

## Backseat Driver Boundary

When both Joyride and Backseat Driver are installed: **"use the REPL" means `joyride_evaluate_code`**, not `clojure_evaluate_code`. Joyride's REPL runs in the Extension Host with promise-aware semantics. Other Backseat Driver tools (ClojureDocs, symbol info, structural editing, output log) remain useful for Joyride work.
