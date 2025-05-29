(ns joyride.lm-tool
  (:require ["vscode" :as vscode]
            [joyride.sci :as sci]))

(defn execute-code+
  "Execute ClojureScript code in the SCI environment with optional namespace"
  ([code] (execute-code+ code "user"))
  ([code _namespace]
   (try
     (sci/eval-string code)
     (catch js/Error e
       (throw (js/Error. (str "ClojureScript execution failed: " (.-message e))))))))

(defn prepare-invocation
  "Prepare confirmation message with rich code preview"
  [options _token]
  (let [code (.-code ^js (.-input options))
        namespace (or (.-namespace ^js (.-input options)) "user")]
    #js {:invocationMessage "Executing ClojureScript in Joyride environment"
         :confirmationMessages
         #js {:title "Execute ClojureScript Code"
              :message (vscode/MarkdownString.
                        (str "**Execute the following ClojureScript:**\n\n"
                             "```clojure\n"
                             code
                             "\n```\n\n"
                             "**In namespace:** " namespace "\n\n"
                             "This will run in Joyride's SCI environment with full VS Code API access."))}}))

(defn invoke-tool
  "Execute ClojureScript code with enhanced error handling"
  [options ^js stream token]
  (let [code (.-code ^js (.-input options))
        namespace (or (.-namespace ^js (.-input options)) "user")]
    (try
      ;; Check for cancellation
      (when (.-isCancellationRequested ^js token)
        (throw (js/Error. "Operation was cancelled")))

      ;; Execute the code
      (let [result (execute-code+ code namespace)]
        ;; Stream results
        (.markdown stream (str "**Evaluation result:**\n\n```clojure\n" result "\n```"))
        vscode/LanguageModelToolResult.Empty)

      (catch js/Error e
        ;; Enhanced error information
        (.markdown stream
                   (str "**Error executing ClojureScript:**\n\n"
                        "```\n" (.-message e) "\n```\n\n"
                        "**Code that failed:**\n\n"
                        "```clojure\n" code "\n```"))
        vscode/LanguageModelToolResult.Empty))))

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
