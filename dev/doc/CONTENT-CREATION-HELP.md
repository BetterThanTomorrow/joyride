# Joyride Content Creation UX Redesign

**Document Date:** June 18, 2025
**Current Joyride Version:** Based on codebase analysis

## Progress Checklist

- [x] **Create README.md Template** - Added template in `assets/getting-started-content/user/README.md`
- [x] **Minimal Extension Activation** - Modified `extension.cljs` to only create README on first install
- [x] **Add README Creation Function** - Created `maybe-create-user-readme+` in `getting_started.cljs`
- [x] **Test Minimal Creation** - Verified both fresh install and existing config scenarios work
- [x] **When Contexts Setup** - Added script existence contexts to `when_contexts.cljs`
- [x] **When Contexts Function** - Created `update-script-contexts!` function with REPL testing
- [x] **Centralize path->uri Utility** - Moved to `utils.cljs` and updated all modules to use it
- [x] **Command Registration** - Add new commands to `package.json`
- [x] **Command Implementation** - Create command handler functions
- [x] **Menu Integration** - Update user script menus to include create options
  - [x] Add user-menu-conf+ function to scripts_handler.cljs
  - [x] Update run-user-script+ to use new menu configuration
  - [x] Update open-user-script+ to use new menu configuration
  - [x] Test creation options appear in both menus
    - [x] User menus
      - [x] Activate script option when no activate script exists
      - [x] No activate script option when activate script exists
      - [x] Hello script option when no script other than activate exists
      - [x] No hello script option when a non-activate script exists
    - [ ] Workspace menus
      - [x] Activate script option when no activate script exists
      - [x] No activate script option when activate script exists
      - [ ] Hello script option when no script other than activate exists
        - ERROR: With only an activcate script in place, the hello option is not there
      - [x] No hello script option when a non-activate script exists
  - [x] Test file creation and opening works correctly
  - [ ] **Fix conditional logic** - Creation options should disappear when files exist
    - [x] Add script existence checking to user-menu-conf+ (like workspace-menu-conf+)
    - [x] Test that user activate creation option disappears after file is created
    - [x] Test that user hello creation option disappears after file is created
    - [ ] Test conditional hello script creation (only show when no scripts or only activate exists)
- [ ] **Context Updates** - Call context updates after file creation/deletion
- [ ] **Refactor Content Creation** - Break down `maybe-create-user-content+` into individual functions
- [ ] **E2E Tests** - Add comprehensive test coverage
- [ ] **Manual Testing** - Test all scenarios end-to-end

---

## Vision: New User Experience

### Desired Behavior

1. **User installs Joyride**
2. **If no `~/.config/joyride/README.md` exists, create one** with getting started pointers
3. **Nothing else happens automatically**

### Additional functionality

New commands to be added:
- **Create User Activate Script** - Creates `~/.config/joyride/scripts/user_activate.cljs`
- **Create Hello Joyride User Script** - Creates `~/.config/joyride/scripts/hello_joyride_user_script.cljs`
- **Create Workspace Activate Script** - Creates `<workspace>/.joyride/scripts/workspace_activate.cljs`
- **Create Hello Joyride Workspace Script** - Creates `~/.config/joyride/scripts/hello_joyride_workspace_script.cljs`

These commands should not show up in the Command Palette when the respective activation script exists.

### Enhanced Menu Behavior

The Run/Open User/Workspace script menus should include the respective "Create Activate Script" commands if the activation script does not exist:

- **Run User Script** and **Open User Script** menus include "Create User Activate Script" if `user_activate.cljs` doesn't exist
- **Run User Script** and **Open User Script** menus include "Create Hello Joyride User Script" if `hello_joyride_user_script.cljs` doesn't exist

Modelled after these already existing menu items:
- **Run Workspace Script** and **Open Workspace Script** menus include "Create Workspace Activate Script" if `workspace_activate.cljs` doesn't exist
- **Run Workspace Script** and **Open Workspace Script** menus include "Create Hello Joyride User Script" if `hello_joyride_workspace_script.cljs` doesn't exist

## Current Behavior (As of June 18, 2025)

### Automatic Content Creation on Activation

**Location:** `src/joyride/extension.cljs`, lines 102-103

When Joyride activates, it automatically calls:
```clojure
(getting-started/maybe-create-user-content+)
(getting-started/maybe-create-workspace-config+ false)
```

