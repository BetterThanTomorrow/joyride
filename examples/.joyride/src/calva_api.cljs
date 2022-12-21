(ns calva-api
  (:require ["vscode" :as vscode]
            ["ext://betterthantomorrow.calva$v0" :as calva]
            [joyride.core :as joyride]
            [promesa.core :as p]
            [util.editor :as editor-utils]))

(def oc (joyride.core/output-channel))

(defn evaluate-in-session+ [session-key code]
  (p/let [result (calva/repl.evaluateCode
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
  (evaluate-in-session+ (calva/repl.currentSessionKey) code))

(defn evaluate-selection+ []
  (p/let [code (editor-utils/current-selection-text)
          result (.-result (evaluate+ code))]
    result))

;; Utils for REPL-ing Joyride code, when connected to a project REPL.

(defn joyride-eval-current-form+ []
  (vscode/commands.executeCommand "joyride.runCode" (second (calva/ranges.currentForm))))

(defn joyride-eval-top-level-form+ []
  (vscode/commands.executeCommand "joyride.runCode" (second (calva/ranges.currentTopLevelForm))))

;; Bind to some nice keyboard shortcuts, e.g. like so:
;;  {
;;      "key": "cmd+ctrl+enter",
;;      "command": "joyride.runCode",
;;      "args": "(calva-api/joyride-eval-current-form)",
;;  },
;;  {
;;      "key": "cmd+alt+enter",
;;      "command": "joyride.runCode",
;;      "args": "(calva-api/joyride-eval-top-level-form)",
;;  },

;; Ignore the current (enclosing) form

(defn ignore-current-form []
  (p/let [[range text] (calva/ranges.currentEnclosingForm)]
    (calva/editor.replace vscode/window.activeTextEditor range (str "#_" text))))

;; Bind something like so:
;;     {
;;         "command": "joyride.runCode",
;;         "args": "(calva-api/ignore-current-form)",
;;         "key": "ctrl+alt+c i",
;;     },
