(ns joyride.extension
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [clojure.string :as str]
   [joyride.settings :refer [workspace-scripts-path]]
   [joyride.scripts-menu :refer [show-workspace-scripts-menu+]]
   [promesa.core :as p]
   [sci-configs.funcool.promesa :as pconfig]
   [sci.core :as sci]))

(defn- register-command [^js context command-id var]
  (->> (vscode/commands.registerCommand command-id var)
       (.push (.-subscriptions context))))

(defn debug [& xs]
  (apply vscode/window.showInformationMessage (into-array (mapv str xs))))

(def !ctx (volatile!
           (sci/init {:classes {'js goog/global
                                :allow :all}
                      :namespaces (:namespaces pconfig/config)
                      :load-fn (fn [{:keys [namespace opts]}]
                                 (when ;; assume npm library
                                  (string? namespace)
                                   (if (= "vscode" namespace)
                                     (do (sci/add-class! @!ctx 'vscode vscode)
                                         (sci/add-import! @!ctx (symbol (str @sci/ns)) 'vscode (:as opts))
                                         {:handled true})
                                     (let [mod (js/require namespace)
                                           ns-sym (symbol namespace)]
                                       (sci/add-class! @!ctx ns-sym mod)
                                       (sci/add-import! @!ctx (symbol (str @sci/ns)) ns-sym
                                                        (or (:as opts)
                                                            ns-sym))
                                       {:handled true}))))})))

(defn eval-query []
  (p/let [input (vscode/window.showInputBox #js {:placeHolder "(require '[\"path\" :as path]) (path/resolve \".\")"
                                                 :prompt "Type one or more expressions to evaluate"})
          res (sci/eval-string* @!ctx input)]
    (vscode/window.showInformationMessage (str "The result: " res))))

(defn run-script [& script]
  (let [program (str/join "\n"
                          (map pr-str '[(require '["vscode" :as vscode])
                                        (vscode/window.showInformationMessage "Hello from SCI!")]))]
    (sci/eval-string* @!ctx
                      program
                      #_(fs/readFileSync (path/resolve ws-root ".joyride/scripts/hello.cljs"))))
  (sci/eval-form @!ctx
                 '(do (require '[promesa.core :as p])
                      (p/do
                        (p/delay 2000)
                        (vscode/window.showInformationMessage "Hello from SCI again!!!!!!"))))
  (eval-query))

(defn vscode-read-uri+ [^js uri]
  (try
    (p/let [_ (vscode/workspace.fs.stat uri)
            data (vscode/workspace.fs.readFile uri)
            decoder (js/TextDecoder. "utf-8")
            code (.decode decoder data)]
      code)
    (catch :default e
      (js/console.error "Reading file failed: " (.-message e)))))

(sci/alter-var-root sci/print-fn (constantly *print-fn*))

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
         (sci/eval-string* @!ctx code))
       (p/handle (fn [result error]
                   (if error
                     (js/console.error "Run Workspace Script Failed: " script-path (.-message error))
                     result))))))

(defn load-current-file+ []
  (if-let [current-doc (some->> vscode/window.activeTextEditor
                                (.-document))]
    (-> (p/let [code (vscode-read-uri+ (.-uri current-doc))]
          (sci/eval-string* @!ctx code))
        (p/handle (fn [result error]
                    (if error
                      (js/console.error "Load Current File Failed: " (.-fileName current-doc) (.-message error))
                      result))))
    (vscode/window.showInformationMessage "There is no current document to load")))

(defn evaluate-selection+ []
  (if-let [selection (some->> vscode/window.activeTextEditor
                              (.-selection))]
    (-> (p/let [selected-text (some-> vscode/window.activeTextEditor
                                      (.-document)
                                      (.getText selection))]
          (sci/eval-string* @!ctx selected-text))
        (p/handle (fn [result error]
                    (if error
                      (js/console.error "Evaluate Selection Failed: " (.-message error))
                      result))))
    (vscode/window.showInformationMessage "There is no current document, so no selection")))

(comment
  (run-workspace-script+)
  (run-workspace-script+ ".joyride/scripts/hello.cljs"))

(defn ^:export activate [^js context]
  (register-command context "joyride.runScript" #'run-script)
  (register-command context "joyride.runWorkspaceScript" #'run-workspace-script+)
  (register-command context "joyride.loadCurrentFile" #'load-current-file+)
  (register-command context "joyride.evaluateSelection" #'evaluate-selection+))

(defn ^:export deactivate [])

(comment)
