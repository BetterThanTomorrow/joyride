(ns z-joylib.editor-utils
  (:require ["vscode" :as vsode]
            [promesa.core :as p]))

(defn current-selection []
  (let [editor ^js vscode/window.activeTextEditor
        selection (.-selection editor)]
    selection))

(defn current-document []
  (let [editor ^js vscode/window.activeTextEditor
        document (.-document editor)]
    document))

(defn insert-text!+ 
  ([^js text]
   (insert-text!+ text vscode/window.activeTextEditor (.-active (current-selection))))
  ([text ^js editor ^js position]
   (-> (p/do (.edit editor
                    (fn [^js builder]
                      (.insert builder position text))
                    #js {:undoStopBefore true :undoStopAfter false}))
       (p/catch (fn [e]
                  (js/console.error e))))))

(comment
  (def a-selection (current-selection))
  (aset vscode/window.activeTextEditor "selection" a-selection)
  (insert-text!+ "foo" 
                 vscode/window.activeTextEditor
                 (.-active a-selection))
  (insert-text!+ "foo")
  )