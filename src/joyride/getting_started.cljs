(ns joyride.getting-started
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as string]
            [joyride.config :as conf]
            [joyride.utils :as utils]
            [joyride.when-contexts :as when-contexts]
            [promesa.core :as p]))

(defn- getting-started-content-uri [sub-path]
  (utils/path->uri (utils/extension-path) (concat ["assets" "getting-started-content"] sub-path)))

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
                         (utils/path->uri (conf/user-abs-joyride-path) ["deps.edn"]))
  (p/let [user-activate-uri (utils/path->uri (conf/user-abs-scripts-path) ["scripts" "user_activate.cljs"])
          user-activate-exists?+ (utils/path-or-uri-exists?+ user-activate-uri)]
    (when-not user-activate-exists?+
      (maybe-create-content+ (getting-started-content-uri ["user" "scripts" "user_activate.cljs"])
                             (utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))
      (maybe-create-content+ (getting-started-content-uri ["user" "scripts" "hello_joyride_user_script.cljs"])
                             (utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))
      (maybe-create-content+ (getting-started-content-uri ["user" "scripts" "hello_joyride_user_script.js"])
                             (utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.js"]))
      (maybe-create-content+ (getting-started-content-uri ["user" "src" "my_lib.cljs"])
                             (utils/path->uri (conf/user-abs-src-path) ["my_lib.cljs"])))))

(defn maybe-create-user-readme+ []
  (maybe-create-content+ (getting-started-content-uri ["user" "README.md"])
                         (utils/path->uri (conf/user-abs-joyride-path) ["README.md"])))

(defn maybe-create-workspace-config+ [create-joyride-dir?]
  (p/let [joyride-dir-exists?+ (utils/path-or-uri-exists?+ (utils/path->uri (conf/workspace-abs-joyride-path) "."))]
    (when (or joyride-dir-exists?+ create-joyride-dir?)
      (p/let [deps-uri (utils/path->uri (conf/workspace-abs-joyride-path) ["deps.edn"])
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
                                    (utils/path->uri (conf/workspace-abs-scripts-path) ["workspace_activate.cljs"]))))

(defn maybe-create-workspace-hello-fn+ []
  (p/do
    (maybe-create-workspace-config+ true)
    (maybe-create-and-open-content+ (getting-started-content-uri ["workspace" "scripts" "hello_joyride_workspace_script.cljs"])
                                    (utils/path->uri (conf/workspace-abs-scripts-path) ["hello_joyride_workspace_script.cljs"]))))

(defn update-script-contexts!
  "Updates VS Code context variables based on current script file existence"
  []
  (p/let [user-activate-exists? (utils/path-or-uri-exists?+
                                 (utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))
          user-hello-exists? (utils/path-or-uri-exists?+
                              (utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))
          ;; Workspace contexts - only check if workspace exists
          ws-scripts-path (conf/workspace-abs-scripts-path)
          ws-activate-exists? (when ws-scripts-path
                                (utils/path-or-uri-exists?+
                                 (utils/path->uri ws-scripts-path ["workspace_activate.cljs"])))
          ws-hello-exists? (when ws-scripts-path
                             (utils/path-or-uri-exists?+
                              (utils/path->uri ws-scripts-path ["hello_joyride_workspace_script.cljs"])))]
    ;; Update all contexts
    (when-contexts/set-context! ::when-contexts/joyride.userActivateScriptExists user-activate-exists?)
    (when-contexts/set-context! ::when-contexts/joyride.userHelloScriptExists user-hello-exists?)
    (when-contexts/set-context! ::when-contexts/joyride.workspaceActivateScriptExists (boolean ws-activate-exists?))
    (when-contexts/set-context! ::when-contexts/joyride.workspaceHelloScriptExists (boolean ws-hello-exists?))))

;; Command handlers for individual script creation

(defn create-user-activate-script+
  "Creates user activate script and updates contexts"
  []
  (p/let [source-uri (getting-started-content-uri ["user" "scripts" "user_activate.cljs"])
          _deps-result (maybe-create-content+ (getting-started-content-uri ["user" "deps.edn"])
                                 (utils/path->uri (conf/user-abs-joyride-path) ["deps.edn"]))
          dest-uri (utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"])
          _result (create-content-file+ source-uri dest-uri)]
    (update-script-contexts!)
    (p/let [doc+ (vscode/workspace.openTextDocument dest-uri)]
      (vscode/window.showTextDocument doc+ #js {:preview false, :preserveFocus false}))))

(defn create-user-hello-script+
  "Creates user hello script and updates contexts"
  []
  (p/let [source-uri (getting-started-content-uri ["user" "scripts" "hello_joyride_user_script.cljs"])
          _deps-result (maybe-create-content+ (getting-started-content-uri ["user" "deps.edn"])
                                              (utils/path->uri (conf/user-abs-joyride-path) ["deps.edn"]))
          dest-uri (utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"])
          _result (create-content-file+ source-uri dest-uri)]
    (update-script-contexts!)
    (p/let [doc+ (vscode/workspace.openTextDocument dest-uri)]
      (vscode/window.showTextDocument doc+ #js {:preview false, :preserveFocus false}))))

(defn create-workspace-activate-script+
  "Creates workspace activate script and updates contexts"
  []
  (p/do
    (maybe-create-workspace-config+ true)
    (p/let [source-uri (getting-started-content-uri ["workspace" "scripts" "workspace_activate.cljs"])
            dest-uri (utils/path->uri (conf/workspace-abs-scripts-path) ["workspace_activate.cljs"])
            _result+ (create-content-file+ source-uri dest-uri)]
      (update-script-contexts!)
      (p/let [doc+ (vscode/workspace.openTextDocument dest-uri)]
        (vscode/window.showTextDocument doc+ #js {:preview false, :preserveFocus false})))))

(defn create-workspace-hello-script+
  "Creates workspace hello script and updates contexts"
  []
  (p/do
    (maybe-create-workspace-config+ true)
    (p/let [source-uri (getting-started-content-uri ["workspace" "scripts" "hello_joyride_workspace_script.cljs"])
            dest-uri (utils/path->uri (conf/workspace-abs-scripts-path) ["hello_joyride_workspace_script.cljs"])
            _result+ (create-content-file+ source-uri dest-uri)]
      (update-script-contexts!)
      (p/let [doc+ (vscode/workspace.openTextDocument dest-uri)]
        (vscode/window.showTextDocument doc+ #js {:preview false, :preserveFocus false})))))