This creates **immediately on first activation**:
- `~/.config/joyride/deps.edn`
- `~/.config/joyride/scripts/user_activate.cljs`
- `~/.config/joyride/scripts/hello_joyride_user_script.cljs`
- `~/.config/joyride/scripts/hello_joyride_user_script.js`
- `~/.config/joyride/src/my_lib.cljs`

### Current Workspace Content Creation

**Location:** `src/joyride/scripts_handler.cljs`, lines 190-230

Workspace content is created on-demand through menu commands:
- When user runs "Run Workspace Script" or "Open Workspace Script"
- If no scripts exist (or only `activate.cljs` exists), menu shows creation options:
  - "Create Workspace workspace_activate.cljs"
  - "Create Workspace hello_joyride_workspace_script.cljs"
- Selecting these options creates and opens the files immediately

### Current File Creation Functions

**Location:** `src/joyride/getting_started.cljs`

Key functions:
- `maybe-create-user-content+` - Creates all user content at once
- `maybe-create-workspace-activate-fn+` - Creates workspace activate script + opens it
- `maybe-create-workspace-hello-fn+` - Creates workspace hello script + opens it
- `maybe-create-workspace-config+` - Creates workspace `.joyride/deps.edn`

## Implementation Plan Overview

### Changes Required

1. **Modify Extension Activation**
   - Remove automatic call to `maybe-create-user-content+`
   - Add logic to create only `~/.config/joyride/README.md` if it doesn't exist

2. **Create New Commands**
   - Add "Create User Activate Script" command
   - Add "Create Workspace Activate Script" command
   - Register these in VS Code command palette
   - Add when clause contexts for deactivating the commands

3. **Refactor Content Creation Functions**
   - Break down `maybe-create-user-content+` into smaller, focused functions
   - Create separate functions for individual file creation
   - Ensure deps.edn is still created when needed

4. **Update Menu Logic**
   - Modify menu creation in `scripts_handler.cljs`
   - Add activate script creation options to Run/Open menus when activate scripts don't exist
   - Remove automatic creation of hello scripts

5. **Create README Template**
   - Add new template file: `assets/getting-started-content/user/README.md`
   - Include getting started guidance and links to documentation

### Files to Modify

1. **`src/joyride/extension.cljs`** - Modify activation behavior
2. **`src/joyride/getting_started.cljs`** - Refactor content creation functions
3. **`src/joyride/scripts_handler.cljs`** - Update menu logic
4. **`assets/getting-started-content/user/README.md`** - Create new template (new file)
5. **Package.json** - Add new command registrations

### Key Design Principles

- **Minimal initial footprint** - Only create README on first install
- **On-demand creation** - Let users choose when to create scripts
- **Preserve existing templates** - Don't break current getting-started content
- **Clear command structure** - Separate commands for activate vs example scripts
- **Backwards compatibility** - Don't break existing user setups

### Testing Considerations

- Test with fresh VS Code installation
- Test with existing user content present
- Test workspace scenarios with/without existing .joyride directories
- Verify command palette entries work correctly
- Ensure menu items appear/disappear appropriately based on file existence

## Implementation Details

### Command Registration (package.json)

Add these commands to the `contributes.commands` section:

```json
{
  "command": "joyride.createUserActivateScript",
  "title": "Create User Activate Script",
  "category": "Joyride",
  "when": "!joyride.when-contexts/userActivateScriptExists"
},
{
  "command": "joyride.createUserHelloScript",
  "title": "Create Hello Joyride User Script",
  "category": "Joyride",
  "when": "!joyride.when-contexts/userHelloScriptExists"
},
{
  "command": "joyride.createWorkspaceActivateScript",
  "title": "Create Workspace Activate Script",
  "category": "Joyride",
  "when": "!joyride.when-contexts/workspaceActivateScriptExists"
},
{
  "command": "joyride.createWorkspaceHelloScript",
  "title": "Create Hello Joyride Workspace Script",
  "category": "Joyride",
  "when": "!joyride.when-contexts/workspaceHelloScriptExists"
}
```

### When Clause Contexts

Joyride has an existing `when-contexts` module for managing VS Code context variables. Add the new contexts there:

