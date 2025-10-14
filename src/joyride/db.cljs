(ns joyride.db
  (:require
   ["vscode" :as vscode]))

(def init-db {:output-channel nil
              :output/terminal nil
              :extension-context nil
              :invoked-script nil
              :disposables []
              :workspace-root-path nil
              :flares {}
              :flare-sidebar-views {}})

; dissoc :extension-context when dereffing `!app-db` in the repl.
(defonce !app-db (atom init-db))

(defn extension-context
  "Returns the Joyride ExtensionContext instance.
   See: https://code.visualstudio.com/api/references/vscode-api#ExtensionContext"
  []
  (:extension-context @!app-db))

(defn invoked-script
  "Returns the absolute path of the invoked script when the evaluation is made
   through *Run Script*, otherwise returns `nil`."
  []
  (:invoked-script @!app-db))

(defn ^{:deprecated "0.0.67"} output-channel
  "DEPRECATED: Returns the Joyride OutputChannel instance.
   We still need to support it because it is used by a lot of scripts out there."
  []
  (if (:output-channel @!app-db)
    (:output-channel @!app-db)
    (do
      (swap! !app-db assoc :output-channel (vscode/window.createOutputChannel "Joyride"))
      (.appendLine (:output-channel @!app-db) "This is the old Joyride output destination.\nYou probably should be using `println` instead, to write the the Joyride output terminal.")
      (.appendLine (:output-channel @!app-db) "")
      (:output-channel @!app-db))))

(defn output-terminal
  "Returns the Joyride Output terminal instance."
  []
  (:output/terminal @!app-db))
