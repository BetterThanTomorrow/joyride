(ns integration-test.require-js-test
  (:require [cljs.test :refer [deftest testing is async]]
            [promesa.core :as p]
            ["../js-file" :as js-file]
            ["vscode" :as vscode]))

(deftest script-require-js
  (testing "Imports hello function"
    (async done
           (p/let [script-result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "require_js_script.cljs")]
             (is (= 42
                    script-result)))
           (done))))

(deftest require-js-file
  (testing "Can require js file directly"
    (is (= 42
           js-file/fortytwo))))
