# Joyride Project Summary

## Overview
Joyride is a VS Code extension that makes the editor hackable by allowing users to execute ClojureScript and JavaScript code at runtime. It's inspired by Emacs' extensibility model and powered by SCI (Small Clojure Interpreter), enabling interactive programming directly within VS Code. Users can write scripts, automate workflows, and extend VS Code functionality using Clojure's data-oriented programming paradigm.

## Core Functionality
- **Runtime Code Execution**: Execute ClojureScript/JavaScript code via REPL or keyboard shortcuts
- **User & Workspace Scripts**: Hierarchical script system for personal and project-specific automation
- **VS Code API Access**: Full access to VS Code API and extension APIs from scripts
- **Interactive Development**: REPL-driven development with live code evaluation
- **Activation Scripts**: Automatic script execution on extension/workspace activation
- **nREPL Server**: Built-in nREPL server for external REPL connections (Calva integration)

## Key File Structure

### Core Extension Files
- [`src/joyride/extension.cljs`](src/joyride/extension.cljs) - Main extension entry point, command registration, activation/deactivation
- [`src/joyride/sci.cljs`](src/joyride/sci.cljs) - SCI interpreter setup, code evaluation, namespace resolution
- [`src/joyride/scripts_handler.cljs`](src/joyride/scripts_handler.cljs) - Script discovery, execution, and menu management
- [`src/joyride/nrepl.cljs`](src/joyride/nrepl.cljs) - nREPL server implementation for REPL connectivity
- [`src/joyride/db.cljs`](src/joyride/db.cljs) - Application state management with atoms

### Configuration & Lifecycle
- [`src/joyride/config.cljs`](src/joyride/config.cljs) - Path configuration for user/workspace scripts
- [`src/joyride/lifecycle.cljs`](src/joyride/lifecycle.cljs) - Activation script management and execution
- [`src/joyride/getting_started.cljs`](src/joyride/getting_started.cljs) - Getting started content creation and scaffolding

### Utilities & Support
- [`src/joyride/utils.cljs`](src/joyride/utils.cljs) - Common utilities, file operations, messaging
- [`src/joyride/when_contexts.cljs`](src/joyride/when_contexts.cljs) - VS Code context management
- [`src/joyride/constants.cljs`](src/joyride/constants.cljs) - Application constants and configuration
- [`src/joyride/repl_utils.cljs`](src/joyride/repl_utils.cljs) - REPL-specific utilities
- [`src/joyride/bencode.cljs`](src/joyride/bencode.cljs) - Bencode encoding/decoding for nREPL

### Build Configuration
- [`package.json`](package.json) - VS Code extension manifest, commands, dependencies
- [`shadow-cljs.edn`](shadow-cljs.edn) - ClojureScript build configuration
- [`deps.edn`](deps.edn) - Clojure dependencies and paths

## Script Hierarchy & Classpath

Joyride searches for scripts in this order (first found wins):
1. `<workspace-root>/.joyride/src` - Workspace source files (for libraries)
2. `<workspace-root>/.joyride/scripts` - Workspace scripts
3. `<user-home>/.config/joyride/src` - User source files (for libraries)
4. `<user-home>/.config/joyride/scripts` - User scripts

### Activation Scripts (run automatically)
- `user_activate.cljs` - Runs when Joyride activates (user-level)
- `workspace_activate.cljs` - Runs when workspace opens (workspace-level)

### Default Script Locations
```
~/.config/joyride/
├── deps.edn
├── scripts/
│   ├── user_activate.cljs
│   ├── hello_joyride_user_script.cljs
│   └── hello_joyride_user_script.js
└── src/
    └── my_lib.cljs

.joyride/
├── deps.edn
├── scripts/
│   ├── workspace_activate.cljs
│   └── hello_joyride_workspace_script.cljs
└── src/
    └── workspace_lib.cljs
```

## Key Dependencies

### Core ClojureScript & SCI
```clojure
org.clojure/clojurescript "1.11.132"
org.babashka/sci "fbb8e61a8002583fc9300d39e748d1c1b2449b20"
org.babashka/sci.configs "8253c69a537bcc82e8ff122e5f905fe9d1e303f0"
```

### Async/Promise Handling
```clojure
funcool/promesa "11.0.678"  ; Promise/async handling
```

### Code Manipulation
```clojure
rewrite-clj/rewrite-clj "1.1.48"  ; Clojure code parsing/manipulation
```

### Build & Development
```json
// Node.js dependencies
"@vscode/codicons": "^0.0.30",    // VS Code icons
"fdir": "^5.2.0",                 // Fast directory traversal
"picomatch": "^2.3.1"             // Glob pattern matching

// Development dependencies
"shadow-cljs": "^2.18.0",         // ClojureScript compiler
"@vscode/test-electron": "^2.2.3", // VS Code testing
"vsce": "^2.15.0"                 // VS Code extension packaging
```

