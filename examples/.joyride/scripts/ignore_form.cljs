(ns ignore-form
  (:require ["vscode" :as vsode]
            [promesa.core :as p]
            [joyride.core :as j]
            [z-joylib.editor-utils :as eu]))

(defn main []
  (p/let [editor ^js vscode/window.activeTextEditor
          original-selection (eu/current-selection)
          _ (vscode/commands.executeCommand "paredit.backwardUpSexp")
          insert-position (.-active (eu/current-selection))]
    (aset editor "selection" original-selection)
    (p/do! (eu/insert-text!+ "#_" editor insert-position))))

(when j/*file*
  (main))

(comment  
  (main)
  )