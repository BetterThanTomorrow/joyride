(ns joyride.lifecycle
  (:require ["path" :as path]
            [joyride.config :as conf]
            [joyride.vscode-utils :as vscode-utils]
            [promesa.core :as p]))

(def user-init-script "user_activate.cljs")
(def user-init-script-path (path/join conf/user-config-path
                                      conf/user-scripts-path
                                      user-init-script))

(def workspace-init-script "workspace_activate.cljs")
(def workspace-init-script-path (path/join conf/workspace-scripts-path
                                           workspace-init-script))
(defn workspace-init-script-abs-path []
  (when-let [abs-scripts-path (conf/workspace-abs-scripts-path)]
    (path/join abs-scripts-path
               workspace-init-script)))

(defn init-scripts []
  {:user {:label "User activate"
          :script user-init-script
          :script-path user-init-script-path
          :script-abs-path user-init-script-path}
   :workspace {:label "Workspace activate"
               :script workspace-init-script
               :script-path workspace-init-script-path
               :script-abs-path (workspace-init-script-abs-path)}})

(defn maybe-run-init-script+ [run-fn {:keys [label script script-path script-abs-path]}]
  (vscode-utils/say (str label " script: " script-path))
  (-> (vscode-utils/path-or-uri-exists?+ script-abs-path)
      (p/then (fn [exists?]
                (if exists?
                  (do
                    (vscode-utils/sayln (str ".  Running..."))
                    (run-fn script))
                  (vscode-utils/sayln (str ".  No " label " script present")))))))
