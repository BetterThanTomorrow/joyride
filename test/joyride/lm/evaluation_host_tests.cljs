(ns joyride.lm.evaluation-host-tests
  "Tests for `joyride.lm.evaluation` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.lm.evaluation-host-tests)`."
  (:require [cljs.test :refer [async deftest is use-fixtures]]
            [clojure.string :as string]
            [joyride.integrations.calva :as calva]
            [joyride.lm.evaluation :as evaluation]
            [joyride.sci :as joyride-sci]
            [sci.core :as sci]))

(def ^:private !calva-state (atom nil))

(use-fixtures :once
  {:before (fn []
             (reset! !calva-state calva/state)
             (set! calva/state (constantly :absent)))
   :after (fn []
            (set! calva/state @!calva-state))})

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

(defn- record-calva-log!
  "Stands in a running Calva that shows each message, records what Joyride logs to it, and returns the
   recorded messages and a function that undoes it."
  []
  (let [!logged (atom [])
        original-state calva/state
        original-log calva/log!]
    (set! calva/state (constantly :active))
    (set! calva/log! (fn [message _options]
                       (swap! !logged conj message)
                       {:calva/printed-to ["terminal"] :calva/skipped []}))
    {:logged !logged
     :undo! (fn []
              (set! calva/state original-state)
              (set! calva/log! original-log))}))

(deftest late-rejection-logged-to-calva
  (async done
    (let [{:keys [logged undo!]} (record-calva-log!)]
      (evaluation/execute-code+ {:code "(js/Promise.reject (js/Error. \"late\"))"
                                 :ns "user"
                                 :wait-for-promise? false
                                 :who "host-tests"})
      (evaluation/execute-code+ {:code "(js/Promise.resolve :fine)"
                                 :ns "user"
                                 :wait-for-promise? false
                                 :who "host-tests"})
      (-> (js/Promise. (fn [resolve] (js/setTimeout resolve 50)))
          (.then (fn []
                   (is (= [{:category "evaluationErrorOutput"
                            :text "late\n"
                            :who "host-tests"
                            :ns "user"}]
                          (->> @logged
                               (filter #(= "evaluationErrorOutput" (:category %)))
                               (map #(select-keys % [:category :text :who :ns]))))
                       "The late rejection reaches Calva once, through the echo, and the resolved promise logs no error")))
          (.finally (fn []
                      (undo!)
                      (done)))))))

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

(deftest awaited-rejection-with-non-string-message
  (async done
    (-> (evaluation/execute-code+ {:code "(js/Promise.reject #js {:message 42})"
                                   :ns "user"
                                   :wait-for-promise? true
                                   :who "host-tests"})
        (.then (fn [result]
                 (is (= "#js {:message 42}" (:error result))
                     "The rejection is the error result")))
        (.catch (fn [e] (is (nil? e) "The evaluation resolves to a result")))
        (.finally done))))

(defn- overlap-around-new-print-fn+
  "Starts awaited evaluation A, points SCI's print-fn at a new function, as an nREPL evaluation does,
   and starts awaited evaluation B. Ends A first when `a-first?`, else B first, and resolves to whether
   print-fn is then the new function and print-err-fn the original."
  [a-first?]
  (let [[out-before err-before] (current-print-fns)
        new-out (fn [_])
        open-a! (gate! "joyrideGateD")
        result-a (evaluate-gated+ "joyrideGateD")]
    (sci/alter-var-root sci/print-fn (constantly new-out))
    (let [open-b! (gate! "joyrideGateE")
          result-b (evaluate-gated+ "joyrideGateE")
          [open-first! result-first open-second! result-second] (if a-first?
                                                                  [open-a! result-a open-b! result-b]
                                                                  [open-b! result-b open-a! result-a])]
      (open-first!)
      (-> result-first
          (.then (fn [_]
                   (open-second!)
                   result-second))
          (.then (fn [_]
                   {:out-is-new? (identical? new-out @sci/print-fn)
                    :err-restored? (identical? err-before @sci/print-err-fn)}))
          (.finally (fn []
                      (sci/alter-var-root sci/print-fn (constantly out-before))
                      (js-delete js/globalThis "joyrideGateD")
                      (js-delete js/globalThis "joyrideGateE")))))))

(deftest print-fn-set-between-overlapping-evaluations
  (async done
    (-> (overlap-around-new-print-fn+ true)
        (.then (fn [started-first-ends-first]
                 (is (= {:out-is-new? true :err-restored? true} started-first-ends-first)
                     "Ended in start order, print-fn goes back to the function set between the starts")
                 (overlap-around-new-print-fn+ false)))
        (.then (fn [started-last-ends-first]
                 (is (= {:out-is-new? true :err-restored? true} started-last-ends-first)
                     "Ended in reverse order, print-fn goes back to the function set between the starts")))
        (.catch (fn [e] (is (nil? e) "The evaluations settle")))
        (.finally done))))

(deftest promesa-promise-awaited
  (async done
    (-> (evaluation/execute-code+ {:code "(promesa.core/resolved 1)"
                                   :ns "user"
                                   :wait-for-promise? true
                                   :who "host-tests"})
        (.then (fn [result]
                 (is (= "#<Promise 1>" (:result result))
                     "An awaited promesa promise prints like a native one")))
        (.catch (fn [e] (is (nil? e) "The evaluation resolves to a result")))
        (.finally done))))

(deftest promesa-promise-not-awaited
  (async done
    (let [{:keys [logged undo!]} (record-calva-log!)
          result (evaluation/execute-code+ {:code "(promesa.core/rejected (js/Error. \"late promesa\"))"
                                            :ns "user"
                                            :wait-for-promise? false
                                            :who "host-tests"})]
      (is (string/includes? (:result result) "not awaited")
          "A promesa promise that isn't awaited gets the not awaited message")
      (-> (js/Promise. (fn [resolve] (js/setTimeout resolve 50)))
          (.then (fn []
                   (is (= ["late promesa"]
                          (->> @logged
                               (filter #(= "evaluationErrorOutput" (:category %)))
                               (map :text)))
                       "Its late rejection reaches Calva once, though the echo doesn't follow a promesa promise")))
          (.finally (fn []
                      (undo!)
                      (done)))))))

(defn- evaluate-sync [code ns]
  (evaluation/execute-code+ {:code code
                             :ns ns
                             :wait-for-promise? false
                             :who "host-tests"}))

(deftest evaluation-runs-in-requested-ns
  (let [last-ns-before @joyride-sci/!last-ns]
    (try
      (let [moved (evaluate-sync "(ns joyride-host-tests.elsewhere)" "user")
            in-user (evaluate-sync "(str *ns*)" "user")
            in-other (evaluate-sync "(str *ns*)" "joyride-host-tests.other")]
        (is (= "joyride-host-tests.elsewhere" (:ns moved))
            "The result reports the namespace the evaluation ended in")
        (is (= ["\"user\"" "user"] ((juxt :result :ns) in-user))
            "After another namespace was entered, an evaluation in user runs in user")
        (is (= ["\"joyride-host-tests.other\"" "joyride-host-tests.other"] ((juxt :result :ns) in-other))
            "An evaluation runs in the namespace it names")
        (is (= "joyride-host-tests.other" (str @joyride-sci/!last-ns))
            "The last namespace follows the evaluation, for Run Code and scripts"))
      (finally
        (vreset! joyride-sci/!last-ns last-ns-before)))))