```clojure
;; In src/joyride/when_contexts.cljs
(defonce ^:private !db (atom {:contexts {::joyride.isActive false
                                         ::joyride.isNReplServerRunning false
                                         ;; Add new script existence contexts
                                         ::userActivateScriptExists false
                                         ::userHelloScriptExists false
                                         ::workspaceActivateScriptExists false
                                         ::workspaceHelloScriptExists false}}))

;; Add helper function to update script contexts
(defn update-script-contexts! []
  (p/let [user-activate-exists? (utils/path-or-uri-exists?+
                                 (path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))
          user-hello-exists? (utils/path-or-uri-exists?+
                              (path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))
          ws-activate-exists? (when (conf/workspace-abs-scripts-path)
                                (utils/path-or-uri-exists?+
                                 (path->uri (conf/workspace-abs-scripts-path) ["workspace_activate.cljs"])))
          ws-hello-exists? (when (conf/workspace-abs-scripts-path)
                             (utils/path-or-uri-exists?+
                              (path->uri (conf/workspace-abs-scripts-path) ["hello_joyride_workspace_script.cljs"])))]
    (set-context! ::userActivateScriptExists user-activate-exists?)
    (set-context! ::userHelloScriptExists user-hello-exists?)
    (set-context! ::workspaceActivateScriptExists ws-activate-exists?)
    (set-context! ::workspaceHelloScriptExists ws-hello-exists?)))
```

Then update the package.json when clauses to use the namespaced format:

```json
"when": "!joyride.when-contexts/userActivateScriptExists"
"when": "!joyride.when-contexts/userHelloScriptExists"
"when": "!joyride.when-contexts/workspaceActivateScriptExists"
"when": "!joyride.when-contexts/workspaceHelloScriptExists"
```

Call this function:
- On extension activation
- After creating/deleting script files
- On workspace folder changes

Example integration:
```clojure
;; In getting_started.cljs after file creation
(defn create-user-activate-script+ []
  (p/do
    ;; ... create the file ...
    (when-contexts/update-script-contexts!)))

;; In extension.cljs on activation
(defn activate [context]
  ;; ... existing activation code ...
  (when-contexts/update-script-contexts!))
```

### README.md Template Content

Create `assets/getting-started-content/user/README.md`:

```markdown
# Welcome to Joyride! 🎸

Joyride lets you script VS Code using ClojureScript. This is your user Joyride directory where you can create scripts that enhance your VS Code experience.

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
```

### Migration Strategy

**For Existing Users:**
- No automatic migration - preserve existing setups completely
- New commands will work alongside existing content
- Context variables will properly reflect existing file state

**For New Users:**
- Only README.md created on first install
- Commands guide them to create scripts as needed
- Maintains clean, minimal initial experience

### Backwards Compatibility

- Existing automatic content creation behavior is completely removed
- Existing users with auto-created content are not affected
- All existing script execution and menu functionality preserved
- Template files remain unchanged for consistency

### File Watcher Considerations

Consider adding file system watchers to update context variables when script files are:
- Created outside of Joyride commands (manual creation)
- Deleted by user
- Renamed or moved

## E2E Testing Coverage

The existing e2e test infrastructure in `vscode-test-runner/` provides a solid foundation for testing the content creation changes. Here are specific test scenarios we should add:

### Test Infrastructure Overview

- **Location:** `vscode-test-runner/workspace-1/.joyride/src/integration_test/`
- **Pattern:** Uses `deftest-async` macro for async VS Code API testing
- **Execution:** Via `joyride.runCode` command that runs the test runner
- **Environment:** **SAFE** - Uses isolated tmp directory for user config

#### Critical Safety Mechanism

The test infrastructure in `vscode-test-runner/launch.js` **automatically protects real user configs**:

```javascript
const USER_CONFIG_PATH_KEY = "VSCODE_JOYRIDE_USER_CONFIG_PATH";
if (!process.env[USER_CONFIG_PATH_KEY]) {
  const tmpConfigPath = path.join(
    os.tmpdir(),
    "vscode-test-runner-joyride",
    "user-config"
  );
  // Cleans and recreates tmp directory
  process.env[USER_CONFIG_PATH_KEY] = tmpConfigPath;
}
```

This means:
- **Real user configs are never touched** - Tests use `/tmp/vscode-test-runner-joyride/user-config/`
- **Each test run starts clean** - Tmp directory is deleted and recreated
- **Joyride respects the env var** - `config.cljs` uses `VSCODE_JOYRIDE_USER_CONFIG_PATH` if set
- **No manual setup needed** - The safety mechanism is automatic

