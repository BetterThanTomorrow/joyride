(ns joyride.output
  "Joyride's output: decides, message by message, whether it goes to Calva's Output Destinations or to
   the Joyride Output terminal, and writes it there."
  (:require
   ["os" :as os]
   ["path" :as path]
   ["vscode" :as vscode]
   [clojure.string :as string]
   [joyride.db :as db]
   [joyride.integrations.calva :as calva]
   [joyride.output.route :as route]
   [zprint.core :as zp]))

(def terminal-name "Joyride Output")

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
   :color/black   "\u001b[30m"

   ;; Bright colors
   :color/bright-black   "\u001b[90m"
   :color/bright-red     "\u001b[91m"

   ;; Colored background
   :color/bg-white        "\u001b[47m"
   :color/bg-bright-white "\u001b[107m"
   :color/bg-magenta      "\u001b[45m"
   :color/bg-bright-blue  "\u001b[104m"
   :color/bg-cyan         "\u001b[46m"
   :color/bg-bright-cyan  "\u001b[106m"

   ;; Aliases matching Chalk
   :color/gray :color/bright-black})

(defn- get-ansi-code
  "Get ANSI code for a color key, resolving aliases"
  [color-key]
  (let [code-or-alias (get ansi-codes color-key)]
    (if (keyword? code-or-alias)
      (get ansi-codes code-or-alias)
      code-or-alias)))

(defn- joyride-version []
  (when-let [ext (vscode/extensions.getExtension "betterthantomorrow.joyride")]
    (some-> ext .-packageJSON .-version)))

