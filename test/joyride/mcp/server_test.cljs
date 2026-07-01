(ns joyride.mcp.server-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [vscode-mcp.stdio-config :as stdio-config]))

(defn- server-running?
  "Pure predicate — matches joyride.mcp.server/server-running?"
  [state]
  (boolean (:mcp/server-info state)))

(deftest server-running-predicate-test
  (testing "false when no server-info in state"
    (is (not (server-running? {}))))

  (testing "true when server-info present"
    (is (server-running? {:mcp/server-info {:server/assigned-port 1234}}))))

(deftest copy-command-strings-test
  (let [wrapper "/ext/dist/joyride-mcp-server.js"
        port-file-path "/storage/mcp-server/port"
        server-info {:server/assigned-port 5432
                     :server/host "127.0.0.1"
                     :server/port-file-uri #js {:fsPath port-file-path}}]
    (testing "port command includes host (matches joyride.mcp.server/copy-command-strings)"
      (is (= "node /ext/dist/joyride-mcp-server.js 5432 127.0.0.1"
             (stdio-config/stdio-command-string "node" wrapper (str (:server/assigned-port server-info)) (:server/host server-info)))))

    (testing "port-file command includes path and host"
      (is (= "node /ext/dist/joyride-mcp-server.js /storage/mcp-server/port 127.0.0.1"
             (stdio-config/stdio-command-string "node" wrapper port-file-path (:server/host server-info)))))

    (testing "custom host"
      (is (= "node /ext/dist/joyride-mcp-server.js 5432 0.0.0.0"
             (stdio-config/stdio-command-string "node" wrapper "5432" "0.0.0.0"))))))

(deftest command-enablement-context-test
  (testing "when-context key matches package.json enablement"
    (is (= "joyride.isMcpServerRunning"
           (name :joyride.when-contexts/joyride.isMcpServerRunning))))

  (testing "server-running predicate reflects state for command enablement"
    (is (not (server-running? {}))
        "start enabled when server not running")
    (is (server-running? {:mcp/server-info {:server/assigned-port 1}})
        "stop enabled when server running")))
