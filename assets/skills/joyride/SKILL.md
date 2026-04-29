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

Joyride uses SCI's built-in `^:async`/`await` for async operations. No require needed.

```clojure
;; Use awaitResult: true for all of these:

;; Define async functions with ^:async metadata
(defn ^:async get-user-input []
  (let [input (await (vscode/window.showInputBox #js {:prompt "Enter value:"}))]
    (when input
      (vscode/window.showInformationMessage (str "You entered: " input)))))

;; Capture async result for later REPL use
(defn ^:async find-scripts []
  (let [files (await (vscode/workspace.findFiles "**/*.cljs"))]
    (def found-files files)))
;; Now `found-files` is available in the namespace

;; File reading
(defn ^:async read-content []
  (let [content (await (joyride.core/slurp "some/file.csv"))]
    (def file-content content)))

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

`clojure.core`, `clojure.set`, `clojure.string`, `clojure.walk`, `clojure.data`, `clojure.edn`, `clojure.zip`, `promesa.core` (partial, legacy — prefer `^:async`/`await`), `rewrite-clj`

## Error Handling

```clojure
(try
  (some-operation)
  (catch js/Error e
    {:error (.-message e)}))

;; Async error handling
(defn ^:async safe-operation []
  (try
    (await (risky-async-operation))
    (catch js/Error error
      (vscode/window.showErrorMessage (str "Error: " (.-message error))))))
```

## Backseat Driver Boundary

When both Joyride and Backseat Driver are installed: **"use the REPL" means `joyride_evaluate_code`**, not `clojure_evaluate_code`. Joyride's REPL runs in the Extension Host with promise-aware semantics. Other Backseat Driver tools (ClojureDocs, symbol info, structural editing, output log) remain useful for Joyride work.

## Data-Oriented Design Principles

- **Functions take args, return results.** Side effects are a last resort.
- **Prefer namespaced keywords** with synthetic namespaces (`:my-tool/name`, `:picker/label`) to group related keys. Destructure with `:ns/keys`.
- **Prefer flatness over depth** — flat maps with namespaced keys beat nested maps.
- **Use threading macros** (`->`, `->>`, `some->`) for readable data pipelines.
- **Use `defonce`** for atoms holding UI state — prevents re-initialization on REPL reload.
- **Convert JS objects to Clojure data early.** Use `js->clj`, `(seq js-array)`, or selective property access at the boundaries. Work with Clojure data internally.

### Gather → Transform → Act

Structure code as a data pipeline: gather data from VS Code, transform as pure Clojure data, then act with side effects.

```clojure
(defn ^:async show-doc-info []
  (let [editor vscode/window.activeTextEditor
        doc-data {:doc/uri     (-> editor .-document .-uri .-fsPath)
                  :doc/lang    (-> editor .-document .-languageId)
                  :cursor/line (-> editor .-selection .-active .-line)}]
    (when (= (:doc/lang doc-data) "clojure")
      (vscode/window.showInformationMessage
       (str "Clojure file at line " (:cursor/line doc-data))))))
```

The data map IS the testable unit — print it, filter it, assert on it in the REPL.

### Look for Prior Art

Before building something new, check the user's existing Joyride scripts and source:

```clojure
(defn ^:async find-existing-scripts []
  (let [files (await (vscode/workspace.findFiles
                      (str (joyride/user-joyride-dir) "/**/*.cljs")))]
    (mapv #(.-fsPath %) files)))
```

## REPL State vs Script Execution

- **REPL evaluation**: Definitions exist only in the REPL session memory
- **Script execution**: Loading a file restores all file-based definitions, overwriting REPL changes
- **Redefined functions**: The REPL state IS the current truth until the next script reload

If functions seem to "not work" after REPL definition:
1. Verify the function exists: `(ns-publics 'my.namespace)`
2. Check current namespace: `*ns*`
3. Confirm namespace targeting in evaluation tools

## UI Testing Process

The human developer is the source of truth for UI behavior:

1. Think about what should be tested
2. Summarize the test and expectations to the human
3. Evaluate the test using `awaitResult: true`
4. When the evaluation returns — examine results, ask the human for feedback in chat, stop and listen
5. Iterate

## Fluent JS Object Configuration

Combine `doto` with threading for setting properties on JS objects:

```clojure
(let [picker (vscode/window.createQuickPick)]
  (doto picker
    (-> .-items (set! items))
    (-> .-placeholder (set! "Select..."))
    (-> .-canSelectMany (set! false))
    (.onDidAccept handler)
    (.show)))
```

## Fetching Web Resources

```clojure
(defn ^:async fetch-readme []
  (let [response (await (js/fetch "https://raw.githubusercontent.com/user/repo/main/README.md"))
        text     (await (.text response))]
    (def readme-content text)))
```

## SCI / Scittle Async Gotchas

- Use `^:async` + `await` for async functions in SCI — no require needed
- `js-await` is Squint-specific — fails in SCI with "Unable to resolve symbol"
- Top-level `await` is unsupported — must be inside an `^:async` function
- `promesa.core` is available but legacy — prefer `^:async`/`await` for new code

## Anti-Patterns

| Anti-pattern | Correction |
|---|---|
| `println` / `js/console.log` to inspect | Evaluate sub-expressions directly in the REPL |
| Top-level side effects outside `defn` | Wrap in functions; use script execution guard |
| `load-file` (Clojure built-in) | Use `joyride/load-file` (async, returns promise) |
| `reify` or `deftype` for JS interfaces | Use `#js {}` with function values |
| Forgetting to dispose UI elements | Hold references; register with extension context |
| Reading `@atom` in pure functions | Pass data as function arguments |
| Deep nested maps | Flat maps with namespaced keywords |
| Forward declaring functions | Define before use — rearrange file order |
| Hardcoded fallback configs | Fail fast with clear error messages |
| Mixing business logic with side effects | Pure functions for decisions; thin side-effect layer |
| Starting from scratch | Check existing scripts first (look for prior art) |

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Function not found after REPL eval | Check namespace targeting — may have ended up in `user` |
| Promise result is `#object[Promise]` | Use `await` inside `^:async` fn, or `awaitResult: true` in eval tool |
| Extension API returns nil | Check `isActive` — extension may not be activated yet |
| Status bar item not showing | Call `.show` and verify it's not disposed |
| Script runs on `require` | Add script execution guard |
| `load-file` not working | Use `joyride.core/load-file` (async version) |

## Testing

Pure-function-first design enables testing in the REPL with `cljs.test`:

```clojure
(do (require 'run-all-tests :reload) (run-all-tests/run!+))
```

## References

- [Joyride repo](https://github.com/BetterThanTomorrow/joyride)
- [Flare API docs](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md#joyrideflare)
- [VS Code API](https://code.visualstudio.com/api/references/vscode-api)
- [Promesa docs](https://funcool.github.io/promesa/latest/) (legacy — prefer `^:async`/`await`)
