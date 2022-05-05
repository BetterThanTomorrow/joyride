(ns joyride.when-contexts
  (:require ["vscode" :as vscode]))

(defonce ^:private !db (atom {:contexts {::joyride.isActive false
                                     ::joyride.isNReplServerRunning false}}))

(defn set-context! [k v]
  (swap! !db assoc-in [:contexts k] v)
  (vscode/commands.executeCommand "setContext" (name k) v))

(defn get-context [k]
  (get-in @!db [:contexts (if (string? k)
                            (keyword (str "joyride.when-contexts/" k))
                            k)]))

(comment
  (get-context "joyride.isActive"))