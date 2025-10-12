(ns joyride.lm
  "Main entry point for Language Model tools"
  (:require
   [joyride.lm.evaluation :as evaluation]
   [joyride.lm.human-intelligence :as human-intelligence]))

(defn register-tools!
  "Register all Language Model tools with VS Code's API.
   Returns a collection of disposables for lifecycle management."
  []
  (try
    (let [evaluation (evaluation/register-tool!)
          human-intelligence (human-intelligence/register-tool!)]
      (js/console.log "Joyride LM Tools registered successfully")
      [evaluation human-intelligence])
    (catch js/Error e
      (js/console.error "Failed to register Joyride LM Tools:" (.-message e))
      [])))
