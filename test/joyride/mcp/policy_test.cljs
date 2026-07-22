(ns joyride.mcp.policy-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [vscode-mcp.policy :as policy]))

(deftest joyride-default-lifecycle-test
  (testing "default settings: no explicit auto-start, Cursor auto-register with API"
    (is (policy/should-auto-start? {:mcp/auto-start? false :mcp/auto-register? true :mcp/cursor-available? true})
        "auto-starts in Cursor when autoRegisterCursor is enabled"))

  (testing "default settings: no start outside Cursor without ECA"
    (is (not (policy/should-auto-start? {:mcp/auto-start? false :mcp/auto-register? true :mcp/cursor-available? false}))
        "does not auto-start when Cursor MCP API is unavailable"))

  (testing "explicit auto-start without Cursor"
    (is (policy/should-auto-start? {:mcp/auto-start? true :mcp/auto-register? false :mcp/cursor-available? false})
        "auto-starts when autoStartServer is enabled"))

  (testing "ECA auto-register auto-starts when extension and workspace present"
    (is (policy/should-auto-start? {:mcp/auto-start? false
                                    :mcp/auto-register? false
                                    :mcp/cursor-available? false
                                    :mcp/auto-register-eca? true
                                    :mcp/eca-available? true
                                    :mcp/workspace-root-present? true})
        "auto-starts when autoRegisterEca is enabled and ECA is available"))

  (testing "ECA setting false does not auto-start for ECA alone"
    (is (not (policy/should-auto-start? {:mcp/auto-start? false
                                         :mcp/auto-register? false
                                         :mcp/cursor-available? false
                                         :mcp/auto-register-eca? false
                                         :mcp/eca-available? true
                                         :mcp/workspace-root-present? true})))))

(deftest joyride-cursor-registration-test
  (testing "registers when auto-register, API available, and port file present"
    (is (policy/should-register-with-cursor? {:mcp/auto-register? true :mcp/cursor-available? true :mcp/port-file-present? true})))

  (testing "does not register without port file"
    (is (not (policy/should-register-with-cursor? {:mcp/auto-register? true :mcp/cursor-available? true :mcp/port-file-present? false}))))

  (testing "does not register when auto-register disabled"
    (is (not (policy/should-register-with-cursor? {:mcp/auto-register? false :mcp/cursor-available? true :mcp/port-file-present? true})))))

(deftest joyride-eca-registration-test
  (testing "registers with ECA when setting, extension, port file, and workspace present"
    (is (policy/should-register-with-eca? {:mcp/auto-register-eca? true
                                           :mcp/eca-available? true
                                           :mcp/port-file-present? true
                                           :mcp/workspace-root-present? true})))

  (testing "does not register with ECA when setting is false"
    (is (not (policy/should-register-with-eca? {:mcp/auto-register-eca? false
                                                :mcp/eca-available? true
                                                :mcp/port-file-present? true
                                                :mcp/workspace-root-present? true})))))
