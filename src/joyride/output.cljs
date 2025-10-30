(ns joyride.output
  "Terminal output utilities for Joyride evaluations."
  (:require
   ["vscode" :as vscode]
   [clojure.string :as string]
   [joyride.db :as db]
   [zprint.core :as zp]))

(def terminal-name "Joyride Output")

(def terminal-banner
  (str "Joyride Evaluation Output"
       "\r\n"
       "This (pseudo) terminal displays Joyride messages, evaluated code, results, and output."
       "\r\n\r\n"))

(defn- set-did-last-terminate-line! [value]
  (swap! db/!app-db assoc :output/did-last-terminate-line value))

;; ============================================================================
;; ANSI Color Support
;; ============================================================================

(def ansi-codes
  "ANSI escape sequence codes matching Calva's terminal output colors"
  {:color/reset "\u001b[0m"

   ;; Basic colors
   :color/red     "\u001b[31m"
   :color/green   "\u001b[32m"

   ;; Bright colors
   :color/bright-black   "\u001b[90m"
   :color/bright-red     "\u001b[91m"

   ;; Colored background
   :color/bg-white        "\u001b[47m"
   :color/bg-magenta      "\u001b[45m"
   :color/bg-bright-blue  "\u001b[104m"

   ;; Aliases matching Chalk
   :color/gray :color/bright-black})

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
    {:output/eval-ns :color/bg-magenta
     :output/eval-out :color/bright-black
     :output/eval-err :color/bright-red
     :output/other-out :color/green
     :output/other-err :color/bright-red
     :output/info :color/bg-white}

    (light-theme?)
    {:output/eval-ns :color/bg-magenta
     :output/eval-out :color/gray
     :output/eval-err :color/red
     :output/other-out :color/green
     :output/other-err :color/red
     :output/info :color/bg-white}

    :else  ;; Dark theme
    {:output/eval-ns :color/bg-magenta
     :output/eval-out :color/gray
     :output/eval-err :color/bright-red
     :output/other-out :color/white
     :output/other-err :color/bright-red
     :output/info :color/bg-white}))

