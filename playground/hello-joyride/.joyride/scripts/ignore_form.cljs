(ns ignore-form
  (:require ["vscode" :as vsode]
            [promesa.core :as p]
            [z-joylib.vscode-utils :as vsu]))

(defn restore-selection! [editor original-selection before-edit-selection text]
  (let [current-selection (.-selection editor)
        original-active (.-active original-selection)
        before-edit-active (.-active before-edit-selection)
        current-active (.-active current-selection)
        next-active (if (.isAfter current-active before-edit-active)
                      (.with original-active
                             (.-line original-active)
                             (+ (.-character original-active) (count text)))
                      original-active)]
    (aset editor "selection" (vscode/Selection. next-active next-active))))

(defn main []
  (p/let [editor ^js vscode/window.activeTextEditor
          original-selection (vsu/current-selection)
          _ (vscode/commands.executeCommand "paredit.backwardUpSexp")
          before-edit-selection (vsu/current-selection)]
    (p/do 
          (vsu/insert-text!+ "#_")
          (aset "selection" editor original-selection)
          (restore-selection! editor original-selection before-edit-selection "#_"))))

(main)