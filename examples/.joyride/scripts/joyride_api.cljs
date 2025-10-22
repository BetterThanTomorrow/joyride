(ns joyride-api
  (:require ["vscode" :as vscode]
            ["ext://betterthantomorrow.joyride" :as joy-api]
            [promesa.core :as p]
            [joyride.core :as joyride]))

(def joyrideExt (vscode/extensions.getExtension "betterthantomorrow.joyride"))

(comment
  ;; Starting the nREPL server
  (-> (joy-api/startNReplServer)
      (p/catch (fn [e] (println (.-message e) e))))
  ;; (Oh, yes, it's already started, of course.)
  ;; Try first stopping the server? That will not help,
  ;; because you also need to disconnect from it before
  ;; it is stopped for real.
  )

(comment
  ;; Getting contexts
  (joy-api/getContextValue "joyride.isNReplServerRunning")

  ;; Non-Joyride context keys returns `nil`
  (joy-api/getContextValue "foo.bar")

  ;; NB: Use the extension instance for this!
  (joy-api/getContextValue "joyride.isActive")
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
  (doc joyride/extension-context))

;; in addition to the extension context, joyride.core also has:
;; * *file*             - the absolute path of the file where an
;;                        evaluation takes place
;; * invoked-script - the absolute path of a script being run
;;                        `nil` in other execution contexts
;; * output-terminal - Joyride's output channel

(ns your-awesome-script
  (:require [joyride.core :as joyride]
            ...))

(.show (joyride/output-terminal))
(print "Writing to the ")
(println "Joyride output channel.")
(println "Joyride extension path: " (-> (joyride/extension-context)
                                        .-extension
                                        .-extensionPath))
(println "joyride/*file*: " joyride/*file*)
(println "Invoked script: " (joyride/invoked-script))
(println "🎉")

;; Try both invoking this file as a script, and loading it in the REPL