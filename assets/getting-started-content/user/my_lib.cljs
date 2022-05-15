(ns my-lib
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

;; Assuming this namespace i required by `activate.cljs`
;; you can reach vars in `my-lib` using `my-lib/<symbol>` in
;; `joyride.runCode` keybindings without requiring `my-lib``
;; there.

;; As an example, take the feature request on Calva to add a
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