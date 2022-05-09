(ns fontsize
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

(defn set-global-fontsize [pts]
  (-> (vscode/workspace.getConfiguration)
      (.update "editor.fontSize" pts true))
  nil)

(when (= (joyride/invoked-script) joyride/*file*)
  (set-global-fontsize 12))

;; live demo here: https://twitter.com/borkdude/status/1519709769157775360
