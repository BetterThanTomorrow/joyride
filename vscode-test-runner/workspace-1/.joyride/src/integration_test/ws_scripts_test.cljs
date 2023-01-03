(ns integration-test.ws-scripts-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(deftest-async run-a-ws-script
  (testing "Runs a workspace script"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")]
      (is (= :a-ws-script
             result)))))

(deftest-async run-a-ws-javascript-script-underscores
  (testing "Runs a workspace script written in JavaScript, named with undercore separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.js")]
      (is (= 42
             (.-fortytwo result))))))