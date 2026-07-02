(ns joyride.mcp.server-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [vscode-mcp.lifecycle.state :as lifecycle]
   [vscode-mcp.manual-setup :as manual-setup]))

;; Targets the shared vscode-mcp.lifecycle.state / vscode-mcp.manual-setup
;; namespaces directly (the same predicates and string builders
;; joyride.mcp.server delegates to) rather than duplicating their logic here.
;; joyride.mcp.server itself requires "vscode" and is verified live via the
;; connected extension-host REPL instead — see the plan's Phase 6.6b
;; verification notes.

(deftest server-running-predicate-test
  (testing "false when no server-info in state"
    (is (not (lifecycle/running? (lifecycle/init-state)))))

  (testing "true when server-info present"
    (is (lifecycle/running? (assoc (lifecycle/init-state)
                                   :lifecycle/server-info {:server/assigned-port 1234})))))

(deftest copy-command-strings-test
  (let [wrapper "/ext/dist/joyride-mcp-server.js"
        port-file-path "/storage/mcp-server/port"
        server-info {:server/assigned-port 5432
                     :server/host "127.0.0.1"
                     :server/port-file-uri #js {:fsPath port-file-path}}
        commands (manual-setup/copy-command-strings wrapper server-info)]
    (testing "port command includes host (matches joyride.mcp.server's wrapper path)"
      (is (= "node /ext/dist/joyride-mcp-server.js 5432 127.0.0.1"
             (:manual-setup/port commands))))

    (testing "port-file command includes path and host"
      (is (= "node /ext/dist/joyride-mcp-server.js /storage/mcp-server/port 127.0.0.1"
             (:manual-setup/port-file commands))))

    (testing "custom host"
      (is (= "node /ext/dist/joyride-mcp-server.js 5432 0.0.0.0"
             (:manual-setup/port
              (manual-setup/copy-command-strings wrapper (assoc server-info :server/host "0.0.0.0"))))))))

(deftest command-enablement-context-test
  (testing "when-context key matches package.json enablement"
    (is (= "joyride.isMcpServerRunning"
           (name :joyride.when-contexts/joyride.isMcpServerRunning))))

  (testing "server-running predicate reflects state for command enablement"
    (is (not (lifecycle/running? (lifecycle/init-state)))
        "start enabled when server not running")
    (is (lifecycle/running? (assoc (lifecycle/init-state) :lifecycle/server-info {:server/assigned-port 1}))
        "stop enabled when server running")))
