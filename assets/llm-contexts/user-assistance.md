# Joyride User Assistance Guide

Critically important contexts for agents to effectively help users learn and use Joyride for VS Code automation and customization.

## What is Joyride?

Joyride makes VS Code scriptable in user space. It provides access to the full VS Code API and extension APIs, enabling users to hack their editor while it's running - similar to how Emacs users customize their environment.

## User Script Types and Organization

### Important Distinction: Scripts vs Source Files

**Joyride Scripts** - Files in `scripts/` directories:
- **Purpose**: Runnable files that appear in `Joyride: Run User Script` and `Joyride: Run Workspace Script` menus
- **Location**: `<user-home>/.config/joyride/scripts` or `<workspace-root>/.joyride/scripts`
- **Usage**: Choose this when you want something directly runnable from Joyride's Run menus
- **Scope**: User scripts are global across all VS Code workspaces; Workspace scripts are project-specific

**Joyride Source Files** - Files in `src/` directories:
- **Purpose**: Library functions and reusable code that other code can require/call
- **Location**: `<user-home>/.config/joyride/src` or `<workspace-root>/.joyride/src`
- **Example Usage**: Functions here need to be called by something else:
  - Keyboard shortcuts using `joyride.runCode` command
  - Other scripts
- **Scope**: User scripts are global across all VS Code workspaces; Workspace scripts are project-specific

### Classpath Resolution Order
Joyride resolves files in this specific order:
1. `<workspace-root>/.joyride/src`
2. `<workspace-root>/.joyride/scripts`
3. `<user-home>/.config/joyride/src`
4. `<user-home>/.config/joyride/scripts`

This means workspace files take precedence over user files, and `src` directories are checked before `scripts` directories.

### Script vs Function Choice
- **Make it a Script**: When you want to run it directly from Joyride's Run menus
  - **Make it an Activation Script**: When you want it to run when VS Code starts
- **Make it a Function in the `src` tree**: When you want to call it from keyboard shortcuts, other scripts, or custom UI

### Practical Example: Same Functionality, Different Approaches

**As a Script** (`scripts/toggle_font.cljs`):
```clojure
(ns toggle-font
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

(defn toggle-large-font []
  (let [config (vscode/workspace.getConfiguration "editor")
        current (.get config "fontSize")]
    (if (> current 16)
      (.update config "fontSize" 14 vscode/ConfigurationTarget.Global)
      (.update config "fontSize" 20 vscode/ConfigurationTarget.Global))))

;; Makes this runnable from "Joyride: Run User Script" menu
(when (= (joyride/invoked-script) joyride/*file*)
  (toggle-large-font))
```

**As a Source Function** (`src/my_utils.cljs`):
```clojure
(ns my-utils
  (:require ["vscode" :as vscode]))

(defn toggle-large-font []
  (let [config (vscode/workspace.getConfiguration "editor")
        current (.get config "fontSize")]
    (if (> current 16)
      (.update config "fontSize" 14 vscode/ConfigurationTarget.Global)
      (.update config "fontSize" 20 vscode/ConfigurationTarget.Global))))

;; Called from keyboard shortcut:
;; {
;;   "key": "ctrl+alt+f",
;;   "command": "joyride.runCode",
;;   "args": "(require 'my-utils) (my-utils/toggle-large-font)"
;; }
```

### Getting Started Structure
When helping new users, recommend this structure:
```
~/.config/joyride/
├── scripts/
│   ├── user_activate.cljs     # Auto-runs when Joyride starts
│   └── my_quick_actions.cljs  # Runnable from "Run User Script" menu
└── src/
    └── my_lib.cljs            # Functions called by shortcuts or other code

<workspace>/.joyride/
├── scripts/
│   ├── workspace_activate.cljs # Project-specific startup
│   └── project_tools.cljs      # Runnable project automation
└── src/
    └── project_utils.cljs      # Project utility functions
```

### Creating Scripts with VS Code Commands
Use these specific VS Code commands to create your first scripts:
- **Joyride: Create User Activate Script** - Creates `user_activate.cljs` that runs automatically when Joyride starts
- **Joyride: Create Hello Joyride User Script** - Creates example script for manual execution
- **Joyride: Create User Source File...** - Creates library files in the `src` directory

