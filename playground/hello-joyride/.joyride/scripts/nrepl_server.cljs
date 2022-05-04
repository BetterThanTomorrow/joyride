(ns nrepl_server
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))


(comment
  (def joyride (vscode/extensions.getExtension "betterthantomorrow.joyride"))
  (def joyApi (.-exports joyride))
  (-> (.startNReplServer joyApi)
      (p/catch #()))
  )