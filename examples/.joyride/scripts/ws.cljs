(ns ws
  (:require ["vscode" :as vscode]
            [util.workspace :refer [root slurp-file+]]))

(vscode/window.showInformationMessage (str "The workspace root is: " (root)) "OK")

((^:async fn []
   (let [file-content (await (slurp-file+ ".joyride/scripts/ws.cljs"))]
     (vscode/window.showInformationMessage (str "This src file contains: " (pr-str file-content)) "OK"))))

