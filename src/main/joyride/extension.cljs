(ns joyride.extension
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.nrepl :as nrepl]
   [joyride.sci :as jsci]
   [sci.core :as sci]
   [joyride.scripts-menu :refer [show-workspace-scripts-menu+]]
   [joyride.settings :refer [workspace-scripts-path]]
   [joyride.utils :refer [vscode-read-uri+ info]]
   [promesa.core :as p]))

(def !db (atom {}))

(defn- register-command [^js context command-id var]
  (->> (vscode/commands.registerCommand command-id var)
       (.push (.-subscriptions context))))

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
                  (str "message\n=> "))]
     (say (str prefix result)))))

(defn run-code
  ([]
   (p/let [input (vscode/window.showInputBox #js {:title "Run Code"
                                                  :placeHolder "(require '[\"vscode\" :as vscode]) (vscode/showInformationMessage \"Hello World!\" [\"Hi there\"])"
                                                  :prompt "Enter some code to be evaluated"})]
     (when input
       (run-code input))))
  ([code]
   (let [result (jsci/eval-string code)]
     (say-result result))))

(defn choose-file [default-uri]
  (vscode/window.showOpenDialog #js {:canSelectMany false
                                     :defaultUri default-uri
                                     :openLabel "Open script"}))

(defn run-workspace-script+
  ([]
   (p/let [picked-script (show-workspace-scripts-menu+)
           script-path (:section-path picked-script)]
     (run-workspace-script+ script-path)))
  ([script-path]
   (-> (p/let [abs-path (path/join vscode/workspace.rootPath workspace-scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)
               code (vscode-read-uri+ script-uri)]
         (sci/with-bindings {sci/file abs-path}
           (jsci/eval-string code)))
       (p/handle (fn [result error]
                   (if error
                     (do
                       (say-error (str (js/console.error "Run Workspace Script Failed: " script-path (.-message error))))
                       (js/console.error "Run Workspace Script Failed: " script-path (.-message error) error))
                     (do (say-result (str script-path " evaluated.") result)
                         result)))))))

(def !server (volatile! nil))

(defn start-nrepl []
  (vreset! !server (nrepl/start-server {})))

(defn stop-nrepl []
  (nrepl/stop-server @!server))

(comment
  (run-workspace-script+)
  (run-workspace-script+ ".joyride/scripts/hello.cljs"))

(defn ^:export activate [^js context]
  (swap! !db assoc :output-channel (vscode/window.createOutputChannel "Joyride"))
  (register-command context "joyride.runCode" #'run-code)
  (register-command context "joyride.runWorkspaceScript" #'run-workspace-script+)
  (register-command context "joyride.startNRepl" #'start-nrepl)
  (register-command context "joyride.stopNRepl" #'stop-nrepl)
  (register-command context "joyride.enableNReplMessageLogging" #'nrepl/enable-message-logging!)
  (register-command context "joyride.disableNReplMessageLogging" #'nrepl/disable-message-logging!)
  (say "🟢 Joyride VS Code with Clojure. 🚗"))

(defn ^:export deactivate [])

(comment)
