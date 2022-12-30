(ns integration-test.runner
  (:require [cljs.test]
            [integration-test.db :as db]
            [promesa.core :as p]))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

(def old-fail (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (old-fail m)
  (swap! db/!state update :fail inc))

(def old-error (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (old-error m)
  (swap! db/!state update :error inc))

(def old-end-run-tests (get-method cljs.test/report [:cljs.test/default :end-run-tests]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (old-end-run-tests m)
  (let [{:keys [running fail error]} @db/!state]
    (println "Runner: tests run, results:" (select-keys  @db/!state [:fail :error]))
    (if (zero? (+ fail error))
      (p/resolve! running true)
      (p/reject! running true))))

(defn run-all-tests []
  (let [ws-activate-waiter (p/deferred)
        running (p/deferred)]
    (swap! db/!state assoc
           :ws-activate-waiter ws-activate-waiter
           :running running)
    (.then ws-activate-waiter (fn []
                                (println "Runner: ws-activate-waiter promise resolved, running tests")
                                (require '[integration-test.workspace-activate-test])
                                (require '[integration-test.ws-scripts-test])
                                #_(require '[integration-test.npm-test])
                                (cljs.test/run-tests 'integration-test.workspace-activate-test
                                                     'integration-test.ws-scripts-test
                                                     #_'integration-test.npm-test)))
    running))

(comment
  (run-all-tests)
  :rcf)

