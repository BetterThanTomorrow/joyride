---
name: joyride-workspace-scripting
description: >-
  Joyride Workspace scripts — scripts and source in <workspace>/.joyride/.
  Covers workspace activation, project-specific automation, Workspace scripts
  vs User scripts precedence, and team sharing. Use when creating or editing
  Workspace scripts or source, setting up workspace_activate.cljs, or building
  project-specific automation.
---

# Joyride Workspace Scripting

Workspace scripts and source files live in `<workspace>/.joyride/` and are scoped to the project.

If you haven't loaded the `joyride` skill yet, load it now — it covers core evaluation patterns, async handling, and API reference.

## Workspace scripts

- Scripts in `.joyride/scripts/` are runnable via `Joyride: Run Workspace Script`
- Source files in `.joyride/src/` are requireable by any Joyride code when the workspace is open
- Workspace scripts take **precedence** over User scripts on the classpath

### Classpath precedence

1. `<workspace-root>/.joyride/src` (highest priority)
2. `<workspace-root>/.joyride/scripts`
3. `<user-home>/.config/joyride/src`
4. `<user-home>/.config/joyride/scripts`

A workspace `my-utils` namespace shadows a user `my-utils` namespace.

## Workspace activation

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

## Script execution guard

Same pattern as user scripts:

```clojure
(when (= (joyride/invoked-script) joyride/*file*)
  (main))
```

## npm dependencies

Install in the workspace Joyride directory or project root:

```bash
cd <workspace>/.joyride && npm install some-package
```

Or install at the project root — Joyride resolves from the workspace root too.

## Clojure dependencies — `.joyride/deps.edn`

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.11.54"}
        funcool/promesa {:mvn/version "9.0.471"}}
 :paths ["src" "scripts"]}
```

The Promesa dependency is for clojure-lsp analysis — at runtime, prefer `^:async`/`await`.

## clojure-lsp configuration

To get clojure-lsp to analyze workspace Joyride code:

1. Add a `:source-alias` to `.joyride/.lsp/config.edn`:
   ```clojure
   {:source-aliases #{:joyride}}
   ```

2. Add a `:joyride` alias to the project root `deps.edn`:
   ```clojure
   {:aliases {:joyride {:extra-deps {joyride/workspace {:local/root ".joyride"}}}}}
   ```

## Team sharing

Include `.joyride/` in version control to share scripts with your team:

- `deps.edn` — Clojure dependencies (e.g., libraries for data processing)
- `scripts/` — Runnable automation for the project
- `src/` — Shared utility functions
- Consider adding a `README.md` inside `.joyride/` explaining the scripts and keybindings team members should add

Team members need the Joyride extension installed. Scripts auto-run via `workspace_activate.cljs` and appear in the workspace script menu.

## Workspace scripts vs User scripts

| | Workspace scripts (`.joyride/`) | User scripts (`~/.config/joyride/`) |
|---|---|---|
| Applies to | This project only | All workspaces |
| Shareable with the team | Yes — commit to the repo | No — personal setup |
| Classpath | Wins over User scripts | Provides defaults |
| Typical use | Project tooling, build helpers | Personal editor customizations |
| Activation | `workspace_activate.cljs` | `user_activate.cljs` |

Put something in User scripts when it helps in every workspace. Put it in Workspace scripts when it is project-specific or meant to be shared with the team.

## Workspace commands

- **Joyride: Run Workspace Script** — run a workspace script by name
- **Joyride: Create Workspace Activate Script** — creates `workspace_activate.cljs`
- **Joyride: Create Hello Joyride Workspace Script** — creates example script

## Calva connect helpers

Optional workspace library (e.g. `.joyride/src/repl_connect.cljs`) for activate / reload when a project uses a `repl` + `connect` task graph. Task traps and `"ok"`: load the core `joyride` skill's [vscode.md](../joyride/references/vscode.md) and [calva.md](../joyride/references/calva.md).

```clojure
(defn- session-named? [session-key]
  (boolean (and session-key
                (some (fn [s] (= session-key (.-replSessionKey s)))
                      (or (some-> (vscode/extensions.getExtension "betterthantomorrow.calva")
                                  .-exports .-v1 .-repl .listSessions)
                          #js [])))))

(defn- task-named? [label]
  (boolean (some (fn [ex] (= label (some-> ex .-task .-name)))
                 (.-taskExecutions vscode/tasks))))

(defn ^:async connect!
  [{:keys [connect-sequence session-key]}]
  (await (vscode/commands.executeCommand
          "calva.connect"
          (clj->js {:connectSequence connect-sequence})))
  "ok")

(defn ^:async start!
  [{:keys [connect-task repl-task session-key]
    :as opts}]
  (cond
    (session-named? session-key) nil
    (task-named? repl-task) (await (connect! opts))
    :else
    (when-let [task (->> (await (vscode/tasks.fetchTasks))
                         (filter (fn [t] (= connect-task (.-name t))))
                         first)]
      (await (vscode/tasks.executeTask task))))
  "ok")
```

Connect-task input:

```clojure
"(require '[repl-connect]) (repl-connect/connect! {:connect-sequence \"the-sequence\" :session-key \"the-session\"}) \"ok\""
```

Activate:

```clojure
(repl-connect/start! {:connect-task "connect"
                      :repl-task "repl"
                      :connect-sequence "the-sequence"
                      :session-key "the-session"})
```

`:session-key` is Calva's `replSessionKey`. Do not default it to `"clj"` when two Clojure REPLs share `replType` `"clj"`.
