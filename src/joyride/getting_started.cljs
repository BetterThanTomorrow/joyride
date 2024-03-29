(ns joyride.getting-started
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as string]
            [joyride.config :as conf]
            [joyride.utils :as utils]
            [promesa.core :as p]))

(defn- path->uri [base-path sub-path]
  (apply (.-joinPath vscode/Uri) (vscode/Uri.file base-path) sub-path))

(defn- getting-started-content-uri [sub-path]
  (path->uri (utils/extension-path) (concat ["assets" "getting-started-content"] sub-path)))

(defn- create-content-file+ [source-uri ^js destination-uri]
  (js/console.info "Creating " ^String (.-fsPath destination-uri))
  (p/do (vscode/workspace.fs.createDirectory (->> destination-uri
                                                  .-fsPath
                                                  path/dirname
                                                  vscode/Uri.file))
        (vscode/workspace.fs.copy source-uri destination-uri)
        destination-uri))

(defn- maybe-create-content+
  "Copies `source-uri` to `destination-uri` if the latter does not exist.
   Returns nil if `destination-uri` already exists."
  [source-uri destination-uri]
  (p/let [exists?+ (utils/path-or-uri-exists?+ destination-uri)]
    (when-not exists?+
      (create-content-file+ source-uri destination-uri))))

(defn maybe-create-user-content+ []
  (maybe-create-content+ (getting-started-content-uri ["user" "deps.edn"])
                         (path->uri (conf/user-abs-joyride-path) ["deps.edn"]))
  (p/let [user-activate-uri (path->uri (conf/user-abs-scripts-path) ["scripts" "user_activate.cljs"])
          user-activate-exists?+ (utils/path-or-uri-exists?+ user-activate-uri)]
    (when-not user-activate-exists?+
      (maybe-create-content+ (getting-started-content-uri ["user" "scripts" "user_activate.cljs"])
                             (path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))
      (maybe-create-content+ (getting-started-content-uri ["user" "scripts" "hello_joyride_user_script.cljs"])
                             (path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))
      (maybe-create-content+ (getting-started-content-uri ["user" "scripts" "hello_joyride_user_script.js"])
                             (path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.js"]))
      (maybe-create-content+ (getting-started-content-uri ["user" "src" "my_lib.cljs"])
                             (path->uri (conf/user-abs-src-path) ["my_lib.cljs"])))))

(defn maybe-create-workspace-config+ [create-joyride-dir?]
  (p/let [joyride-dir-exists?+ (utils/path-or-uri-exists?+ (path->uri (conf/workspace-abs-joyride-path) "."))]
    (when (or joyride-dir-exists?+ create-joyride-dir?)
      (p/let [deps-uri (path->uri (conf/workspace-abs-joyride-path) ["deps.edn"])
              _created?+ (maybe-create-content+ (getting-started-content-uri ["workspace" "deps.edn"])
                                                deps-uri)]
        deps-uri))))

(defn create-and-open-content-file+ [source destination]
  (fn []
    (p/-> (create-content-file+ source destination)
          (vscode/workspace.openTextDocument)
          (vscode/window.showTextDocument
           #js {:preview false, :preserveFocus false}))))

(defn maybe-create-and-open-content+ [source destination]
  (p/let [exists?+ (utils/path-or-uri-exists?+ destination)]
    (when-not exists?+
      (create-and-open-content-file+ source destination))))

(defn maybe-create-workspace-activate-fn+ []
  (p/do
    (maybe-create-workspace-config+ true)
    (maybe-create-and-open-content+ (getting-started-content-uri ["workspace" "scripts" "workspace_activate.cljs"])
                                    (path->uri (conf/workspace-abs-scripts-path) ["workspace_activate.cljs"]))))

(defn maybe-create-workspace-hello-fn+ []
  (p/do
    (maybe-create-workspace-config+ true)
    (maybe-create-and-open-content+ (getting-started-content-uri ["workspace" "scripts" "hello_joyride_workspace_script.cljs"])
                                    (path->uri (conf/workspace-abs-scripts-path) ["hello_joyride_workspace_script.cljs"]))))