(ns joyride.lm-tool.core
  "Core logic for LM tool functionality - pure functions without VS Code dependencies")

(defn format-confirmation-message
  "Generate confirmation message data for code execution"
  [code namespace wait-for-promise?]
  {:type :confirmation
   :title "Run Joyride Code"
   :code code
   :ns namespace
   :wait-for-promise? wait-for-promise?
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
  [^js input]
  (let [code (.-code input)
        ns (or (.-namespace input) "user")
        wait-for-promise? (or (.-awaitResult input) false)]
    {:code code :ns ns :wait-for-promise? wait-for-promise?}))

(defn validate-input
  "Validate LM tool input data"
  [{:keys [code ns wait-for-promise?]}]
  (cond
    (nil? code) {:valid? false :error "Code is required"}
    (empty? code) {:valid? false :error "Code cannot be empty"}
    (not (string? code)) {:valid? false :error "Code must be a string"}
    (not (string? ns)) {:valid? false :error "Namespace must be a string"}
    (not (boolean? wait-for-promise?)) {:valid? false :error "awaitResult must be a boolean"}
    :else {:valid? true :error nil}))

(defn confirmation-message->markdown
  "Convert confirmation message data to markdown string"
  [{:keys [code ns wait-for-promise? description]}]
  (str "**Execute the following ClojureScript:**\n\n"
       "```clojure\n"
       code
       "\n```\n\n"
       "**In namespace:** " ns "\n\n"
       "**Wait for promise?:** " wait-for-promise? "\n\n"
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
