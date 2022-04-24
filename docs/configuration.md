# Configuration

VS Code settings to configure Joy.

You can specify two sources for Joy scripts:

* `joy.scriptsPath.user`: An absolute path
* `joy.scriptsPath.workspace`: A workspace relative path 

## Commands

The `joy.commands` setting is an array of objects, each with the following fields (required fields in **bold**):

* **`name`**: The name of the command as it will appear in the **Run Script** menu.
* `key`: Will prefix `name` in the **Run Script** menu. Specifying a key enumerated in [VS Code Keybindingds Accepted Keys](https://code.visualstudio.com/docs/getstarted/keybindings#_accepted-keys), except `space`, will bind the command to the keyboard shortcut `ctrl+shift+space <key>`.
* `root`: Either `user` or `workspace`. Defaults to the `user` if in **User** settings and `workspace` if in **Workspace** settings.
* **`source`**: An object with one of:
  * `file`: A string with the path to the file that should be loaded, the path is relative to the `root`
  * `code`: A string with the code to be evaluated 

### User and Workspace command conflicts

The **Run Script** menu will contain the concatenation of the commands defined in the **User** and **Workspace** configuration. If two commands are bound to the same keyboard shortcut, the **User** defined one will win.

