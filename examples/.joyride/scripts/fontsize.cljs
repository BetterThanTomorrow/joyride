(ns fontsize
  (:require ["vscode" :as vscode]))

(defn set-global-fontsize [pts]
  (-> (vscode/workspace.getConfiguration)
      (.update "editor.fontSize" pts true))
  nil)

(comment
  (set-global-fontsize 12)
  )
