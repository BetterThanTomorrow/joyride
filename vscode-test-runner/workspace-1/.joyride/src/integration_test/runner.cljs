(ns integration-test.runner
  (:require [cljs.test]
            [integration-test.db :as db]
            [promesa.core :as p]))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (js/process.stdout.write (str "=== " (-> m :var meta :name) " ")))

(defmethod cljs.test/report [:cljs.test/default :end-test-var] [m]
  (js/process.stdout.write " ===\n"))

(def old-pass (get-method cljs.test/report [:cljs.test/default :pass]))

(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (binding [*print-fn* js/process.stdout.write] (old-pass m)) 
  (js/process.stdout.write "✅")
  (swap! db/!state update :pass inc))

(def old-fail (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (binding [*print-fn* js/process.stdout.write] (old-fail m))
  (js/process.stdout.write "❌")
  (swap! db/!state update :fail inc))

(def old-error (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (binding [*print-fn* js/process.stdout.write] (old-error m))
  (js/process.stdout.write "🚫")
  (swap! db/!state update :error inc))

(def old-end-run-tests (get-method cljs.test/report [:cljs.test/default :end-run-tests]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (old-end-run-tests m)
  (let [{:keys [running pass fail error]} @db/!state
        passed-minimum-threshold 20
        fail-reason (cond
                      (< 0 (+ fail error)) "FAILURE: Some tests failed or errored"
                      (< pass passed-minimum-threshold) (str "FAILURE: Less than " passed-minimum-threshold " assertions passed")
                      :else nil)]
    (println "Runner: tests run, results:" (select-keys  @db/!state [:pass :fail :error]))
    (if fail-reason
      (p/reject! running fail-reason)
      (p/resolve! running true))))

;; We rely on that the user_activate.cljs script is run before workspace_activate.cljs
(defn- run-when-ws-activated [tries]
  (if (:ws-activated? @db/!state)
    (do
      (println "Runner: Workspace activated, running tests...")
      (require '[integration-test.activate-test])
      (require '[integration-test.scripts-test])
      (require '[integration-test.require-js-test])
      (require '[integration-test.require-extension-test])
      (require '[integration-test.npm-test])
      (require '[integration-test.nrepl-test])
      (cljs.test/run-tests 'integration-test.activate-test
                           'integration-test.scripts-test
                           'integration-test.require-js-test
                           'integration-test.require-extension-test
                           'integration-test.npm-test
                           'integration-test.nrepl-test))
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

