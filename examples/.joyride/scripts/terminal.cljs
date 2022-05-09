(ns terminal
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

(defn main []
   ;; start a terminal called nbb in $HOME/dev/nbb
  (let [terminal (vscode/window.createTerminal
                  #js {:name "nbb"})]
    
    ;; make it visible
    (.show terminal true)

    ;; send an initial command to it
    (.sendText terminal "npx nbb")))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))

;; see live demo here:
;; https://twitter.com/borkdude/status/1519304323703971841
