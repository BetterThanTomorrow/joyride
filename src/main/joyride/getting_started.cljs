(ns joyride.getting-started
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.utils :as utils]
            [promesa.core :as p]))

(defn- getting-started-user-content-uri [& sub-path]
  (let [dir-uri (vscode/Uri.file
                 (path/join (utils/extension-path) "assets" "getting-started-content"))]
    (if sub-path
      (apply (.-joinPath vscode/Uri) dir-uri sub-path)
      dir-uri)))

(defn- maybe-create-user-content-file+ [& sub-path]
  (p/let [user-dir-uri ^js (-> (conf/user-abs-scripts-path)
                               (vscode/Uri.file))
          source-uri ^js (apply getting-started-user-content-uri sub-path)
          dest-uri (apply (.-joinPath vscode/Uri) user-dir-uri sub-path)
          dest-uri-exists? (utils/path-or-uri-exists?+ dest-uri)]
    (when-not dest-uri-exists?
      (js/console.info "Creating " ^String (.-fsPath dest-uri))
      (vscode/workspace.fs.copy source-uri dest-uri))))

(defn maybe-create-user-content+ []
  (p/do! (maybe-create-user-content-file+ "activate.cljs")
         (maybe-create-user-content-file+ "hello_joyride_user_script.cljs")))
