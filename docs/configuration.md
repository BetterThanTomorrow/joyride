# Configuration

VS Code settings to configure Joyride.

You can specify two sources for Joyride scripts:

- `joyride.scriptsPath.user`: An absolute path
- `joyride.scriptsPath.workspace`: A workspace relative path

The sources ar used the following way:

- The **Joyride: Run Script** menu (default keybinding `ctrl+alt+j`) will list the contents of these directories.
- Joyride has two configured commands for running scripts that take a relative path as an argument. You can use these commands in custom keybindings.
    1. `joyride.runUserScript`
    2. `joyride.runWorkspaceScript`

An example `keybindings.json` fragment:

```json
  {
    "key": "cmd+j",
    "command": "joyride.runUserScript",
    "args": "a-script.cljs"
  },
  {
    "key": "alt+shift+.",
    "command": "joyride.runWorkspaceScript",
    "args": "a-script.cljs"
  }
```

This would bind `cmd+j` to Joyride running the script `<joyride.scriptsPath.user>/a-script.cljs` and `alt/option+shift+.` to `<joyride.scriptsPath.workspace>/a-script.cljs`.
