(ns util.editor
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(defn current-selection []
  (let [editor ^js vscode/window.activeTextEditor
        selection (.-selection editor)]
    selection))

(defn current-document []
  (let [editor ^js vscode/window.activeTextEditor
        document (.-document editor)]
    document))

(defn current-selection-text []
  (.getText (current-document) (current-selection)))

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

(defn delete-range!
  [^js editor ^js range]
  (-> (p/do (.edit editor
                   (fn [^js builder]
                     (.delete builder range))
                   #js {:undoStopBefore true :undoStopAfter false}))
      (p/catch (fn [e]
                 (js/console.error e)))))

(comment
  (def a-selection (current-selection))
  (aset vscode/window.activeTextEditor "selection" a-selection)
  (insert-text!+ "foo" 
                 vscode/window.activeTextEditor
                 (.-active a-selection))
  (insert-text!+ "foo")
  )