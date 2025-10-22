# Joyride Terminal Output Implementation Plan

**Issue**: [#244 - Switch from Output Channel to Terminal for LM Tool Evaluation Output](https://github.com/BetterThanTomorrow/joyride/issues/244)

## User-Facing Feature Description

### What Users Will Experience

When evaluating Joyride code through **any mechanism** (LM tool, `joyride.evaluateCode` command, or other evaluation commands), all evaluation activity becomes visible in a dedicated **"Joyride Output"** terminal.

#### Before (Current Behavior - Output Channel)
- Results and stdout/stderr appear in "Joyride" Output Channel
- Code being evaluated is **not** shown (no echo)
- Output Channel mixed with other non-evaluation messages
- Less visibility into evaluation flow

#### After (New Behavior - Terminal)
- **Dedicated Terminal**: A "Joyride Output" terminal shows **only** evaluation activity
- **Echo Evaluations**: See exactly what code is being evaluated (was not shown before)
- **All Evaluation Sources**: LM tool, commands (`joyride.evaluateCode`, etc.), and scripts
- **Real-Time Output**: stdout, stderr, and results appear immediately in terminal
- **ANSI Colors**: Syntax highlighting and color-coded output categories
- **Always Visible**: Terminal persists across evaluations, building a history

### Example Terminal Output

```
Joyride Evaluation Output
This terminal displays evaluation results, output, and code.

; cljs:user
(require '[clojure.string :as str])
nil

; cljs:user
(println "Processing files...")
Processing files...
nil

; cljs:user
(str/upper-case "hello world")
"HELLO WORLD"

; cljs:user
(/ 1 0)
Error: Divide by zero
```

### User Benefits

1. **Transparency**: See what code is being evaluated (new - wasn't echoed before)
2. **Dedicated Space**: Evaluation output separate from other Joyride messages
3. **Debugging**: Identify issues when evaluations don't work as expected
4. **Learning**: Understand how Joyride APIs are being used
5. **History**: Review all evaluation activity in one place
6. **Consistency**: Same terminal output regardless of evaluation source (LM tool, commands, scripts)

### Configuration

The current Output Channel has a behavior controlled by the dynamic var `*show-when-said?*` (defaults to `false`) that prevents automatic showing except in error cases. **We should preserve this behavior** for the terminal.

**Proposed approach**:
1. Keep existing behavior: terminal doesn't auto-show by default
2. Bind similar behavior for terminal output (like `*show-when-said?*`)
3. Automatically show terminal only when evaluation errors occur
4. Users can manually show/hide terminal as needed

**No new configuration setting needed** - The existing behavioral pattern (controlled via dynamic var) works well and should be maintained for terminal output.

### Interaction

- **Terminal is read-only** for programmatic output
- **Terminal persists** across multiple evaluations
- **Can be closed** by user (recreated on next evaluation)
- **Can be hidden** (use "Toggle Panel" to show/hide terminal panel)

---

## Implementation Plan

### Phase 1: Create Output Module

**File**: `src/joyride/output.cljs`

**Tasks**:
1. [x] Create namespace with VS Code and string requires
2. [x] Implement state atoms for singleton pattern:
   - `!output-pty` - Pseudoterminal instance
   - `!output-terminal` - Terminal instance
   - `!did-last-terminate-line` - Line ending state
3. [x] Implement `create-output-terminal` function:
   - Create EventEmitter instances
   - Return #js object with Pseudoterminal interface
   - Include onDidWrite, onDidClose, open, close, handleInput, write
4. [x] Implement terminal lifecycle functions:
   - `get-output-pty` - Lazy initialization with singleton pattern
   - `show-terminal` - Show terminal with focus control
   - `write-to-terminal` - Low-level write function

**REPL Verification**:
```clojure
(require '[joyride.output :as output])
(output/show-terminal false)
;; => Terminal appears in panel
(output/write-to-terminal "Test message\r\n")
;; => "Test message" appears in terminal
```

**Acceptance Criteria**:
- [x] Terminal created with name "Joyride Output"
- [x] Terminal appears in panel when shown
- [x] Messages written to terminal display correctly
- [x] Singleton pattern prevents duplicate terminals

### Phase 2: Implement Core Output Functions

**File**: `src/joyride/output.cljs` (continued)

**Tasks**:
1. [x] Implement `normalize-line-endings`:
   - Regex replace `\n` with `\r\n`
   - Preserve existing `\r\n`
2. [x] Implement `update-line-termination-state!`:
   - Check if message ends with `\n`
   - Update atom state
3. [x] Implement `append`:
   - Normalize line endings
   - Write to terminal
   - Update line termination state
4. [x] Implement `append-line`:
   - Call append with message + `\r\n`

**REPL Verification**:
```clojure
(require '[joyride.output :as output] :reload)
(output/show-terminal false)
(output/append "First part")
(output/append " continues\n")
;; => "First part continues" on one line
(output/append-line "Second line")
;; => "Second line" on new line
```

**Acceptance Criteria**:
- [x] `append` writes without adding newline
- [x] `append-line` adds newline at end
- [x] Line endings converted correctly (`\n` → `\r\n`)
- [x] State tracking works (line termination atom updates)

### Phase 3: Add Category-Specific Output Functions

**File**: `src/joyride/output.cljs` (continued)

**Tasks**:
1. [x] Implement evaluation output functions:
   - `append-eval-out` / `append-line-eval-out`
   - `append-eval-err` / `append-line-eval-err`
2. [x] Implement other output functions:
   - `append-other-out` / `append-line-other-out`
   - `append-other-err` / `append-line-other-err`
3. [x] Implement `append-clojure-eval`:
   - Take code and options map `{:ns :repl-session-type}`
   - Write namespace comment line if ns provided
   - Write code with append-line

**REPL Verification**:
```clojure
(require '[joyride.output :as output] :reload)
(output/show-terminal false)
(output/append-clojure-eval "(+ 1 2)" {:ns "user" :repl-session-type "cljs"})
;; => Shows:
;;    ; cljs:user
;;    (+ 1 2)
(output/append-line-eval-out "stdout message")
(output/append-line-eval-err "Error: something failed")
```

**Acceptance Criteria**:
- [x] All category functions exist and work
- [x] `append-clojure-eval` formats with namespace comment
- [x] Functions route to correct underlying append functions
- [x] Different categories visually distinguishable (even without colors initially)

### Phase 4: Integrate with Evaluation System

**Files**:
- `src/joyride/lm/evaluation.cljs` (LM tool)
- `src/joyride/sci.cljs` or evaluation command handler (commands like `joyride.evaluateCode`)

**Tasks**:
1. [x] Add require for `joyride.output` to relevant files
2. [x] Identify the core evaluation function(s) that all evaluation paths use
3. [ ] Add dynamic var `*show-terminal-on-output?*` (intentionally skipped; terminal never auto-reveals)
4. [x] Modify core evaluation or its callers to add terminal output:
   - Show evaluated code before execution (NEW - enable echo)
   - Display stdout after execution (if non-empty)
   - Display stderr after execution (if non-empty)
   - Display result or error
5. [x] Handle both sync and async paths
6. [x] Show terminal with `preserve-focus? true` (terminal remains hidden unless the user opens it)

**Code Changes Pattern** (apply wherever evaluation happens):
```clojure
;; Add to requires
[joyride.output :as output]

;; Add dynamic var (in output namespace)
(def ^{:dynamic true
       :doc "Should the terminal be revealed after output?
             Default: `false` (matches *show-when-said?* pattern)"}
  *show-terminal-on-output?* false)

;; Before evaluation:
;; Always write to terminal but conditionally show it
(output/append-clojure-eval code-to-evaluate
                            {:ns current-ns
                             :repl-session-type "cljs"})
(when output/*show-terminal-on-output?*
  (output/show-terminal true))  ; Show but don't steal focus

;; After evaluation (both sync and async paths):
;; Display stdout
(when-let [stdout (:stdout result)]
  (when-not (string/blank? stdout)
    (output/append-eval-out stdout)))

;; Display stderr
(when-let [stderr (:stderr result)]
  (when-not (string/blank? stderr)
    (output/append-eval-err stderr)))

;; Display result or error
(if-let [error (:error result)]
  (do
    (binding [output/*show-terminal-on-output?* true]  ; Show on errors
      (output/append-line-eval-err (str "Error: " error))
      (when output/*show-terminal-on-output?*
        (output/show-terminal true))))
  (output/append-clojure-eval
   (pr-str (:result result))
   {:ns result-ns :repl-session-type "cljs"}))
```

**Integration Points to Investigate**:
- LM tool: `joyride.lm.evaluation/invoke-tool` - probably always show for LM tool
- Commands: Find where `joyride.evaluateCode` and similar commands execute code
- Error contexts: Where `*show-when-said?*` is bound to `true` (see `scripts_handler.cljs` lines 114, 129, 435)
- Look for: Common evaluation function that captures stdout/stderr (like `execute-code+`)
- Strategy: Either modify the common function or wrap its callers
- Pattern: Match existing `*show-when-said?*` behavior for terminal visibility

**REPL Verification**:
```clojure
;; Test via LM tool path
(require '[joyride.lm.evaluation :as eval] :reload)
(eval/execute-code+ {:code "(println \"test\")\n(+ 1 2)"
                     :ns "user"
                     :wait-for-promise? false})
;; => Check terminal shows:
;;    ; cljs:user
;;    (println "test")
;;    (+ 1 2)
;;    test
;;    ; cljs:user
;;    3

;; Test via command path (once identified)
;; Execute joyride.evaluateCode command and verify terminal output
```

**Acceptance Criteria**:
- [x] **All evaluation sources** route to terminal output (LM tool, commands, scripts)
- [x] Evaluated code appears in terminal before execution (echo enabled)
- [x] stdout appears in terminal after execution
- [x] stderr appears in terminal after execution
- [x] Results appear in terminal after execution
- [x] **Terminal auto-show behavior matches Output Channel behavior**:
   - [x] Doesn't auto-show by default (terminal stays hidden unless opened manually)
   - [ ] Auto-shows on errors (future consideration; currently opt-out)
   - [ ] LM tool evaluations show terminal (pending manual confirmation in dev host)
- [x] Terminal doesn't steal focus when shown (`preserve-focus? true`)
- [x] Both sync and async evaluation paths work
- [x] Output Channel no longer receives evaluation output (only other messages)

### Phase 5: Add ANSI Color Support

**File**: `src/joyride/output.cljs`

**Objective**: Match Calva's terminal output color scheme completely - both output categories AND Clojure syntax highlighting.

**Tasks**:

**5.1 Output Category Colors** ✅ COMPLETE
- [x] Implement `get-theme-colors` - Detect light vs dark theme
- [x] Implement `get-ansi-code` - ANSI code map with aliases
- [x] Implement `ansi-escape-seq?` - Check for existing ANSI sequences
- [x] Implement `colorize` - Add color code + message + reset code
- [x] Implement `maybe-colorize` - Only colorize if no existing ANSI codes
- [x] Update category functions to use colorization:
  - `append-eval-out` → gray
  - `append-eval-err` → red/bright red
  - `append-other-out` → green/gray
  - `append-other-err` → red/bright red

**5.2 Clojure Syntax Highlighting** ✅ COMPLETE
- [x] Integrate zprint with ANSI color support
- [x] Define Calva-compatible color maps for light/dark themes
  - Keywords, symbols, strings, numbers, etc.
- [x] Implement `syntax-highlight-clojure` function using zprint
- [x] Update `append-clojure-eval` to syntax-highlight code
- [x] Update result display to syntax-highlight values
- [x] Handle edge cases (zprint failures, fallback to plain text)

**REPL Verification**:
```clojure
(require '[joyride.output :as output] :reload)
(output/show-terminal false)

;; Test output category colors
(output/append-eval-out "This should be gray\n")
(output/append-eval-err "This should be red\n")

;; Test syntax highlighting
(output/append-clojure-eval "(defn foo [x] (* x 2))" {:ns "user"})
;; => Keywords blue, symbols default, numbers cyan
(output/append-clojure-eval "{:name \"Alice\" :age 30}" {:ns "user"})
;; => Keywords blue, strings yellow/green
```

**Acceptance Criteria**:
- [x] Output category colors match Calva (gray, red, green)
- [x] Colors work in both light and dark themes
- [x] Existing ANSI codes preserved (not double-wrapped)
- [x] Color reset codes applied correctly
- [x] **Clojure syntax highlighting matches Calva defaults**
- [x] **Keywords, strings, numbers, etc. use correct colors**
- [x] **Syntax highlighting works in both light and dark themes**
- [x] **Results (values) are syntax-highlighted like code**
- [x] Output remains readable if syntax highlighting fails (fallback)

### Phase 6: Testing & Validation

**Manual Testing Checklist**:

**Basic Terminal Functionality**:
- [ ] Terminal appears with correct name "Joyride Output"
- [ ] Terminal persists across multiple evaluations
- [ ] Terminal can be closed and recreated
- [ ] **Terminal doesn't auto-show by default** (matches Output Channel behavior)
- [ ] **Terminal auto-shows on evaluation errors** (like `*show-when-said?*`)
- [ ] Terminal respects preserve-focus setting when shown

**Line Handling**:
- [ ] Messages without newlines stay on same line
- [ ] Messages with newlines break correctly
- [ ] `append-line` adds proper line breaks
- [ ] Mixed newline styles handled correctly

**Output Categories**:
- [ ] Evaluated code shows with namespace comment
- [ ] stdout displays correctly (evalOut)
- [ ] stderr displays correctly (evalErr)
- [ ] Results display correctly
- [ ] Errors display correctly

**All Evaluation Sources**:
- [ ] LM tool evaluations appear in terminal
- [ ] `joyride.evaluateCode` command evaluations appear in terminal
- [ ] Other evaluation commands appear in terminal
- [ ] Script evaluations appear in terminal (if applicable)
- [ ] Multiple evaluations in same session work
- [ ] Long output doesn't break terminal
- [ ] Terminal updates in real-time

**Edge Cases**:
- [ ] Empty stdout/stderr handled gracefully
- [ ] Very long output displays without issues
- [ ] Special characters display correctly
- [ ] Unicode characters work properly
- [ ] Terminal survives VS Code window reload

**ANSI Colors** (if implemented):
- [ ] Colors appear in dark theme
- [ ] Colors appear in light theme
- [ ] Existing ANSI codes preserved
- [ ] Color reset works correctly

### Phase 7: Documentation & Cleanup

**Tasks**:
1. Add docstrings to all public functions
2. Update CHANGELOG.md:
   ```markdown
   ### [Unreleased]
   #### Changed
   - Language Model Tool evaluations now display in "Joyride Output" terminal instead of Output Channel for transparency and real-time feedback. [#244](https://github.com/BetterThanTomorrow/joyride/issues/244)
   ```
3. Consider adding section to main README about terminal output
4. Update any existing documentation mentioning Output Channel

**Documentation Review**:
- [ ] All functions have clear docstrings
- [ ] CHANGELOG updated
- [ ] README reflects terminal output (if mentioned)
- [ ] No stale references to Output Channel behavior

---

## Implementation Notes

### Dependencies

**Required**:
- `["vscode" :as vscode]` - Terminal and EventEmitter APIs
- `[clojure.string :as string]` - String manipulation
- `[promesa.core :as p]` - Promise handling (already in evaluation.cljs)

**No new dependencies needed** - All required APIs available in VS Code and ClojureScript core.

### File Structure

```
src/joyride/
├── output.cljs (NEW)
├── sci.cljs (POSSIBLY MODIFIED - if commands route through here)
└── lm/
    └── evaluation.cljs (MODIFIED)
```

**Note**: Need to investigate where `joyride.evaluateCode` and similar commands are implemented to ensure all evaluation paths route to terminal output.

### State Management

All state managed via `defonce` atoms in `joyride.output`:
- Singleton terminal instance prevents duplicates
- Line termination state enables smart newline insertion
- State persists across hot-reloads during development

**Auto-Show Behavior**:
- Dynamic var `*show-terminal-on-output?*` controls terminal visibility (defaults to `false`)
- Matches existing `*show-when-said?*` pattern for Output Channel
- Terminal is created and written to regardless of auto-show setting
- Users can manually show/hide terminal at any time

### Error Handling

**Terminal Creation Failures**:
- Unlikely (VS Code API stable)
- If it fails, evaluation continues but output not visible
- Consider logging to console if terminal creation fails

**Line Ending Edge Cases**:
- Regex handles mixed `\n` and `\r\n` correctly
- State tracking prevents double newlines
- Empty strings handled gracefully

### Performance Considerations

**Terminal Writing**:
- Synchronous operation (fast)
- No accumulation/buffering needed
- Multiple small writes acceptable (terminal handles efficiently)

**Singleton Pattern**:
- Terminal created once and reused
- Prevents resource leaks
- Matches Calva's proven approach

### Development Workflow

**Recommended Order**:
1. Create `output.cljs` with basic terminal creation
2. Test terminal creation and display in REPL
3. Add core functions (append, append-line)
4. Test writing to terminal in REPL
5. Add category-specific functions
6. Test category functions in REPL
7. Integrate with evaluation system
8. Test full evaluation flow
9. Add colors (optional)
10. Final testing and documentation

**Hot-Reload Considerations**:
- Use `defonce` for atoms (preserve state during reload)
- Terminal instance survives hot-reload
- May need to manually close terminal during development to test recreation

### Rollback Plan

If issues discovered after release:

1. **Quick Fix**: Add configuration to disable terminal output
2. **Rollback**: Revert changes to `evaluation.cljs`, remove `output.cljs`
3. **Root Cause**: Terminal creation, line endings, or VS Code API issues most likely

Configuration flag could be:
```clojure
;; In config.cljs
(defn use-terminal-output? []
  (get-config "joyride.lm.useTerminalOutput" true))

;; In evaluation.cljs
(when (config/use-terminal-output?)
  (output/append-clojure-eval ...))
```

But this should only be added if problems arise - default implementation is terminal-only.

---

## Success Criteria

### Definition of Done

- [x] `src/joyride/output.cljs` created with full implementation
- [x] **All evaluation paths** integrated with terminal output:
   - [x] LM tool (`joyride.lm.evaluation`)
   - [x] Command evaluations (`joyride.evaluateCode`, etc.)
   - [x] Any other evaluation mechanisms
- [x] Output Channel no longer receives evaluation output
- [ ] All REPL verification tests pass
- [ ] Manual testing checklist completed
- [ ] CHANGELOG.md updated
- [ ] No compilation warnings
- [ ] No linting errors
- [ ] Hot-reload tested and working
- [ ] Extension Development Host testing completed
- [ ] Documentation updated

### Validation with User

**When user returns**, they should:
1. Read "User-Facing Feature Description" section
2. Confirm understanding matches intent
3. Suggest any corrections needed
4. Approve proceeding with implementation

---

## References

### Related Documentation

- [Calva Terminal Output Implementation Guide](./calva-terminal-output-implementation.md) - Comprehensive guide with REPL-verified patterns, Pseudoterminal implementation details, and TypeScript→ClojureScript translations
- [GitHub Issue #244](https://github.com/BetterThanTomorrow/joyride/issues/244) - Switch from Output Channel to Terminal for LM Tool Evaluation Output

### Joyride Source Files

- [`src/joyride/vscode_utils.cljs`](../../src/joyride/vscode_utils.cljs) - Current `*show-when-said?*` pattern (lines 45-62)
- [`src/joyride/scripts_handler.cljs`](../../src/joyride/scripts_handler.cljs) - Error contexts where auto-show is enabled (lines 114, 129, 435)
- [`src/joyride/lm/evaluation.cljs`](../../src/joyride/lm/evaluation.cljs) - LM tool evaluation with stdout/stderr capture
- [`src/joyride/db.cljs`](../../src/joyride/db.cljs) - Current Output Channel management

### Historical Context

- [CHANGELOG.md](../../CHANGELOG.md) - Issues #36 and #46 about not auto-showing Output Channel
  - v0.0.9 (2022-05-11): "Don't unconditionally show the Joyride output channel on start"
  - v0.0.8 (2022-05-09): "Don't automatically show the Joyride output channel when scripts run"

---

**Document Status**: In progress – awaiting dev host validation
**Created**: 2025-10-13
**Next Step**: Verify async stdout visibility and script result formatting in the dev host

### Outstanding Follow-ups

- [x] Investigate suppressing per-form `=>` echoes when running scripts so output mirrors `load-file`.
