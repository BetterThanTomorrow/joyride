# Joyride User Assistance Guide

This document provides essential context for Language Learning Models (LLMs) to effectively help users learn and use Joyride for VS Code automation and customization.

## What is Joyride?

Joyride makes VS Code scriptable using ClojureScript and the Small Clojure Interpreter (SCI). It provides access to the full VS Code API and extension APIs, enabling users to hack their editor while it's running - similar to how Emacs users customize their environment.

## User Script Types and Organization

### Script Locations

**User Scripts (Global)**
- Location: `<user-home>/.config/joyride/scripts`
- Scope: Available across all VS Code workspaces
- Use for: Personal customizations, global utilities

**Workspace Scripts**
- Location: `<workspace-root>/.joyride/scripts`
- Scope: Specific to the current workspace
- Use for: Project-specific automation, team-shared scripts

**Source Files**
- User: `<user-home>/.config/joyride/src`
- Workspace: `<workspace-root>/.joyride/src`
- Use for: Reusable namespaces and libraries

### Getting Started Structure
When helping new users, recommend this structure:
```
~/.config/joyride/
├── scripts/
│   ├── user_activate.cljs     # Auto-runs when Joyride starts
│   └── my-utilities.cljs      # Custom utility scripts
└── src/
    └── my-lib.cljs            # Reusable functions

<workspace>/.joyride/
├── scripts/
│   ├── workspace_activate.cljs # Project-specific startup
│   └── project-tools.cljs      # Project automation
└── src/
    └── project-utils.cljs      # Project-specific utilities
```

## Essential APIs for User Scripts

### VS Code API Access
```clojure
(ns your-script
  (:require ["vscode" :as vscode]))

;; Common patterns users need
(vscode/window.showInformationMessage "Hello!")
(vscode/commands.executeCommand "workbench.action.files.save")
(vscode/window.showQuickPick #js ["Option 1" "Option 2"])
```

### Joyride Core API
```clojure
(ns your-script
  (:require [joyride.core :as joyride]))

;; Key functions users should know:
joyride/*file*                    ; Current file path
(joyride/invoked-script)          ; Script being run (nil in REPL)
(joyride/extension-context)       ; VS Code extension context
(joyride/output-channel)          ; Joyride's output channel
joyride/user-joyride-dir          ; User joyride directory path
```

### Extension APIs
```clojure
;; How to access other extensions safely
(when-let [ext (vscode/extensions.getExtension "ms-python.python")]
  (when (.-isActive ext)
    (let [python-api (.-exports ext)]
      ;; Use Python extension API safely
      (-> python-api .-environments .-known count))))

;; Always check if extension is available first
(defn get-python-info []
  (if-let [ext (vscode/extensions.getExtension "ms-python.python")]
    (if (.-isActive ext)
      {:available true
       :env-count (-> ext .-exports .-environments .-known count)}
      {:available false :reason "Extension not active"})
    {:available false :reason "Extension not installed"}))
```

## Common User Patterns

### Script Execution Guard
```clojure
;; Essential pattern - only run when invoked as script, not when loaded in REPL
(when (= (joyride/invoked-script) joyride/*file*)
  (main))
```

### Activation Scripts
Users can create scripts that run automatically when Joyride starts:

**User Activation**: `~/.config/joyride/scripts/user_activate.cljs`
```clojure
(ns user-activate
  (:require [joyride.core :as joyride]))

;; Runs automatically when Joyride activates
(println "Joyride activated!")
```

**Workspace Activation**: `<workspace>/.joyride/scripts/workspace_activate.cljs`
```clojure
(ns workspace-activate
  (:require [joyride.core :as joyride]))

;; Runs for this workspace only
(println "Workspace Joyride activated!")
```

### Managing Disposables (Important for Users)
```clojure
;; Always register disposables with extension context
(let [disposable (vscode/workspace.onDidOpenTextDocument handler)]
  (.push (.-subscriptions (joyride/extension-context)) disposable))
```

### Promise Handling
```clojure
(ns your-script
  (:require [promesa.core :as p]))

;; Users need to understand async operations
(p/let [result (vscode/window.showInputBox #js {:prompt "Enter value:"})]
  (when result
    (vscode/window.showInformationMessage (str "You entered: " result))))
```

## User Development Workflow

### REPL-Driven Development
1. **Start nREPL server**: Command palette → "Joyride: Start nREPL Server"
2. **Connect with Calva**: Use "Calva: Start Joyride REPL and Connect"
3. **Develop interactively**: Load files, evaluate expressions, test functions

### Common Commands Users Need
- `joyride.runCode` - Evaluate ClojureScript code directly
- `joyride.evaluateSelection` - Evaluate selected text
- `joyride.runUserScript` - Run user script by name
- `joyride.runWorkspaceScript` - Run workspace script by name

### Keyboard Shortcuts Setup
Help users add to VS Code's `keybindings.json`:
```json
{
  "key": "ctrl+alt+j u",
  "command": "joyride.runUserScript",
  "args": "my-script.cljs"
}
```

For inline code execution:
```json
{
  "key": "ctrl+alt+j r",
  "command": "joyride.runCode",
  "args": "(my-namespace/my-function)"
}
```

## Common User Script Examples

### Basic Information Message
```clojure
(ns hello-world
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

(defn greet []
  (vscode/window.showInformationMessage "Hello from Joyride!"))

;; Run when script is executed
(when (= (joyride/invoked-script) joyride/*file*)
  (greet))
```

### File Processing
```clojure
(ns file-processor
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [promesa.core :as p]))

(defn process-current-file []
  (if-let [editor vscode/window.activeTextEditor]
    (let [document (.-document editor)
          file-path (.-fileName document)
          content (.getText document)]
      (vscode/window.showInformationMessage
        (str "Processing " (path/basename file-path)
             " (" (count content) " characters)")))
    (vscode/window.showWarningMessage "No active file to process")))

(when (= (joyride/invoked-script) joyride/*file*)
  (process-current-file))
```

