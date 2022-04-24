(ns congas.extension
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["vscode" :as vscode]
   [promesa.core :as p]
   [sci.core :as sci]))

(defn register-disposable [^js context ^js disposable]
  (.push context.subscriptions disposable))

(defn- register-command [command]
  (vscode/commands.registerCommand (-> command meta :command) #(command)))

(defn- setup-command [^js context command]
  (->> (register-command command)
       (register-disposable context)))

(def ctx (sci/init {:classes {'js goog/global
                              :allow :all}}))

(defn ^{:command "congas.runScript"} run-script [& script]
  (println "BOOM!")
  (let [ws-folder ^js (first js/vscode.workspace.workspaceFolders)
        ws-root ws-folder.uri.fsPath]
    (sci/eval-string* ctx
                      "(js/vscode.window.showInformationMessage \"Hello from SCI!!!!!!\")"
                      #_(fs/readFileSync (path/resolve ws-root ".congas/scripts/hello.cljs"))))
  )
; /Users/pez/Desktop/empty/congas/scripts/hello.cljs
(defn activate [^js context]
  (aset js/globalThis "vscode" vscode)
  (setup-command context #'run-script))

(comment)
