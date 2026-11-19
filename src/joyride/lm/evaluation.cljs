(ns joyride.lm.evaluation
  (:require
   ["vscode" :as vscode]
   [joyride.lm.eval.core :as core]
   [joyride.lm.eval.validation :as validation]
   [joyride.output :as output]
   [joyride.printing :as printing]
   [joyride.repl-utils :as repl-utils]
   [joyride.sci :as joyride-sci]
   [joyride.integrations.calva :as calva-int]
   [joyride.who-tracking :as who-tracking]
   [promesa.core :as p]
   [sci.core :as sci]
   [sci.ctx-store :as store]))

(def ^:private not-awaited-message "Promise returned but not awaited (fire-and-forget mode)")

(defonce ^:private !captures (atom {:base nil :active []}))

(defn- current-print-fns []
  {:out-fn @sci/print-fn
   :err-fn @sci/print-err-fn})

(defn- set-print-fns! [{:keys [out-fn err-fn]}]
  (sci/alter-var-root sci/print-fn (constantly out-fn))
  (sci/alter-var-root sci/print-err-fn (constantly err-fn)))

(defn- start-capture!
  "Points SCI's print functions at fresh buffers and returns the capture, with `:capture/out` and `:capture/err` buffers."
  []
  (let [out (atom "")
        err (atom "")
        fns {:out-fn (fn [s] (swap! out str s))
             :err-fn (fn [s] (swap! err str s))}]
    (swap! !captures (fn [{:keys [active] :as captures}]
                       (cond-> (update captures :active conj fns)
                         (empty? active) (assoc :base (current-print-fns)))))
    (set-print-fns! fns)
    {:capture/out out
     :capture/err err
     :capture/fns fns}))

