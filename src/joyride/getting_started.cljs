(ns joyride.getting-started
  (:require
   ["vscode" :as vscode]
   [joyride.config :as conf]
   [joyride.content-utils :as content-utils]
   [joyride.vscode-utils :as vscode-utils]
   [promesa.core :as p]))

(defn create-and-open-content-file+ [source destination]
  (p/-> (content-utils/create-content-file+ source destination)
        (vscode/workspace.openTextDocument)
        (vscode/window.showTextDocument
         #js {:preview false, :preserveFocus false})))

(defn maybe-create-and-open-content+ [source destination]
  (p/let [exists?+ (vscode-utils/path-or-uri-exists?+ destination)]
    (when-not exists?+
      (create-and-open-content-file+ source destination))))

(defn maybe-create-workspace-activate-script+ []
  (p/do
    (content-utils/maybe-create-workspace-config+)
    (maybe-create-and-open-content+
     (content-utils/default-content-uri ["workspace" "scripts" "workspace_activate.cljs"])
     (vscode-utils/path->uri (conf/workspace-abs-scripts-path) ["workspace_activate.cljs"]))))

(defn maybe-create-workspace-hello-script+ []
  (p/do
    (content-utils/maybe-create-workspace-config+)
    (maybe-create-and-open-content+
     (content-utils/default-content-uri ["workspace" "scripts" "hello_joyride_workspace_script.cljs"])
     (vscode-utils/path->uri (conf/workspace-abs-scripts-path) ["hello_joyride_workspace_script.cljs"]))))

(defn maybe-create-user-activate-script+
  []
  (p/do
    (content-utils/maybe-create-user-config+)
    (maybe-create-and-open-content+
     (content-utils/default-content-uri ["user" "src" "joy_button.cljs"])
     (vscode-utils/path->uri (conf/user-abs-src-path) ["joy_button.cljs"]))
    (maybe-create-and-open-content+
     (content-utils/default-content-uri ["user" "scripts" "user_activate.cljs"])
     (vscode-utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))))

(defn maybe-create-user-hello-script+
  []
  (p/do
    (content-utils/maybe-create-user-config+)
    (content-utils/maybe-create-content+ (content-utils/default-content-uri ["user" "scripts" "hello_joyride_user_script.js"])
                                         (vscode-utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.js"]))
    (maybe-create-and-open-content+
     (content-utils/default-content-uri ["user" "scripts" "hello_joyride_user_script.cljs"])
     (vscode-utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))))