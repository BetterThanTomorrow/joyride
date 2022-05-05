(ns joyride-api
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))


(def joyride (vscode/extensions.getExtension "betterthantomorrow.joyride"))

(def joyApi (.-exports joyride))

(comment
  
  ;; Starting the nREPL server
  (-> (.startNReplServer joyApi)
      (p/catch #(println %)))
  
  ;; Getting contexts
  (.getContextValue joyApi "joyride.isNReplServerRunning")

  ;; Non-Joyride context keys returns nil
  (.getContextValue joyApi "foo.bar")

  ;; NB: Use the extension instance for this!
  (.getContextValue joyApi "joyride.isActive")
  ;; Like so:
  (.-isActive joyride)
  )