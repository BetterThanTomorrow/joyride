(ns joyride.output
  "Terminal output utilities for Joyride evaluations."
  (:require
   ["vscode" :as vscode]
   [clojure.string :as string]
   [joyride.db :as db]))

(def terminal-name "Joyride Output")

(def terminal-banner
  (str "Joyride Evaluation Output\r\n"
       "This terminal displays evaluation results, output, and code.\r\n\r\n"))

(defn- get-output-pty []
  (:output/pty @db/!app-db))

(defn- set-output-pty! [pty]
  (swap! db/!app-db assoc :output/pty pty))

(defn- get-output-terminal []
  (:output/terminal @db/!app-db))

(defn- set-output-terminal! [terminal]
  (swap! db/!app-db assoc :output/terminal terminal))

(defn- get-did-last-terminate-line []
  (get @db/!app-db :output/did-last-terminate-line true))

(defn- set-did-last-terminate-line! [value]
  (swap! db/!app-db assoc :output/did-last-terminate-line value))

;; ============================================================================
;; ANSI Color Support
;; ============================================================================

(def ansi-codes
  "ANSI escape sequence codes matching Calva's terminal output colors"
  {:reset "\u001b[0m"

   ;; Basic colors
   :red     "\u001b[31m"
   :green   "\u001b[32m"

   ;; Bright colors
   :bright-black   "\u001b[90m"
   :bright-red     "\u001b[91m"

   ;; Aliases matching Chalk
   :gray :bright-black
   :grey :bright-black})

(defn- get-ansi-code
  "Get ANSI code for a color key, resolving aliases"
  [color-key]
  (let [code-or-alias (get ansi-codes color-key)]
    (if (keyword? code-or-alias)
      (get ansi-codes code-or-alias)
      code-or-alias)))

(defn- current-theme-kind
  "Get current VS Code theme kind"
  []
  (.-kind vscode/window.activeColorTheme))

(defn- light-theme?
  "Check if current theme is light"
  []
  (= (current-theme-kind) vscode/ColorThemeKind.Light))

(defn- dark-theme?
  "Check if current theme is dark"
  []
  (= (current-theme-kind) vscode/ColorThemeKind.Dark))

(defn- high-contrast-theme?
  "Check if current theme is high contrast"
  []
  (or (= (current-theme-kind) vscode/ColorThemeKind.HighContrast)
      (= (current-theme-kind) vscode/ColorThemeKind.HighContrastLight)))

(defn- get-output-colors
  "Get color scheme based on current VS Code theme.
   Matches Calva's terminal output colors."
  []
  (cond
    (high-contrast-theme?)
    {:eval-out :bright-black
     :eval-err :bright-red
     :other-out :green
     :other-err :bright-red}

    (light-theme?)
    {:eval-out :gray
     :eval-err :red
     :other-out :green
     :other-err :red}

    :else  ;; Dark theme
    {:eval-out :gray
     :eval-err :bright-red
     :other-out :grey
     :other-err :bright-red}))

(defn- ansi-escape-seq?
  "Check if message contains ANSI escape sequences"
  [message]
  (boolean (re-find #"\u001b\[" message)))

(defn- colorize
  "Add ANSI color codes to message"
  [color-key message]
  (let [color-code (get-ansi-code color-key)
        reset-code (get-ansi-code :reset)]
    (str color-code message reset-code)))

(defn- maybe-colorize
  "Colorize message only if it doesn't already contain ANSI codes.
   This preserves user-provided or library-generated colors."
  [color-key message]
  (if (ansi-escape-seq? message)
    message
    (colorize color-key message)))

(defn- colorize-by-category
  "Apply category-appropriate color to message based on current theme"
  [category message]
  (let [colors (get-output-colors)
        color-key (get colors category)]
    (maybe-colorize color-key message)))

;; ============================================================================
;; Terminal Management
;; ============================================================================

(defn normalize-line-endings
  "Convert Unix line endings to terminal friendly CRLF sequences."
  [message]
  (string/replace message #"\r?\n" "\r\n"))

(defn create-output-terminal
  "Create a Joyride pseudo terminal implementation."
  []
  (let [write-emitter (vscode/EventEmitter.)
        close-emitter (vscode/EventEmitter.)]
    #js {:onDidWrite (.-event write-emitter)
         :onDidClose (.-event close-emitter)
         :open (fn [_initial-dimensions]
                 (.fire write-emitter terminal-banner))
         :close (fn []
                  (.fire close-emitter)
                  (set-output-terminal! nil)
                  (set-output-pty! nil))
         :handleInput (fn [data]
                        (when (= data "\r")
                          (.fire write-emitter "\r\n")))
         :write (fn [message]
                  (let [normalized (normalize-line-endings (str message))]
                    (.fire write-emitter normalized)))}))

(defn ensure-terminal!
  "Ensure the output terminal exists and return the backing pseudoterminal."
  []
  (when-not (get-output-pty)
    (let [pty (create-output-terminal)
          terminal (vscode/window.createTerminal #js {:name terminal-name
                                                      :pty pty})]
      (set-output-pty! pty)
      (set-output-terminal! terminal)
      (set-did-last-terminate-line! true)))
  (get-output-pty))

(defn show-terminal
  "Reveal the Joyride output terminal if it exists or can be created."
  [preserve-focus?]
  (ensure-terminal!)
  (when-let [terminal (get-output-terminal)]
    (.show terminal preserve-focus?)))

(defn write-to-terminal
  "Write a raw message to the Joyride terminal."
  [message]
  (let [pty (ensure-terminal!)]
    (.write pty message)))

(defn update-line-state!
  "Track whether the last appended message terminated the line."
  [normalized-message]
  (set-did-last-terminate-line! (string/ends-with? normalized-message "\r\n")))

(defn append
  "Append message without forcing a trailing newline."
  [message]
  (when (some? message)
    (let [normalized (normalize-line-endings (str message))]
      (write-to-terminal normalized)
      (update-line-state! normalized))))

(defn append-line
  "Append message and ensure a trailing newline."
  [message]
  (append (str message "\n")))

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

(defn append-eval-result
  "Append the value returned from an evaluation."
  [message]
  (append-line message))

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

(defn append-clojure-eval
  "Echo evaluated code to the terminal with namespace context."
  ([code]
   (append-clojure-eval code {}))
  ([code {:keys [ns]}]
   (when (not= ns (:output/last-printed-ns @db/!app-db))
     (swap! db/!app-db assoc :output/last-printed-ns ns)
     (append-line-other-out (str "; " ns)))
   (append-line code)))