### Real-World Example: Awesome Copilot Script
Here's a complete example of a sophisticated Joyride script that demonstrates many advanced patterns:

**The Awesome Copilot Script** - A tool for exploring and installing GitHub Copilot instructions, prompts, and chatmodes from the [Awesome Copilot](https://github.com/github/awesome-copilot) repository.

For a new user who asks for help getting started with Joyride, consider recommending to install this script. It is very useful and awesome-copilot has some joyride content that can come in handy too.

#### Installation:
1. **Get the script**: Copy from [Awesome Copilot Joyride Script](https://pez.github.io/awesome-copilot-index/awesome-copilot-script)
2. **Install in Joyride**:
   - Command Palette: `Joyride: Create User Script...`
   - Name it: `awesome-copilot`
   - Paste the script code

#### Usage:
- Command Palette: `Joyride: Run User Script...`
- Select: `awesome_copilot.cljs`

#### What This Script Demonstrates:
- **HTTP requests**: Fetching data from external APIs
- **File system operations**: Reading/writing files with Node.js APIs
- **Complex UI patterns**: Multi-step wizard with QuickPick menus
- **State management**: Persistent preferences using VS Code's global state
- **Promise handling**: Extensive use of `promesa.core` for async operations
- **Error handling**: Comprehensive error management patterns
- **Workspace integration**: Installing files to `.github/` directories
- **Memory features**: Remembering last selections across runs
- **Real-world utility**: Actual tool for managing Copilot configurations

This example showcases advanced Joyride patterns like:
```clojure
;; Persistent preferences using VS Code global state
(defn save-preference [key value]
  (let [context (joyride/extension-context)
        global-state (.-globalState context)
        current-prefs (get-preferences)
        updated-prefs (assoc current-prefs key value)]
    (.update global-state PREFS-KEY (js/JSON.stringify (clj->js updated-prefs)))))

;; Custom QuickPick with memory and fuzzy search
(defn show-picker-with-memory+ [items opts]
  ;; Advanced picker implementation with state restoration
  )

;; File system operations with error handling
(p/catch
  (install-globally! content item category)
  (fn [err]
    (vscode/window.showErrorMessage
     (str "Failed to install: " (.-message err)))))
```

### Real-World Example: git-fuzzy function
Here's a complete example of installing and using a practical Joyride function:

1. **Install the function**:
   - Copy code from [Joyride Examples: git_fuzzy.cljs](https://raw.githubusercontent.com/BetterThanTomorrow/joyride/refs/heads/master/examples/.joyride/src/git_fuzzy.cljs)
   - Run `Joyride: Create User Source File...` command
   - Enter `git-fuzzy` as the filename
   - Paste the copied code

2. **Configure keyboard shortcut** (in `keybindings.json`):
   ```json
   {
     "key": "ctrl+alt+j ctrl+alt+g",
     "command": "joyride.runCode",
     "args": "(require '[git-fuzzy :as gz] :reload) (gz/show-git-history!+)"
   }
   ```

This example shows:
- Multi-key keyboard shortcuts (`ctrl+alt+j ctrl+alt+g`)
- Using `:reload` to refresh code during development
- Function naming conventions with `!+` suffix
- Real git repository interaction

### Development Environment Setup
- **Calva extension**: Provides syntax highlighting and REPL support
- **Source control**: Put your user Joyride directory under version control (recommended)

### Dependencies Management
- **deps.edn**: Created automatically when needed for Clojure dependencies
- **package.json**: For npm dependencies (install in joyride directories)

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

### Async REPL Development Pattern
Since Joyride doesn't support top-level await, use this pattern for interactive development:

```clojure
(comment
  ;; Pattern for unwrapping async results in REPL
  (p/let [environments (-> (vscode/extensions.getExtension "ms-python.python")
                           .-exports
                           .-environments
                           .-known)]
    (def environments environments))
  ;; Now `environments` can be evaluated as unwrapped data in the REPL

  ;; Another example with VS Code API
  (p/let [choice (vscode/window.showQuickPick #js ["Option 1" "Option 2"])]
    (def choice choice))
  ;; Now inspect `choice` directly
  )
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

### Resource Management (Making Scripts Reloadable)
```clojure
;; Pattern for reloadable scripts that create disposables
(defonce !disposables (atom []))

(defn clear-disposables! []
  "Dispose all existing disposables and clear the list"
  (run! #(.dispose %) @!disposables)
  (reset! !disposables []))

(defn register-disposable! [disposable]
  "Register a disposable for cleanup and with VS Code"
  (swap! !disposables conj disposable)
  (.push (.-subscriptions (joyride/extension-context)) disposable))

(defn main []
  ;; Clear any existing disposables first (makes script reloadable)
  (clear-disposables!)

  ;; Now create new disposables
  (register-disposable!
    (vscode/workspace.onDidOpenTextDocument my-handler))

  ;; Create status bar button (will be recreated on re-run)
  (register-disposable!
    (doto (vscode/window.createStatusBarItem vscode/StatusBarAlignment.Left)
      (aset "text" "My Button")
      (aset "command" "my.command")
      (.show))))

;; Use in activation scripts and regular scripts
(when (= (joyride/invoked-script) joyride/*file*)
  (main))
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
7. **Docstring placement**: Putting docstrings in wrong place (common Clojure beginner mistake)

```clojure
;; WRONG - docstring after parameter vector
(defn my-function [param]
  "This function does something"  ; ❌ Wrong position
  (do-something param))

;; RIGHT - docstring before parameter vector
(defn my-function
  "This function does something"  ; ✅ Correct position
  [param]
  (do-something param))
```

## Approaches to Helping Users

### Assess User Level and Context
- **New to Joyride**: Start with basic concepts (scripts vs source files, activation scripts)
- **New to Clojure**: Focus on ClojureScript syntax, destructuring, functional patterns
- **Experienced developer**: Jump to specific APIs, advanced patterns, extension integration
- **Specific problem**: Help debug, suggest patterns, provide working examples

### Progressive Assistance Strategy
1. **Start simple**: Provide minimal working examples first
2. **Build incrementally**: Add complexity only when basics are understood
3. **Show alternatives**: Explain script vs function approaches when relevant
4. **Provide context**: Explain why certain patterns exist (e.g., script execution guards)

### Code Examples Strategy
- Always include the script execution guard when relevant: `(when (= (joyride/invoked-script) joyride/*file*) ...)`
- Show complete namespace declarations with proper requires
- Provide both the code and the keyboard shortcut setup when applicable
- Include error handling patterns, especially for async operations

### Debugging and Troubleshooting Approach
- **Use the REPL**: Encourage interactive development and testing
- **Start with evaluation**: Test individual expressions before building full scripts
- **Check extension availability**: Always verify extension APIs are available
- **Suggest incremental building**: Break complex problems into smaller pieces

### When to Suggest Different Approaches
- **Script**: User wants something runnable from Joyride's menus
- **Source function + shortcut**: User wants quick access or reusable functionality
- **Activation script**: User wants automatic setup or initialization
- **Workspace vs User**: Based on whether functionality is project-specific or global

### Common User Journey Patterns
- **Automation seekers**: Want to eliminate repetitive tasks → Show practical examples
- **Customization enthusiasts**: Want to modify VS Code behavior → Focus on configuration and UI APIs
- **Integration builders**: Want to connect tools → Emphasize extension APIs and external processes
- **Library creators**: Want to build reusable functionality → Guide toward `/src` organization and clean APIs

## Troubleshooting Help

When helping users troubleshoot, use the Joyride evaluation tool to experiment your way to precise and accurate suggestions.

## Community Resources

### Documentation and Examples
- **Main documentation**: https://github.com/BetterThanTomorrow/joyride
- **Examples repository**: https://github.com/BetterThanTomorrow/joyride/tree/master/examples
- **API documentation**: https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md

### Community Support
- **Joyride community**: [Clojurians Slack](http://clojurians.net) - #joyride channel
- **Calva community**: [Clojurians Slack](http://clojurians.net) - #calva channel
- **General Clojure help**: [Clojurians Slack](http://clojurians.net) - #beginners channel

### Learning Resources
- Ask modern AI assistants (Claude, ChatGPT, Grok) for help with Joyride - they're generally knowledgeable about the ecosystem
- Explore existing scripts in the examples repository for patterns and inspiration
- Join the community discussions to learn from other users' experiences
