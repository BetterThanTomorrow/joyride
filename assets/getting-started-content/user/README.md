# Welcome to Joyride! 🎸

Joyride lets you script VS Code using ClojureScript. This is your user Joyride
directory where you can create scripts that make VS Code even better, and
tailored to you and your workflows.

Some important distinctions/terms:
- A **Joyride Script** is a file in the `scripts` subdirectory that will you can
  run from the **Joyride: Run User Script...** menu.
- A **Joyride Source file** is a file in the `src` subdirectory. It can be library
  functions for **Scripts**, and can also expose functions that you can call
  from keyboard shortcut bindings, using `joyride.runCode` command. E.g.:
  ```json
  {
    "key": "ctrl+alt+j ctrl+alt+g",
    "command": "joyride.runCode",
    "args": "(require '[git-fuzzy :as gz] :reload) (gz/show-git-history!+)"
  },
  ```
- **User** scripts and source files. Code in this directory, that can be accessed
  and run from the Joyride **User** commands. User code is global to all VS Code
  windows.
- **Workspace** scripts and source files. Code in the `./joyride` subdirectory of
  projects you have opened in a VS Code window. Workspace code is local to the,
  well, workspace.
- The `scripts` and `src` directories from both **Workspace** and **User** are
  part of Joyride's “classpath” and resolved in this order:
  1. `<workspace-root>/.joyride/src`
  1. `<workspace-root>/.joyride/scripts`
  1. `<user-home>/.config/joyride/src`
  1. `<user-home>/.config/joyride/scripts`

## Getting Started

### 1. Create Your First Script
Use VS Code commands to create your first Joyride scripts:
- **Create User Activate Script** - Runs automatically when Joyride starts
- **Create Hello Joyride User Script** - Example script to run manually

### 2. Install Calva (Recommended)
For the best Joyride development experience, install the Calva extension:
- Provides syntax highlighting and REPL support
- Use `Calva: Start Joyride REPL and Connect` to interact with your scripts

### 3. Explore Examples
Check out the Joyride examples repository:
https://github.com/BetterThanTomorrow/joyride/tree/master/examples

### 4. Run Scripts
- Use `Joyride: Run User Script` to run your scripts
- Scripts in this directory can access the full VS Code API

## Directory Structure

- `scripts/` - Your Joyride scripts go here
- `src/` - Reusable Clojure code (created when needed)
- `deps.edn` - Clojure dependencies (created when needed)

## Next Steps

- Read the Joyride documentation: https://github.com/BetterThanTomorrow/joyride
- Join the Calva community: https://clojurians.slack.com (#calva channel)
- Start scripting and have fun! 🎉
