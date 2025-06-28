(ns workspace-activate
  (:require [joyride.core :as joyride]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(defonce !db (atom {:disposables []}))

;; To make the activation script re-runnable we dispose of
;; event handlers and such that we might have registered
;; in previous runs.
(defn- clear-disposables! []
  (run! (fn [disposable]
          (.dispose disposable))
        (:disposables @!db))
  (swap! !db assoc :disposables []))

;; Pushing the disposables on the extension context's
;; subscriptions will make VS Code dispose of them when the
;; Joyride extension is deactivated.
(defn- push-disposable [disposable]
  (swap! !db update :disposables conj disposable)
  (-> (joyride/extension-context)
      .-subscriptions
      (.push disposable)))

(defn- main []
  (println "Hello World, from main workspace_activate.cljs script")
  (clear-disposables!)
  #_(push-disposable '...) ; Here is where you add disposables you
                           ; create as part of workspace init
  ; In the deault workspace activate script for the User project we open the README
  (p/let [workspace-folder (first vscode/workspace.workspaceFolders)
          readme-path (vscode/Uri.joinPath (.-uri workspace-folder) "/README.md")]
    (vscode/commands.executeCommand "markdown.showPreview" readme-path)))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))