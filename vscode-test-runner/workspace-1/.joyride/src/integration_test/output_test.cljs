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

(deftest-async sequential-evals-succeed
  (testing "Multiple sequential evals succeed (exercises who-tracking recording)"
    (p/let [r1 (vscode/commands.executeCommand "joyride.runCode" "(+ 1 1)")
            r2 (vscode/commands.executeCommand "joyride.runCode" "(+ 2 2)")
            r3 (vscode/commands.executeCommand "joyride.runCode" "(+ 3 3)")]
      (is (= 2 r1) "First eval returns correct result")
      (is (= 4 r2) "Second eval returns correct result")
      (is (= 6 r3) "Third eval returns correct result"))))

(deftest-async eval-with-side-effects
  (testing "Eval with println succeeds (exercises output capture path)"
    (p/let [result (vscode/commands.executeCommand "joyride.runCode"
                                                   "(do (println \"hello\") 42)")]
      (is (= 42 result)
          "Eval with println returns final value"))))

(deftest-async eval-preserves-namespace-state
  (testing "Namespace state persists across evals (who-tracking doesn't interfere)"
    (p/let [_ (vscode/commands.executeCommand "joyride.runCode" "(def test-output-var 99)")
            result (vscode/commands.executeCommand "joyride.runCode" "test-output-var")]
      (is (= 99 result)
          "Var defined in first eval is accessible in second eval"))))
