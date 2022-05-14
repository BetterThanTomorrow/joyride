(ns joyride.extension
  (:require ["vscode" :as vscode]
            [joyride.db :as db]
            [joyride.getting-started :as getting-started]
            [joyride.life-cycle :as life-cycle]
            [joyride.nrepl :as nrepl]
            [joyride.sci :as jsci]
            [joyride.scripts-handler :as scripts-handler]
            [joyride.utils :as utils :refer [info jsify]]
            [joyride.when-contexts :as when-contexts]
            [promesa.core :as p]))

(defn- register-command! [^js context command-id var]
  (let [disposable (vscode/commands.registerCommand command-id var)]
    (swap! db/!app-db update :disposables conj disposable)
    (.push (.-subscriptions context) disposable)))

(defn- clear-disposables! []
  (swap! db/!app-db assoc :disposables [])
  (p/run! (fn [^js disposable]
            (.dispose disposable))
          (:disposables @db/!app-db)))

(defn run-code
  ([]
   (p/let [input (vscode/window.showInputBox #js {:title "Run Code"
                                                  ;; "(require '[\"vscode\" :as vscode]) (vscode/window.showInformationMessage \"Hello World!\" [\"Hi there\"])"
                                                  :placeHolder "(inc 41)"
                                                  :prompt "Enter some code to be evaluated"})]
     (when input
       (run-code input))))
  ([code]
   (p/let [result (jsci/eval-string code)]
     (utils/say-result result))))

(defn choose-file [default-uri]
  (vscode/window.showOpenDialog #js {:canSelectMany false
                                     :defaultUri default-uri
                                     :openLabel "Open script"}))

(defn start-nrepl-server+ [root-path]
  (nrepl/start-server+ {:root-path (or root-path vscode/workspace.rootPath)}))

(def api (jsify {:startNReplServer start-nrepl-server+
                 :getContextValue (fn [k]
                                    (when-contexts/context k))}))

(defn ^:export activate [^js context]
  (when context
    (swap! db/!app-db assoc
           :output-channel (vscode/window.createOutputChannel "Joyride")
           :extension-context context
           :workspace-root-path vscode/workspace.rootPath)
    (-> (getting-started/maybe-create-user-content+)
        (p/catch
         (fn [e]
           (js/console.error "Joyride activate error" e)))
        (p/then
         (fn [_r]
           (p/do! (life-cycle/maybe-run-init-script+ scripts-handler/run-user-script+
                                                     (:user (life-cycle/init-scripts)))
                  (when vscode/workspace.rootPath
                    (life-cycle/maybe-run-init-script+ scripts-handler/run-workspace-script+
                                                       (:workspace (life-cycle/init-scripts))))
                  (utils/sayln "🟢 Joyride VS Code with Clojure. 🚗💨"))))))

  (let [{:keys [extension-context]} @db/!app-db]
    (register-command! extension-context "joyride.runCode" #'run-code)
    (register-command! extension-context "joyride.runWorkspaceScript" #'scripts-handler/run-workspace-script+)
    (register-command! extension-context "joyride.runUserScript" #'scripts-handler/run-user-script+)
    (register-command! extension-context "joyride.openWorkspaceScript" #'scripts-handler/open-workspace-script+)
    (register-command! extension-context "joyride.openUserScript" #'scripts-handler/open-user-script+)
    (register-command! extension-context "joyride.startNReplServer" #'start-nrepl-server+)
    (register-command! extension-context "joyride.stopNReplServer" #'nrepl/stop-server)
    (register-command! extension-context "joyride.enableNReplMessageLogging" #'nrepl/enable-message-logging!)
    (register-command! extension-context "joyride.disableNReplMessageLogging" #'nrepl/disable-message-logging!)
    (when-contexts/set-context! ::when-contexts/joyride.isActive true)
    api))

(defn ^:export deactivate []
  (when-contexts/set-context! ::when-contexts/joyride.isActive false)
  (when (nrepl/server-running?)
    (nrepl/stop-server))
  (clear-disposables!))

(defn before [done]
  (-> (clear-disposables!)
      (p/then done)))

(defn after []
  (info "shadow-cljs reloaded Joyride")
  (js/console.log "shadow-cljs Reloaded"))

(comment
  (def ba (before after)))