## Available APIs & Functions

### Extension Commands
```javascript
// Run code dynamically
vscode.commands.executeCommand('joyride.runCode', '(+ 1 2 3)')

// Evaluate current selection
vscode.commands.executeCommand('joyride.evaluateSelection')

// Run scripts
vscode.commands.executeCommand('joyride.runUserScript', 'my-script.cljs')
vscode.commands.executeCommand('joyride.runWorkspaceScript', 'workspace-script.cljs')

// Script management
vscode.commands.executeCommand('joyride.openUserScript')
vscode.commands.executeCommand('joyride.openWorkspaceScript')

// nREPL management
vscode.commands.executeCommand('joyride.startNReplServer')
vscode.commands.executeCommand('joyride.stopNReplServer')
vscode.commands.executeCommand('joyride.enableNReplMessageLogging')
vscode.commands.executeCommand('joyride.disableNReplMessageLogging')
```

### Default Keyboard Shortcuts
```json
"ctrl+alt+j space" - Run Code
"ctrl+alt+j enter" - Evaluate Selection
"ctrl+alt+j u"     - Run User Script
"ctrl+alt+j w"     - Run Workspace Script
```

### Extension API Exports
```javascript
const joyride = vscode.extensions.getExtension("betterthantomorrow.joyride").exports;

// Evaluate code programmatically
const result = await joyride.runCode('(str "Hello " "World")');

// Start nREPL server
const port = await joyride.startNReplServer();

// Get context values
const isActive = joyride.getContextValue('joyride.isActive');
```

### Script Examples

**User Script (`~/.config/joyride/scripts/example.cljs`)**:
```clojure
(ns example
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(defn show-message []
  (vscode/window.showInformationMessage "Hello from Joyride!"))

;; Only run when invoked as script, not when loaded in REPL
(when (= (joyride/invoked-script) joyride/*file*)
  (show-message))
```

**Workspace Script (`.joyride/scripts/workspace-example.cljs`)**:
```clojure
(ns workspace-example
  (:require ["vscode" :as vscode]
            ["fs" :as fs]
            ["path" :as path]))

(def root-path (-> (first vscode/workspace.workspaceFolders) .-uri .-fsPath))
(fs/writeFileSync (path/join root-path "output.txt") "Generated by Joyride!")
```

**Activation Script Pattern**:
```clojure
(ns user-activate
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

(defonce !db (atom {:disposables []}))

;; Re-runnable pattern - clear previous disposables
(defn- clear-disposables! []
  (run! #(.dispose %) (:disposables @!db))
  (swap! !db assoc :disposables []))

;; Register disposables with VS Code for cleanup
(defn- push-disposable! [disposable]
  (swap! !db update :disposables conj disposable)
  (-> (joyride/extension-context)
      .-subscriptions
      (.push disposable)))

(defn- my-main []
  (clear-disposables!)
  (push-disposable!
   (vscode/workspace.onDidOpenTextDocument
    (fn [doc]
      (println "Document opened:" (.-fileName doc))))))

(when (= (joyride/invoked-script) joyride/*file*)
  (my-main))
```

## Architecture Patterns

### Functional Data-Oriented Design
- Immutable data structures with `atom` for state management
- Promise-based async operations using Promesa
- Namespace-based code organization following Clojure conventions
- Minimal mutable state, concentrated in `db.cljs`

### Extension Lifecycle Management
```clojure
;; Disposable pattern for VS Code resources
(defn- register-command! [context command-id var]
  (let [disposable (vscode/commands.registerCommand command-id var)]
    (swap! db/!app-db update :disposables conj disposable)
    (.push (.-subscriptions context) disposable)))

;; Re-runnable activation scripts
(defn- clear-disposables! []
  (run! #(.dispose %) (:disposables @!db))
  (swap! !db assoc :disposables []))
```

### Script Discovery & Execution
- Dynamic script menu generation from filesystem using `fdir`
- URI-based file handling for cross-platform compatibility
- Namespace resolution following Clojure conventions
- JavaScript script support via Node.js `require`

### Extension API Integration
```clojure
;; Safe extension requiring pattern
(-> (vscode/extensions.getExtension "some.extension")
    (.activate)
    (p/then (fn [_] (require '[extension-namespace]))))

;; Extension API access with submodules
["ext://betterthantomorrow.calva$v0" :as calva :refer [repl ranges]]
```

## Development Workflow

### Build & Development
```bash
# Install dependencies
npm install

# Watch mode for development (starts nREPL server)
npm run watch

# Build for production
npm run release

# Package extension
npm run package

# Run integration tests
npm run integration-test

# Clean build artifacts
npm run clean
```

