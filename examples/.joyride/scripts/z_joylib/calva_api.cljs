(ns z-joylib.calva-api
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]
            [z-joylib.editor-utils :as editor-utils]))

(def oc (joyride.core/output-channel))
(def calva (vscode/extensions.getExtension "betterthantomorrow.calva"))
(def calvaApi (-> calva
                  .-exports
                  .-v0
                  (js->clj :keywordize-keys true)))

(defn text-for-ranges-key [ranges-key]
  (second ((get-in calvaApi [:ranges ranges-key]))))

(defn evaluate-in-session+ [session-key code]
  (p/let [result ((get-in [:repl :evaluateCode] calvaApi)
                  session-key
                  code
                  #js {:stdout #(.append oc %)
                       :stderr #(.append oc (str "Error: " %))})]
         (.-result result)))

(defn clj-evaluate+ [code]
  (evaluate-in-session+ "clj" code))

(defn cljs-evaluate+ [code]
  (evaluate-in-session+ "cljs" code))

(defn evaluate+
  "Evaluates `code` in whatever the current session is."
  [code]
  (evaluate-in-session+ ((get-in calvaApi [:repl :currentSessionKey])) code))

(defn evaluate-selection+ []
  (p/let [code (editor-utils/current-selection-text)
          result (.-result (evaluate+ code))]
    result))

;; Utils for REPL-ing Joyride code, when connected to a project REPL.

(defn joyride-eval-current-form+ []
  (vscode/commands.executeCommand "joyride.runCode" (text-for-ranges-key :currentForm)))

(defn joyride-eval-top-level-form+ []
  (vscode/commands.executeCommand "joyride.runCode" (text-for-ranges-key :currentTopLevelForm)))

;; Bind to some nice keyboard shortcuts, e.g. like so:
;;  {
;;      "key": "cmd+ctrl+enter",
;;      "command": "joyride.runCode",
;;      "args": "(z-joylib.calva-api/joyride-eval-current-form)",
;;  },
;;  {
;;      "key": "cmd+alt+enter",
;;      "command": "joyride.runCode",
;;      "args": "(z-joylib.calva-api/joyride-eval-top-level-form)",
;;  },

;; Convenience function making it easier to restart clojure-lsp

(defn restart-clojure-lsp []
  (p/do (vscode/commands.executeCommand "calva.clojureLsp.stop")
        (vscode/commands.executeCommand "calva.clojureLsp.start")))