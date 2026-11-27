(ns joyride.integrations.calva
  "Optional Calva integration for logging Joyride output to Calva's subscriber bus.
   When Calva is running, agent evaluations are relayed via `repl.log()` so that
   Backseat Driver can query them. When Calva is absent or hasn't started, all operations are no-ops."
  (:require
   ["vscode" :as vscode]))

(defn- calva-extension []
  (vscode/extensions.getExtension "betterthantomorrow.calva"))

(defn state-of
  "Returns the state of the Calva `extension`: `:absent` without one, `:inactive` before it has started, `:active` after.
   VS Code doesn't hand out a disabled extension, so a disabled Calva is `:absent`."
  [^js extension]
  (cond
    (nil? extension) :absent
    (.-isActive extension) :active
    :else :inactive))

(defn repl-api
  "Returns the `repl` API of the Calva `extension` when it is active, otherwise `nil`.
   Reads the exports only then, since VS Code throws when they are read before."
  [^js extension]
  (when (= :active (state-of extension))
    (when-let [^js v1 (some-> extension .-exports .-v1)]
      (.-repl v1))))

(defn log-to-calva!
  "Log an output message to Calva's subscriber bus.
   No-ops when Calva is not installed or hasn't started."
  [{:keys [category text who ns]}]
  (when-let [log-fn (some-> (repl-api (calva-extension)) .-log)]
    (try
      (log-fn #js {:category category
                   :text text
                   :who who
                   :ns ns
                   :replSessionKey "joyride"})
      (catch js/Error e
        (js/console.warn "Joyride: Failed to log to Calva:" (.-message e))))))
