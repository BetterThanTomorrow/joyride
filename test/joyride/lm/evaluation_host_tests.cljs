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

(defn- current-print-fns []
  [@sci/print-fn @sci/print-err-fn])

(defn- overlap+
  "Starts two awaited evaluations, ends them in the order `first-k` then `second-k`, and resolves
   to whether SCI printed elsewhere between them, and through the original print functions after both."
  [first-k second-k]
  (let [print-fns-before (current-print-fns)
        open! {"joyrideGateA" (gate! "joyrideGateA")
               "joyrideGateB" (gate! "joyrideGateB")}
        results {"joyrideGateA" (evaluate-gated+ "joyrideGateA")
                 "joyrideGateB" (evaluate-gated+ "joyrideGateB")}]
    ((open! first-k))
    (-> (results first-k)
        (.then (fn [_]
                 (let [capturing-between? (not= print-fns-before (current-print-fns))]
                   ((open! second-k))
                   (.then (results second-k)
                          (fn [_]
                            {:capturing-between? capturing-between?
                             :restored? (= print-fns-before (current-print-fns))}))))))))

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

(deftest print-fn-replaced-during-awaited-evaluation
  (async done
    (let [[out-before err-before] (current-print-fns)
          nrepl-like-out (fn [_])
          open! (gate! "joyrideGateC")
          result (evaluate-gated+ "joyrideGateC")]
      (sci/alter-var-root sci/print-fn (constantly nrepl-like-out))
      (open!)
      (-> result
          (.then (fn [_]
                   (is (identical? nrepl-like-out @sci/print-fn)
                       "A print-fn set by someone else meanwhile is left in place")
                   (is (identical? err-before @sci/print-err-fn)
                       "print-err-fn goes back to the original")))
          (.finally (fn []
                      (sci/alter-var-root sci/print-fn (constantly out-before))
                      (js-delete js/globalThis "joyrideGateC")
                      (done)))))))
