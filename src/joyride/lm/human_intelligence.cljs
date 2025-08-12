(ns joyride.lm.human-intelligence
  "Human input request tool for language models"
  (:require
   [clojure.string :as string]
   [promesa.core :as p]
   [joyride.lm.core :as core]
   ["vscode" :as vscode]))

(def timeout-ms 10000)
(def timeout-s (/ timeout-ms 1000))

(defn request-human-input!
  "Show an input box to collect human input with auto-dismiss timeout.
   Returns a promise resolving to the user input or default message."
  [prompt]
  (let [!state (atom {::result ::empty->what-would-rich-hickey-do?})]
    (p/create
     (fn [resolve-fn _reject]
       (let [input-box (vscode/window.createInputBox)]
         (set! (.-title input-box) "Your Copilot needs your input")
         (set! (.-prompt input-box) prompt)
         (set! (.-placeholder input-box) (str "Start typing to cancel auto-dismiss (" timeout-s "s timeout)..."))
         (set! (.-ignoreFocusOut input-box) true)
         (swap! !state assoc ::timeout-id
                (js/setTimeout (fn [] (.hide input-box)) timeout-ms))
         (.onDidChangeValue input-box (fn [_] (when-let [timeout-id (::timeout-id @!state)]
                                                (swap! !state dissoc ::timeout-id)
                                                (js/clearTimeout timeout-id))))
         (.onDidAccept input-box (fn []
                                   (let [value (.-value input-box)]
                                     (when-not (string/blank? value)
                                       (swap! !state assoc ::result value))
                                     (.hide input-box))))
         (.onDidHide input-box (fn []
                                 (.dispose input-box)
                                 (resolve-fn (str (::result @!state)))))
         (.show input-box))))))

(defn invoke-tool!
  "Invoke handler for the human input tool. Extracts prompt and returns tool result."
  []
  (fn [^js options ^js _token]
    (let [input (.-input options)
          prompt (.-prompt input)]
      (try
        (p/let [answer (request-human-input! prompt)]
          (core/create-success-result (str answer)))
        (catch js/Error e
          (core/create-error-result (.-message e)))))))

(defn register-tool!
  "Register the human input tool with VS Code's LM API. Returns disposable."
  []
  (try
    (let [tool-impl #js {:invoke (invoke-tool!)}
          disposable (vscode/lm.registerTool "joyride_request_human_input" tool-impl)]
      (js/console.log "joyride_request_human_input LM Tool registered successfully")
      disposable)
    (catch js/Error e
      (js/console.error "Failed to register joyride_request_human_input LM Tool:" (.-message e))
      nil)))
