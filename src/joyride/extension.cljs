(ns joyride.extension
  (:require ["vscode" :as vscode]
            [joyride.db :as db]
            [joyride.getting-started :as getting-started]
            [joyride.lifecycle :as life-cycle]
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
  (-> (p/run! (fn [^js disposable]
                (.dispose disposable))
              (:disposables @db/!app-db))
      (.then (fn []
               (swap! db/!app-db assoc :disposables [])))))

(defn run-code+
  ([]
   (p/let [input (vscode/window.showInputBox #js {:title "Run Code"
                                                  ;; "(require '[\"vscode\" :as vscode]) (vscode/window.showInformationMessage \"Hello World!\" [\"Hi there\"])"
                                                  :placeHolder "(inc 41)"
                                                  :prompt "Enter some code to be evaluated"})]
     (when input
       (run-code+ input))))
  ([code]
   (-> (p/let [result (jsci/eval-string code)]
         ;; Maybe we should skip printing here?
         (utils/say-result result)
         result)
       (p/catch (fn [e]
                  (utils/say-error (str (ex-message e) "\n  " (ex-data e)))
                  (throw e))))))

(defn evaluate-selection+
  "Evaluates the selection by first copying it to the clipboard and reading it from there.
   Restores the original clipboard content after reading it from the clipboard.
   The reason to do it like this is that it will work regardless of where text i selected.
   Also in Markdown previews or terminal, or QuickPick prompts, or anywhere."
  []
  ; Delay needed when run from the command palette
  ; lest the command palette is active when the copy is performed
  (p/do
    (p/create
     (fn [resolve _reject]
       (js/setTimeout resolve 200)))
    (p/let [original-clipboard-text (vscode/env.clipboard.readText)
            _ (vscode/commands.executeCommand "editor.action.clipboardCopyAction")
            selected-text (vscode/env.clipboard.readText)
            _ (vscode/env.clipboard.writeText original-clipboard-text)]
      (when (not-empty selected-text)
        (run-code+ selected-text)))))

(defn choose-file [default-uri]
  (vscode/window.showOpenDialog #js {:canSelectMany false
                                     :defaultUri default-uri
                                     :openLabel "Open script"}))

(defn start-nrepl-server+ [root-path]
  (nrepl/start-server+ {:root-path (or root-path vscode/workspace.rootPath)}))

(def api (jsify {:startNReplServer start-nrepl-server+
                 :getContextValue when-contexts/context
                 :runCode run-code+}))

(defn ^:export activate [^js context]
  (js/console.info "Joyride activate START")
  (when context
    (swap! db/!app-db assoc
           :output-channel (vscode/window.createOutputChannel "Joyride")
           :extension-context context
           :workspace-root-path vscode/workspace.rootPath))

  (let [{:keys [extension-context]} @db/!app-db]
    (register-command! extension-context "joyride.runCode" #'run-code+)
    (register-command! extension-context "joyride.evaluateSelection" #'evaluate-selection+)
    (register-command! extension-context "joyride.runWorkspaceScript" #'scripts-handler/run-workspace-script+)
    (register-command! extension-context "joyride.runUserScript" #'scripts-handler/run-user-script+)
    (register-command! extension-context "joyride.openWorkspaceScript" #'scripts-handler/open-workspace-script+)
    (register-command! extension-context "joyride.openUserScript" #'scripts-handler/open-user-script+)
    (register-command! extension-context "joyride.startNReplServer" #'start-nrepl-server+)
    (register-command! extension-context "joyride.stopNReplServer" #'nrepl/stop-server+)
    (register-command! extension-context "joyride.enableNReplMessageLogging" #'nrepl/enable-message-logging!)
    (register-command! extension-context "joyride.disableNReplMessageLogging" #'nrepl/disable-message-logging!)
    (when-contexts/set-context! ::when-contexts/joyride.isActive true)

    (when context
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
    (js/console.info "Joyride activate END")
    api))

(defn ^:export deactivate []
  (when-contexts/set-context! ::when-contexts/joyride.isActive false)
  (when (nrepl/server-running?)
    (nrepl/stop-server+))
  (clear-disposables!))

(defn before [done]
  (js/console.log "shadow-cljs before reloading joyride")
  (-> (clear-disposables!)
      (p/then done)))

(defn after []
  (info "shadow-cljs reloaded Joyride")
  (js/console.log "shadow-cljs Reloaded Joyride")
  (activate nil))

(comment
  (def ba (before after)))
