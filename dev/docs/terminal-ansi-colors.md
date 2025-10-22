# Terminal ANSI Colors for Joyride Output

**Calva-Compatible Color Scheme for Terminal Output**

This document defines ANSI color implementation for Joyride's terminal output, matching Calva's proven color scheme to provide a familiar experience for Calva users.

## Overview

Joyride's terminal output will use ANSI escape sequences to colorize different output categories, matching Calva's terminal destination colors while adapting to the user's VS Code theme (Light/Dark/HighContrast).

## ANSI Color Reference

### ANSI Escape Sequence Format

```
\u001b[<code>m<text>\u001b[0m
  │     │      │      │
  │     │      │      └─ Reset code (returns to default)
  │     │      └──────── Your text
  │     └─────────────── Color/style code
  └───────────────────── Escape sequence start
```

### Standard ANSI Codes

```clojure
;; Basic colors (30-37 for foreground)
{:black   30
 :red     31
 :green   32
 :yellow  33
 :blue    34
 :magenta 35
 :cyan    36
 :white   37}

;; Bright colors (90-97 for foreground)
{:bright-black   90
 :bright-red     91
 :bright-green   92
 :bright-yellow  93
 :bright-blue    94
 :bright-magenta 95
 :bright-cyan    96
 :bright-white   97}

;; Special codes
{:reset 0      ;; Reset all attributes
 :bold 1       ;; Bold/bright text
 :dim 2        ;; Dimmed text
 :italic 3     ;; Italic text
 :underline 4} ;; Underlined text
```

## Calva's Color Scheme

### Source: Calva's `output.ts`

