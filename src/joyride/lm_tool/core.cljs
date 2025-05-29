(ns joyride.lm-tool.core
  "Core logic for LM tool functionality - pure functions without VS Code dependencies"
  (:require [joyride.sci :as sci]))

(defn execute-code
  "Execute ClojureScript code in the SCI environment with optional namespace.
   Returns a map with :result, :error, and :namespace keys."
  ([code] (execute-code code "user"))
  ([code namespace]
   (try
     {:result (sci/eval-string code)
      :error nil
      :namespace namespace}
     (catch js/Error e
       {:result nil
        :error (.-message e)
        :namespace namespace}))))

(defn format-confirmation-message
  "Generate confirmation message content for code execution"
  [code namespace]
  (str "**Execute the following ClojureScript:**\n\n"
       "```clojure\n"
       code
       "\n```\n\n"
       "**In namespace:** " namespace "\n\n"
       "This will run in Joyride's SCI environment with full VS Code API access."))

(defn format-result-message
  "Format successful execution result for display"
  [result]
  (str "**Evaluation result:**\n\n```clojure\n" result "\n```"))

(defn format-error-message
  "Format error message for display"
  [error code]
  (str "**Error executing ClojureScript:**\n\n"
       "```\n" error "\n```\n\n"
       "**Code that failed:**\n\n"
       "```clojure\n" code "\n```"))

(defn extract-input-data
  "Extract code and namespace from LM tool input options"
  [input]
  (let [code (.-code ^js input)
        namespace (or (.-namespace ^js input) "user")]
    {:code code :namespace namespace}))

(defn validate-input
  "Validate LM tool input data"
  [{:keys [code namespace]}]
  (cond
    (nil? code) {:valid? false :error "Code is required"}
    (empty? code) {:valid? false :error "Code cannot be empty"}
    (not (string? code)) {:valid? false :error "Code must be a string"}
    (not (string? namespace)) {:valid? false :error "Namespace must be a string"}
    :else {:valid? true :error nil}))
