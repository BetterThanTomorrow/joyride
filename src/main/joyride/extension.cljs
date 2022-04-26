(ns joyride.extension
  (:require
   ["vscode" :as vscode]
   [clojure.string :as str]
   [sci-configs.funcool.promesa :as pconfig]
   [sci.core :as sci]
   ["path" :as path]
   [promesa.core :as p]))

(defn register-disposable [^js context ^js disposable]
  (.push (.-subscriptions context) disposable))

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
            _ (def data data)
            decoder (js/TextDecoder. "utf-8")
            code (.decode decoder data)]
      code)
    (catch :default e
      (js/console.error "Rading file failed: " (.-message e)))))

;; TODO get this from settings
(def workspace-scripts-path ".joyride/scripts")

(defn run-workspace-script+ [script-path]
  (->
   (p/let [abs-path (path/join vscode/workspace.rootPath script-path)
           script-uri (vscode/Uri.file abs-path)
           code (vscode-read-uri+ script-uri)]
     (sci/eval-string code))
   (p/handle (fn [result error]
                       (if error
                         (js/console.error "Run Workspace Script Failed: " script-path (.-message error))
                         result)))))

(comment
  (run-workspace-script+ ".joyride/scripts/hello.cljs")
  )

(defn- register-command [command-id var]
  (vscode/commands.registerCommand command-id var))

(defn- setup-command [^js context command-id var]
  (->> (register-command command-id var)
       (register-disposable context)))

(defn ^:export activate [^js context]
  (setup-command context "joyride.runScript" #'run-script)
  (setup-command context "joyride.runWorkspaceScript" #'run-workspace-script+))

(defn ^:export deactivate [])

(comment)
