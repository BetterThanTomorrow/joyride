---
name: joyride-workspace-scripting
description: >-
  Joyride Workspace scope scripting — scripts and source files in
  <workspace>/.joyride/. Covers workspace activation, project-specific
  automation, workspace vs user scope precedence, and team sharing.
  Use when: creating or editing Workspace scripts/source files, setting
  up workspace_activate.cljs, or building project-specific automation.
---

# Joyride Workspace Scripting

Workspace scope scripts and source files live in `<workspace>/.joyride/` and are scoped to the project.

If you haven't loaded the `joyride` skill yet, load it now — it covers core evaluation patterns, async handling, and API reference.

## Workspace Scope

- Scripts in `.joyride/scripts/` are runnable via `Joyride: Run Workspace Script`
- Source files in `.joyride/src/` are requireable by any Joyride code when the workspace is open
- Workspace files take **precedence** over User files on the classpath

### Classpath Precedence

1. `<workspace-root>/.joyride/src` (highest priority)
2. `<workspace-root>/.joyride/scripts`
3. `<user-home>/.config/joyride/src`
4. `<user-home>/.config/joyride/scripts`

A workspace `my-utils` namespace shadows a user `my-utils` namespace.

## Workspace Activation

`workspace_activate.cljs` runs when Joyride activates in this workspace. Same disposable management pattern as user activation:

```clojure
(defonce !db (atom {:disposables []}))

(defn push-disposable! [disposable]
  (swap! !db update :disposables conj disposable)
  (.push (.-subscriptions (joyride/extension-context)) disposable))

(defn clear-disposables! []
  (run! (fn [d] (.dispose d)) (:disposables @!db))
  (swap! !db assoc :disposables []))
```

See [references/workspace_activate.cljs](references/workspace_activate.cljs) for a workspace activation example with event handler registration.

## Script Execution Guard

Same pattern as user scripts:

```clojure
(when (= (joyride/invoked-script) joyride/*file*)
  (main))
```

## Workspace vs User: When to Choose

| Choose Workspace | Choose User |
|---|---|
| Project-specific automation | Personal tools used across all projects |
| Team-shared scripts | Personal keyboard shortcuts |
| Build/test helpers for this project | Global status bar buttons |
| Project-specific VS Code configuration | Editor customizations |

## npm Dependencies

Install in the workspace Joyride directory or project root:

```bash
cd <workspace>/.joyride && npm install some-package
```

Or install at the project root — Joyride resolves from the workspace root too.

## Team Sharing

Include `.joyride/` in version control to share scripts with your team:

- `deps.edn` — Clojure dependencies (e.g., libraries for data processing)
- `scripts/` — Runnable automation for the project
- `src/` — Shared utility functions
- Consider adding a `README.md` inside `.joyride/` explaining the scripts

## Workspace Commands

- **Joyride: Run Workspace Script** — run a workspace script by name
- **Joyride: Create Workspace Activate Script** — creates `workspace_activate.cljs`
- **Joyride: Create Hello Joyride Workspace Script** — creates example script
