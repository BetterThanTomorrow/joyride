(ns terminal
  (:require ["os" :as os]
            ["path" :as path]
            ["vscode" :as vscode]))

;; start a terminal called nbb in $HOME/dev/nbb
(def terminal
  (vscode/window.createTerminal
   #js {:name "nbb"
        :cwd (path/join (os/homedir) "dev" "nbb")}))

;; make it visible
(terminal.show true)

;; send an initial command to it
(terminal.sendText "bb dev")

;; see live demo here:
;; https://twitter.com/borkdude/status/1519272015705911296
