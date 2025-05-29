(ns joyride.lm-tool
  (:require
   ["vscode" :as vscode]
   [joyride.lm-tool.core :as core]
   [joyride.repl-utils :as repl-utils]
   [joyride.sci :as joyride-sci]
   [promesa.core :as p]
   [sci.core :as sci]
   [sci.ctx-store :as store]))

(defn execute-code+
  "Execute ClojureScript code in Joyride's SCI environment with VS Code APIs.
   Returns a map with :result, :error, :namespace, :stdout, and :stderr keys.
   Uses Joyride's full SCI context with VS Code API access and extensions."
  ([code] (execute-code+ code "user"))
  ([code ns]
   (let [stdout-buffer (atom "")
         stderr-buffer (atom "")
         original-print-fn @sci/print-fn
         original-print-err-fn @sci/print-err-fn]
     (try
       ;; Set up output capture
       (sci/alter-var-root sci/print-fn (constantly (fn [s] (swap! stdout-buffer str s))))
       (sci/alter-var-root sci/print-err-fn (constantly (fn [s] (swap! stderr-buffer str s))))

       ;; Execute the code using Joyride's SCI context with VS Code APIs
       ;; Handle namespace switching like the nREPL implementation does
       (p/let [target-ns (symbol ns)
             ;; Try to resolve namespace or default to user
               resolved-ns (try
                             (repl-utils/the-sci-ns (store/get-ctx) target-ns)
                             (catch js/Error _
                             ;; If namespace doesn't exist, create it or use user
                               (try
                                 (sci/eval-form (store/get-ctx)
                                                (list 'clojure.core/create-ns (list 'quote target-ns)))
                                 (catch js/Error _
                                   @joyride-sci/!last-ns))))
               result (sci/binding [sci/ns resolved-ns]
                        (joyride-sci/eval-string code))]
         {:result result
          :error nil
          :namespace (str @sci/ns)
          :stdout @stdout-buffer
          :stderr @stderr-buffer})

       (catch js/Error e
         {:result nil
          :error (.-message e)
          :namespace ns
          :stdout @stdout-buffer
          :stderr @stderr-buffer})

       (finally
         ;; Restore original print functions
         (sci/alter-var-root sci/print-fn (constantly original-print-fn))
         (sci/alter-var-root sci/print-err-fn (constantly original-print-err-fn)))))))

(defn prepare-invocation
  "Prepare confirmation message with rich code preview"
  [options _token]
  (let [input-data (core/extract-input-data ^js (.-input options))
        validation (core/validate-input input-data)]
    (if (:valid? validation)
      (let [confirmation-data (core/format-confirmation-message (:code input-data) (:namespace input-data))]
        #js {:invocationMessage "Executing ClojureScript in Joyride environment"
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
      (let [error-data (core/format-error-message (:error validation) (:code input-data) "" "")
            error-markdown (core/error-message->markdown error-data)]
        (vscode/LanguageModelToolResult. #js [(vscode/LanguageModelTextPart. error-markdown)]))
      (try
        ;; Check for cancellation
        (when (.-isCancellationRequested ^js token)
          (throw (js/Error. "Operation was cancelled")))

        ;; Execute the code using Joyride's SCI context with VS Code APIs
        (p/let [result (execute-code+ (:code input-data) (:namespace input-data))]
          (if (:error result)
            (let [error-data (core/format-error-message (:error result) (:code input-data)
                                                        (:stdout result) (:stderr result))]
              (vscode/LanguageModelToolResult.
               #js [(vscode/LanguageModelTextPart.
                     (js/JSON.stringify (clj->js error-data)))]))
            (let [result-data (core/format-result-message (:result result)
                                                          (:stdout result) (:stderr result))]
              (vscode/LanguageModelToolResult. #js [(vscode/LanguageModelTextPart.
                                                     (js/JSON.stringify (clj->js result-data)))]))))

        (catch js/Error e
          ;; Enhanced error information
          (let [error-data (core/format-error-message (.-message e) (:code input-data) "" "")
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
      (js/console.log "Joyride LM Tool registered successfully")
      disposable)
    (catch js/Error e
      (js/console.error "Failed to register Joyride LM Tool:" (.-message e))
      nil)))
