---
description: 'Patterns for effective VS Code extension development using ClojureScript, shadow-cljs, and hot-reload workflows. Intended to be installed in the workspace (`.github/instructions/`).'
applyTo: '**'
---

# VS Code Extension Development with ClojureScript

## shadow-cljs watcher

The shadow-cljs watcher should be running, and the `cljs` REPL connected to the running extension development host. If the REPL isn't connected, stop and ask the human to start and connect it.

### The REPL and interactive programming
* Develop and validate solutions entirely in REPL before editing files
* Prefer inline def and related techniques instead of `println` for debugging
* Depending on the change the extension `activate` function may need to be called
* TDD in REPL: write tests as `cljs.test` tests (mixing with experimental expressions to get the tests succinct and to the point), verify behavior interactively
* Use `cljs.test` in the REPL to run tests
* Validate changes through REPL state inspection instead of committing to files
* At the human's signal, write changes to files and let shadow-cljs hot reload
* App state inspection: app-db can contain circular references, use targeted `dissoc` when inspecting
* Some warnings about proposed API may arise from inspecting VS Code API objects (`dissoc` or `select-keys` are your friends)

### Compiler and hot reload
* Shadow-cljs watches source files and compiles on save
* Hot reload updates running extension without restart
* Check shadow-cljs watch task output and Calva output log for compilation errors
* Compilation warnings appear in Calva Output Log
* Problems panel shows linting information

### Test runner
* Shadow-cljs runs unit tests on file changes
* Test results appear in the Calva Output Log
* Failed tests show: test name, file location (file:line:column), expected vs actual values

### Calva output log
* REPL evaluation results and errors
* Extension lifecycle events
* Useful for debugging REPL interactions
* Shows compilation and evaluation feedback

## AI and human cooperation
* AI evaluates code in REPL and inspects internal state
* Human verifies UI/visual results that AI cannot perceive
* AI checks app-db state, human confirms user experience
* AI is relentless in trying to figure out how to use the REPL to inspect the state of the developed extension, almost anything is inspectable
* Protocol: AI evaluates → verifies state → asks human → human confirms → iterate
* Remember to stop when you have asked the human to verify something
* Combines AI's rapid testing with human's UX validation

## The extension development host
* Separate VS Code window for testing extension under development
* Runs the compiled extension in isolated environment
* Has own settings, state, and workspace
* Changes hot-reload without restarting (except package.json changes)

## The extension manifest
* Manifest (`package.json`) changes such as commands, views, configuration, activation events, when contexts, require extension development host restart
* The human is responsible for restarting the extension development host
* *NB*: Extension host restart will completely reset the REPL state. The AI needs to be confident it can recreate this state before the human restarts the extension development host.

## Testing Strategy
* With unit testing no `vscode` can be required. Factor code so that pure functions reside in namespaces which do not require `vscode` and remain unit testable
* Use the integration/e2e testing framework to test code that needs `vscode`
* The REPL provides extensive introspection powers, making AI "manual" testing important and valuable
* UI testing protocols, human and AI together, see also **AI and human cooperation**
* Feature validation workflow, human and AI together, see also **AI and human cooperation**
