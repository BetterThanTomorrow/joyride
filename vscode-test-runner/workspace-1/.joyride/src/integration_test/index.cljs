(ns integration-test.index
  (:require [cljs.test]
            [promesa.core :as p]))

(def !state (atom {:running nil
                   :fail 0
                   :error 0}))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

(def old-fail (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (old-fail m)
  (swap! !state update :fail inc))

(def old-error (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (old-error m)
  (swap! !state update :error inc))

(def old-end-run-tests (get-method cljs.test/report [:cljs.test/default :end-run-tests]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (old-end-run-tests m)
  (let [{:keys [running fail error]} @!state]
    (if (zero? (+ fail error))
      (p/resolve! running true)
      (p/reject! running true))))

(defn run-all-tests []
  (p/do!
   (p/delay 1000) ; This waits for workspace_activate.cljs to run
   (require '[integration-test.workspace-activate-test])
   (require '[integration-test.ws-scripts-test])
   #_(require '[integration-test.npm-test])

   (cljs.test/run-tests 'integration-test.workspace-activate-test
                        'integration-test.ws-scripts-test
                        #_'integration-test.npm-test))
  (let [running (p/deferred)]
    (swap! !state assoc :running running)
    running))

(comment
  (run-all-tests)
  :rcf)

