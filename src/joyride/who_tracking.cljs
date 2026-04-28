(ns joyride.who-tracking
  "Tracks which evaluators (`who`) have been active, enabling cross-agent
   awareness via `otherWhosSinceLast`. When agent A evaluates, all other
   tracked agents accumulate A in their 'others' set. On their next eval,
   they receive (and drain) that set — a consume-once signal that someone
   else touched the REPL."
  (:require [joyride.db :as db]))



(defn record-evaluation!
  "Record that `who` evaluated. Adds `who` to every other tracked
   evaluator's 'others since last' set."
  [who]
  (when who
    (swap! db/!app-db update :who/tracking
           (fn [tracking]
             (let [tracking (or tracking {})
                   updated (reduce-kv
                            (fn [m tracked-who others]
                              (if (not= tracked-who who)
                                (assoc m tracked-who (conj others who))
                                (assoc m tracked-who others)))
                            {}
                            tracking)]
               (if (contains? updated who)
                 updated
                 (assoc updated who #{})))))))

(defn get-other-whos-since-last!
  "Returns a vector of other `who` values that evaluated since `who`'s
   last call, then clears the set (consume-once). Returns nil if empty."
  [who]
  (let [others (get-in @db/!app-db [:who/tracking who])]
    (when (seq others)
      (swap! db/!app-db assoc-in [:who/tracking who] #{})
      (vec others))))