(defn- terminal-banner []
  (let [cmd-style (str (get-ansi-code :color/bg-bright-white)
                       (get-ansi-code :color/black))
        reset (get-ansi-code :color/reset)
        version (or (joyride-version) "dev")]
    (str "Joyride Evaluation Output (v" version ")\r\n"
         "This (pseudo) terminal displays Joyride messages, evaluated code, results, and output.\r\n\r\n"
         "To reveal this terminal, use the command " cmd-style " Joyride: Open Joyride Output Terminal " reset ".\r\n"
         "If this terminal stops updating, close the tab and run that command again. Earlier output will be gone.\r\n\r\n"
         "https://github.com/BetterThanTomorrow/joyride\r\n"
         "Please consider sponsoring: https://github.com/sponsors/PEZ ♥️\r\n\r\n")))

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
    {:output/eval-who :color/bg-bright-cyan
     :output/eval-ns :color/bg-magenta
     :output/eval-out :color/bright-black
     :output/eval-err :color/bright-red
     :output/other-out :color/green
     :output/other-err :color/bright-red
     :output/info :color/bg-white}

    (light-theme?)
    {:output/eval-who :color/bg-cyan
     :output/eval-ns :color/bg-magenta
     :output/eval-out :color/gray
     :output/eval-err :color/red
     :output/other-out :color/green
     :output/other-err :color/red
     :output/info :color/bg-white}

    :else  ;; Dark theme
    {:output/eval-who :color/bg-bright-cyan
     :output/eval-ns :color/bg-magenta
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

(defn- normalize-line-endings
  "Convert Unix line endings to terminal friendly CRLF sequences."
  [message]
  (string/replace message #"\r?\n" "\r\n"))

(defn- forget-output-terminal!
  "Clear the Joyride Output singleton. With a terminal, only if it is the tracked one."
  ([]
   (swap! db/!app-db assoc :output/terminal nil :output/pty nil))
  ([terminal]
   (when (identical? terminal (:output/terminal @db/!app-db))
     (forget-output-terminal!))))

(defn- output-terminal-live?
  []
  (let [^js terminal (:output/terminal @db/!app-db)]
    (boolean (and terminal
                  (not (.-exitStatus terminal))
                  (some #(identical? % terminal)
                        (.-terminals vscode/window))))))

(defn register-lifecycle!
  "Subscribe once to onDidCloseTerminal. Push the disposable onto the extension context."
  [^js context]
  (let [sub (.onDidCloseTerminal vscode/window
                                 (fn [t]
                                   (forget-output-terminal! t)))]
    (swap! db/!app-db update :disposables conj sub)
    (.push (.-subscriptions context) sub)))

(defn- create-pty!
  "Create a Joyride pseudo terminal implementation."
  []
  (let [write-emitter (vscode/EventEmitter.)
        close-emitter (vscode/EventEmitter.)
        impl #js {:onDidWrite (.-event write-emitter)
                  :onDidClose (.-event close-emitter)
                  :open (fn [_]
                          (.fire write-emitter (terminal-banner)))
                  :handleInput (fn [data]
                                 (.fire write-emitter (string/replace data #"\r" "\r\n")))
                  :write (fn [message]
                           (let [normalized (normalize-line-endings (str message))]
                             (.fire write-emitter normalized)))}]
    (set! (.-close impl)
          (fn []
            (.fire close-emitter)
            (when (identical? impl (:output/pty @db/!app-db))
              (forget-output-terminal!))))
    impl))

(defn ensure-terminal!
  "Ensure the output terminal exists and return the backing pseudoterminal."
  []
  (when-not (output-terminal-live?)
    (forget-output-terminal!))
  (if-let [pty (:output/pty @db/!app-db)]
    pty
    (let [pty (create-pty!)
          terminal (vscode/window.createTerminal #js {:name terminal-name
                                                      :pty pty})]
      (swap! db/!app-db assoc
             :output/terminal terminal
             :output/pty pty
             :output/did-last-terminate-line true)
      pty)))

(defn- write-to-terminal!
  "Write a raw message to the Joyride terminal."
  [message]
  (let [pty (ensure-terminal!)]
    (.write pty message)))

(defn- update-line-state!
  "Track whether the last appended message terminated the line."
  [normalized-message]
  (set-did-last-terminate-line! (string/ends-with? normalized-message "\r\n")))

(defn- append!
  "Append message without forcing a trailing newline."
  [message]
  (when (some? message)
    (let [normalized (normalize-line-endings (str message))]
      (write-to-terminal! normalized)
      (update-line-state! normalized))))

(defn- append-line!
  "Append message and ensure a trailing newline."
  [message]
  (append! (str message "\n")))

(defn- info-line
  "Build the info line string with optional who badge and ns badge.
   The who badge is left out for the who values Joyride sets itself."
  [{:keys [who ns]}]
  (let [who-badge (when (and who (route/who-badge? who))
                    (str (colorize-by-category :output/eval-who (str " " who " ")) " "))
        ns-badge (colorize-by-category :output/eval-ns (str " " ns " "))]
    (str (colorize-by-category :output/info "; ") (or who-badge "") ns-badge)))

(defn- info-key
  "Returns what the info line shows for `who` and `ns`: the who only when it gets a badge."
  [{:keys [who ns]}]
  {:who (when (route/who-badge? who) who)
   :ns ns})

(defn- info-changed?
  "Check if the info line context has changed since last output."
  [opts]
  (not= (:output/last-info @db/!app-db) (info-key opts)))

(defn- maybe-append-info-line!
  "Append info line if who or ns has changed since last output."
  [opts]
  (when (info-changed? opts)
    (swap! db/!app-db assoc :output/last-info (info-key opts))
    (append-line! (info-line opts))))

;; ============================================================================
;; Output messages
;; ============================================================================

(def ^:private stream-colors
  {:output.category/evaluation-output :output/eval-out
   :output.category/evaluation-error-output :output/eval-err
   :output.category/other-output :output/other-out
   :output.category/other-error-output :output/other-err})

(defn- write-stream-to-joyride-output!
  "Writes the stream `message` to the Joyride Output terminal in its category's color. An `other-output`
   message with an ns, which nREPL `load-file` writes, gets the info line before it."
  [{:output/keys [category text who ns line?]}]
  (when (and ns (= :output.category/other-output category))
    (maybe-append-info-line! {:who who :ns ns}))
  (let [colored (colorize-by-category (stream-colors category) text)]
    (if line?
      (append-line! colored)
      (append! colored))))

(defn- write-to-joyride-output!
  "Writes `message` to the Joyride Output terminal."
  [{:output/keys [category text who ns promise?] :as message}]
  (case category
    :output.category/evaluated-code (do (maybe-append-info-line! {:who who :ns ns})
                                        (append-line! (syntax-highlight-clojure text)))
    :output.category/evaluation-results (append-line! (if promise?
                                                        (str "#<Promise " (syntax-highlight-clojure text) ">")
                                                        (syntax-highlight-clojure text)))
    (write-stream-to-joyride-output! message)))

(defn- use-calva-setting? []
  (.get (vscode/workspace.getConfiguration "joyride") "useCalvaOutputDestinations" true))

(defn- route-inputs
  "Returns what decides where `message` goes now."
  [{:output/keys [who calva-client?]}]
  (let [calva-state (calva/state)]
    {:setting/use-calva? (use-calva-setting?)
     :calva/state calva-state
     :calva/display? (route/display-api? (calva/version))
     :joyride/starting? (boolean (:joyride/starting? @db/!app-db))
     :terminal/live? (and (= :inactive calva-state)
                          (output-terminal-live?))
     :output/who who
     :output/calva-client? calva-client?}))

(defn- calva-message
  "Returns `message` as Calva's `log()` takes it, or `nil` when it has no text, which `log()` refuses."
  [message]
  (let [calva-message (route/calva-message message)]
    (when (seq (:text calva-message))
      calva-message)))

(defn- log-message-for-subscribers!
  "Hands `message` to Calva's output subscribers, such as Backseat Driver, without showing it, when Calva is running."
  [message]
  (some-> (calva-message message)
          (calva/log! {})))

(defn- write-to-calva!
  "Writes `message` to Calva's Output Destinations, with the `evaluation` promise of its code, and to
   Joyride Output too when Calva shows it nowhere people read. Returns the evaluation id Calva gave it."
  [message evaluation]
  (when-let [calva-message (calva-message message)]
    (let [result (calva/log! calva-message
                             (cond-> {:display true}
                               evaluation (assoc :evaluation evaluation)))]
      (if (route/joyride-output-after-calva? result)
        (do (write-to-joyride-output! message)
            (swap! db/!app-db dissoc :output/last-place))
        (when (seq (:calva/printed-to result))
          (swap! db/!app-db assoc :output/last-place (select-keys result [:calva/printed-to]))))
      (:calva/evaluation-id result))))

;; ============================================================================
;; Held output
;; ============================================================================

(defn- take-held!
  "Empties the held messages and returns them."
  []
  (let [[{:output/keys [held]}] (swap-vals! db/!app-db assoc :output/held [])]
    held))

(defn- flush-held-to-calva!
  "Writes the held messages to Calva, in order: to its Output Destinations, or only to its output
   subscribers for those already shown in Joyride Output."
  []
  (doseq [{:held/keys [message shown?]} (take-held!)]
    (if shown?
      (log-message-for-subscribers! message)
      (write-to-calva! message nil))))

(defn- flush-held-to-joyride-output!
  "Writes the held messages not yet shown to Joyride Output, in order, and keeps them all held, as
   shown, for Calva's output subscribers."
  []
  (let [held (take-held!)]
    (swap! db/!app-db update :output/held (fnil into []) (map #(assoc % :held/shown? true) held))
    (doseq [{:held/keys [message shown?]} held]
      (when-not shown?
        (write-to-joyride-output! message)))))

(defn- stop-held-check! []
  (when-let [interval (:output/held-check @db/!app-db)]
    (js/clearInterval interval)
    (swap! db/!app-db dissoc :output/held-check)))

(defn- check-held!
  "Writes the held messages to Calva once it has started, and stops checking when nothing is held."
  []
  (when (= :active (calva/state))
    (flush-held-to-calva!))
  (when (empty? (:output/held @db/!app-db))
    (stop-held-check!)))

(defn- ensure-held-check!
  "Checks every second whether Calva has started, while messages are held. The check is disposed with
   Joyride's disposables, at hot reload and deactivation."
  []
  (when-not (:output/held-check @db/!app-db)
    (swap! db/!app-db assoc :output/held-check (js/setInterval check-held! 1000))
    (swap! db/!app-db update :disposables conj #js {:dispose stop-held-check!})))

(defn- hold!
  "Holds `message` until Calva starts, as already `shown?` in Joyride Output or not."
  [message shown?]
  (swap! db/!app-db update :output/held route/hold {:held/message message
                                                    :held/shown? shown?})
  (ensure-held-check!)
  nil)

;; ============================================================================
;; Routing
;; ============================================================================

(defn- write!
  "Writes `message` where Joyride's output goes, after the messages held before it. Returns the
   evaluation id Calva gave an evaluation's code, when it gave one."
  [{:output/keys [calva-client?] :as message} evaluation]
  (let [inputs (route-inputs message)
        where (route/route inputs)]
    (case where
      :hold (hold! message false)
      :calva (do (flush-held-to-calva!)
                 (write-to-calva! message evaluation))
      :joyride-output (do (when (= :active (:calva/state inputs))
                            (flush-held-to-calva!))
                          (flush-held-to-joyride-output!)
                          (write-to-joyride-output! message)
                          (swap! db/!app-db dissoc :output/last-place)
                          (when-not calva-client?
                            (log-message-for-subscribers! message))
                          (when (route/hold-copy-for-subscribers? inputs where)
                            (hold! message true))
                          nil)
      :nowhere (when (= :active (:calva/state inputs))
                 (flush-held-to-calva!)))))

(defn show-terminal!
  "Reveal the Joyride output terminal, created when missing, with the held messages written to it."
  ([] (show-terminal! true))
  ([preserve-focus?]
   (flush-held-to-joyride-output!)
   (ensure-terminal!)
   (when-let [terminal (:output/terminal @db/!app-db)]
     (.show terminal preserve-focus?)
     terminal)))

(defn- destination-file-path
  "Returns the absolute path of the Calva file path destination `destination`, resolved as Calva
   resolves it: `~/` against the home directory, and a relative path against the first workspace folder."
  [destination]
  (cond
    (.startsWith destination "~/") (path/join (os/homedir) (subs destination 2))
    (path/isAbsolute destination) destination
    :else (let [^js folder (some-> vscode/workspace.workspaceFolders first)]
            (path/resolve (or (some-> folder .-uri .-fsPath) "")
                          destination))))

(defn reveal!
  "Reveals where Joyride's last message went: the Calva Output Destination it was printed to, or
   Joyride Output, with the held messages written to it."
  []
  (let [{:reveal/keys [command file]} (route/reveal-target (:output/last-place @db/!app-db))]
    (cond
      command (vscode/commands.executeCommand command)
      file (-> (vscode/window.showTextDocument (vscode/Uri.file (destination-file-path file))
                                               #js {:preserveFocus true})
               (.then identity
                      (fn [e]
                        (js/console.warn "Joyride: Could not open the output file:" (.-message e)))))
      :else (show-terminal!))))

(defn output-terminal
  "Returns the Joyride Output terminal, created when missing, with the held messages written to it."
  []
  (flush-held-to-joyride-output!)
  (ensure-terminal!)
  (:output/terminal @db/!app-db))

(def ^:private default-context {:output/who "joyride"})

(defn- write-text!
  "Writes `text` in `category`, as a line when `line?`, with the output `context` of the evaluation or the writer."
  [category line? text context]
  (write! (merge default-context
                 context
                 {:output/category category
                  :output/text (str text)
                  :output/line? line?})
          nil))

(defn log-for-subscribers!
  "Hands `text` in `category`, with the output `context`, to Calva's output subscribers, such as
   Backseat Driver, without showing it anywhere."
  [category text context]
  (log-message-for-subscribers! (merge default-context
                                       context
                                       {:output/category category
                                        :output/text text})))

(defn append-eval-out!
  "Append stdout generated during evaluation, with the evaluation's output `context`."
  ([message] (append-eval-out! message nil))
  ([message context] (write-text! :output.category/evaluation-output false message context)))

(defn append-eval-err!
  "Append stderr generated during evaluation, with the evaluation's output `context`."
  ([message] (append-eval-err! message nil))
  ([message context] (write-text! :output.category/evaluation-error-output false message context)))

(defn append-line-eval-err!
  "Append stderr and ensure a newline, with the evaluation's output `context`."
  ([message] (append-line-eval-err! message nil))
  ([message context] (write-text! :output.category/evaluation-error-output true message context)))

(defn append-other-out!
  "Append non-evaluation stdout messages, with the writer's output `context`."
  ([message] (append-other-out! message nil))
  ([message context] (write-text! :output.category/other-output false message context)))

(defn append-line-other-out!
  "Append non-evaluation stdout and ensure newline, with the writer's output `context`."
  ([message] (append-line-other-out! message nil))
  ([message context] (write-text! :output.category/other-output true message context)))

(defn append-other-err!
  "Append non-evaluation stderr messages, with the writer's output `context`."
  ([message] (append-other-err! message nil))
  ([message context] (write-text! :output.category/other-error-output false message context)))

(defn append-line-other-err!
  "Append non-evaluation stderr and ensure newline, with the writer's output `context`."
  ([message] (append-line-other-err! message nil))
  ([message context] (write-text! :output.category/other-error-output true message context)))

(defn append-eval-result!
  "Append the value returned from an evaluation, with the evaluation's output `context`."
  ([data] (append-eval-result! data nil))
  ([data context]
   (write! (merge default-context
                  context
                  {:output/category :output.category/evaluation-results
                   :output/text data})
           nil)))

(defn append-promise-result!
  "Appends the printed value a promise from an evaluation settled to, as `#<Promise <value>>`, with the
   evaluation's output `context`."
  ([data] (append-promise-result! data nil))
  ([data context]
   (write! (merge default-context
                  context
                  {:output/category :output.category/evaluation-results
                   :output/text data
                   :output/promise? true})
           nil)))

(defn start-evaluation!
  "Writes `code` as the code of an evaluation. Returns the evaluation's output context, `context` with
   the evaluation id Calva gave it, as `:evaluation/context`, and the function that ends the evaluation,
   as `:evaluation/end!`. The caller calls it once the evaluation has settled, on every path."
  [code context]
  (let [!end (volatile! nil)
        evaluation (js/Promise. (fn [resolve] (vreset! !end resolve)))
        evaluation-id (write! (merge default-context
                                     context
                                     {:output/category :output.category/evaluated-code
                                      :output/text code})
                              evaluation)]
    {:evaluation/context (cond-> context
                           evaluation-id (assoc :output/evaluation-id evaluation-id))
     :evaluation/end! (fn [] (@!end nil))}))
