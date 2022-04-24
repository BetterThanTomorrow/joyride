(ns congas.extension
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["vscode" :as vscode]
   [promesa.core :as p]
   [sci.core :as sci]))

(defn register-disposable [^js context ^js disposable]
  (js/console.log "BOOM!!!!!!")
  (js/console.log "BOOM ctx" (some? context) "disp" (some? disposable))
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
(defn ^:export activate [^js context]
  (aset js/globalThis "vscode" vscode)
  (setup-command context #'run-script))

(defn ^:export deactivate []
  )

(comment)
