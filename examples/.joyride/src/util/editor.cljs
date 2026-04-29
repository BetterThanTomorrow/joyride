(ns util.editor
  (:require ["vscode" :as vscode]))

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

(defn ^:async insert-text!+
  ([^js text]
   (insert-text!+ text vscode/window.activeTextEditor (.-active (current-selection))))
  ([text ^js editor ^js position]
   (try
     (await (.edit editor
                   (fn [^js builder]
                     (.insert builder position text))
                   #js {:undoStopBefore true :undoStopAfter false}))
     (catch :default e
       (js/console.error e)))))

(defn ^:async delete-range!+
  [^js editor ^js range]
  (try
    (await (.edit editor
                  (fn [^js builder]
                    (.delete builder range))
                  #js {:undoStopBefore true :undoStopAfter false}))
    (catch :default e
      (js/console.error e))))

(def delete-range! delete-range!+) ;; backwards compatible

(defn ^:async replace-range!+
  "Defaults to the current selection."
  ([^js text]
   (replace-range!+ text vscode/window.activeTextEditor (.-active (current-selection)) (current-selection)))
  ([^js text ^js editor ^js position ^js range]
   (try
     (await (.edit editor
                   (fn [^js builder]
                     (.delete builder range)
                     (.insert builder position text))
                   #js {:undoStopBefore true :undoStopAfter false}))
     (catch :default e
       (js/console.error e)))))

(comment
  (def a-selection (current-selection))
  (aset vscode/window.activeTextEditor "selection" a-selection)
  (insert-text!+ "foo"
                 vscode/window.activeTextEditor
                 (.-active a-selection))
  (insert-text!+ "foo")
  )