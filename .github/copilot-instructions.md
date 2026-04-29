For comprehensive development guidance, invoke the `@joyride-dev` agent.

## What Joyride Is

Joyride makes VS Code hackable in user space — the Emacs/ELisp model for VS Code. A ClojureScript scripting runtime powered by SCI (Small Clojure Interpreter), giving users full Extension Host access with live REPL interaction. Built with shadow-cljs, hot-reloading into the Extension Development Host.

## REPL Environments

- **Primary REPL**: Backseat Driver CLJS (`clojure_evaluate_code`, replSessionKey: `"cljs"`) — for developing the extension itself
- **User interaction**: Local Joyride (`joyride_evaluate_code`) — quick input, progress, questions
- **Testing user API**: Dev host Joyride (via `joyride.sci/eval-string` through Backseat Driver) — verify user-facing behavior
- **Shadow-cljs build tooling**: CLJ session (replSessionKey: `"clj"`) — build system only

When the user reports issues, they occurred in the Extension Development Host — use the Backseat Driver REPL to investigate.

## State Inspection Safety

- `@db/!app-db` — always `dissoc :extension-context` before inspecting (circular references)
- VS Code API objects — use `select-keys` or `dissoc`, never print raw
- When-contexts — `(:contexts @when-contexts/!db)`
- nREPL state — `@nrepl/!db`
