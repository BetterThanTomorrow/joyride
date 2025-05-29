(ns joyride.lm-tool.core
  "Core logic for LM tool functionality - pure functions without VS Code dependencies")

(defn format-confirmation-message
  "Generate confirmation message data for code execution"
  [code namespace]
  {:type :confirmation
   :title "Run Joyride Code"
   :code code
   :namespace namespace
   :description "This will run in Joyride's SCI environment with full VS Code API access."})

(defn format-result-message
  "Format successful execution result as structured data"
  [result stdout stderr]
  {:type :success
   :result result
   :stdout stdout
   :stderr stderr})

(defn format-error-message
  "Format error message as structured data"
  [error code stdout stderr]
  {:type :error
   :error error
   :code code
   :stdout stdout
   :stderr stderr})

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

(defn confirmation-message->markdown
  "Convert confirmation message data to markdown string"
  [{:keys [code namespace description]}]
  (str "**Execute the following ClojureScript:**\n\n"
       "```clojure\n"
       code
       "\n```\n\n"
       "**In namespace:** " namespace "\n\n"
       description))

(defn error-message->markdown
  "Convert error message data to markdown string"
  [{:keys [error code stdout stderr]}]
  (let [base (str "**Error executing ClojureScript:**\n\n"
                  "```\n" error "\n```\n\n"
                  "**Code that failed:**\n\n"
                  "```clojure\n" code "\n```")]
    (cond-> base
      (and stdout (not-empty stdout))
      (str "\n\n**Output before error:**\n\n```\n" stdout "```")

      (and stderr (not-empty stderr))
      (str "\n\n**Additional errors:**\n\n```\n" stderr "```"))))
