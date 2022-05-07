(ns joyride.config
  (:require ["os" :as os]
            ["path" :as path]))

(def user-config-path (path/join (os/homedir) ".config"))
(def user-scripts-path (path/join "joyride" "scripts"))
(def workspace-scripts-path (path/join ".joyride" "scripts"))

(def user-init-script "activate.cljs")
(def workspace-init-script "activate.cljs")
(def init-scripts {:user {:label "User activate"
                          :script user-init-script
                          :script-path (path/join user-config-path
                                                  user-scripts-path
                                                  user-init-script)}
                   :workspace {:label "Workspace activate"
                               :script user-init-script
                               :script-path (path/join workspace-scripts-path
                                                       workspace-init-script)}})
