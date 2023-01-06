(ns example.write-a-file
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]))

(defn info [& xs]
  (vscode/window.showInformationMessage (str/join " " xs)))

(def root-path (-> (first vscode/workspace.workspaceFolders) .-uri .-fsPath))
(info "The root path of this workspace:" root-path)
(fs/writeFileSync (path/resolve root-path "test-from-cljs-script.txt") 
                  "Written from a Workspace ClojureScript Script!")