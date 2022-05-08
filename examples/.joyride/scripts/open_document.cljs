(ns open-document
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(defn show-random-example []
  (p/let [examples (vscode/workspace.findFiles ".joyride/scripts/**/*.cljs")
          random-pick (rand-nth examples)]
    (p/-> (vscode/workspace.openTextDocument random-pick)
          (vscode/window.showTextDocument
           #js {:preview false, :preserveFocus false}))))

(defn my-main []
  (p/-> (vscode/window.showInformationMessage "Welcome to the Joyride examples workspace. Want to look at a random example?" "Yes" "No")
        (p/then (fn [choice]
                  (case choice
                    "Yes" (show-random-example)
                    "No" :no
                    :none)))))

(when (= (joyride/get-invoked-script) joyride/*file*)
  (my-main))