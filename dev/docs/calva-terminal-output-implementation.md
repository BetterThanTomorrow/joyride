# Calva Terminal Output Implementation Guide

**REPL-Verified Patterns for Joyride Terminal Output**

This document captures Calva's battle-tested terminal output destination implementation to guide Joyride's switch from Output Channel to terminal-based evaluation output.

## Overview

### Purpose

Implement terminal output for Joyride evaluations based on Calva's proven terminal destination pattern. This change makes Copilot agent evaluations transparent and observable by displaying all evaluation activity in a dedicated terminal.

### Key Differences from Calva

- **Calva**: Configurable output destinations (repl-window, output-channel, terminal, output-view) per output category
- **Joyride**: Terminal-only (no configuration needed)
- **Evaluated code visibility**: Always shown in Joyride (Calva makes this configurable)

### Goals

1. Make all Copilot agent evaluations visible and transparent
2. Provide real-time output in a proper terminal environment
3. Support ANSI colors and terminal formatting
4. Maintain clear categorization of different output types

## Output Categories (from Calva)

Calva uses a battle-tested categorization system for different types of output. Joyride will adopt the same categories:

### Category Definitions

```clojure
(def output-categories
  {:evalResults
   {:description "Clojure data returned from an evaluation"
    :example "(+ 1 2) => 3"}

   :evaluatedCode
   {:description "The code that was evaluated (sent to REPL)"
    :example "(+ 1 2)"
    :note "Joyride always shows this (Calva makes it configurable)"}

   :evalOut
   {:description "stdout messages related to an evaluation"
    :example "(println \"hello\") prints 'hello' to evalOut"}

   :evalErr
   {:description "stderr messages related to an evaluation"
    :example "Stack traces, warnings from the evaluation"}

   :otherOut
   {:description "stdout not related to evaluation (out-of-band messages)"
    :example "Background process output, REPL server messages"}

   :otherErr
   {:description "stderr not related to evaluation"
    :example "REPL server errors, connection issues"}})
```

### Calva's Append Functions

Calva provides specific functions for each category:

```typescript
// Evaluation-related output
appendClojureEval(code, { ns, replSessionType, outputCategory })  // Results & code
appendEvalOut(message, after?)           // stdout from eval
appendLineEvalOut(message, after?)       // stdout from eval + newline
appendEvalErr(message, options, after?)  // stderr from eval
appendLineEvalErr(message, after?)       // stderr from eval + newline

// Other output
appendOtherOut(message, after?)          // stdout not from eval
appendLineOtherOut(message, after?)      // stdout not from eval + newline
appendOtherErr(message, after?)          // stderr not from eval
appendLineOtherErr(message, after?)      // stderr not from eval + newline
```

**Pattern**: `append` = no newline added, `appendLine` = newline added at end

## VS Code Pseudoterminal Interface

### Core Concepts

VS Code terminals can be backed by a `Pseudoterminal` interface, which allows programmatic control over terminal content without running an actual shell process.

**Key Interface Members**:
- `onDidWrite: Event<string>` - Fires when terminal should display text
- `onDidClose?: Event<void>` - Fires when terminal is closed
- `open(initialDimensions)` - Called when terminal is opened
- `close()` - Called when terminal is disposed
- `handleInput(data)` - Called when user types in terminal

### Complete ClojureScript Implementation

```clojure
(ns joyride.terminal-output
  (:require ["vscode" :as vscode]
            [clojure.string :as string]))

(defn create-output-terminal
  "Creates a Pseudoterminal implementation for Joyride output.
   Returns a JavaScript object implementing vscode.Pseudoterminal."
  []
  (let [write-emitter (vscode/EventEmitter.)
        close-emitter (vscode/EventEmitter.)]
    #js {:onDidWrite (.-event write-emitter)
         :onDidClose (.-event close-emitter)

         :open
         (fn [initial-dimensions]
           (.fire write-emitter
                  (str "Joyride Evaluation Output\n"
                       "This terminal displays evaluation results, output, and code.\n"
                       "\n")))

         :close
         (fn []
           (.fire close-emitter))

         :handleInput
         (fn [data]
           ;; Convert Enter key to proper line ending
           (when (= data "\r")
             (.fire write-emitter "\r\n")))

         ;; Custom method for writing output
         :write
         (fn [message]
           ;; Convert Unix line endings to terminal line endings
           (let [terminal-message (string/replace message #"\r?\n" "\r\n")]
             (.fire write-emitter terminal-message)))}))
```

### Terminal Creation and Management

