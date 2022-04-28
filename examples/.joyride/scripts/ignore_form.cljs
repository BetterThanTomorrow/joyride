(ns ignore-form
  (:require ["vscode" :as vsode]
            [promesa.core :as p]
            [z-joylib.editor-utils :as eu]))

(def f *file*)
(defonce run-main? true)

(comment
  ;; Loading this in the REPL w/o evaluating `main`:
  (defonce run-main? false)  ; <- First evaluate this
  (ns-unmap *ns* 'run-main?) ; <- Evaluate this when you are done
                             ;    or want to test-run the script
  )

(defn main []
  (p/let [editor ^js vscode/window.activeTextEditor
          original-selection (eu/current-selection)
          _ (vscode/commands.executeCommand "paredit.backwardUpSexp")
          insert-position (.-active (eu/current-selection))]
    (aset editor "selection" original-selection)
    (p/do! (eu/insert-text!+ "#_" editor insert-position))))

(when run-main?
  (main))

(comment  
  (main)
  )