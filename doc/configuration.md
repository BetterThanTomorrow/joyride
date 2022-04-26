# Configuration

VS Code settings to configure Joyride.

You can specify two sources for Joyride scripts:

- `joyride.scriptsPath.user`: An absolute path
- `joyride.scriptsPath.workspace`: A workspace relative path. Defaults to `.joyride/scripts`.

The sources ar used the following way:

- The **Joyride: Run Script** menu (default keybinding `ctrl+alt+j`) will list the contents of these directories.
- Joyride has two configured commands for running scripts that take a relative path as an argument. You can use these commands in custom keybindings.
    1. `joyride.runUserScript`
    2. `joyride.runWorkspaceScript`

An example `keybindings.json` fragment:

```json
  {
    "key": "cmd+1",
    "command": "joyride.runUserScript",
    "args": "my_user_script.cljs"
  },
  {
    "key": "cmd+2",
    "command": "joyride.runWorkspaceScript",
    "args": "my_workspace_script.cljs"
  }
```

This would bind `cmd+1` to Joyride running the script `<joyride.scriptsPath.user>/my_user_script.cljs` and `cmd+2` to `<joyride.scriptsPath.workspace>/my_workspace_.cljs`.
