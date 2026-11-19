# VS Code from Joyride

Load when running VS Code commands or tasks.

## Any VS Code command from a task

A task cannot call a VS Code command directly. A command-typed input runs `joyride.runCode`, and a shell task echoes the input so VS Code resolves it.

Swap the command id and args for any VS Code command. The `"ok"` at the end stays. Calva connect from a task: [calva.md](calva.md).

## Background tasks and dependsOn

A background shell task starts a long-running process (`isBackground` true). A problem matcher `endsPattern` is the ready signal. Optional `runOptions.instanceLimit` 1.

The `connect` task `dependsOn` the `repl` task (`dependsOrder` sequence). VS Code waits for the matcher before running `connect`. That is the cold-start wait.

`executeTask` of the `repl` task itself does **not** wait for the matcher; it resolves when the process starts. The matcher has not fired yet.

From Joyride, `executeTask` the **`connect` leaf**, not a parent compound that only `dependsOn` it. An optional compound default build that `dependsOn` `connect` is for humans (Run Task). `executeTask` of that compound may start `repl` without running `connect`.

A window reload often leaves the **`repl` task** running. `connect` is a one-shot `echo` of an input, so it is not in `taskExecutions`. `dependsOn` then waits for the matcher to fire again, which it will not, so `connect` never runs. If `repl` is already in `vscode/tasks.taskExecutions`, invoke the command the `connect` task would have run. Do not `executeTask` `connect`. Calva: [calva.md](calva.md).

Generic labels: `repl` (background nREPL, `endsPattern` `nREPL server started`), `connect` (the echo task).

```json
{
  "version": "2.0.0",
  "inputs": [
    {
      "id": "runTheCommand",
      "type": "command",
      "command": "joyride.runCode",
      "args": "(require '[\"vscode\" :as vscode]) (vscode/commands.executeCommand \"the.command\") \"ok\""
    }
  ],
  "tasks": [
    {
      "label": "repl",
      "type": "shell",
      "command": "… nREPL …",
      "isBackground": true,
      "runOptions": { "instanceLimit": 1 },
      "problemMatcher": {
        "owner": "repl",
        "pattern": {
          "regexp": "^(~never~)(.*)$",
          "file": 1,
          "message": 2
        },
        "background": {
          "activeOnStart": true,
          "beginsPattern": ".",
          "endsPattern": "nREPL server started"
        }
      }
    },
    {
      "label": "connect",
      "dependsOn": ["repl"],
      "dependsOrder": "sequence",
      "type": "shell",
      "command": "echo ${input:runTheCommand}",
      "problemMatcher": [],
      "presentation": {
        "reveal": "silent",
        "close": true,
        "panel": "shared",
        "showReuseMessage": false
      }
    }
  ]
}
```

A human default build can `dependsOn` `connect`. Do not `executeTask` that compound from Joyride.

## Restarting a task

`vscode/tasks.fetchTasks` and `executeTask` are async. A second `executeTask` while that task is running does nothing. Restart: stop matching `taskExecutions`, then start again. The process must actually die so the port frees (a `bb` REPL is the usual case). Restart a dependent chain from the task that owns it.
