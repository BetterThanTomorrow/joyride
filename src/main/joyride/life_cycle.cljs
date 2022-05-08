(ns joyride.life-cycle
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.utils :as utils]
            [promesa.core :as p]))

(def user-init-script "activate.cljs")
(def user-init-script-path (path/join conf/user-config-path
                                      conf/user-scripts-path
                                      user-init-script))
(def user-init-script-abs-path user-init-script-path)

(def workspace-init-script "activate.cljs")
(def workspace-init-script-path (path/join conf/workspace-scripts-path
                                           workspace-init-script))
(def workspace-init-script-abs-path (path/join vscode/workspace.rootPath
                                               workspace-init-script-path))

(def init-scripts {:user {:label "User activate"
                          :script user-init-script
                          :script-path user-init-script-path
                          :script-abs-path user-init-script-abs-path}
                   :workspace {:label "Workspace activate"
                               :script user-init-script
                               :script-path workspace-init-script-path
                               :script-abs-path workspace-init-script-abs-path}})

(defn maybe-run-init-script+ [run-fn {:keys [label script script-path script-abs-path]}]
  (utils/say (str label " script: " script-path))
  (-> (utils/path-exists?+ script-abs-path)
      (p/then (fn [exists?]
                (if exists?
                  (do
                    (utils/say (str "  Running..."))
                    (run-fn script))
                  (utils/say (str "  No " label " script present")))))))