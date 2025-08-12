(ns joyride.lm
  "Main entry point for Language Model tools"
  (:require
   [joyride.lm.evaluation :as evaluation]
   [joyride.lm.docs :as docs]
   [joyride.lm.human-intelligence :as human-intelligence]))

(defn register-tools!
  "Register all Language Model tools with VS Code's API.
   Returns a collection of disposables for lifecycle management."
  [extension-context]
  (try
    (let [evaluation (evaluation/register-tool!)
          agent-guide (docs/register-tool! extension-context "joyride_basics_for_agents" docs/agent-guide-path)
          user-assistance (docs/register-tool! extension-context "joyride_assisting_users_guide" docs/user-assistance-guide-path)
          human-intelligence (human-intelligence/register-tool!)]
      (js/console.log "Joyride LM Tools registered successfully")
      [agent-guide evaluation user-assistance human-intelligence])
    (catch js/Error e
      (js/console.error "Failed to register Joyride LM Tools:" (.-message e))
      [])))
