(ns activate
  (:require [joyride.core :as joyride]
            ["vscode" :as vscode]))

(defonce !ws-db (atom {:disposables []}))

;; To make the activation script re-runnable we dispose of
;; event handlers and such that we might have registered
;; in previous runs.
(defn- ws-clear-disposables! []
  (run! (fn [disposable]
          (.dispose disposable))
        (:disposables @!ws-db))
  (swap! !ws-db assoc :disposables []))

;; Pushing the disposables on the extension context's
;; subscriptions will make VS Code dispose of them when the
;; Joyride extension is deactivated.
(defn- ws-push-disposable [disposable]
  (swap! !ws-db update :disposables conj disposable)
  (-> (joyride/extension-context)
      .-subscriptions
      (.push disposable)))

(defn- ws-main []
  (println "Hello World, from Workspace activate.cljs script")
  (ws-clear-disposables!)
  (ws-push-disposable
   ;; It might surprise you to see how often and when this happens,
   ;; and when it doesn't happen.
   (vscode/workspace.onDidOpenTextDocument
    (fn [doc]
      (println "[Joyride example]" 
               (.-languageId doc) 
               "document opened:" 
               (.-fileName doc))))))

(when (= (joyride/invoked-script) joyride/*file*)
  (ws-main))
