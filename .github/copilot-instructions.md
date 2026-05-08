For comprehensive development guidance, invoke the `@joyride-dev` agent.

## What Joyride Is

Joyride makes VS Code hackable in user space — the Emacs/ELisp model for VS Code. A ClojureScript scripting runtime powered by SCI (Small Clojure Interpreter), giving users full Extension Host access with live REPL interaction. Built with shadow-cljs, hot-reloading into the Extension Development Host.

## REPL Environments

- **Primary REPL**: Backseat Driver CLJS (`clojure_evaluate_code`, replSessionKey: `"cljs"`) — for developing the extension itself
- **User interaction**: Local Joyride (`joyride_evaluate_code`) — quick input, progress, questions
- **Testing user API**: Dev host Joyride (via `joyride.sci/eval-string` through Backseat Driver) — verify user-facing behavior
- **Shadow-cljs build tooling**: CLJ session (replSessionKey: `"clj"`) — build system only

When the user reports issues, they occurred in the Extension Development Host — use the Backseat Driver REPL to investigate.

## Watch Task

The "Watch" task (`shadow-cljs watch :extension :test-watch`) runs continuously, compiling the extension and auto-running all unit tests (namespaces matching `-test$`) on every file save. After making changes, check the Watch task output to verify zero compilation warnings/errors and all tests passing. The latest status is at the tail of the output. Trust the watcher — do not run separate terminal commands to compile or test.

## Unit Tests

When adding or modifying functionality, look for opportunities to add unit tests for pure logic. Unit tests run in Node.js via shadow-cljs and cannot require `vscode` — factor code so that pure logic lives in namespaces free of VS Code dependencies. Strive to make code more pure and testable. Test files go in `test/` mirroring the `src/` structure (e.g., `src/joyride/foo.cljs` → `test/joyride/foo_test.cljs`). The watcher picks up new test namespaces automatically.

## State Inspection Safety

- `@db/!app-db` — always `dissoc :extension-context` before inspecting (circular references)
- VS Code API objects — use `select-keys` or `dissoc`, never print raw
- When-contexts — `(:contexts @when-contexts/!db)`
- nREPL state — `@nrepl/!db`

## Bundled AI Context (chatInstructions / Skills)

Skills are registered as `chatInstructions` in `package.json`, not `chatSkills`. This is a workaround for [microsoft/vscode#313263](https://github.com/microsoft/vscode/issues/313263) — VS Code doesn't resolve file paths for `chatSkills`, making them unloadable by agents. When the bug is fixed, move them back to `chatSkills`. Do not "fix" this by moving them to `chatSkills` until the VS Code issue is resolved.

### Marker file for migration detection

The file `assets/getting-started-content/user/.github/llm-contexts-<version>.txt` is a marker that signals existing users have completed the LLM context migration. The filename contains the Joyride version. When updating the bundled instructions or skills:

1. Update the marker filename to match the new version
2. Update all references to the marker filename:
   - `src/joyride/content_utils.cljs` — `maybe-create-user-llm-contexts-marker+`
   - `assets/skills/joyride-update-llm-contexts/SKILL.md` — Step 0 and Step 4
   - `assets/instructions/joyride.instructions.md` — migration check section
