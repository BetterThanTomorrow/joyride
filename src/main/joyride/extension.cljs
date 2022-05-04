(ns joyride.extension
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [clojure.pprint :as pprint]
            [joyride.config :as conf]
            [joyride.nrepl :as nrepl]
            [joyride.sci :as jsci]
            [joyride.scripts-menu :refer [show-script-picker+]]
            [joyride.utils :refer [info vscode-read-uri+ jsify]]
            [promesa.core :as p]
            [sci.core :as sci]))

(defonce !db (atom {}))

(defn- register-command! [^js context command-id var]
  (let [disposable (vscode/commands.registerCommand command-id var)]
    (swap! !db update :disposables conj disposable)
    (.push (.-subscriptions context) disposable)))

(defn- clear-disposables! []
  (doseq [^js disposable (:disposables @!db)]
    (p/all
     (.dispose disposable)))
  (swap! !db assoc :disposables []))

(def ^{:dynamic true
       :doc "Should the Joyride output channel be revealed after `say`?
             Default: `true`"}
  *show-when-said?* true)

(defn say [message]
  (let [channel ^js (:output-channel @!db)]
    (.appendLine channel message)
    (when *show-when-said?*
      (.show channel true))))

(defn say-error [message]
  (say (str "ERROR: " message)))

(defn say-result
  ([result]
   (say-result nil result))
  ([message result]
   (let [prefix (if (empty? message)
                  "=> "
                  (str message "\n=> "))]
     (.append ^js (:output-channel @!db) prefix)
     (say (with-out-str (pprint/pprint result))))))

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
     (say-result result))))

(defn choose-file [default-uri]
  (vscode/window.showOpenDialog #js {:canSelectMany false
                                     :defaultUri default-uri
                                     :openLabel "Open script"}))

(defn run-script+
  ([title base-path scripts-path]
   (p/let [picked-script (show-script-picker+ title
                                              base-path
                                              scripts-path)
           script-path (:relative-path picked-script)]
     (run-script+ title base-path scripts-path script-path)))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)
               code (vscode-read-uri+ script-uri)]
         (sci/with-bindings {sci/file abs-path}
           (jsci/eval-string code)))
       (p/handle (fn [result error]
                   (if error
                     (do
                       (say-error (str title " Failed: " script-path " " (.-message error)))
                       (js/console.error title "Failed: " script-path (.-message error) error))
                     (do (say-result (str script-path " evaluated.") result)
                         result)))))))

(def run-workspace-script-args ["Run Workspace Script"
                                vscode/workspace.rootPath
                                conf/workspace-scripts-path])

(defn run-workspace-script+
  ([]
   (apply run-script+ run-workspace-script-args))
  ([script]
   (apply run-script+ (conj run-workspace-script-args script))))

(def run-user-script-args ["Run User Script"
                           conf/user-config-path
                           conf/user-scripts-path])

(defn run-user-script+
  ([]
   (apply run-script+ run-user-script-args))
  ([script]
   (apply run-script+ (conj run-user-script-args script))))

(def api (jsify {:startNReplServer nrepl/start-server+}))

(defn ^:export activate' [^js context]
  (when context
    (reset! !db {:output-channel (vscode/window.createOutputChannel "Joyride")
                 :extension-context context
                 :disposables []})
    (say "🟢 Joyride VS Code with Clojure. 🚗"))
  (let [{:keys [extension-context]} @!db]
    (register-command! extension-context "joyride.runCode" #'run-code)
    (register-command! extension-context "joyride.runWorkspaceScript" #'run-workspace-script+)
    (register-command! extension-context "joyride.runUserScript" #'run-user-script+)
    (register-command! extension-context "joyride.startNReplServer" #'nrepl/start-server+)
    (register-command! extension-context "joyride.stopNReplServer" #'nrepl/stop-server)
    (register-command! extension-context "joyride.enableNReplMessageLogging" #'nrepl/enable-message-logging!)
    (register-command! extension-context "joyride.disableNReplMessageLogging" #'nrepl/disable-message-logging!)
    api))

(defn activate [^js context]
  (def a (activate' context))
  a)

(defn ^:export deactivate []
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