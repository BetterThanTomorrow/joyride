For comprehensive development guidance, invoke the `@joyride-dev` agent.

## What Joyride Is

Joyride makes VS Code hackable in user space — the Emacs/ELisp model for VS Code. A ClojureScript scripting runtime powered by SCI (Small Clojure Interpreter), giving users full Extension Host access with live REPL interaction. Built with shadow-cljs, hot-reloading into the Extension Development Host.

## REPL Environments

- **Primary REPL**: Backseat Driver CLJS (`clojure_evaluate_code`, replSessionKey: `"cljs"`) — for developing the extension itself
- **User interaction**: Local Joyride (`joyride_evaluate_code`) — quick input, progress, questions
- **Testing user API**: Dev host Joyride (via `joyride.sci/eval-string` through Backseat Driver) — verify user-facing behavior
- **Shadow-cljs build tooling**: CLJ session (replSessionKey: `"clj"`) — build system only

When the user reports issues, they occurred in the Extension Development Host — use the Backseat Driver REPL to investigate.

## Development Philosophy

REPL-first, data-oriented, functional, step-by-step, involve the human often. Prefer destructuring, maps, namespaced keywords, flatness. Side effects as last resort. `¬println` — prefer inline `def` for debugging.

Show code blocks with `in-ns` form first so the reader knows the namespace context.

## Naming Conventions

- `function!` — side effects
- `function+` — returns promise
- `function!+` — both
- `function?` — predicate
- `!atom-name` — atom (e.g., `!app-db`, `!db`)

## Key Namespaces

| Namespace | Purpose |
|---|---|
| `joyride.extension` | Entry point: activate, deactivate, hot-reload hooks |
| `joyride.db` | App state: `!app-db`, init-db, accessor functions |
| `joyride.sci` | SCI interpreter: namespaces, load-fn, eval-string |
| `joyride.config` | Path resolution: user-dir, workspace-dir |
| `joyride.lifecycle` | Activation scripts: user_activate, workspace_activate |
| `joyride.output` | Terminal output: ANSI colors, theme-aware, who-tracking |
| `joyride.when-contexts` | When-context management: `!db`, `set-context!` |
| `joyride.nrepl` | nREPL server: `!db`, middleware, bencode |
| `joyride.flare` | Flare API: panels, sidebars, hiccup, messaging |
| `joyride.flare.sidebar` | Sidebar provider: webview view registration |
| `joyride.flare.panel` | Panel creation: replicant rendering |
| `joyride.lm` | Language Model tool entry and registration |
| `joyride.lm.evaluation` | LM tool implementation: code execution, output capture |
| `joyride.lm.eval.core` | Pure functions: input validation, message formatting |
| `joyride.lm.eval.validation` | Pure validation: bracket check via parinfer |
| `joyride.who-tracking` | Evaluator awareness: cross-evaluator attribution |

## State Inspection Safety

- `@db/!app-db` — always `dissoc :extension-context` before inspecting (circular references)
- VS Code API objects — use `select-keys` or `dissoc`, never print raw
- When-contexts — `(:contexts @when-contexts/!db)`
- nREPL state — `@nrepl/!db`

## Watcher Gate

Verify watcher health after every code change:
- **shadow-cljs watch**: check task output — compilation errors are blocking
- **Test runner**: check Calva Output Log — failures are blocking (format: test name, file:line:column, expected/actual)
- **Problems panel**: linting info — address root causes
- Don't proceed to next change until current change is verified

## Hot Reload & Restart

- shadow-cljs watches `src/`, compiles on save, hot-reloads into dev host
- `package.json` changes (commands, views, config, activation events, when-contexts) require dev host restart
- Restart resets REPL state completely — human executes restart, AI never restarts directly
- Some changes may need calling the extension's `activate` function to re-register commands/hooks

## Testing Layers

- **Unit tests** (`test/joyride/`): Pure logic, no VS Code APIs, `cljs.test`
- **Integration tests** (`vscode-test-runner/workspace-1/.joyride/src/integration_test/`): VS Code API dependent, `deftest-async`, run with `npm run integration-test`
- **REPL manual testing**: AI evaluates, human verifies UI

## Structural Editing

All Clojure file edits use structural editing tools (`replace_top_level_form`, `insert_top_level_form`, `clojure_append_code`) — never text replacement. Multiple edits: bottom-to-top (highest line number first).

## Async Convention

- Extension source code: `promesa.core` (`p/let`, `p/do`, `p/chain`)
- User scripts: SCI async/await (`^:async` + `await`) — Promesa only for advanced combinators
