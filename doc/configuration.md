# Configuration

VS Code settings to configure Joyride.

There is no configuration. 😀

There are some conventions, though. Joyride looks for workspace scripts in:

```
.joyride/scripts/**/*.cljs
```

The scripts are displayed in the **Joyride: Run Workspace Script** menu (default keybinding `cmd/ctrl+.`)

And you can configure keyboard shortcuts to the `joyride.runWorkspaceScript` giving it a script as an argument.

An example `keybindings.json`:

```json
[
  ...
  {
    "key": "cmd+f1",
    "command": "joyride.runWorkspaceScript",
    "args": "my_first_script.cljs"
  },
  {
    "key": "cmd+f2",
    "command": "joyride.runWorkspaceScript",
    "args": "acme/my_super_script.cljs"
  },
  {
    "key": "cmd+alt+.",
    "command": "joyride.runWorkspaceScript"
  },
  ...
]
```

This would bind `cmd+f1` to Joyride running the script `.joyride/scripts/my_first_script.cljs` and `cmd+f2` to `.joyride/scripts/acme/my_super_script.cljs`.

Please note the last item in the snippet. Because of VS Code internals, binding the `joyride.runWorkspaceScript` command in `keybindingds.json` messes with the bindings and general presentation of the ”plain” command. This last binding, restores this. It also will make whatever binding you choose override any VS Code or extension default.