(ns my-lib
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

;; Assuming this namespace i required by `activate.cljs`
;; you can reach vars in `my-lib` using `my-lib/<symbol>` in
;; `joyride.runCode` keybindings without requiring `my-lib``
;; there.


;; An example: VS Code does not provide a way to reliably start a
;; find-in-file with regular expressions toggled on.
;; This function does that:

(defn find-with-regex-on []
  (let [selection vscode/window.activeTextEditor.selection
        document vscode/window.activeTextEditor.document
        selectedText (.getText document selection)
        regexp-chars (js/RegExp. #"[.?+*^$\\|(){}[\]]" "g")
        newline-chars (js/RegExp. #"\n" "g")
        escapedText (-> selectedText
                        (.replace regexp-chars "\\$&")
                        (.replace newline-chars "\\n?$&"))]
    (vscode/commands.executeCommand "editor.actions.findWithArgs" #js {:isRegex true
                                                                       :searchString escapedText})))
;; Bind it to a shortcut:
;; {
;;     "key": "cmd+ctrl+alt+f",
;;     "command": "joyride.runCode",
;;     "args": "(my-lib/find-with-regex-on)",
;; },
;; (Or bind it to the default find-in-file shortcut if you like)



;; Another example: take the feature request on Calva to add a
;; **Restart clojure-lsp** command. It can be implemented with
;; with this function:

(defn restart-clojure-lsp []
  (p/do (vscode/commands.executeCommand "calva.clojureLsp.stop")
        (vscode/commands.executeCommand "calva.clojureLsp.start")))

;; And then this shortcut definition in `keybindings.json`
;; {
;;     "key": "<some-keyboard-shortcut>",
;;     "command": "joyride.runCode",
;;     "args": "(my-lib/restart-clojure-lsp)"
;; },
    
;; If you get complaints about `my-lib` not found, you probably
;; have not required it from `activate.cljs`


