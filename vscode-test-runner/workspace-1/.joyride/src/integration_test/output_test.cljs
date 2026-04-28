(ns integration-test.output-test
  (:require [cljs.test :refer [is testing]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(deftest-async eval-produces-output-without-error
  (testing "Evaluating code via Joyride command succeeds (exercises who/info-line path)"
    (p/let [result (vscode/commands.executeCommand "joyride.runCode" "(+ 1 2)")]
      (is (= 3 result)
          "Eval via runCode command works with updated output pipeline"))))
