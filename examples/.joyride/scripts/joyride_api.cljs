(ns joyride-api
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

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
  (require '[joyride.core :as joyride])
  (-> (joyride/get-extension-context)
      .-extension
      .-exports))