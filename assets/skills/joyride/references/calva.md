# Calva from Joyride

Load when standing up or connecting a Calva REPL from Joyride or from a task.

Task graph, matcher wait, window reload, and `"ok"`: [vscode.md](vscode.md). **jack-in** starts and connects. **connect** attaches to an already running nREPL (the task-started server).

`calva.autoConnectRepl` is connect-on-open when a port file exists. That is a different path from this task + activate pattern.

## calva.jackIn

Command `calva.jackIn`. Options:

- `connectSequence` — a sequence object, or a string name Calva looks up

Pass a sequence object when you want to name the project type and the project root in the call. Fields:

- `name`
- `projectType` — `"babashka"`, `"deps.edn"`, …
- `cljsType` — `"none"` when the project has no ClojureScript
- `projectRootPath` — absolute path. Calva uses it as the project root.
- `menuSelections` — optional. For `deps.edn`, `:cljAliases []` launches with no aliases. Leave `:cljAliases` out and Calva asks which aliases to launch with when the project has aliases.

The command returns a JavaScript object, which trips Joyride's printer up. End the evaluation with `"ok"`.

Both these are prompt-less for the user:

```clojure
(vscode/commands.executeCommand
  "calva.jackIn"
  (clj->js {:connectSequence {:name "Babashka"
                              :projectType "babashka"
                              :cljsType "none"
                              :projectRootPath "/absolute/path/to/project"}}))
"ok"
```

`deps.edn`, we provide `cljAliases` to suppress the prompt:

```clojure
(vscode/commands.executeCommand
  "calva.jackIn"
  (clj->js {:connectSequence {:name "deps.edn"
                              :projectType "deps.edn"
                              :cljsType "none"
                              :projectRootPath "/absolute/path/to/project"
                              :menuSelections {:cljAliases []}}}))
"ok"
```

Inform yourself from project documentation and/or the user which alieses should be used.

## calva.connect

Command `calva.connect`. Options:

- `connectSequence` — string name of a `calva.replConnectSequences` entry, or a sequence object
- optional `host`, `port`

Pass the sequence name as a string. Calva looks it up with `getConnectSequences`. From Joyride:

```clojure
(vscode/commands.executeCommand
  "calva.connect"
  (clj->js {:connectSequence "the-sequence"}))
```

`calva.autoSelectNReplPortFromPortFile` defaults true: if the port file exists, no host:port prompt.

No port file → prompt reason `no-port-file` (`No nREPL port file found. Enter host:port for the nREPL server.`). That is connect before nREPL has written the port file. Typical cause: `executeTask` of the background `repl` task, which does not wait for the matcher.

## listSessions

Calva exports `v1.repl`. `replType` is e.g. `"clj"`.

```clojure
(some-> (vscode/extensions.getExtension "betterthantomorrow.calva")
        .-exports .-v1 .-repl .listSessions)
```

## connect! and start!

Optional workspace helpers (e.g. `.joyride/src/repl_connect.cljs`). Full copy-paste example: [joyride-workspace-scripting](../../joyride-workspace-scripting/SKILL.md#calva-connect-helpers).

- `connect!` `{:connect-sequence :session-key}` — `calva.connect` with that sequence. Last value `"ok"`.
- `start!` `{:connect-task :repl-task :connect-sequence :session-key}`:
  - already a Calva session named `:session-key` → skip
  - `:repl-task` already in `vscode/tasks.taskExecutions` → `connect!` only (do not `executeTask` the connect task)
  - else `executeTask` the **connect** leaf (not the parent compound)

`:session-key` is Calva's `replSessionKey` (from `replSessionNames.primary`). Do not default it to `"clj"` — two Clojure REPLs share `replType` `"clj"`.
