(ns user-activate
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

;; This is the Joyride User `activate.cljs` script. It will run
;; as the first thing when Joyride is activated, making it a good
;; place for you to initialize things. E.g. install event handlers,
;; print motivational messages, or whatever.

;; You can run this and other User scripts with the VS Code command:
;;   *Joyride: Run User Script*

;;;;;;;;;;;;;;;;;
;;; REPL practice
(comment
  ;; Use Calva to start the Joyride REPL and connect it. The command:
  ;;   *Calva: Start Joyride REPL and Connect*
  ;; Then evaluate some code in this Rich comment block. Or just write
  ;; some new code and evaluate that. Or whatever. It's fun!

  (-> 4
      (* 10)
      (+ 1)
      inc)

  (p/let [choice (vscode/window.showInformationMessage "Be a Joyrider 🎸" "Yes" "Of course!")]
    (if choice
      (.appendLine (joyride/output-channel)
                   (str "You choose: " choice " 🎉"))
      (.appendLine (joyride/output-channel)
                   "You just closed it? 😭")))

  ;; New to Calva and/or Clojure? Use the Calva command:
  ;;   *Calva: Start Joyride REPL and Connect*
  ;; It will guide you through the basics.
  :rcf)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

(defn- my-main []
  (println "Hello World, from my-main in user_activate.cljs script")
  (clear-disposables!) ;; Any disposables add with `push-disposable!`
                       ;; will be cleared now. You can push them anew.

  (push-disposable! (#_{:clj-kondo/ignore [:unresolved-symbol]}
                     (requiring-resolve 'highlight-thousands/activate!)))

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
                  ;; The Calva extension is required from`calva-api`
                  ;; which will work fine since now Calva is active.
                  (require '[calva-api])
                  (require '[clojuredocs])))
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

