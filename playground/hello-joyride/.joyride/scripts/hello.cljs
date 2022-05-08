(ns hello
  (:require ["vscode" :as vscode]
            [joyride.core :as j]))

(def foo :foo)
(def f j/*file*)

(defn main []
  (vscode/window.showInformationMessage "Joyride says hello from a selection! 👋"))

(comment
  j/*file*

  (def ext-ctx (joyride/get-extension-context))
  (.-extensionPath ext-ctx)
  )

(when :invoked-file=*file*
  (main))