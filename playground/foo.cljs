(ns foo
  (:require ["vscode" :as vscode]
            [joyride.core :as j]))

#_(in-ns 'hello)
(def foo :foo)
(def f j/*file*)

(defn main []
  (vscode/window.showInformationMessage "Joyride says hello from a selection! 👋"))

(comment
  j/*file*)

(when :invoked-file=*file*
  (main))