(ns integration-test.index
  (:require [cljs.test]
            [promesa.core :as p]))

(def !results (atom {:fail 0
                     :error 0}))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

(def old-fail (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (old-fail m)
  (swap! !results update :fail inc))

(def old-error (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (old-error m)
  (swap! !results update :error inc))

(defn run-all-tests []
  (p/create (fn [resolve, reject]
              (p/do!
               (p/delay 1000) ; This waits for workspace_activate.cljs to run
               (require '[integration-test.workspace-activate-test])

               ;; This fails when accessing integration-test.run-a-ws-script-test/symbol-1
               ;; if required from the ns form
               (require '[integration-test.ws-scripts-test])
               (require '[integration-test.npm-test])

               (cljs.test/run-tests 'integration-test.workspace-activate-test)
               (cljs.test/run-tests 'integration-test.ws-scripts-test)
               (cljs.test/run-tests 'integration-test.npm-test)
               
               ;; Running only matching namespaces fails
               ;; Error: Doesn't support name: user-activate 
               ;; (cljs.test/run-all-tests #"integration-test")
               
               (println "!results" @!results)
               (let [{:keys [fail error]} @!results]
                 (if (zero? (+ fail error))
                   (resolve true)
                   (reject true)))))))

(comment
  (run-all-tests)
  :rcf)

