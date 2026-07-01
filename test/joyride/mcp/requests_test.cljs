(ns joyride.mcp.requests-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [vscode-mcp.manifest :as manifest]))

(defn- mock-context [tools]
  #js {:extension #js {:packageJSON #js {:contributes #js {:languageModelTools tools}}}})

(deftest joyride-evaluate-code-gating-test
  (let [ctx (mock-context
             #js [#js {:name "joyride_evaluate_code"
                       :when "config.joyride.lm.enableReplTool"}
                   #js {:name "joyride_request_human_input"}])]
    (testing "evaluate tool allowed when enableReplTool is true"
      (is (= :allowed
             (manifest/tool-call-allowed? ctx "joyride_evaluate_code"
              {:settings {"config.joyride.lm.enableReplTool" true}}))))

    (testing "evaluate tool disabled when enableReplTool is false"
      (is (= :disabled
             (manifest/tool-call-allowed? ctx "joyride_evaluate_code"
              {:settings {"config.joyride.lm.enableReplTool" false}}))))

    (testing "human input tool has no when clause"
      (is (= :allowed (manifest/tool-call-allowed? ctx "joyride_request_human_input"))))

    (testing "unknown tool is not a manifest decision"
      (is (= :unknown (manifest/tool-call-allowed? ctx "missing_tool"))))))
