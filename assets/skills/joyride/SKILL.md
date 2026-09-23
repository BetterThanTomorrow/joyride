---
name: joyride
description: >-
  Joyride core — REPL evaluation, async patterns, VS Code API access,
  Flares, JS interop, and available libraries. Use when: working with
  Joyride evaluation, writing ClojureScript in any Joyride context,
  creating Flares/WebViews, exploring VS Code APIs via the REPL, or
  using joyride_evaluate_code.
---
# Joyride — Core Skill

> **Reference files**: This document links to `references/*.cljs` files. Resolve them relative to this file's path using `read_file`.

Joyride is user-space scripting for VS Code. SCI ClojureScript in the Extension Host, with a live REPL. Thus Joyride makes VS Code hackable in user space — the Emacs/ELisp model for VS Code.

NB: Joyride's MCP is its own server. `joyride_evaluate_code` does not go through Backseat Driver.

NB: Clojure conventions apply. This is SCI: `^:async` / `await`, not Squint's `js-await`.

Deeper layers (load when needed):

- [vscode.md](references/vscode.md) — printable last values; commands from a task; background `dependsOn` and matcher wait
- [calva.md](references/calva.md) — Calva REPL from Joyride or a task
- [standup.md](references/standup.md) — open a folder / start tasks (external agents)
- [flares_examples.cljs](references/flares_examples.cljs) — Flare examples

## Tools

### `joyride_evaluate_code`

Execute ClojureScript in VS Code's Extension Host. **"Use the REPL" means this tool.**

**Parameters:**

- `code` — ClojureScript to evaluate
- `who` — stable kebab-case slug identifying you as an evaluator (e.g. `joyride-helper`)
- `namespace` — target namespace (defaults to `user`). When showing code to the user, prepend `(in-ns 'namespace)` in the code block
- `awaitResult` — whether to wait for async resolution

**`awaitResult` decision:**

- `true` — when you need the resolved value: user input dialogs, file operations, extension API calls, messages with buttons
- `false` (default) — synchronous operations, fire-and-forget async (e.g. simple information messages). **Never** use `true` for sync code — it hangs indefinitely

## Interactive programming

1. Explore first — evaluate subexpressions to understand current state
2. Test incrementally — build from small verified pieces
3. Validate continuously — check each step before proceeding
4. Evaluate subexpressions, not `println` — direct evaluation gives actual data
5. Show visual results — information messages, Flares, or markdown previews
6. Only update files when asked — prefer REPL exploration

## Joyride Core API

```clojure
(require '[joyride.core :as joyride])

joyride/*file*
(joyride/invoked-script)          ; nil in the REPL
(joyride/extension-context)
(joyride/output-channel)
joyride/user-joyride-dir
joyride/slurp                     ; async
joyride/load-file                 ; async; SCI has no load-file
(joyride/js-properties obj)       ; Get all properties of a JS object
```

## VS Code API Access

```clojure
(require '["vscode" :as vscode])
(require '["fs" :as fs] '["path" :as path])

(vscode/window.showInformationMessage "Hello!")
(some-> vscode/window.activeTextEditor .-document .-fileName)
```

Use another extension only after `getExtension` and `.-isActive`. Convert JS to Clojure data at the edge (`js->clj`, `seq`, or pick properties). Work with maps inside. Commands, tasks, and printable last values: [vscode.md](references/vscode.md).

## ns `:require`

In `scripts/` and `src/` files, every namespace you call as `ns/sym` belongs on the `ns` `:require`. SCI resolves those symbols when the file loads. A `(require ...)` inside a function — including inside `try` — runs too late.

```clojure
(ns my-script
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [my.lib]))
```

REPL eval can `require` as you go.

## Async

```clojure
(defn ^:async get-user-input []
  (let [input (await (vscode/window.showInputBox #js {:prompt "Enter value:"}))]
    input))

;; Capture async result for later REPL use
(defn ^:async find-scripts []
  (let [files (await (vscode/workspace.findFiles "**/*.cljs"))]
    (def found-files files)))
;; Now `found-files` is available in the namespace

;; File reading
(defn ^:async read-content []
  (let [content (await (joyride.core/slurp "some/file.csv"))]
    (def file-content content)))

```

No top-level `await`. Use `promesa.core` for this. For non-top-level: prefer `^:async` / `await`.

## Flares (Joyride powered Webviews)

```clojure
(require '[joyride.flare :as flare])

(flare/flare!+ {:html [:h1 "Hello"] :title "My Flare" :key "example"})
(flare/flare!+ {:html [:div [:h2 "Sidebar"]] :key :sidebar-1})  ; slots 1-5
```

`(flare/close! key)`, `(flare/ls)`, `(flare/close-all!)`. Styles are maps. Messaging: `:message-handler` and `post-message!+`. More: [flares_examples.cljs](references/flares_examples.cljs).

## Scripts vs source

- `scripts/` — run from the Joyride script menus
- `src/` — libraries, shortcuts, `joyride.runCode`

Classpath, first match wins: workspace `.joyride/src`, `.joyride/scripts`, then user `~/.config/joyride/src` and `scripts`.

Libraries in the host: `clojure.core`, `clojure.set`, `clojure.string`, `clojure.walk`, `clojure.data`, `clojure.edn`, `clojure.zip`, `rewrite-clj`, `babashka.fs`.

## Design

Gather from VS Code, transform as data, then act. Namespaced keywords. `defonce` for atoms that must survive a reload. Look at existing user and workspace scripts before writing a new one.

REPL defs live in memory until a script load overwrites them. If a function "vanished", check `*ns*` and `ns-publics`.

### Data-Oriented Design Principles

- **Functions take args, return results.** Side effects are a last resort.
- **Prefer namespaced keywords** with synthetic namespaces (`:my-tool/name`, `:picker/label`) to group related keys. Destructure with `:ns/keys`.
- **Prefer flatness over depth** — flat maps with namespaced keys beat nested maps.
- **Use threading macros** (`->`, `->>`, `some->`) for readable data pipelines.
- **Use `defonce`** for atoms holding UI state — prevents re-initialization on REPL reload.
- **Convert JS objects to Clojure data early.** Use `js->clj`, `(seq js-array)`, or selective property access at the boundaries. Work with Clojure data internally.
## Shared traps (summary)

Load [vscode.md](references/vscode.md) / [calva.md](references/calva.md) when you hit these:

- Printable last value — end scripts and `joyride.runCode` with `"ok"` (or another Clojure value); JS objects throw on `IWriter`
- `executeTask` the connect **leaf**, not a parent that only `dependsOn` it
- After reload, if `repl` is already running → call connect (do not re-`executeTask` connect; the matcher will not fire)
- Session key, not default `"clj"`, when two Clojure REPLs share `replType`

(SCI `ns` `:require` at file load is under **ns `:require`** above.)

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

## Frequent mistakes

- `load-file` instead of `joyride/load-file`
- `reify` / `deftype` for JS — use `#js {}`
- `js-await` (Squint). SCI will not resolve it
- Forgetting to dispose UI objects
- `executeTask` as a restart (stop matching `taskExecutions` first, then start again)
