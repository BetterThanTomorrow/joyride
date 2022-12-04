(ns ws
  (:require ["vscode" :as vscode]
            [promesa.core :as p]
            [util.workspace :refer [root slurp-file+]]))

(vscode/window.showInformationMessage (str "The workspace root is: " (root)) "OK")

(p/let [file-content (slurp-file+ ".joyride/scripts/ws.cljs")]
  (vscode/window.showInformationMessage (str "This src file contains: " (pr-str file-content)) "OK"))

