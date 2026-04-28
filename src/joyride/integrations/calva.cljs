(ns joyride.integrations.calva
  "Optional Calva integration for logging Joyride output to Calva's subscriber bus.
   When Calva is installed, agent evaluations are relayed via `repl.log()` so that
   Backseat Driver can query them. When Calva is absent, all operations are no-ops."
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]))

(defn- get-calva-log-fn
  "Returns Calva's repl.log function if Calva is installed, nil otherwise.
   Caches the result in app-db."
  []
  (if (contains? @db/!app-db :calva/log-fn)
    (:calva/log-fn @db/!app-db)
    (let [log-fn (when-let [ext (.getExtension vscode/extensions "betterthantomorrow.calva")]
                   (.. (.-exports ext) -v1 -repl -log))]
      (swap! db/!app-db assoc :calva/log-fn log-fn)
      log-fn)))

(defn log-to-calva!
  "Log an output message to Calva's subscriber bus.
   No-ops when Calva is not installed."
  [{:keys [category text who ns]}]
  (when-let [log-fn (get-calva-log-fn)]
    (try
      (log-fn #js {:category category
                   :text text
                   :who who
                   :ns ns
                   :replSessionKey "joyride"})
      (catch js/Error e
        (js/console.warn "Joyride: Failed to log to Calva:" (.-message e))))))
