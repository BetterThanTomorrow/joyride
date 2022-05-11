(ns joyride.config
  (:require ["os" :as os]
            ["path" :as path]
            [joyride.db :as db]))

(def user-config-path (path/join (os/homedir) ".config"))
(def user-scripts-path (path/join "joyride" "scripts"))
(defn user-abs-scripts-path 
  "Returns the absolute path to the User scripts directory
   It's a function because `workspace-abs-scripts-path` is."
  []
  (path/join user-config-path user-scripts-path))

(def workspace-scripts-path (path/join ".joyride" "scripts"))
(defn workspace-abs-scripts-path
  "Returns the absolute path to the Workspace scripts directory
   returns `nil` if there is not workspace root"
  []
  (when-let [workspace-root (:workspace-root-path db/!app-db)]
    (path/join workspace-root workspace-scripts-path)))