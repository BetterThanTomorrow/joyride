(ns select-folder
  (:require ["vscode" :as vscode]))

(def options
  #js
   {:canSelectMany false,
    :openLabel "Select",
    :canSelectFiles false,
    :canSelectFolders true})

(.then (vscode/window.showOpenDialog options)
       (fn [fileUri]
         (when (and fileUri (nth fileUri 0))
           (.log js/console
                 (str "Selected file: "
                      (.-fsPath (nth fileUri 0)))))))