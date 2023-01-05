(ns integration-test.user-scripts-test
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
      (def result result)
      (is (= "Hello World!"
             (.hello result))))))