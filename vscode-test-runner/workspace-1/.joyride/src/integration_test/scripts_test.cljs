(ns integration-test.scripts-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(deftest-async run-a-user-script
  (testing "Runs a user script"
    (p/let [result (vscode/commands.executeCommand "joyride.runUserScript" "hello_joyride_user_script.cljs")]
      (is (= "🎸"
             result)))))

(deftest-async run-a-user-javascript-script
  (testing "Runs a user script written in JavaScript"
    (p/let [result (vscode/commands.executeCommand "joyride.runUserScript" "hello_joyride_user_script.js")]
      (is (= "Hello World!"
             (.hello result))))))

(deftest-async run-a-ws-script
  (testing "Runs a workspace script"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")]
      (is (= :a-ws-script
             result)))))

(deftest-async run-a-ws-javascript-script-dashes
  (testing "Runs a workspace script written in JavaScript, named with dash separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a-ws-script.js")]
      (is (= 42
             (.-fortytwo result))))))

(deftest-async run-a-ws-javascript-script-underscores
  (testing "Runs a workspace script written in JavaScript, named with undercore separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.js")]
      (is (= 42
             (.-fortytwo result))))))