From [Calva's output.ts](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L40-L76):

```typescript
const lightTheme = {
  evalSeparatorSessionType: customChalk.bgGreen,
  evalSeparatorNs: customChalk.bgBlue,
  evalOut: customChalk.gray,
  evalErr: customChalk.red,
  otherOut: customChalk.green,
  otherErr: customChalk.red,
};

const darkTheme = {
  evalSeparatorSessionType: customChalk.bgWhite,
  evalSeparatorNs: customChalk.bgWhiteBright,
  evalOut: customChalk.gray,
  evalErr: customChalk.redBright,
  otherOut: customChalk.grey,
  otherErr: customChalk.redBright,
};
```

### Chalk.js Color Mapping

```clojure
;; Chalk colors → ANSI codes
{:gray       "\u001b[90m"   ;; Bright black (used for evalOut in both themes)
 :red        "\u001b[31m"   ;; Red (evalErr in light theme)
 :redBright  "\u001b[91m"   ;; Bright red (evalErr in dark theme)
 :green      "\u001b[32m"   ;; Green (otherOut in light theme)
 :grey       "\u001b[90m"}  ;; Same as gray (otherOut in dark theme)
```

## Joyride Color Implementation

### Color Definitions

```clojure
(ns joyride.output
  (:require ["vscode" :as vscode]
            [clojure.string :as string]))

(def ansi-codes
  "ANSI escape sequence codes"
  {:reset "\u001b[0m"

   ;; Foreground colors
   :black   "\u001b[30m"
   :red     "\u001b[31m"
   :green   "\u001b[32m"
   :yellow  "\u001b[33m"
   :blue    "\u001b[34m"
   :magenta "\u001b[35m"
   :cyan    "\u001b[36m"
   :white   "\u001b[37m"

   ;; Bright foreground colors
   :bright-black   "\u001b[90m"  ;; aka gray
   :bright-red     "\u001b[91m"
   :bright-green   "\u001b[92m"
   :bright-yellow  "\u001b[93m"
   :bright-blue    "\u001b[94m"
   :bright-magenta "\u001b[95m"
   :bright-cyan    "\u001b[96m"
   :bright-white   "\u001b[97m"

   ;; Aliases matching Chalk
   :gray :bright-black
   :grey :bright-black})

(defn get-ansi-code
  "Get ANSI code for a color key, resolving aliases"
  [color-key]
  (let [code-or-alias (get ansi-codes color-key)]
    (if (keyword? code-or-alias)
      (get ansi-codes code-or-alias)  ;; Resolve alias
      code-or-alias)))
```

### Theme Detection

```clojure
(defn current-theme-kind
  "Get current VS Code theme kind"
  []
  (.-kind vscode/window.activeColorTheme))

(defn light-theme?
  "Check if current theme is light"
  []
  (= (current-theme-kind) vscode/ColorThemeKind.Light))

(defn dark-theme?
  "Check if current theme is dark"
  []
  (= (current-theme-kind) vscode/ColorThemeKind.Dark))

(defn high-contrast-theme?
  "Check if current theme is high contrast"
  []
  (or (= (current-theme-kind) vscode/ColorThemeKind.HighContrast)
      (= (current-theme-kind) vscode/ColorThemeKind.HighContrastLight)))
```

### Color Scheme Selection

```clojure
(defn get-output-colors
  "Get color scheme based on current VS Code theme.
   Matches Calva's terminal output colors."
  []
  (cond
    ;; High contrast themes use more pronounced colors
    (high-contrast-theme?)
    {:eval-out :white
     :eval-err :bright-red
     :other-out :bright-green
     :other-err :bright-red
     :reset :reset}

    ;; Light theme colors (match Calva)
    (light-theme?)
    {:eval-out :gray        ;; Gray for stdout from eval
     :eval-err :red          ;; Red for stderr from eval
     :other-out :green       ;; Green for other stdout
     :other-err :red         ;; Red for other stderr
     :reset :reset}

    ;; Dark theme colors (match Calva)
    :else
    {:eval-out :gray         ;; Gray for stdout from eval
     :eval-err :bright-red    ;; Bright red for stderr from eval
     :other-out :grey         ;; Gray for other stdout
     :other-err :bright-red   ;; Bright red for other stderr
     :reset :reset}))
```

### Colorization Functions

```clojure
(defn ansi-escape-seq?
  "Check if message contains ANSI escape sequences"
  [message]
  (boolean (re-find #"\u001b\[" message)))

(defn colorize
  "Add ANSI color codes to message.
   color-key: Keyword from ansi-codes map"
  [color-key message]
  (let [color-code (get-ansi-code color-key)
        reset-code (get-ansi-code :reset)]
    (str color-code message reset-code)))

(defn maybe-colorize
  "Colorize message only if it doesn't already contain ANSI codes.
   This preserves user-provided or library-generated colors."
  [color-key message]
  (if (ansi-escape-seq? message)
    message  ;; Already colored, don't double-wrap
    (colorize color-key message)))

(defn colorize-by-category
  "Apply category-appropriate color to message based on current theme"
  [category message]
  (let [colors (get-output-colors)
        color-key (get colors category)]
    (maybe-colorize color-key message)))
```

### Integration with Output Functions

```clojure
;; Update existing append functions to use colors

(defn append-eval-out
  "Append stdout generated during evaluation."
  [message]
  (let [colored (colorize-by-category :eval-out message)]
    (append colored)))

(defn append-line-eval-out
  "Append stdout and ensure a newline."
  [message]
  (let [colored (colorize-by-category :eval-out message)]
    (append-line colored)))

(defn append-eval-err
  "Append stderr generated during evaluation."
  [message]
  (let [colored (colorize-by-category :eval-err message)]
    (append colored)))

(defn append-line-eval-err
  "Append stderr and ensure a newline."
  [message]
  (let [colored (colorize-by-category :eval-err message)]
    (append-line colored)))

(defn append-other-out
  "Append non-evaluation stdout messages."
  [message]
  (let [colored (colorize-by-category :other-out message)]
    (append colored)))

(defn append-line-other-out
  "Append non-evaluation stdout and ensure newline."
  [message]
  (let [colored (colorize-by-category :other-out message)]
    (append-line colored)))

(defn append-other-err
  "Append non-evaluation stderr messages."
  [message]
  (let [colored (colorize-by-category :other-err message)]
    (append colored)))

(defn append-line-other-err
  "Append non-evaluation stderr and ensure newline."
  [message]
  (let [colored (colorize-by-category :other-err message)]
    (append-line colored)))
```

## Clojure Syntax Highlighting

### Evaluated Code and Results

For evaluated Clojure code and results, we should consider syntax highlighting similar to how Calva does it. However, this is complex and may be deferred to Phase 5 of the implementation plan.

**Options**:

1. **No highlighting** (simplest): Display code in default terminal color
   ```clojure
   (defn append-clojure-eval
     [code {:keys [ns]}]
     (when ns
       (append-line-other-out (str "; " ns)))
     (append-line code))
   ```

2. **Simple semantic coloring**: Use different colors for different token types
   ```clojure
   ;; Example: Keywords in cyan, strings in yellow, etc.
   ;; This requires a tokenizer/parser
   ```

3. **Zprint-based highlighting**: Use zprint's color maps
   ```clojure
   ;; Zprint has ANSI color support built in
   (require '[zprint.core :as zp])
   (zp/zprint-str code {:color? true})
   ```

**Recommendation**: Start with option 1 (no highlighting for code/results), focus on getting output categories colored correctly first. Add syntax highlighting in a later phase if desired.

### Zprint ANSI Color Integration (Future)

If we want syntax highlighting for code/results:

```clojure
(require '[zprint.core :as zp])

(defn syntax-highlight-clojure
  "Apply syntax highlighting to Clojure code using zprint"
  [code]
  (zp/zprint-str code
                 {:color-map (if (light-theme?)
                              ;; Light theme colors
                              {:brace :blue
                               :bracket :blue
                               :keyword :magenta
                               :string :green
                               :number :cyan
                               :nil :red
                               :true :red
                               :false :red}
                              ;; Dark theme colors
                              {:brace :bright-blue
                               :bracket :bright-blue
                               :keyword :bright-magenta
                               :string :bright-green
                               :number :bright-cyan
                               :nil :bright-red
                               :true :bright-red
                               :false :bright-red})
                  :color? true}))

(defn append-clojure-eval
  "Append evaluation code or results with syntax highlighting"
  [code {:keys [ns]}]
  (when ns
    (append-line-other-out (str "; " ns)))
  (append-line (syntax-highlight-clojure code)))
```

## Testing Colors in REPL

### Manual Color Testing

```clojure
(require '[joyride.output :as output] :reload)

;; Test basic color functions
(output/get-ansi-code :red)
;; => "\u001b[31m"

(output/get-ansi-code :gray)
;; => "\u001b[90m"

;; Test theme detection
(output/current-theme-kind)
;; => 2 (Dark) or 1 (Light)

(output/light-theme?)
;; => false (if using dark theme)

(output/dark-theme?)
;; => true (if using dark theme)

;; Test color scheme
(output/get-output-colors)
;; => {:eval-out :gray, :eval-err :bright-red, :other-out :grey, :other-err :bright-red, :reset :reset}

;; Test colorization
(output/colorize :red "Error message")
;; => "\u001b[31mError message\u001b[0m"

;; Test in terminal
(output/show-terminal false)
(output/append-line-eval-out "This should be gray")
(output/append-line-eval-err "This should be red/bright-red")
(output/append-line-other-out "This should be green/gray")
(output/append-line-other-err "This should be red/bright-red")
```

### Visual Verification

Create a test function that displays all categories:

```clojure
(defn test-all-colors!
  "Display all output categories with colors for visual verification"
  []
  (output/show-terminal false)
  (output/append-line-other-out "\n=== Joyride Output Color Test ===\n")

  (output/append-line-other-out (str "Theme: "
                                     (if (output/light-theme?) "Light" "Dark")
                                     "\n"))

  (output/append-other-out "evalOut: ")
  (output/append-line-eval-out "This is stdout from evaluation")

  (output/append-other-out "evalErr: ")
  (output/append-line-eval-err "This is stderr from evaluation")

  (output/append-other-out "otherOut: ")
  (output/append-line-other-out "This is other stdout")

  (output/append-other-out "otherErr: ")
  (output/append-line-other-err "This is other stderr")

  (output/append-line-other-out "\n=== End Color Test ===\n"))

;; Run the test
(test-all-colors!)
```

### Testing ANSI Preservation

```clojure
;; Test that existing ANSI codes are preserved
(output/show-terminal false)

;; This should appear in its original color (cyan)
(output/append-line-eval-out "\u001b[36mManually colored cyan\u001b[0m")

;; This should get automatic gray coloring
(output/append-line-eval-out "Automatically colored")
```

## Implementation Checklist

### Phase 1: Core ANSI Infrastructure
- [x] Define `ansi-codes` map with all color codes
- [x] Implement `get-ansi-code` with alias resolution
- [x] Implement theme detection functions
- [x] Implement `get-output-colors` for theme-aware scheme

### Phase 2: Colorization Functions
- [ ] Implement `ansi-escape-seq?` detection
- [ ] Implement `colorize` basic function
- [ ] Implement `maybe-colorize` with preservation
- [ ] Implement `colorize-by-category` theme-aware function

### Phase 3: Integration
- [ ] Update `append-eval-out` / `append-line-eval-out`
- [ ] Update `append-eval-err` / `append-line-eval-err`
- [ ] Update `append-other-out` / `append-line-other-out`
- [ ] Update `append-other-err` / `append-line-other-err`

### Phase 4: Testing
- [ ] REPL test basic color functions
- [ ] REPL test theme detection
- [ ] Visual verification in terminal (light theme)
- [ ] Visual verification in terminal (dark theme)
- [ ] Test ANSI code preservation
- [ ] Test theme switching behavior

### Phase 5: Future Enhancements (Optional)
- [ ] Zprint integration for syntax highlighting
- [ ] Custom color map configuration
- [ ] Additional color schemes (high contrast variants)

## Benefits of This Approach

1. **Calva Compatibility**: Matches Calva's proven color scheme, familiar to users
2. **Theme Awareness**: Automatically adapts to light/dark/high-contrast themes
3. **Preservation**: Respects existing ANSI codes in messages
4. **Simplicity**: Straightforward color-by-category mapping
5. **Extensibility**: Easy to add more categories or adjust colors
6. **Testability**: All functions are pure and REPL-testable

## Color Rationale

### Why Gray for evalOut?
- **Deemphasize**: stdout from evaluation is less important than the result
- **Readability**: Gray provides good contrast without being distracting
- **Calva Consistency**: Matches what Calva users expect

### Why Bright Red for evalErr (Dark Theme)?
- **Attention**: Errors need to stand out
- **Contrast**: Bright red is more visible on dark backgrounds
- **Calva Consistency**: Matches Calva's approach

### Why Green/Gray Distinction?
- **Light Theme**: Green for non-eval output (positive, informational)
- **Dark Theme**: Gray for non-eval output (stays consistent with eval stdout)
- **Calva Consistency**: Follows Calva's proven pattern

## References

### Calva Sources
- [output.ts#L40-L76](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L40-L76) - Color scheme definitions
- [output.ts#L318-L343](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L318-L343) - Colorization in `append` function
- [output.ts#L195-L223](https://github.com/BetterThanTomorrow/calva/blob/main/src/results-output/output.ts#L195-L223) - ANSI detection and theme handling

### External Resources
- [ANSI Escape Codes](https://en.wikipedia.org/wiki/ANSI_escape_code)
- [Chalk.js Documentation](https://github.com/chalk/chalk) - Reference for color names
- [VS Code ColorThemeKind API](https://code.visualstudio.com/api/references/vscode-api#ColorThemeKind)
- [Zprint ANSI Colors](https://github.com/kkinnear/zprint#ansi-terminal-colors)

---

**Document Status**: Ready for implementation
**Created**: 2025-10-13
**Next Step**: Implement Phase 1-3 in `src/joyride/output.cljs`
