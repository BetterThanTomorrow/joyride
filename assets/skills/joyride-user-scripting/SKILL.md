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

## Clojure Dependencies — `deps.edn`

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.11.54"}
        funcool/promesa {:mvn/version "9.0.471"}}
 :paths ["src" "scripts"]}
```

This file configures the classpath for clojure-lsp analysis. The Promesa dependency is for clojure-lsp — at runtime, prefer `^:async`/`await`.

## clojure-lsp Configuration

To get clojure-lsp to analyze user Joyride code:

1. Add a `:source-alias` to `<user-joyride-dir>/.lsp/config.edn`:
   ```clojure
   {:source-aliases #{:joyride-user}}
   ```

2. Add `:joyride-user` to your global/user `deps.edn` aliases:
   ```clojure
   {:aliases
    {:joyride-user {:extra-deps {joyride/user {:local/root "<user-joyride-dir>"}}}}}
   ```

## Version Control

Put the User Joyride directory under version control — treat it like dotfiles.

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

## Example: keybinding-palette

A personal command palette for your keybindings. Reads `keybindings.json` (including comments via JSONC parsing), renders keys with Unicode symbols (⌘⌥⇧⌃), and executes the selected command — including commands with arguments. Add `"title"` fields to your keybindings for best experience.

Demonstrates: npm dependency (`jsonc-parser`), Node.js `fs`/`path`/`os` modules, platform-specific path construction, QuickPick with `matchOnDescription` and `matchOnDetail`, command execution with args.

See [references/keybinding_palette.cljs](references/keybinding_palette.cljs) for the complete implementation.

### Installation

1. **Install the npm dependency** in the user Joyride directory:
   ```bash
   cd ~/.config/joyride && npm install jsonc-parser
   ```

2. **Create the source file**: Use `Joyride: Create User Source File...` → name it `keybinding_palette`, then paste the reference content. Or copy the file directly to `~/.config/joyride/src/keybinding_palette.cljs`.

3. **Add the keybinding** to VS Code's `keybindings.json`:
   ```json
   {
     "title": "Keybinding Command Palette",
     "key": "ctrl+alt+j ctrl+alt+j",
     "command": "joyride.runCode",
     "args": "(require '[keybinding-palette :as kp] :reload) (kp/show-palette!+)"
   }
   ```

4. **Add `"title"` fields** to other keybindings in `keybindings.json` — these become the searchable labels in the palette. Keybindings without titles are filtered out.

**Platform note:** The `keybindings-path` function currently constructs a macOS-specific path (`~/Library/Application Support/...`). For Linux or Windows, adapt the path construction to use the appropriate VS Code config directory.
