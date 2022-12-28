(ns integration-test.index
  (:require [cljs.test]
            [promesa.core :as p]
            ["vscode" :as vscode]))

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
               ;; Only way I found to wait for the promise and check something from the namespace
               (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")
               (p/delay 1000) ; This waits both for the above workspace script and for workspace_activate.cljs
                              ; to run
               (require '[integration-test.workspace-activate-test])
               (require '[integration-test.run-a-ws-script-test])
               (cljs.test/run-tests 'integration-test.workspace-activate-test)
               (cljs.test/run-tests 'integration-test.run-a-ws-script-test)
               
               ;; Running only matching namespaces fails
               ;; Error: Doesn't support name: user-activate 
               ;; (cljs.test/run-all-tests #"integration-test")
               
               (println "!results" @!results)
               (let [{:keys [fail error]} @!results]
                 (if (zero? (+ fail error))
                   (resolve true)
                   (reject true)))))))