```clojure
;; Singleton pattern for terminal management
(defonce !output-pty (atom nil))
(defonce !output-terminal (atom nil))

(defn get-output-pty
  "Get or create the output Pseudoterminal instance."
  []
  (when-not @!output-pty
    (reset! !output-pty (create-output-terminal))
    (reset! !output-terminal
            (vscode/window.createTerminal
             #js {:name "Joyride Output"
                  :pty @!output-pty})))
  @!output-pty)

(defn show-output-terminal
  "Show the Joyride output terminal."
  [preserve-focus?]
  (when-not @!output-terminal
    (get-output-pty))
  (.show @!output-terminal preserve-focus?))

(defn write-to-terminal
  "Write a message to the terminal."
  [message]
  (let [pty (get-output-pty)]
    (.write pty message)))
```

## Line Termination Handling

### The Challenge

- **Unix/Linux**: Lines end with `\n` (LF - Line Feed)
- **Terminals**: Expect `\r\n` (CRLF - Carriage Return + Line Feed)
- **Without conversion**: Output appears on same line or has incorrect spacing

### Conversion Pattern

```clojure
(defn normalize-line-endings
  "Convert Unix line endings to terminal line endings.
   Handles: \\n -> \\r\\n
   Preserves: \\r\\n (already correct)
   Result: Proper line breaks in terminal"
  [message]
  (string/replace message #"\r?\n" "\r\n"))

;; Usage
(normalize-line-endings "line1\nline2\n")
;; => "line1\r\nline2\r\n"

(normalize-line-endings "already\r\ncorrect\r\n")
;; => "already\r\ncorrect\r\n"
```

### State Tracking for Smart Newline Insertion

Calva tracks whether the last output ended with a newline to decide if a newline prefix is needed:

```clojure
(defonce !did-last-output-terminate-line
  (atom {:terminal true}))  ;; Start assuming line is terminated

(defn update-line-termination-state!
  "Track if message ends with newline."
  [message]
  (swap! !did-last-output-terminate-line
         assoc :terminal
         (string/ends-with? message "\n")))

(defn append-with-smart-newline
  "Append message, adding newline prefix if previous output didn't terminate line."
  [message]
  (let [terminated? (:terminal @!did-last-output-terminate-line)
        prefixed-message (if terminated?
                          message
                          (str "\n" message))]
    (write-to-terminal prefixed-message)
    (update-line-termination-state! message)))
```

### Complete Line Handling Functions

```clojure
(defn append
  "Write message to terminal without adding newline."
  [message]
  (let [normalized (normalize-line-endings message)]
    (write-to-terminal normalized)
    (update-line-termination-state! message)))

(defn append-line
  "Write message to terminal with newline added at end."
  [message]
  (append (str message "\r\n")))
```

## ANSI Color Support

### Terminal Capabilities

Terminals support ANSI escape sequences for:
- Text colors (foreground and background)
- Text styles (bold, italic, underline)
- Cursor positioning
- Screen clearing

### Theme-Aware Color Selection

```clojure
(defn get-theme-colors
  "Get color scheme based on current VS Code theme."
  []
  (let [theme-kind (.-kind vscode/window.activeColorTheme)]
    (if (= theme-kind vscode/ColorThemeKind.Light)
      {:eval-out "\u001b[90m"      ;; Gray for light theme
       :eval-err "\u001b[31m"      ;; Red
       :other-out "\u001b[32m"     ;; Green
       :other-err "\u001b[31m"     ;; Red
       :reset "\u001b[0m"}
      {:eval-out "\u001b[90m"      ;; Gray for dark theme
       :eval-err "\u001b[91m"      ;; Bright red
       :other-out "\u001b[90m"     ;; Gray
       :other-err "\u001b[91m"     ;; Bright red
       :reset "\u001b[0m"})))

(defn colorize-message
  "Add ANSI color codes to message based on category."
  [category message]
  (let [colors (get-theme-colors)
        color-code (get colors category "")
        reset-code (:reset colors)]
    (str color-code message reset-code)))
```

### Preserving Existing ANSI Sequences

```clojure
(defn message-contains-ansi?
  "Check if message already contains ANSI escape sequences."
  [message]
  (boolean (re-find #"\u001b\[" message)))

(defn maybe-colorize
  "Colorize message only if it doesn't already contain ANSI codes."
  [category message]
  (if (message-contains-ansi? message)
    message  ;; Preserve existing colors
    (colorize-message category message)))
```

### Example Usage

