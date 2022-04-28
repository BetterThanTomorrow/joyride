(ns ignore-form
  (:require ["vscode" :as vsode]
            [promesa.core :as p]
            [z-joylib.vscode-utils :as vsu]))

(defn main []
  (p/let [editor ^js vscode/window.activeTextEditor
          original-selection (vsu/current-selection)
          _ (vscode/commands.executeCommand "paredit.backwardUpSexp")
          before-edit-selection (vsu/current-selection)]
    (aset editor "selection" original-selection)
    (p/do! (vsu/insert-text!+ editor before-edit-selection "#_"))))

(main)