(defn- end-capture!
  "Ends `capture`. Each of SCI's print functions that still prints through it goes back to the latest
   capture still running, or to the print function from before the first capture."
  [{:capture/keys [fns]}]
  (let [{:keys [base active]} (swap! !captures update :active (fn [active]
                                                               (filterv #(not= fns %) active)))
        target (or (peek active) base)]
    (when (identical? @sci/print-fn (:out-fn fns))
      (sci/alter-var-root sci/print-fn (constantly (:out-fn target))))
    (when (identical? @sci/print-err-fn (:err-fn fns))
      (sci/alter-var-root sci/print-err-fn (constantly (:err-fn target))))))

(defn- bracket-error-result [ns {:keys [error balanced-code parinfer-error]}]
  (cond-> {:result nil
           :error error
           :ns ns
           :stdout ""
           :stderr ""}
    balanced-code (assoc :balanced-code balanced-code)
    parinfer-error (assoc :parinfer-error parinfer-error)))

(defn- resolve-ns
  "Returns the SCI namespace named `ns`, created when missing, or the last used namespace when it can't be created."
  [ns]
  (let [target-ns (symbol ns)]
    (try
      (repl-utils/the-sci-ns (store/get-ctx) target-ns)
      (catch js/Error _
        (try
          (sci/eval-form (store/get-ctx) (list 'clojure.core/create-ns (list 'quote target-ns)))
          (catch js/Error _
            @joyride-sci/!last-ns))))))

(defn- eval-in-ns [code ns]
  (sci/binding [sci/ns (resolve-ns ns)]
    (joyride-sci/eval-string code)))

(defn- result-str
  "Returns `result` printed within the `joyride.lm` result limits. An awaited promise's value
   prints as `#<Promise …>`, and a promise that isn't awaited as the not awaited message."
  [result {:keys [promise? wait-for-promise?]}]
  (let [{:keys [max-length max-depth]} (joyride-sci/get-eval-config)
        opts {:print-length max-length
              :print-level max-depth
              :fallback-length max-length
              :fallback-level max-depth}]
    (cond
      (not promise?) (printing/value->str result opts)
      wait-for-promise? (str "#<Promise " (printing/value->str result opts) ">")
      :else (printing/value->str {:type "promise"
                                  :message not-awaited-message
                                  :toString (str result)}
                                 opts))))

(defn- log-result!
  [{:keys [error result-str stdout stderr who ns]}]
  (let [log! (fn [category text]
               (calva-int/log-to-calva! {:category category :text text :who who :ns ns}))]
    (if error
      (log! "evaluationErrorOutput" error)
      (log! "evaluationResults" result-str))
    (when (seq stdout)
      (log! "evaluationOutput" stdout))
    (when (seq stderr)
      (log! "evaluationErrorOutput" stderr))))

(defn- make-result
  "Returns the tool result for an evaluation `outcome`, with the output `capture` collected, and logs it to Calva."
  [{:keys [result error who] :as outcome} {:capture/keys [out err]}]
  (let [printed (result-str result outcome)
        other-whos (who-tracking/get-other-whos-since-last! who)
        result-ns (str @sci/ns)
        stdout @out
        stderr @err]
    (log-result! {:error error :result-str printed :stdout stdout :stderr stderr :who who :ns result-ns})
    (cond-> {:result printed
             :error error
             :ns result-ns
             :stdout stdout
             :stderr stderr}
      (seq other-whos) (assoc "otherWhosSinceLast" other-whos))))

(defn- awaited-outcome+
  "Evaluates `code` and resolves to the evaluation's outcome, with a promise result awaited.
   Native promise calls keep a rejection with `nil` a rejection, which promesa turns into a resolution."
  [{:keys [code ns who]}]
  (let [!raw (volatile! nil)]
    (.then (js/Promise. (fn [resolve]
                          (resolve (vreset! !raw (eval-in-ns code ns)))))
           (fn [v]
             {:result v :promise? (instance? js/Promise @!raw) :wait-for-promise? true :who who})
           (fn [e]
             {:error (printing/error-message e) :who who}))))

(defn- execute-awaited+ [input capture]
  (-> (awaited-outcome+ input)
      (.then #(make-result % capture))
      (.finally #(end-capture! capture))))

(defn- execute-sync [{:keys [code ns who]} capture]
  (try
    (let [result (eval-in-ns code ns)]
      (make-result {:result result :promise? (instance? js/Promise result) :who who} capture))
    (catch :default e
      (make-result {:error (printing/error-message e) :who who} capture))
    (finally
      (end-capture! capture))))

(defn- log-evaluation! [{:keys [code who]}]
  (who-tracking/record-evaluation! who)
  (output/append-clojure-eval! code {:who who :ns (str @sci/ns)})
  (calva-int/log-to-calva! {:category "evaluatedCode"
                            :text code
                            :who who
                            :ns (str @sci/ns)}))

(defn execute-code+
  "Execute ClojureScript code in Joyride's SCI environment with VS Code APIs.
   Returns a map with :result, :error, :ns, :stdout, and :stderr keys.
   Uses Joyride's full SCI context with VS Code API access and extensions.
   When wait-for-promise? is false, returns result synchronously (no promise)."
  [{:keys [code ns wait-for-promise?] :as input}]
  (let [bracket-validation (validation/validate-brackets code)]
    (if-not (:valid? bracket-validation)
      (bracket-error-result ns bracket-validation)
      (do
        (log-evaluation! input)
        (let [capture (start-capture!)]
          (if wait-for-promise?
            (execute-awaited+ input capture)
            (execute-sync input capture)))))))

(defn prepare-invocation
  "Prepare confirmation message with rich code preview"
  [options _token]
  (let [input-data (core/extract-input-data ^js (.-input options))
        validation (core/validate-input input-data)]
    (if (:valid? validation)
      (let [confirmation-data (core/format-confirmation-message input-data)]
        #js {:invocationMessage "Running Joyride code in the VS Code environment"
             :confirmationMessages
             #js {:title (:title confirmation-data)
                  :message (vscode/MarkdownString.
                            (core/confirmation-message->markdown confirmation-data))}})
      (throw (js/Error. (:error validation))))))

(defn invoke-tool
  "Execute ClojureScript code with enhanced error handling"
  [options token]
  (let [input-data (core/extract-input-data ^js (.-input options))
        validation (core/validate-input input-data)]
    (if-not (:valid? validation)
      (let [error-data (core/format-error-message {:error (:error validation)
                                                   :code (:code input-data)
                                                   :stdout ""
                                                   :stderr ""})
            error-markdown (core/error-message->markdown error-data)]
        (vscode/LanguageModelToolResult. #js [(vscode/LanguageModelTextPart. error-markdown)]))
      (try
        ;; Check for cancellation
        (when (.-isCancellationRequested ^js token)
          (throw (js/Error. "Operation was cancelled")))

        ;; Execute the code - result may be sync or async depending on wait-for-promise?
        (let [result (execute-code+ input-data)]
          (if (:wait-for-promise? input-data)
            ;; Async case - result is a promise, use p/let
            (p/let [resolved-result result]
              (if (:error resolved-result)
                (let [error-data (core/format-error-message (merge {:code (:code input-data)}
                                                                   resolved-result))]
                  (vscode/LanguageModelToolResult.
                   #js [(vscode/LanguageModelTextPart.
                         (js/JSON.stringify (clj->js error-data)))]))
                (let [result-data (core/format-result-message resolved-result)]
                  (vscode/LanguageModelToolResult. #js [(vscode/LanguageModelTextPart.
                                                         (js/JSON.stringify (clj->js result-data)))]))))
            ;; Sync case - result is immediate, no promises
            (if (:error result)
              (let [error-data (core/format-error-message (merge {:code (:code input-data)}
                                                                 result))]
                (vscode/LanguageModelToolResult.
                 #js [(vscode/LanguageModelTextPart.
                       (js/JSON.stringify (clj->js error-data)))]))
              (let [result-data (core/format-result-message result)]
                (vscode/LanguageModelToolResult. #js [(vscode/LanguageModelTextPart.
                                                       (js/JSON.stringify (clj->js result-data)))])))))

        (catch js/Error e
          ;; Enhanced error information
          (let [error-data (core/format-error-message {:error (.-message e)
                                                       :code (:code input-data)
                                                       :stdout ""
                                                       :stderr ""})
                error-markdown (core/error-message->markdown error-data)]
            (vscode/LanguageModelToolResult. #js [(vscode/LanguageModelTextPart. error-markdown)])))))))

(defn create-joyride-tool
  "Create the Joyride Language Model Tool implementation"
  []
  #js {:prepareInvocation prepare-invocation
       :invoke invoke-tool})

(defn register-tool!
  "Register the Joyride tool with VS Code's Language Model API.
   Returns the disposable for proper lifecycle management."
  []
  (try
    (let [disposable (vscode/lm.registerTool "joyride_evaluate_code" (create-joyride-tool))]
      (js/console.log "Joyride Eval LM Tool registered successfully")
      disposable)
    (catch js/Error e
      (js/console.error "Failed to register Joyride Eval LM Tool:" (.-message e))
      nil)))
