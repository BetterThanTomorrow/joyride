# Joyride Test Project

This project is what the dev Extension Host opens when Joyride is developed. Its Joyride is the one under development, built from this repo's `out/`. Agents here test it.

## For AI agents working in this project

Load the **`joyride-testing`** skill before you test anything. It says how to check that your Joyride tool reaches this window's Joyride, which REPL sessions to expect, and how to test each part of Joyride. The **`joyride`** skill covers Joyride itself, and the **`backseat-driver`** skill covers the Calva REPL tools.

This window uses the user's real Joyride directory, `~/.config/joyride`. Leave it alone.

## Project structure

```
.joyride/src/test_project.cljs   - Starts and stops the REPLs and the tic-tac-toe Flare
.joyride/scripts/                - workspace_activate.cljs, which starts them when the window opens
tic-tac-toe/                     - A Scittle tic-tac-toe built with Replicant, shown in a Flare
.github/                         - The testing skill and the joyride-tester agent
```

## Starting the REPLs and the Flare

When the window opens, `.joyride/scripts/workspace_activate.cljs` runs `test-project/start!`. It starts this window's Joyride nREPL server and connects Calva to it, as the session `joyride`. Then it starts Calva's WebSocket server on port 13400 and opens the tic-tac-toe Flare, whose page connects to it, as the session `tic-tac-toe`. It looks at what is already running first, so it only starts what is missing. The VS Code tasks **test project: start** and **test project: stop** start and stop all of it. The connect sequences are in `test_project.cljs`, not in the settings.

The settings start Joyride's MCP server with the window, so an agent here reaches this window's Joyride.

The tic-tac-toe game comes from the Backseat Driver workshop. Its state is `replicant-tictactoe.core/!store`, and `replicant-tictactoe.core/event-handler!` makes moves, such as `[:tic 1 1]` and `[:reset]`.
