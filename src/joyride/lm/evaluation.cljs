(ns joyride.lm.evaluation
  (:require
   ["vscode" :as vscode]
   [joyride.balance :as balance]
   [joyride.lm.eval.core :as core]
   [joyride.repl-utils :as repl-utils]
   [joyride.sci :as joyride-sci]
   [promesa.core :as p]
   [sci.core :as sci]
   [sci.ctx-store :as store]))

(defn execute-code+
  "Execute ClojureScript code in Joyride's SCI environment with VS Code APIs.
   Returns a map with :result, :error, :ns, :stdout, and :stderr keys.
   Uses Joyride's full SCI context with VS Code API access and extensions.
   When wait-for-promise? is false, returns result synchronously (no promise)."
  [{:keys [code ns wait-for-promise?]}]
  (let [inferred (balance/infer-parens code)
        balanced-code (if (:success inferred)
                        (:text inferred)
                        code)
        balancing-occurred? (not= code balanced-code)
        stdout-buffer (atom "")
        stderr-buffer (atom "")
        original-print-fn @sci/print-fn
        original-print-err-fn @sci/print-err-fn
        setup-capture! (fn []
                         (sci/alter-var-root sci/print-fn (constantly (fn [s] (swap! stdout-buffer str s))))
                         (sci/alter-var-root sci/print-err-fn (constantly (fn [s] (swap! stderr-buffer str s)))))
        restore-fns! (fn []
                       (sci/alter-var-root sci/print-fn (constantly original-print-fn))
                       (sci/alter-var-root sci/print-err-fn (constantly original-print-err-fn)))
        make-result (fn [result error wait-for-promise?]
                      (cond-> {:result (if (and (not wait-for-promise?)
                                                (not error)
                                                (instance? js/Promise result))
                                         {:type "promise"
                                          :message "Promise returned but not awaited (fire-and-forget mode)"
                                          :toString (str result)}
                                         result)
                               :error error
                               :ns (str @sci/ns)
                               :stdout @stdout-buffer
                               :stderr @stderr-buffer}
                        balancing-occurred?
                        (merge
                         {:balancing-note "The code provided for evaluation had unbalanced brackets and was automatically balanced before evaluation. Please update your code records with the corrected version from `balanced-code` in this response."
                          :balanced-code balanced-code})))]
    (when balancing-occurred?
      (js/console.log "[Evaluation] Code was unbalanced:" code "balanced-code:" balanced-code))
    (if wait-for-promise?
      ;; Async path with p/let (existing behavior)
      (try
        (setup-capture!)
        (p/-> (p/let [target-ns (symbol ns)
                      resolved-ns (try
                                    (repl-utils/the-sci-ns (store/get-ctx) target-ns)
                                    (catch js/Error _
                                      (try
                                        (sci/eval-form (store/get-ctx)
                                                       (list 'clojure.core/create-ns (list 'quote target-ns)))
                                        (catch js/Error _
                                          @joyride-sci/!last-ns))))
                      result (sci/binding [sci/ns resolved-ns]
                               (joyride-sci/eval-string balanced-code))]
                (make-result result nil wait-for-promise?))
              (p/catch (fn [e] ;; Todo, this doesn't catch evalutation errors
                         (make-result nil (.-message e) wait-for-promise?)))
              (p/finally
                (restore-fns!)))
        (catch js/Error e
          (make-result nil (.-message e) wait-for-promise?)))
      ;; Sync path without promises
      (try
        (setup-capture!)
        (let [target-ns (symbol ns)
              resolved-ns (try
                            (repl-utils/the-sci-ns (store/get-ctx) target-ns)
                            (catch js/Error _
                              (try
                                (sci/eval-form (store/get-ctx)
                                               (list 'clojure.core/create-ns (list 'quote target-ns)))
                                (catch js/Error _
                                  @joyride-sci/!last-ns))))
              result (sci/binding [sci/ns resolved-ns]
                       (joyride-sci/eval-string balanced-code))]
          (make-result result nil wait-for-promise?))
        (catch js/Error e
          (make-result nil (.-message e) wait-for-promise?))
        (finally
          (restore-fns!))))))

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
