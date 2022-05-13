(ns joyride.extension
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.db :as db]
            [joyride.getting-started :as getting-started]
            [joyride.life-cycle :as life-cycle]
            [joyride.nrepl :as nrepl]
            [joyride.sci :as jsci]
            [joyride.scripts-menu :refer [show-script-picker+]]
            [joyride.utils :as utils :refer [info jsify vscode-read-uri+]]
            [joyride.when-contexts :as when-contexts]
            [promesa.core :as p]
            [sci.core :as sci]))

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

(defn handle-script-menu-selection+ 
  [{:keys [title] :as menu-conf} script-fn+ base-path scripts-path]
  (p/let [pick (show-script-picker+ menu-conf base-path scripts-path)]
     (when pick
       (let [relative-path (:relative-path pick)
             function (:function pick)]
         (cond
           relative-path (script-fn+ title base-path scripts-path relative-path)
           function (function))))))

(defn run-script+
  ([menu-conf base-path scripts-path]
   (handle-script-menu-selection+ menu-conf run-script+ base-path scripts-path))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)
               code (vscode-read-uri+ script-uri)]
         (swap! db/!app-db assoc :invoked-script abs-path)
         (sci/with-bindings {sci/file abs-path}
           (jsci/eval-string code)))
       (p/handle (fn [result error]
                   (swap! db/!app-db assoc :invoked-script nil)
                   (if error
                     (binding [utils/*show-when-said?* true]
                       (utils/say-error (str title " Failed: " script-path " " (.-message error))))
                     (do (utils/say-result (str script-path " evaluated.") result)
                         result)))))))

(defn open-script+
  ([menu-conf base-path scripts-path]
   (handle-script-menu-selection+ menu-conf open-script+ base-path scripts-path))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)]
         (p/-> (vscode/workspace.openTextDocument script-uri)
               (vscode/window.showTextDocument
                #js {:preview false, :preserveFocus false})))
       (p/catch (fn [error]
                  (binding [utils/*show-when-said?* true]
                    (utils/say-error (str title " Failed: " script-path " " (.-message error)))))))))

(defn run-or-open-workspace-script-args [menu-conf-or-title]
  [menu-conf-or-title
   vscode/workspace.rootPath
   conf/workspace-scripts-path])

(defn run-or-open-user-script-args [menu-conf-or-title]
  [menu-conf-or-title
   conf/user-config-path
   conf/user-scripts-path])

(declare open-user-script+)
(declare open-workspace-script+)

(defn run-workspace-script+
  ([]
   (apply run-script+ (run-or-open-workspace-script-args
                       {:title "Run Workspace Script"
                        :more-menu-items {:label "Open Workspace Script"
                                          :function open-workspace-script+}})))
  ([script]
   (apply run-script+ (conj (run-or-open-workspace-script-args "Run") script))))

(defn run-user-script+
  ([]
   (apply run-script+ (run-or-open-user-script-args
                       {:title "Run User Script"
                         :more-menu-items {:label "Open User Script"
                                           :function open-user-script+}})))
  ([script]
   (apply run-script+ (conj (run-or-open-user-script-args "Run") script))))

(defn open-workspace-script+
  ([]
   (apply open-script+ (run-or-open-workspace-script-args 
                        {:title "Open Workspace Script"
                         :more-menu-items {:label "Run Workspace Script"
                                           :function run-workspace-script+}})))
  ([script]
   (apply open-script+ (conj (run-or-open-workspace-script-args "Open") script))))

(defn open-user-script+
  ([]
   (apply open-script+ (run-or-open-user-script-args
                        {:title "Open User Script"
                         :more-menu-items {:label "Run User Script"
                                           :function run-user-script+}})))
  ([script]
   (apply open-script+ (conj (run-or-open-user-script-args "Open") script))))

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
           (p/do! (life-cycle/maybe-run-init-script+ run-user-script+
                                                     (:user life-cycle/init-scripts))
                  (when vscode/workspace.rootPath
                    (life-cycle/maybe-run-init-script+ run-workspace-script+
                                                       (:workspace life-cycle/init-scripts)))
                  (utils/sayln "🟢 Joyride VS Code with Clojure. 🚗💨"))))))

  (let [{:keys [extension-context]} @db/!app-db]
    (register-command! extension-context "joyride.runCode" #'run-code)
    (register-command! extension-context "joyride.runWorkspaceScript" #'run-workspace-script+)
    (register-command! extension-context "joyride.runUserScript" #'run-user-script+)
    (register-command! extension-context "joyride.openWorkspaceScript" #'open-workspace-script+)
    (register-command! extension-context "joyride.openUserScript" #'open-user-script+)
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