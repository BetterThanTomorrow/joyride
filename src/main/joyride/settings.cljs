(ns joyride.settings
  (:require ["os" :as os]
            ["path" :as path]))

;; TODO: Pick these from settings

(def user-scripts-path (str (path/join (os/homedir) ".joyride/scripts")))
(def workspace-scripts-path ".joyride/scripts")
