(ns integration-test.run-a-ws-script-test
  (:require [cljs.test :refer [deftest testing is async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(deftest run-a-ws-script
  (testing "Runs a workspace script"
    ;; TODO: This doesn't work, 0 assertions
    (async done
           (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")]
             (is (= :a-ws-script-2
                    result))
             (done)))))