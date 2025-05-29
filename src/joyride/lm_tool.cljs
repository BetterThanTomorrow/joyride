(ns joyride.lm-tool
  (:require ["vscode" :as vscode]
            [joyride.lm-tool.core :as core]))

(defn execute-code+
  "Execute ClojureScript code in the SCI environment with optional namespace"
  ([code] (execute-code+ code "user"))
  ([code namespace]
   (let [result (core/execute-code code namespace)]
     (if (:error result)
       (throw (js/Error. (str "ClojureScript execution failed: " (:error result))))
       (:result result)))))

(defn prepare-invocation
  "Prepare confirmation message with rich code preview"
  [options _token]
  (let [input-data (core/extract-input-data ^js (.-input options))
        validation (core/validate-input input-data)]
    (if (:valid? validation)
      #js {:invocationMessage "Executing ClojureScript in Joyride environment"
           :confirmationMessages
           #js {:title "Execute ClojureScript Code"
                :message (vscode/MarkdownString.
                          (core/format-confirmation-message (:code input-data) (:namespace input-data)))}}
      (throw (js/Error. (:error validation))))))

(defn invoke-tool
  "Execute ClojureScript code with enhanced error handling"
  [options ^js stream token]
  (let [input-data (core/extract-input-data ^js (.-input options))
        validation (core/validate-input input-data)]
    (if-not (:valid? validation)
      (do
        (.markdown stream (core/format-error-message (:error validation) (:code input-data)))
        vscode/LanguageModelToolResult.Empty)
      (try
        ;; Check for cancellation
        (when (.-isCancellationRequested ^js token)
          (throw (js/Error. "Operation was cancelled")))

        ;; Execute the code using pure logic
        (let [result (core/execute-code (:code input-data) (:namespace input-data))]
          (if (:error result)
            (.markdown stream (core/format-error-message (:error result) (:code input-data)))
            (.markdown stream (core/format-result-message (:result result))))
          vscode/LanguageModelToolResult.Empty)
        
        (catch js/Error e
          ;; Enhanced error information
          (.markdown stream (core/format-error-message (.-message e) (:code input-data)))
          vscode/LanguageModelToolResult.Empty)))))

(defn create-joyride-tool
  "Create the Joyride Language Model Tool implementation"
  []
  #js {:prepareInvocation prepare-invocation
       :invoke invoke-tool})

(defn register-tool!
  "Register the Joyride tool with VS Code's Language Model API"
  []
  (when (.-lm vscode)
    (try
      (vscode/lm.registerTool "joyride_evaluate_code" (create-joyride-tool))
      (js/console.log "Joyride LM Tool registered successfully")
      (catch js/Error e
        (js/console.error "Failed to register Joyride LM Tool:" (.-message e))))))