### REPL Development
1. Start watch mode: `npm run watch`
2. Launch extension in debug mode: `F5`
3. In Extension Development Host: **Calva: Start Joyride REPL and Connect**
4. Connect to shadow-cljs build `:extension`
5. Evaluate code interactively in script files
6. Use `(joyride/invoked-script)` to detect script vs REPL execution

### Shadow-cljs Configuration
```clojure
{:target :node-library
 :js-options {:js-provider :shadow
              :keep-native-requires true
              :keep-as-require #{"vscode"}}
 :exports {:activate joyride.extension/activate}
 :devtools {:before-load-async joyride.extension/before
            :after-load joyride.extension/after}}
```

### Testing Setup
- Integration tests in [`vscode-test-runner/workspace-1/`](vscode-test-runner/workspace-1/)
- Test workspace with sample scripts and configurations
- Automated testing via VS Code Test API
- Tests cover script execution, nREPL, extension requiring, and core functionality

## Extension Points

### Custom Script Libraries
Create reusable libraries in `src/` directories:
```clojure
;; In ~/.config/joyride/src/my_lib.cljs
(ns my-lib
  (:require ["vscode" :as vscode]))

(defn find-with-regex-on []
  ;; Custom functionality
  )
```

Require from activation scripts:
```clojure
;; In ~/.config/joyride/scripts/user_activate.cljs
(ns user-activate
  (:require [my-lib]))
```

Use in keyboard shortcuts:
```json
{
  "key": "cmd+f1",
  "command": "joyride.runCode",
  "args": "(my-lib/find-with-regex-on)"
}
```

### Sidecar Extensions
Create custom VS Code extensions dynamically:
```clojure
;; Package and install custom extension manifest
(defn install-sidecar!+ []
  (-> (exec!+ "npx vsce package" {:cwd sidecar-dir})
      (p/then #(exec!+ "code --install-extension my-sidecar.vsix"))))

;; Register custom commands and views
(push-disposables!
 (vscode/commands.registerCommand "my.command" my-handler)
 (vscode/window.registerTreeDataProvider "my.view" my-provider))
```

### Extension API Integration
```clojure
;; Access other extension APIs
["ext://betterthantomorrow.calva$v0" :refer [repl ranges]]

;; Use extension-specific functionality
(def current-form-text (second (ranges.currentForm)))
```

## Configuration Options

### User Settings
```json
{
  "joyride.nreplHostAddress": "127.0.0.1"  // nREPL server host
}
```

### Environment Variables
- `VSCODE_JOYRIDE_USER_CONFIG_PATH` - Override default user config path

### When Clause Contexts
- `joyride.isActive` - Extension activation status
- `joyride.isNReplServerRunning` - nREPL server status

## Implementation Patterns

### Safe Extension Requiring
```clojure
;; In activation scripts, safely require extensions that may not be active
(-> (vscode/extensions.getExtension "some.extension")
    (.activate)
    (p/then (fn [_] (require '[extension-namespace])))
    (p/catch (fn [error]
               (vscode/window.showErrorMessage (str "Failed: " error)))))
```

### File System Operations
```clojure
(defn slurp-file+ [ws-file]
  (p/let [uri (vscode/Uri.joinPath (workspace-root) ws-file)
          data (vscode/workspace.fs.readFile uri)]
    (.decode (js/TextDecoder. "utf-8") data)))
```

### State Management
```clojure
;; Central app state
(defonce !app-db (atom {:disposables []
                        :extension-context nil
                        :workspace-root-path nil
                        :invoked-script nil}))
```

### Error Handling
```clojure
;; Consistent error reporting
(binding [utils/*show-when-said?* true]
  (utils/say-error (str "Failed: " (.-message error))))
```

## Development Guidelines

### Script Development Best Practices
1. **Use activation pattern**: Check `(= (joyride/invoked-script) joyride/*file*)` for script execution
2. **Manage disposables**: Use `clear-disposables!` and `push-disposable!` patterns
3. **Handle promises**: Use `promesa.core` for async operations
4. **Namespace organization**: Follow Clojure naming conventions
5. **Extension safety**: Safely require extensions in activation scripts

### Code Organization
- Keep pure functions in `src/` directories for reusability
- Use `scripts/` for executable entry points
- Separate concerns: UI, business logic, and VS Code integration
- Follow functional programming principles with minimal mutable state

### Testing Approach
- Integration tests cover real VS Code extension scenarios
- Test script execution, REPL connectivity, and extension APIs
- Use workspace-based testing with sample configurations
- Validate both ClojureScript and JavaScript script execution

This summary provides a comprehensive foundation for understanding Joyride's architecture, capabilities, and development patterns, enabling effective assistance with minimal additional context.
