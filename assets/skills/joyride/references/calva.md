# Calva from Joyride

Load when standing up or connecting a Calva REPL from Joyride or from a task.

Task graph, matcher wait, window reload, and `"ok"`: [vscode.md](vscode.md). **jack-in** starts and connects. **connect** attaches to an already running nREPL (the task-started server).

`calva.autoConnectRepl` is connect-on-open when a port file exists. That is a different path from this task + activate pattern.

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
