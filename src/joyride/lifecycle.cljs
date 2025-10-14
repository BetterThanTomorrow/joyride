(ns joyride.lifecycle
  (:require ["path" :as path]
            [joyride.config :as conf]
            [joyride.vscode-utils :as vscode-utils]
            [promesa.core :as p]
            [joyride.output :as output]))

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
  (output/append-other-out (str label " script: " script-path))
  (-> (vscode-utils/path-or-uri-exists?+ script-abs-path)
      (p/then (fn [exists?]
                (if exists?
                  (do
                    (output/append-line-other-out (str ".  Running..."))
                    (run-fn script))
                  (output/append-line-other-out (str ".  No " label " script present")))))))
