(ns joyride.lm
  "Main entry point for Language Model tools"
  (:require
   [joyride.lm.eval :as eval]
   [joyride.lm.docs :as docs]))

(defn register-tools!
  "Register all Language Model tools with VS Code's API.
   Returns a collection of disposables for lifecycle management."
  []
  (try
    (let [eval-disposable (eval/register-tool!)
          docs-disposable (docs/register-tool!)]
      (js/console.log "Joyride LM Tools registered successfully")
      [eval-disposable docs-disposable])
    (catch js/Error e
      (js/console.error "Failed to register Joyride LM Tools:" (.-message e))
      [])))
