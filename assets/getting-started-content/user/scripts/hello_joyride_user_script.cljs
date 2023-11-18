(ns hello-joyride-user-script
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

;; This is an example Joyride User Script. You run it using the
;; command: *Joyride: Run User Script* and select this script
;; from the menu. Or you can bind a keyboard shortcut that
;; runs it. See the README at: 
;;    https://github.com/BetterThanTomorrow/joyride/
;; for how to do that. (And more)

(defn my-main []
  (p/let [choice (vscode/window.showInformationMessage "Hello World from a Joyride User script. Do you want to open the Joyride examples page in your browser" "Yes" "No")]
    (case choice
      "Yes" (vscode/env.openExternal (vscode/Uri.parse "https://github.com/BetterThanTomorrow/joyride/tree/master/examples"))
      "No" (doto (joyride/output-channel)
             (.show true)
             (.appendLine "Well, that page is here, if you change your mind:\n  https://github.com/BetterThanTomorrow/joyride/tree/master/examples"))
      (println "¯\\_(ツ)_/¯"))))

;; You probably also want to install the Calva VS Code Extension.
;; New to Calva and/or Clojure? Use the Calva command:
;;   *Fire up the Getting Started REPL*
;; It will guide you through the basics.

;; Use Calva to start the Joyride REPL and connect it.
;; There's a REPL button in the status bar that will show you
;; a menu where Joyride is one of the options.

;; Then evaluate some code in this Rich comment block. (Place
;; the cursor in some code and press `Alt/Option+Enter`)
;; Or write some new code and evaluate that. Or whatever.
;; It's fun!

(comment
  (-> 10
      (* 4)
      (+ 1)
      inc)

  (vscode/window.showInformationMessage (str "Hello, answer: " (+ (* 4 10) 1 1)) ()))

;; When loading this file in the REPL, using the command:
;;   *Calva: Load Current File and Dependencies*
;; the following call to `my-main` will not be made. Because
;; you are not invoking the file as a script.

(when (= (joyride/invoked-script) joyride/*file*)
  (my-main))

;; Call `my-main` by placing the cursor next to, and outside
;; the opening or closing parens, then press `Ctrl+Enter`.

"🎸"