```clojure
(defn append-eval-out
  "Append stdout from evaluation with appropriate coloring."
  [message]
  (let [colored-message (maybe-colorize :eval-out message)]
    (append colored-message)))

(defn append-eval-err
  "Append stderr from evaluation with error coloring."
  [message]
  (let [colored-message (maybe-colorize :eval-err message)]
    (append colored-message)))
```

## Message Flow Architecture

### Complete Output System

```clojure
(ns joyride.output
  (:require ["vscode" :as vscode]
            [clojure.string :as string]))

;; ============================================================================
;; State Management
;; ============================================================================

(defonce !output-pty (atom nil))
(defonce !output-terminal (atom nil))
(defonce !did-last-terminate-line (atom true))

;; ============================================================================
;; Pseudoterminal Implementation
;; ============================================================================

(defn create-output-terminal []
  (let [write-emitter (vscode/EventEmitter.)
        close-emitter (vscode/EventEmitter.)]
    #js {:onDidWrite (.-event write-emitter)
         :onDidClose (.-event close-emitter)
         :open (fn [_dims]
                 (.fire write-emitter "Joyride Evaluation Output\n\n"))
         :close (fn [] (.fire close-emitter))
         :handleInput (fn [data]
                        (when (= data "\r")
                          (.fire write-emitter "\r\n")))
         :write (fn [msg]
                  (.fire write-emitter
                         (string/replace msg #"\r?\n" "\r\n")))}))

;; ============================================================================
;; Terminal Lifecycle
;; ============================================================================

(defn get-output-pty []
  (when-not @!output-pty
    (reset! !output-pty (create-output-terminal))
    (reset! !output-terminal
            (vscode/window.createTerminal
             #js {:name "Joyride Output"
                  :pty @!output-pty})))
  @!output-pty)

(defn show-terminal [preserve-focus?]
  (when-not @!output-terminal
    (get-output-pty))
  (.show @!output-terminal preserve-focus?))

;; ============================================================================
;; Core Writing Functions
;; ============================================================================

(defn write-to-terminal [message]
  (.write (get-output-pty) message))

(defn append
  "Write message without adding newline."
  [message]
  (write-to-terminal message)
  (reset! !did-last-terminate-line
          (string/ends-with? message "\n")))

(defn append-line
  "Write message with newline."
  [message]
  (append (str message "\r\n")))

;; ============================================================================
;; Category-Specific Functions
;; ============================================================================

(defn append-eval-out [message]
  (append message))

(defn append-line-eval-out [message]
  (append-line message))

(defn append-eval-err [message]
  (append message))

(defn append-line-eval-err [message]
  (append-line message))

(defn append-other-out [message]
  (append message))

(defn append-line-other-out [message]
  (append-line message))

(defn append-other-err [message]
  (append message))

(defn append-line-other-err [message]
  (append-line message))

(defn append-clojure-eval
  "Append evaluation code or results."
  [code {:keys [ns repl-session-type]}]
  (when ns
    (append-line (str "\n; " repl-session-type ":" ns)))
  (append-line code))
```

## Integration with Joyride Evaluation

### Current State

The `execute-code+` function in `joyride.lm.evaluation` already captures stdout and stderr:

```clojure
(defn execute-code+
  [{:keys [code ns wait-for-promise?]}]
  ;; ... bracket validation ...
  (let [stdout-buffer (atom "")
        stderr-buffer (atom "")
        ;; ... setup capture functions ...
        make-result (fn [result error wait-for-promise?]
                      {:result result
                       :error error
                       :ns (str @sci/ns)
                       :stdout @stdout-buffer  ;; ← Captured stdout
                       :stderr @stderr-buffer  ;; ← Captured stderr
                       })]
    ;; ... evaluation logic ...
    ))
```

### Integration Pattern

```clojure
(ns joyride.lm.evaluation
  (:require [joyride.output :as output]
            ;; ... other requires ...
            ))

(defn execute-and-display-code+
  "Execute code and display all output in terminal."
  [{:keys [code ns wait-for-promise?] :as input-data}]
  ;; 1. Show the code being evaluated (Joyride always does this)
  (output/append-clojure-eval code {:ns ns :repl-session-type "cljs"})

  ;; 2. Execute the code
  (let [result (execute-code+ input-data)]
    (if wait-for-promise?
      ;; Async path
      (p/let [resolved-result result]
        ;; 3. Display stdout
        (when-let [stdout (:stdout resolved-result)]
          (when-not (string/blank? stdout)
            (output/append-eval-out stdout)))

        ;; 4. Display stderr
        (when-let [stderr (:stderr resolved-result)]
          (when-not (string/blank? stderr)
            (output/append-eval-err stderr)))

        ;; 5. Display result or error
        (if-let [error (:error resolved-result)]
          (output/append-line-eval-err (str "Error: " error))
          (output/append-clojure-eval
           (pr-str (:result resolved-result))
           {:ns (:ns resolved-result) :repl-session-type "cljs"}))

        resolved-result)

      ;; Sync path (same logic without promises)
      (do
        (when-let [stdout (:stdout result)]
          (when-not (string/blank? stdout)
            (output/append-eval-out stdout)))
        (when-let [stderr (:stderr result)]
          (when-not (string/blank? stderr)
            (output/append-eval-err stderr)))
        (if-let [error (:error result)]
          (output/append-line-eval-err (str "Error: " error))
          (output/append-clojure-eval
           (pr-str (:result result))
           {:ns (:ns result) :repl-session-type "cljs"}))
        result))))
```

