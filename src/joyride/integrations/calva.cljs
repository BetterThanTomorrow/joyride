(ns joyride.integrations.calva
  "Optional Calva integration: Joyride's output to Calva's Output Destinations and its output
   subscribers, through `repl.log()`. When Calva is absent or hasn't started, it does nothing."
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

(defn state
  "Returns the state of Calva in this window, as `state-of` does."
  []
  (state-of (calva-extension)))

(defn- log-result
  "Returns the `result` of Calva's `log()` as data."
  [^js result]
  (cond-> {:calva/printed-to (vec (.-printedTo result))
           :calva/skipped (vec (.-skipped result))}
    (.-evaluationId result) (assoc :calva/evaluation-id (.-evaluationId result))))

(defn log!
  "Logs `message` with Calva's `log()` and its `options`. Returns Calva's result as data, or `nil` when
   Calva isn't running, its `log()` returned nothing, as a Calva without the display API does, or threw."
  [message options]
  (when-let [^js api (repl-api (calva-extension))]
    (try
      (some-> (.log api (clj->js message) (clj->js options)) log-result)
      (catch :default e
        (js/console.warn "Joyride: Failed to log to Calva:" (.-message e))
        nil))))
