(ns joyride.lm
  "Main entry point for Language Model tools"
  (:require
   [joyride.lm.evaluation :as evaluation]))

(defn register-tools!
  "Register all Language Model tools with VS Code's API.
   Returns a collection of disposables for lifecycle management."
  []
  (try
    (let [evaluation (evaluation/register-tool!)]
      (js/console.log "Joyride LM Tools registered successfully")
      [evaluation])
    (catch js/Error e
      (js/console.error "Failed to register Joyride LM Tools:" (.-message e))
      [])))
