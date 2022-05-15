(ns joyride.db)

(def init-db {:output-channel nil
              :extension-context nil
              :invoked-script nil
              :disposables []
              :workspace-root-path nil})

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

(defn output-channel
  "Returns the Joyride OutputChannel instance.
   See: https://code.visualstudio.com/api/references/vscode-api#OutputChannel"
  []
  (:output-channel @!app-db))