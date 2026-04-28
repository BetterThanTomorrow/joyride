(ns joyride.who-tracking
  "Tracks which evaluators (`who`) have been active, enabling cross-agent
   awareness via `otherWhosSinceLast`. When agent A evaluates, all other
   tracked agents accumulate A in their 'others' set. On their next eval,
   they receive (and drain) that set — a consume-once signal that someone
   else touched the REPL.")

(def ^:private !tracking
  "Map of {who #{other-whos-since-last-eval}}"
  (atom {}))

(defn record-evaluation!
  "Record that `who` evaluated. Adds `who` to every other tracked
   evaluator's 'others since last' set."
  [who]
  (when who
    (swap! !tracking
           (fn [tracking]
             (let [updated (reduce-kv
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
  (let [others (get @!tracking who)]
    (when (seq others)
      (swap! !tracking assoc who #{})
      (vec others))))
