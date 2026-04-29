(ns open-document
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

(defn ^:async show-random-example []
  (let [examples (await (vscode/workspace.findFiles ".joyride/scripts/**/*.cljs"))
        random-pick (rand-nth examples)
        doc (await (vscode/workspace.openTextDocument random-pick))]
    (await (vscode/window.showTextDocument doc
                                           #js {:preview false, :preserveFocus false}))))

(defn ^:async my-main []
  (let [choice (await (vscode/window.showInformationMessage "Welcome to the Joyride examples workspace. Want to look at a random example?" "Yes" "No"))]
    (case choice
      "Yes" (await (show-random-example))
      "No" :no
      :none)))

(when (= (joyride/invoked-script) joyride/*file*)
  (my-main))