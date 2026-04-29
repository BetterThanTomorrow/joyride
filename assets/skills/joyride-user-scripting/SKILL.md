---
name: joyride-user-scripting
description: >-
  Joyride User scope scripting — scripts and source files in
  ~/.config/joyride/. Covers user activation scripts, global keyboard
  shortcuts, disposable management, and npm dependencies. Use when:
  creating or editing User scripts/source files, setting up
  user_activate.cljs, or configuring global keybindings.
---

# Joyride User Scripting

> **Reference files**: This document links to `references/*.cljs` files. Resolve them relative to this file's path using `read_file`.

User scope scripts and source files live in `~/.config/joyride/` and are available globally across all VS Code windows.

If you haven't loaded the `joyride` skill yet, load it now — it covers core evaluation patterns, async handling, and API reference.

## Activation Script

`user_activate.cljs` runs automatically when Joyride activates. Use it to set up persistent UI elements, event handlers, and other disposables.

The canonical pattern uses a `!db` atom to track disposables for clean reload:

```clojure
(defonce !db (atom {:disposables []}))

(defn push-disposable! [disposable]
  (swap! !db update :disposables conj disposable)
  (.push (.-subscriptions (joyride/extension-context)) disposable))

(defn clear-disposables! []
  (run! (fn [d] (.dispose d)) (:disposables @!db))
  (swap! !db assoc :disposables []))
```

See [references/user_activate.cljs](references/user_activate.cljs) for the full activation script with the Joy status bar button.

## Script Execution Guard

Scripts run their main logic only when invoked as a script, not when loaded in the REPL:

```clojure
(when (= (joyride/invoked-script) joyride/*file*)
  (main))
```

Without this guard, loading a script namespace in the REPL triggers its side effects.

## Source Files and `^:export`

Source files in `src/` expose functions callable from keyboard shortcuts:

```clojure
(ns my-utils
  (:require ["vscode" :as vscode]))

(defn ^:export my-command []
  (vscode/window.showInformationMessage "Hello!"))
```

Call from keybinding:
```json
{
  "key": "ctrl+alt+j m",
  "command": "joyride.runCode",
  "args": "(require '[my-utils :as u] :reload) (u/my-command)"
}
```

See [references/joy_button.cljs](references/joy_button.cljs) for a complete source file example with status bar items and QuickPick.

## Keyboard Shortcuts

Add to VS Code's `keybindings.json`:

```json
// Run a specific script
{
  "key": "ctrl+alt+j u",
  "command": "joyride.runUserScript",
  "args": "my-script.cljs"
}

// Execute inline code (calling a function from src/)
{
  "key": "ctrl+alt+j r",
  "command": "joyride.runCode",
  "args": "(require '[my-ns :as m] :reload) (m/my-function)"
}
```

Use `:reload` during development to pick up changes.

## Managing Disposables

Register disposables with the extension context so they're cleaned up properly:

```clojure
(let [disposable (vscode/workspace.onDidOpenTextDocument handler)]
  (.push (.-subscriptions (joyride/extension-context)) disposable))
```

For reloadable scripts (activation scripts, status bar items), use the `clear-disposables!` + `push-disposable!` pattern from the activation section above. This prevents event handlers from piling up on re-runs.

## npm Dependencies

Install in the user Joyride directory:

```bash
cd ~/.config/joyride && npm install lodash
```

Then require in scripts:
```clojure
(require '["lodash" :as _])
```

## Getting Started Commands

- **Joyride: Create User Activate Script** — creates `user_activate.cljs`
- **Joyride: Create Hello Joyride User Script** — creates example script
- **Joyride: Create User Source File...** — creates a source file in `src/`
- **Joyride: Run User Script** — run a user script by name

## Example: git-fuzzy

A sophisticated source file demonstrating extension API access (vscode.git), progressive QuickPick batch-loading, live diff preview via `onDidChangeActive`, and item-level action buttons.

See [references/git_fuzzy.cljs](references/git_fuzzy.cljs) for the complete implementation.

Install as a user source file (`Joyride: Create User Source File...` → `git-fuzzy`) and bind to a shortcut:
```json
{
  "key": "ctrl+alt+j ctrl+alt+g",
  "command": "joyride.runCode",
  "args": "(require '[git-fuzzy :as gz] :reload) (gz/show-git-history!+)"
}
```
