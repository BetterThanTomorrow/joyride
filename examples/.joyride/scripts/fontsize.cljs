(ns fontsize
  (:require ["vscode" :as vscode]))

(defn set-global-fontsize [dots]
  (-> (vscode/workspace.getConfiguration)
      (.update "editor.fontSize" dots true))
  nil)

(comment
  (set-global-fontsize 12)
  )
