(ns activate
  (:require ["vscode" :as vscode]
            [promesa.core :as p]
            [joyride.core :as joyride]))

;; This is the Joyride User `activate.cljs` script. It will run
;; as the first thing when Joyride is activated, making it a good
;; place for you to initialize things. E.g. install event handlers,
;; print motivational messages, or whatever.

;; You can run this and other User scripts with the command:
;;   *Joyride: Run User Script*

(comment
  ;; Use Calva to start the Joyride REPL and connect it. Then
  ;; evaluate some code in this Rich comment block. Or just write
  ;; sme new code and evaluate that. Or whatever. It's fun!

  ;; New to Calva and/or Clojure? Use the Calva command:
  ;;   *Fire up the Getting Started REPL*
  ;; It will guide you through the basics.

  (-> 4
      (* 10)
      (+ 1)
      inc)

  (p/let [choice (vscode/window.showInformationMessage "Be a Joyrider 🎸" "Yes" "Of course!")]
    (if choice
      (.appendLine (joyride/output-channel)
                   (str "You choose: " choice " 🎉"))
      (.appendLine (joyride/output-channel)
                   "You just closed it? 😭"))))


;; This following code is why you see the Joyride output channel
;; on startup.

(doto (joyride/output-channel)
  (.show true) ;; specifically this line. It shows the channel.
  (.appendLine "Welcome Joyrider! This is your User activation script speaking.")
  (.appendLine "Tired of this message popping up? It's the script doing it. Edit it away!"))

"🎉"

;; For more examples see:
;;   https://github.com/BetterThanTomorrow/joyride/tree/master/examples