### New Test File: `content_creation_test.cljs`

Should cover these scenarios:

#### Initial Installation Behavior
```clojure
(deftest-async minimal-initial-creation
  (testing "Only README.md is created on first activation"
    ;; NOTE: Tests automatically use isolated tmp config via
    ;; VSCODE_JOYRIDE_USER_CONFIG_PATH - real user configs are safe!
    (p/let [user-config-path (user-abs-joyride-path)]
      ;; Verify clean state - tmp dir should be empty initially
      ;; Trigger activation or content creation
      ;; Assert only README.md exists in user config path
      ;; Assert no scripts directory exists yet
      )))

(deftest-async readme-content-validation
  (testing "README.md contains expected getting started content"
    ;; Check file exists and has proper content in tmp config dir
    ))
```

#### Command Availability Tests
```clojure
(deftest-async create-user-activate-command-availability
  (testing "Create User Activate Script command available when script doesn't exist"
    ;; Assert command is available in palette
    ;; Execute command
    ;; Assert file is created and opened
    ;; Assert command becomes unavailable
    ))

(deftest-async create-hello-user-script-command-availability
  (testing "Create Hello User Script command conditional availability"
    ;; Similar pattern for hello script
    ))
```

#### Menu Integration Tests
```clojure
(deftest-async user-script-menu-shows-create-options
  (testing "Run User Script menu includes create options when scripts don't exist"
    ;; Execute runUserScript command (menu version)
    ;; Check menu options include create commands
    ;; Select create option, verify file creation
    ))

(deftest-async workspace-script-menu-consistency
  (testing "Workspace script menus work consistently with new pattern"
    ;; Verify existing workspace menu behavior still works
    ;; Test both activate and hello script creation
    ))
```

#### File System State Tests
```clojure
(deftest-async deps-edn-creation-timing
  (testing "deps.edn created when first script is created"
    ;; Start with clean state
    ;; Create user activate script
    ;; Assert deps.edn also created
    ;; Verify deps.edn content matches template
    ))

(deftest-async src-directory-creation
  (testing "src directory and my_lib.cljs created appropriately"
    ;; Test when src content should be created
    ;; May need to be part of hello script creation or separate command
    ))
```

#### Backwards Compatibility Tests
```clojure
(deftest-async existing-user-content-preserved
  (testing "Existing user installations are not modified"
    ;; Pre-populate user config with existing files
    ;; Trigger activation
    ;; Assert no files are modified or created
    ))

(deftest-async migration-behavior
  (testing "Users with old content can still use new commands"
    ;; Test mixed scenarios where some files exist but not others
    ))
```

### Test Utilities Needed

Add to `integration_test/db.cljs` or new utility namespace:

```clojure
(defn get-test-user-config-path []
  ;; Returns the safe tmp config path being used by tests
  ;; This will be something like /tmp/vscode-test-runner-joyride/user-config/
  )

(defn clean-test-user-config! []
  ;; Helper to reset the tmp user config directory for tests
  ;; Safe to use since it only affects the isolated test environment
  )

(defn file-exists? [path]
  ;; Promise-based file existence check
  )

(defn get-command-availability [command-id]
  ;; Check if command is available in palette
  )

(defn simulate-menu-interaction [menu-command expected-options]
  ;; Helper for testing menu flows
  )
```

### Integration with CI

- Extend existing Circle CI configuration
- Ensure tests run with clean user config for each scenario
- Add test for extension package.json command registrations
- Verify when clauses work correctly for command enabling/disabling

### Manual Testing Checklist

Create `dev/doc/CONTENT-CREATION-MANUAL-TESTS.md` with scenarios:

1. **Fresh Install Experience**
   - Install extension in clean VS Code
   - Verify only README.md created
   - Test command palette entries
   - Test menu flows

2. **Upgrade Experience**
   - Test with existing Joyride installations
   - Verify no disruption to existing content
   - Test new commands work alongside old content

3. **Cross-Platform Testing**
   - Test user config path resolution on Windows/Mac/Linux
   - Verify file permissions and directory creation

4. **Edge Cases**
   - Partial existing content (some files but not others)
   - Permission errors during file creation
   - Network drive or non-standard config locations
