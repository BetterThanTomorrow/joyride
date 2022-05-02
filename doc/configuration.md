# Configuration

VS Code settings to configure Joyride.

There is no configuration. 😀

There are some conventions, though. Joyride looks for scripts in:

* User scripts: `<user home>/.config/joyride/scripts/**/*.cljs`
* Workspace scripts: `.joyride/scripts/**/*.cljs`

The scripts are displayed in menus accessible via the commands:

* **Joyride: Run User Script**, default keybinding `ctrl+shift+,`
* **Joyride: Run Workspace Script**, default keybinding `ctrl+shift+.`

You can configure keyboard shortcuts to the commands `joyride.runUserScript`, and `joyride.runWorkspaceScript` giving them a script as an argument.

An example `keybindings.json`:

```json
[
  ...
  {
    "key": "cmd+f1",
    "command": "joyride.runUserScript",
    "args": "acme/my_super_script.cljs"
  },
  {
    "key": "cmd+f2",
    "command": "joyride.runWorkspaceScript",
    "args": "my_first_ws_script.cljs"
  },
  {
    "key": "cmd+f3",
    "command": "joyride.runCode",
    "args": "(require '[\"vscode\" :as vscode]) (vscode/window.showInformationMessage \"Hello World!\")"
  },
  {
    "key": "cmd+alt+,",
    "command": "joyride.runUserScript"
  },
  {
    "key": "cmd+alt+.",
    "command": "joyride.runWorkspaceScript"
  },
  {
    "key": "ctrl+shift+alt+space",
    "command": "joyride.runCode"
  },
  ...
]
```

This would bind:

* `cmd+f1` to Joyride running the script `<user home>/.config/joyride/scripts/acme/my_super_script.cljs`
* `cmd+f2` to `.joyride/scripts/my_first_ws_script.cljs`
* `cmd+f3` to run code showing a VS Code info box
* `cmd+alt+,` to bring up the user scripts menu
* `cmd+alt+,` to bring up the workspace scripts menu
* `ctrl+shift+alt+space` to the bring up the prompt asking for code to evaluate

The three last items are important (long story). You should bind non-args calls to the commands last in after any bindings with args, even if you bind them to their default shortcuts. You can take the opportunity to rebind them if you like.