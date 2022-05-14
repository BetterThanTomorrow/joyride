(ns joyride.getting-started
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.utils :as utils]
            [promesa.core :as p]))

(defn- getting-started-content-uri [section & sub-path]
  (let [dir-uri (vscode/Uri.file
                 (path/join (utils/extension-path)
                            "assets"
                            "getting-started-content"
                            section))]
    (if sub-path
      (apply (.-joinPath vscode/Uri) dir-uri sub-path)
      dir-uri)))

(defn destination-uri [base-path _section & sub-path]
  (p/let [base-path-uri ^js (vscode/Uri.file base-path)]
    (apply (.-joinPath vscode/Uri) base-path-uri sub-path)))

(defn- create-content-file+ [dest-uri _base-path section & sub-path]
  (p/let [source-uri ^js (apply getting-started-content-uri section sub-path)]
    (js/console.info "Creating " ^String (.-fsPath dest-uri))
    (vscode/workspace.fs.copy source-uri dest-uri)))

(defn user-activate-uri-section-and-subpath []
  [(conf/user-abs-scripts-path)
   "user"
   "activate.cljs"])

(defn user-hello-uri-section-and-subpath []
  [(conf/user-abs-scripts-path)
   "user"
   "hello_joyride_user_script.cljs"])

(defn workspace-activate-uri-section-and-subpath []
  [(conf/workspace-abs-scripts-path)
   "workspace"
   "activate.cljs"])

(defn workspace-hello-uri-section-and-subpath []
  [(conf/workspace-abs-scripts-path)
   "workspace" 
   "hello_joyride_workspace_script.cljs"])

(defn- dest-uri-uri-exists?+ [section-and-subpath]
  (p/let [dest-uri (apply destination-uri section-and-subpath)
          exists?+ (utils/path-or-uri-exists?+ dest-uri)]
    [dest-uri exists?+]))

(defn maybe-create-user-content+ []
  (p/let [section-and-subpath (user-activate-uri-section-and-subpath)
          [activate-dest-uri activate-exists?+] (dest-uri-uri-exists?+ section-and-subpath)]
    (when-not activate-exists?+
      (apply (partial create-content-file+ activate-dest-uri) section-and-subpath)))
  (p/let [section-and-subpath (user-hello-uri-section-and-subpath)
          [hello-dest-uri hello-exists?+] (dest-uri-uri-exists?+ section-and-subpath)]
    (when-not hello-exists?+ 
      (apply (partial create-content-file+ hello-dest-uri) section-and-subpath))))

(defn maybe-create-workspace-activate-fn+ []
  (p/let [section-and-subpath (workspace-activate-uri-section-and-subpath)
          [activate-dest-uri activate-exists?+] (dest-uri-uri-exists?+ section-and-subpath)]
    (when-not activate-exists?+
      (fn [] 
        (apply (partial create-content-file+ activate-dest-uri) section-and-subpath)))))