(defn- ansi-escape-seq?
  "Check if message contains ANSI escape sequences"
  [message]
  (boolean (when message
             (re-find #"\u001b\[" message))))

(defn- colorize
  "Add ANSI color codes to message"
  [color-key message]
  (let [color-code (get-ansi-code color-key)
        reset-code (get-ansi-code :color/reset)]
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
;; Clojure Syntax Highlighting (Calva-compatible)
;; ============================================================================

(defn- syntax-highlight-clojure
  "Apply syntax highlighting to Clojure code using zprint with Calva default colors"
  [code]
  (try
    (zp/zprint-file-str code
                        "Joyride"
                        {:color? true
                         :map {:comma? false}
                         :color-map {:brace :white,
                                     :bracket :white,
                                     :char :none,
                                     :comma :none,
                                     :comment :italic,
                                     :deref :blue,
                                     :false :blue,
                                     :fn :yellow,
                                     :hash-brace :white,
                                     :hash-paren :white,
                                     :keyword :magenta,
                                     :left :none,
                                     :nil :blue,
                                     :none :blue,
                                     :number :blue,
                                     :paren :white,
                                     :quote :white,
                                     :regex :green,
                                     :right :none,
                                     :string :green,
                                     :symbol :black,
                                     :syntax-quote-paren :none
                                     :true :blue,
                                     :uneval :none,
                                     :user-fn :yellow}
                         :parse-string? true})
    (catch js/Error _e
      ;; If zprint fails, return code as-is
      code)))

;; ============================================================================
;; Terminal Management
;; ============================================================================

(defn normalize-line-endings
  "Convert Unix line endings to terminal friendly CRLF sequences."
  [message]
  (string/replace message #"\r?\n" "\r\n"))

(defn- create-pty!
  "Create a Joyride pseudo terminal implementation."
  []
  (let [write-emitter (vscode/EventEmitter.)
        close-emitter (vscode/EventEmitter.)]
    #js {:onDidWrite (.-event write-emitter)
         :onDidClose (.-event close-emitter)
         :close (fn []
                  (.fire close-emitter)
                  (fn []
                    (.fire close-emitter)
                    (when-let [terminal (:output/terminal @db/!app-db)]
                      (.dispose terminal))))
         :open (fn [_]
                 (.fire write-emitter terminal-banner))
         :handleInput (fn [data]
                        (.fire write-emitter (string/replace data #"\r" "\r\n")))
         :write (fn [message]
                  (let [normalized (normalize-line-endings (str message))]
                    (.fire write-emitter normalized)))}))

(defn ensure-terminal!
  "Ensure the output terminal exists and return the backing pseudoterminal."
  []
  (if-let [pty (:output/pty @db/!app-db)]
    pty
    (let [pty (create-pty!)
          terminal (vscode/window.createTerminal #js {:name terminal-name
                                                      :pty pty})
          dispose-fn (.-dispose terminal)]
      (swap! db/!app-db assoc
             :output/terminal terminal
             :output/pty pty
             :output/did-last-terminate-line true)
      (set! (.-dispose terminal) (fn []
                                   (swap! db/!app-db assoc
                                          :output/terminal nil
                                          :output/pty nil)
                                   (dispose-fn)))
      pty)))

(defn show-terminal!
  "Reveal the Joyride output terminal if it exists or can be created."
  ([] (show-terminal! true))
  ([preserve-focus?]
   (ensure-terminal!)
   (when-let [terminal (:output/terminal @db/!app-db)]
     (.show terminal preserve-focus?)
     terminal)))

(defn write-to-terminal!
  "Write a raw message to the Joyride terminal."
  [message]
  (let [pty (ensure-terminal!)]
    (.write pty message)))

(defn update-line-state!
  "Track whether the last appended message terminated the line."
  [normalized-message]
  (set-did-last-terminate-line! (string/ends-with? normalized-message "\r\n")))

(defn append!
  "Append message without forcing a trailing newline."
  [message]
  (when (some? message)
    (let [normalized (normalize-line-endings (str message))]
      (write-to-terminal! normalized)
      (update-line-state! normalized))))

(defn append-line!
  "Append message and ensure a trailing newline."
  [message]
  (append! (str message "\n")))

(defn append-eval-out!
  "Append stdout generated during evaluation."
  [message]
  (append! (colorize-by-category :output/eval-out message)))

(defn append-eval-err!
  "Append stderr generated during evaluation."
  [message]
  (append! (colorize-by-category :output/eval-err message)))

(defn append-line-eval-err!
  "Append stderr and ensure a newline."
  [message]
  (append-line! (colorize-by-category :output/eval-err message)))

(defn append-other-out!
  "Append non-evaluation stdout messages."
  [message]
  (append! (colorize-by-category :output/other-out message)))

(defn append-line-other-out!
  "Append non-evaluation stdout and ensure newline."
  [message]
  (append-line! (colorize-by-category :output/other-out message)))

(defn append-other-err!
  "Append non-evaluation stderr messages."
  [message]
  (append! (colorize-by-category :output/other-err message)))

(defn append-line-other-err!
  "Append non-evaluation stderr and ensure newline."
  [message]
  (append-line! (colorize-by-category :output/other-err message)))

(defn- maybe-append-and-set-ns! [ns]
  (when (not= ns (:output/last-printed-ns @db/!app-db))
    (swap! db/!app-db assoc :output/last-printed-ns ns)
    (append-line-other-out! (str (colorize-by-category :output/info "; ")
                                 (colorize-by-category :output/eval-ns ns)))))

(defn append-eval-result!
  "Append the value returned from an evaluation."
  [data {:keys [ns]}]
  (maybe-append-and-set-ns! ns)
  (append-line! (syntax-highlight-clojure data)))

(defn append-clojure-eval!
  "Echo evaluated code to the terminal with namespace context."
  [code]
  (let [highlighted (syntax-highlight-clojure code)]
    (append-line! highlighted)))