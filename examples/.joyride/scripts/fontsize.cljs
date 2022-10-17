(ns fontsize
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

;; Manipulates the editor font size.

;; **NB: This changes the *global/User* font size. If you have configured
;; `editor.fontSize` in the *Workspace*, nothing will appear to happen,
;; you might be in for a surprise when opening ;; other workspaces.**
;; I.e.: Please disable any Workspace font size setting before trying this.

(defn set-global-fontsize [pts]
  (-> (vscode/workspace.getConfiguration)
      (.update "editor.fontSize" pts true))
  nil)

(when (= (joyride/invoked-script) joyride/*file*)
  (set-global-fontsize 12))

;; live demo here: https://twitter.com/borkdude/status/1519709769157775360