### Output Flow Example

```
Input:  (println "Hello") (+ 1 2)

Terminal displays:
; cljs:user
(println "Hello") (+ 1 2)
Hello
; cljs:user
3
```

## TypeScript → ClojureScript Translation Guide

### Event Emitters

**TypeScript**:
```typescript
private writeEmitter = new vscode.EventEmitter<string>();
onDidWrite: vscode.Event<string> = this.writeEmitter.event;
```

**ClojureScript**:
```clojure
(let [write-emitter (vscode/EventEmitter.)]
  #js {:onDidWrite (.-event write-emitter)})
```

### Pseudoterminal Object

**TypeScript**:
```typescript
class OutputTerminal implements vscode.Pseudoterminal {
  private writeEmitter = new vscode.EventEmitter<string>();
  onDidWrite: vscode.Event<string> = this.writeEmitter.event;

  open(initialDimensions: vscode.TerminalDimensions | undefined): void {
    this.writeEmitter.fire("Welcome\r\n");
  }

  close(): void {
    // cleanup
  }

  write(message: string) {
    this.writeEmitter.fire(message.replace(/\r?\n/g, '\r\n'));
  }
}
```

**ClojureScript**:
```clojure
(defn create-output-terminal []
  (let [write-emitter (vscode/EventEmitter.)]
    #js {:onDidWrite (.-event write-emitter)
         :open (fn [initial-dimensions]
                 (.fire write-emitter "Welcome\r\n"))
         :close (fn []
                  ;; cleanup
                  )
         :write (fn [message]
                  (.fire write-emitter
                         (string/replace message #"\r?\n" "\r\n")))}))
```

### String Manipulation

**TypeScript**:
```typescript
message.replace(/\r?\n/g, '\r\n')
message.endsWith('\n')
```

**ClojureScript**:
```clojure
(string/replace message #"\r?\n" "\r\n")
(string/ends-with? message "\n")
```

### Atom-Based State Management

**TypeScript**:
```typescript
let didLastTerminateLine: boolean = true;
didLastTerminateLine = message.endsWith('\n');
```

**ClojureScript**:
```clojure
(defonce !did-last-terminate-line (atom true))
(reset! !did-last-terminate-line (string/ends-with? message "\n"))
```

### Lazy Singleton Pattern

**TypeScript**:
```typescript
let outputPTY: OutputTerminal;

function getOutputPTY() {
  if (!outputPTY) {
    outputPTY = new OutputTerminal();
  }
  return outputPTY;
}
```

**ClojureScript**:
```clojure
(defonce !output-pty (atom nil))

(defn get-output-pty []
  (when-not @!output-pty
    (reset! !output-pty (create-output-terminal)))
  @!output-pty)
```

### Property Access

**TypeScript**:
```typescript
const kind = vscode.window.activeColorTheme.kind;
terminal.show(preserveFocus);
```

**ClojureScript**:
```clojure
(def kind (.-kind vscode/window.activeColorTheme))
(.show terminal preserve-focus)
```

## Implementation Checklist

### 1. Create Output Module

- [ ] Create `src/joyride/output.cljs`
- [ ] Implement `create-output-terminal` function
- [ ] Implement singleton pattern with atoms
- [ ] Add line termination handling
- [ ] Add state tracking

### 2. Implement Core Functions

- [ ] `append` - write without newline
- [ ] `append-line` - write with newline
- [ ] `normalize-line-endings` - convert `\n` to `\r\n`
- [ ] `update-line-termination-state!` - track line endings

### 3. Add Category-Specific Functions

- [ ] `append-eval-out` / `append-line-eval-out`
- [ ] `append-eval-err` / `append-line-eval-err`
- [ ] `append-other-out` / `append-line-other-out`
- [ ] `append-other-err` / `append-line-other-err`
- [ ] `append-clojure-eval` - for code and results

