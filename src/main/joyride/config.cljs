(ns joyride.config
  (:require ["os" :as os]
            ["path" :as path]))

(def user-config-path (path/join (os/homedir) ".config"))
(def user-scripts-path (path/join "joyride" "scripts"))
(def workspace-scripts-path (path/join ".joyride" "scripts"))
