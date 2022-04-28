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

(defn insert-text!+ [text]
  (let [edits #js [(vscode/TextEdit.insert (.-active (current-selection)) text)]
        ws-edit (vscode/WorkspaceEdit.)]
    (.set ws-edit (.-uri (current-document)) edits)
    (p/do!
     (vscode/workspace.applyEdit ws-edit))))