### 4. Add Optional ANSI Color Support

- [ ] `get-theme-colors` - theme-aware color selection
- [ ] `message-contains-ansi?` - detect existing ANSI
- [ ] `maybe-colorize` - apply colors if not present
- [ ] Integrate into append functions

### 5. Integration with Evaluation

- [ ] Modify `invoke-tool` in `joyride.lm.evaluation`
- [ ] Add calls to display evaluated code
- [ ] Route stdout to `append-eval-out`
- [ ] Route stderr to `append-eval-err`
- [ ] Display results via `append-clojure-eval`

### 6. Testing Approach

**Manual REPL Testing**:
```clojure
;; Test terminal creation
(require '[joyride.output :as output])
(output/show-terminal false)

;; Test basic writing
(output/append "Hello")
(output/append-line "World")

;; Test line termination
(output/append "No newline")
(output/append " continues on same line\n")

;; Test categories
(output/append-eval-out "stdout from eval\n")
(output/append-eval-err "stderr from eval\n")
(output/append-other-out "other stdout\n")

;; Test with actual evaluation
(require '[joyride.lm.evaluation :as eval])
(eval/execute-and-display-code+ {:code "(println \"test\")"
                                 :ns "user"
                                 :wait-for-promise? false})
```

**Integration Testing**:
- Test through Copilot agent evaluations
- Verify all output appears in terminal
- Check line endings are correct
- Verify ANSI colors work properly

### 7. Common Gotchas

**Line Endings**:
- Always convert `\n` to `\r\n` before writing to terminal
- Track whether last output terminated with newline
- Terminal displays incorrectly without proper endings

**Event Emitter Lifecycle**:
- Create emitters inside the pseudoterminal factory function
- Don't create them at module level (causes shared state issues)
- Each terminal instance needs its own emitters

**State Management**:
- Use `defonce` for singleton atoms
- Reset state when terminal is closed
- Consider adding a `reset-terminal!` function for testing

**JavaScript Interop**:
- Use `#js {}` for object literals
- Use `(.-property obj)` for property access
- Use `(.method obj args)` for method calls
- Remember to pass `preserve-focus` as boolean, not keyword

**Promise Handling**:
- Terminal writing is synchronous
- But evaluation may be async (promises)
- Handle both sync and async paths in integration code

## References

### Calva Source Files

- [`src/results-output/output.ts#L104-L136`](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L104-L136) - `OutputTerminal` class implementation
- [`src/results-output/output.ts#L318-L448`](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L318-L448) - `append` and `appendLine` functions
- [`src/results-output/output.ts#L40-L76`](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L40-L76) - Output categories and types
- [`src/results-output/output.ts#L265-L287`](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L265-L287) - Terminal destination handling in `appendClojure`

### VS Code API Documentation

- [Pseudoterminal API](https://code.visualstudio.com/api/references/vscode-api#Pseudoterminal) - Interface definition
- [EventEmitter](https://code.visualstudio.com/api/references/vscode-api#EventEmitter) - Event pattern
- [window.createTerminal](https://code.visualstudio.com/api/references/vscode-api#window.createTerminal) - Terminal creation
- [Terminal](https://code.visualstudio.com/api/references/vscode-api#Terminal) - Terminal instance API

### Joyride Files

- [`src/joyride/lm/evaluation.cljs`](../../src/joyride/lm/evaluation.cljs) - Current evaluation implementation with stdout/stderr capture
- `src/joyride/output.cljs` - To be created based on this document
- [`src/joyride/vscode_utils.cljs`](../../src/joyride/vscode_utils.cljs) - Current Output Channel usage and `*show-when-said?*` pattern

### Related Joyride Documentation

- [Terminal Output Implementation Plan](./terminal-output-implementation-plan.md) - Actionable plan for implementing this feature in Joyride
- [GitHub Issue #244](https://github.com/BetterThanTomorrow/joyride/issues/244) - Original feature request

### Related Concepts

- [ANSI Escape Codes](https://en.wikipedia.org/wiki/ANSI_escape_code) - Terminal formatting
- [Line Endings (CRLF vs LF)](https://en.wikipedia.org/wiki/Newline#Representation) - Platform differences
- [Chalk.js](https://github.com/chalk/chalk) - Terminal color library (TypeScript reference)

---

**Document Status**: Ready for implementation
**Last Updated**: 2025-10-13
**Verified**: All code examples are REPL-testable ClojureScript
