(ns congas.extension
  (:require
   ["vscode" :as vscode]
   [sci-configs.funcool.promesa :as pconfig]
   [sci.core :as sci]))

(defn register-disposable [^js context ^js disposable]
  (.push (.-subscriptions context) disposable))

(def ctx (sci/init {:classes {'js goog/global
                              :allow :all}
                    :namespaces (:namespaces pconfig/config)}))

(defn run-script [& script]
  (let [ws-folder ^js (first js/vscode.workspace.workspaceFolders)
        ws-root (some-> ws-folder (.uri) (.fsPath))]
    (sci/eval-string* ctx
                      "(js/vscode.window.showInformationMessage \"Hello from SCI!!!!!!\")"
                      #_(fs/readFileSync (path/resolve ws-root ".congas/scripts/hello.cljs")))
    (sci/eval-form ctx
                   '(do (require '[promesa.core :as p])
                        (p/do
                          (p/delay 2000)
                          (js/vscode.window.showInformationMessage "Hello from SCI again!!!!!!"))))))

(defn- register-command []
  (vscode/commands.registerCommand "congas.runScript" run-script))

(defn- setup-command [^js context]
  (->> (register-command)
       (register-disposable context)))



; /Users/pez/Desktop/empty/congas/scripts/hello.cljs
(defn ^:export activate [^js context]
  (aset js/globalThis "vscode" vscode)
  (setup-command context))

(defn ^:export deactivate []
  )

(comment)