### Configuration Management
```clojure
(ns config-helper
  (:require ["vscode" :as vscode]))

(defn get-font-size []
  (-> (vscode/workspace.getConfiguration "editor")
      (.get "fontSize")))

(defn set-font-size [size]
  (-> (vscode/workspace.getConfiguration)
      (.update "editor.fontSize" size vscode/ConfigurationTarget.Global)))

(defn toggle-large-font []
  (let [current (get-font-size)]
    (if (> current 16)
      (set-font-size 14)
      (set-font-size 20))))
```

### Quick Pick Menu
```clojure
(ns quick-actions
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(defn show-action-menu []
  (p/let [choice (vscode/window.showQuickPick
                   #js ["Save All Files" "Close All Editors" "Toggle Sidebar"]
                   #js {:placeHolder "Choose an action"})]
    (case choice
      "Save All Files" (vscode/commands.executeCommand "workbench.action.files.saveAll")
      "Close All Editors" (vscode/commands.executeCommand "workbench.action.closeAllEditors")
      "Toggle Sidebar" (vscode/commands.executeCommand "workbench.action.toggleSidebarVisibility")
      nil)))

(when (= (joyride/invoked-script) joyride/*file*)
  (show-action-menu))
```

### Working with Python Extension
```clojure
(ns python-helper
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(defn show-python-environments []
  (if-let [ext (vscode/extensions.getExtension "ms-python.python")]
    (if (.-isActive ext)
      (let [python-api (.-exports ext)
            envs (-> python-api .-environments .-known)
            env-names (map #(.-path (.-internal %)) envs)]
        (p/let [choice (vscode/window.showQuickPick
                         (clj->js env-names)
                         #js {:placeHolder "Select Python Environment"})]
          (when choice
            (vscode/window.showInformationMessage
              (str "Selected: " choice)))))
      (vscode/window.showWarningMessage "Python extension not active"))
    (vscode/window.showErrorMessage "Python extension not installed")))

(when (= (joyride/invoked-script) joyride/*file*)
  (show-python-environments))
```

## NPM Dependencies for Users

Help users understand how to use npm packages:

### Installation Locations
- User level: `~/.config/joyride/`
- Workspace level: `<workspace>/.joyride/`
- Project root: `<workspace>/`

### Usage Example
```bash
# In ~/.config/joyride/ or workspace .joyride/
npm install lodash
```

```clojure
(ns use-lodash
  (:require ["lodash" :as _]))

(def data [1 2 3 4 5])
(_.reverse data) ; [5 4 3 2 1]
```

## JavaScript Interop for Users

### Objects and Interfaces
```clojure
;; Creating JS objects (deftype not available in SCI)
#js {:onClick (fn [event] (println "Clicked!"))
     :onHover (fn [event] (println "Hovered!"))}

;; Working with JS objects
(let [editor vscode/window.activeTextEditor
      doc (.-document editor)]
  {:file-name (.-fileName doc)
   :language (.-languageId doc)})
```

### Destructuring JS Objects
```clojure
;; Convert and destructure
(let [{:keys [uri fsPath]} (js->clj workspace-folder :keywordize-keys true)]
  (println "Workspace at:" fsPath))
```

## Best Practices for Users

### Error Handling
```clojure
(p/catch
  (risky-async-operation)
  (fn [error]
    (vscode/window.showErrorMessage (str "Oops! " (.-message error)))))
```

### Resource Management
```clojure
;; Always clean up resources
(defonce !disposables (atom []))

(defn register-disposable! [disposable]
  (swap! !disposables conj disposable)
  (.push (.-subscriptions (joyride/extension-context)) disposable))

;; Use in activation scripts
(register-disposable!
  (vscode/workspace.onDidOpenTextDocument my-handler))
```

### Namespace Organization
- Use descriptive namespace names: `my-git-tools`, `project-helpers`
- Group related functionality together
- Use synthetic namespaced keywords: `:ui/theme`, `:git/branch`

## Common User Gotchas

1. **Forgetting the script guard**: Code runs when loaded in REPL without guard
2. **Not managing disposables**: Event handlers pile up on script re-runs
3. **Extension timing**: Extensions might not be ready when script runs
4. **Namespace conflicts**: Same namespace names overwrite each other
5. **Promise handling**: Forgetting to use `promesa.core` for async operations
6. **File paths**: Need absolute paths for VS Code APIs

## Learning Path for New Users

### Beginner (Week 1-2)
1. Install Joyride extension
2. Try the built-in examples
3. Create first `user_activate.cljs`
4. Learn basic VS Code API calls
5. Set up REPL development

### Intermediate (Week 3-4)
1. Create workspace-specific scripts
2. Learn promise handling with `promesa.core`
3. Use npm packages in scripts
4. Create keyboard shortcuts for scripts
5. Build reusable utility functions

### Advanced (Month 2+)
1. Create complex automation workflows
2. Integrate with multiple VS Code extensions
3. Build shared libraries in `/src` folders
4. Contribute to Joyride community examples
5. Create VS Code extension-like functionality

## Troubleshooting Help

### Common Issues
1. **"Script not found"**: Check file location and naming
2. **"Extension not available"**: Check if extension is installed and activated
3. **"Promise rejected"**: Add proper error handling with `p/catch`
4. **"Function not found"**: Check namespace requires and function exports
5. **"File access denied"**: Ensure VS Code has proper file permissions

When helping users troubleshoot, always suggest testing with the Joyride evaluation tool first to isolate issues.
