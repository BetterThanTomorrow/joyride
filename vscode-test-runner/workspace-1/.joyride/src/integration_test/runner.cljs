(ns integration-test.runner
  (:require [cljs.test]
            [integration-test.db :as db]
            [promesa.core :as p]))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (print "=== BEGIN" (-> m :var meta :name)) " ")

(defmethod cljs.test/report [:cljs.test/default :end-test-var] [m]
  (println "\n=== END" (-> m :var meta :name))
  (println))

(def old-pass (get-method cljs.test/report [:cljs.test/default :pass]))

(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (old-pass m)
  (print "✅")
  (swap! db/!state update :pass inc))

(def old-fail (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (old-fail m)
  (print "❌")
  (swap! db/!state update :fail inc))

(def old-error (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (old-error m)
  (print "🚫")
  (swap! db/!state update :error inc))

(def old-end-run-tests (get-method cljs.test/report [:cljs.test/default :end-run-tests]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (old-end-run-tests m)
  (let [{:keys [running fail error]} @db/!state]
    (println "Runner: tests run, results:" (select-keys  @db/!state [:pass :fail :error]))
    (if (zero? (+ fail error))
      (p/resolve! running true)
      (p/reject! running true))))

(defn- run-when-ws-activated [tries]
  (if (:ws-activated? @db/!state)
    (do
      (println "Runner: Workspace activated, running tests")
      (require '[integration-test.workspace-activate-test])
      (require '[integration-test.ws-scripts-test])
      (require '[integration-test.require-js-test])
      (require '[integration-test.npm-test])
      (cljs.test/run-tests 'integration-test.workspace-activate-test
                           'integration-test.ws-scripts-test
                           'integration-test.require-js-test
                           'integration-test.npm-test))
    (do
      (println "Runner: Workspace not activated yet, tries: " tries "- trying again in a jiffy")
      (js/setTimeout #(run-when-ws-activated (inc tries)) 10))))

(defn run-all-tests []
  (let [running (p/deferred)]
    (swap! db/!state assoc
           :running running)
    (run-when-ws-activated 1)
    running))

(comment
  (run-all-tests)
  :rcf)

