(ns z-joylib.vscode-utils
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

(defn insert-text!+ [editor text]
  (p/do (.edit editor
               (fn [^js builder]
                 (.insert builder (.-active (current-selection)) text))
               #js {:undoStopBefore true :undoStopAfter false})
        (p/catch (fn [e]
                   (js/console.error e)))))