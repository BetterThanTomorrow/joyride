---
description: 'Joyride extension source code conventions — async patterns, state management, namespace separation, and naming. Supplements universal Clojure patterns with Joyride-specific rules.'
applyTo: 'src/joyride/**,test/joyride/**'
---

# Joyride Source Conventions

## Naming Conventions

- `function!` — side effects
- `function+` — returns promise
- `function!+` — both side effects and promise
- `function?` — predicate
- `!atom-name` — atom (e.g., `!app-db`, `!db`, `!last-ns`)
- `defn-` — private helpers

## Async Convention

- Extension source code: `promesa.core` (`p/let`, `p/do`, `p/chain`)
- User scripts: SCI async/await (`^:async` + `await`) — Promesa only for advanced combinators
- Never mix paradigms within a module

## State Management

- App state in `db/!app-db` central atom
- Domain state in isolated atoms (`when-contexts/!db`, `nrepl/!db`)
- Access via accessor functions: `(db/extension-context)`, `(db/output-terminal)` — never direct deref in helpers
- Pass data explicitly to helper functions — helpers must not access atoms directly
- Deprecated accessors: `^{:deprecated "version"}` metadata with migration path documented

## Pure/Impure Separation

- Pure logic in sub-namespaces: `lm.eval.core` (pure), `lm.eval.validation` (pure), `lm.evaluation` (side effects)
- Pure namespaces: no VS Code requires, unit testable, deterministic
- Impure namespaces: VS Code + extension context dependent, integration testable

## VS Code API Preference

- File operations: `vscode/workspace.fs` over `node/fs` (remote-friendly)
- Always push disposables to `context.subscriptions`
- Set when-context BEFORE creating dependent resources

## Error Handling

- User-facing boundaries: try/catch with error message or terminal output
- Optional integrations: no-op gracefully (e.g., `(when calva-available? ...)`)
- LM tool bracket validation: reject unbalanced before eval
- Errors always visible somewhere — never swallowed silently
