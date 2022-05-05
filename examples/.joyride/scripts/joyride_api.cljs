(ns joyride-api
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(def joyride (vscode/extensions.getExtension "betterthantomorrow.joyride"))

(def joyApi (.-exports joyride))

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
  
  ;; Non-Joyride context keys returns nil
  (.getContextValue joyApi "foo.bar")

  ;; NB: Use the extension instance for this!
  (.getContextValue joyApi "joyride.isActive")
  ;; Like so:
  (.-isActive joyride)
  ;; (Not that it matters, you can't deactivate Joyride,
  ;;  and you can't talk to a deactivated REPL server.)
  )

