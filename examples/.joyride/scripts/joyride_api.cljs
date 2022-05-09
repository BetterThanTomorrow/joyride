(ns joyride-api
  (:require ["vscode" :as vscode]
            [promesa.core :as p]
            [joyride.core :as joyride]))

(def joyrideExt (vscode/extensions.getExtension "betterthantomorrow.joyride"))
(def joyApi (.-exports joyrideExt))

(comment
  ;; Starting the nREPL server
  (-> (.startNReplServer joyApi)
      (p/catch (fn [e] (println (.-message e) e))))
  ;; (Oh, yes, it's already started, of course.)
  ;; Try first stopping the server? That will not help,
  ;; because you also need to disconnect from it before
  ;; it is stopped for real.
  )

(comment
  ;; Getting contexts
  (.getContextValue joyApi "joyride.isNReplServerRunning")
  
  ;; Non-Joyride context keys returns `nil`
  (.getContextValue joyApi "foo.bar")

  ;; NB: Use the extension instance for this!
  (.getContextValue joyApi "joyride.isActive")
  ;; Like so:
  (.-isActive joyrideExt)
  ;; (Not that it matters, you can't deactivate Joyride,
  ;;  and you can't talk to a deactivated REPL server.)
  )

(comment
  ;; Joyride scripts can also reach the Joyride extension
  ;; through `joyride.core`
  (-> (joyride/extension-context)
      .-extension
      .-exports)
  (require '[clojure.repl :refer [doc]])
  (doc joyride/extension-context)
  )

;; in addition to the extension context, joyride.core also has:
;; * *file*             - the absolute path of the file where an
;;                        evaluation takes place
;; * invoked-script - the absolute path of a script being run
;;                        `nil` in other execution contexts
;; * output-channel - Joyride's output channel

(doto (joyride/output-channel)
  (.show true)
  (.append "Writing to the ")
  (.appendLine "Joyride output channel.")
  (.appendLine (str "Joyride extension path: "
                    (-> (joyride/extension-context)
                        .-extension
                        .-extensionPath)))
  (.appendLine (str "joyride/*file*: " joyride/*file*))
  (.appendLine (str "Invoked script: " (joyride/invoked-script)))
  (.appendLine "🎉"))

;; Try both invoking this file as a script, and loading it in the REPL