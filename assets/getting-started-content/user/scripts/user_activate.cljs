(ns user-activate
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            joy-button
            [promesa.core :as p]
            :reload))

(def show-demo-joyride-button false) ; Initialize to `true` to enable demo button, save
                                     ; Then issue the command *Joyride: Run User Script*
                                     ; and select the activation

;; This is the Joyride User `activate.cljs` script. It will run
;; as the first thing when Joyride is activated, making it a good
;; place for you to initialize things. E.g. install event handlers,
;; print motivational messages, or whatever.

;; You can run this and other User scripts with the VS Code command:
;;   *Joyride: Run User Script*

;;;;;;;;;;;;;;;;;
;;; REPL practice
(comment
  ;; To work with the scripts interactively, install the Calva Extension
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
      (println "You choose: " choice " 🎉")
      (println "You just closed it? 😭")))

  ;; New to Calva and/or Clojure? Use the Calva command:
  ;;   *Calva: Start Joyride REPL and Connect*
  ;; It will guide you through the basics.
  :rcf)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; user_activate.cljs skeleton

;; Keep tally on VS Code disposables we register
(defonce !db (atom {:disposables []}))

;; To make the activation script re-runnable we dispose of
;; event handlers and such that we may have registered
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

  (if show-demo-joyride-button
    (push-disposable! (joy-button/install!))
    (println "Demo Joyr button disabled"))
  )

(when (= (joyride/invoked-script) joyride/*file*)
  (my-main))

;; For more examples see:
;;   https://github.com/BetterThanTomorrow/joyride/tree/master/examples


"🎸"
