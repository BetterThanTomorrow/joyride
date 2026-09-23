# Stand up a window

Load when the folder is not open, or the REPL and tasks are not running, and you are an agent that must unblock yourself. Agents already inside the window usually skip this.

- Open the folder from a Joyride window that is already up, or from a shell: `cursor -n <path>`, `code -n <path>`, or `code-insiders -n <path>`.
- Inspect VS Code tasks, including watchers and the call chain. Start what the project needs.
- Matcher wait, window reload leaving a background task, and `executeTask` of the connect leaf: [vscode.md](vscode.md).
- A task often starts the REPL. Connecting a Calva REPL from Joyride or a task: [calva.md](calva.md).
- Two Joyride evaluation paths can be live in one window (Joyride's MCP, and Calva / Backseat Driver when the human connected that way). Both evaluate roughly the same ClojureScript host. Prefer `joyride_evaluate_code` for Joyride work.
