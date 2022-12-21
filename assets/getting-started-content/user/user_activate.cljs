(ns user-activate
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

;; This is the Joyride User `activate.cljs` script. It will run
;; as the first thing when Joyride is activated, making it a good
;; place for you to initialize things. E.g. install event handlers,
;; print motivational messages, or whatever.

;; You can run this and other User scripts with the command:
;;   *Joyride: Run User Script*

;;; REPL practice
(comment
  ;; Use Calva to start the Joyride REPL and connect it. The command:
  ;;   *Calva: Start Joyride REPL and Connect*
  ;; Then evaluate some code in this Rich comment block. Or just write
  ;; some new code and evaluate that. Or whatever. It's fun!

  ;; New to Calva and/or Clojure? Use the Calva command:
  ;;   *Calva: Fire up the Getting Started REPL*
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

;;; Output channel pop-up
;; This following code is why you see the Joyride output channel
;; on startup.

(doto (joyride/output-channel)
  (.show true) ;; specifically this line. It shows the channel.
  (.appendLine "Welcome Joyrider! This is your User activation script speaking.")
  (.appendLine "Tired of this message popping up? It's the script doing it. Edit it away!")
  (.appendLine "Hint: There is a command: **Open User Script...**"))


;;; user_activate.cljs skeleton

;; Keep tally on VS Code disposables we register
(defonce !db (atom {:disposables []}))

;; To make the activation script re-runnable we dispose of
;; event handlers and such that we might have registered
;; in previous runs.
(defn- clear-disposables! []
  (run! (fn [disposable]
          (.dispose disposable))
        (:disposables @!db))
  (swap! !db assoc :disposables []))

;; Pushing the disposables on the extension context's
;; subscriptions will make VS Code dispose of them when the
;; Joyride extension is deactivated.
(defn- push-disposable! [disposable]
  (swap! !db update :disposables conj disposable)
  (-> (joyride/extension-context)
      .-subscriptions
      (.push disposable)))

(defn- register-problem-hover []
  (push-disposable! (#_{:clj-kondo/ignore [:unresolved-symbol]}
                     (requiring-resolve 'problem-hover/register-diagnostics-handler!)))
  (p/do
    (.appendLine (joyride/output-channel) "Problem Hover Provider: Waiting a bit to register (to get to be topmost hover)...")
    (p/delay 5000)
    (push-disposable! (#_{:clj-kondo/ignore [:unresolved-symbol]}
                       (requiring-resolve 'problem-hover/register-provider!)))
    (.appendLine (joyride/output-channel) "Problem Hover Provider: Registered!")))

(defn- my-main []
  (println "Hello World, from my-main in user_activate.cljs script")
  (clear-disposables!) ;; Any disposables add with `push-disposable!`
                       ;; will be cleared now. You can push them anew.

  ;; To register a diagnostics hover item above the fold, un-ignore this:
  #_(register-problem-hover)

  ;;; require my-lib
  (require '[my-lib])

  ;;; require VS Code extensions
  ;; In an activation.cljs script it can't be guaranteed that a
  ;; particular extension is active, so we can't safely `(:require ..)`
  ;; in the `ns` form. Here's what you can do instead, using Calva
  ;; as the example. To try it for real, copy the example scripts from:
  ;; https://github.com/BetterThanTomorrow/joyride/tree/master/examples 
  ;; Then un-ignore the below form and run
  ;;   *Joyride; Run User Script* -> user_activate.cljs
  ;; (Or reload the VS Code window.)
  #_(-> (vscode/extensions.getExtension "betterthantomorrow.calva")
      ;; Force the Calva extension to activate 
        (.activate)
      ;; The promise will resolve with the extension's API as the result
        (p/then (fn [_api]
                  (.appendLine (joyride/output-channel) "Calva activated. Requiring dependent namespaces.")
                ;; In `my-lib` and  `calva-api` the Calva extension
                ;; is required, which will work fine since now Calva is active.
                  (require '[calva-api])
                  (require '[clojuredocs])
                ;; Code in your keybindings can now use the `my-lib` and/or
                ;; `calva-api` namespace(s)
                  ))
        (p/catch (fn [error]
                   (vscode/window.showErrorMessage (str "Requiring Calva failed: " error))))))

(when (= (joyride/invoked-script) joyride/*file*)
  (my-main))

"🎉"

;; For more examples see:
;;   https://github.com/BetterThanTomorrow/joyride/tree/master/examples

(comment
  (js-keys (second (:disposables @!db)))
  :rcf)

