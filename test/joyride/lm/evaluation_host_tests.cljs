(ns joyride.lm.evaluation-host-tests
  "Tests for `joyride.lm.evaluation` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.lm.evaluation-host-tests)`."
  (:require [cljs.test :refer [async deftest is]]
            [joyride.lm.evaluation :as evaluation]
            [sci.core :as sci]))

(defn- gate!
  "Puts a promise at `js/globalThis[k]` and returns the function that resolves it."
  [k]
  (let [!resolve (volatile! nil)]
    (aset js/globalThis k (js/Promise. (fn [resolve] (vreset! !resolve resolve))))
    #(@!resolve :opened)))

(defn- evaluate-gated+ [k]
  (evaluation/execute-code+ {:code (str "(aget js/globalThis \"" k "\")")
                             :ns "user"
                             :wait-for-promise? true
                             :who "host-tests"}))

(defn- overlap+
  "Starts two awaited evaluations, ends them in the order `first-k` then `second-k`, and resolves
   to whether SCI printed elsewhere between them, and through the original print function after both."
  [first-k second-k]
  (let [print-fn-before @sci/print-fn
        open! {"joyrideGateA" (gate! "joyrideGateA")
               "joyrideGateB" (gate! "joyrideGateB")}
        results {"joyrideGateA" (evaluate-gated+ "joyrideGateA")
                 "joyrideGateB" (evaluate-gated+ "joyrideGateB")}]
    ((open! first-k))
    (-> (results first-k)
        (.then (fn [_]
                 (let [capturing-between? (not (identical? print-fn-before @sci/print-fn))]
                   ((open! second-k))
                   (.then (results second-k)
                          (fn [_]
                            {:capturing-between? capturing-between?
                             :restored? (identical? print-fn-before @sci/print-fn)}))))))))

(deftest overlapping-awaited-evaluations-restore-printing
  (async done
    (-> (overlap+ "joyrideGateA" "joyrideGateB")
        (.then (fn [started-first-ends-first]
                 (is (= {:capturing-between? true :restored? true} started-first-ends-first)
                     "Ended in start order, printing goes back to the original")
                 (overlap+ "joyrideGateB" "joyrideGateA")))
        (.then (fn [started-last-ends-first]
                 (is (= {:capturing-between? true :restored? true} started-last-ends-first)
                     "Ended in reverse order, printing goes back to the original")))
        (.catch (fn [e] (is (nil? e) "The evaluations settle")))
        (.finally (fn []
                    (js-delete js/globalThis "joyrideGateA")
                    (js-delete js/globalThis "joyrideGateB")
                    (done))))))
