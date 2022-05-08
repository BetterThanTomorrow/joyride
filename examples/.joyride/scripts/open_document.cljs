(ns open-document
  (:require [promesa.core :as p]
            ["vscode" :as vscode]))

(defn show-random-example []
  (p/let [examples (vscode/workspace.findFiles ".joyride/scripts/**/*.cljs")
          random-pick (rand-nth examples)]
    (p/-> (vscode/workspace.openTextDocument random-pick)
          (vscode/window.showTextDocument
           #js {:preview false, :preserveFocus false}))))

(p/-> (vscode/window.showInformationMessage "Welcome to the Joyride examples workspace. Want to look at a random example?" "Yes" "No")
      (p/then (fn [choice]
                (case choice
                  "Yes" (show-random-example)
                  "No" :no
                  :none))))
