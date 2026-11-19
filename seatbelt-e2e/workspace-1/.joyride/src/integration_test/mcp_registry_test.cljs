(ns integration-test.mcp-registry-test
  (:require
   [cljs.test :refer [is testing]]
   [clojure.string :as string]
   [integration-test.macros :refer [deftest-async]]
   [integration-test.mcp-client :as mcp-client]
   [promesa.core :as p]
   ["fs" :as fs]
   ["vscode" :as vscode]))

(deftest-async registry-entry-usable-via-bb-mcp
  {:after (vscode/commands.executeCommand "joyride.stopMcpServer")}
  (testing "MCP registry entry is discoverable, connectable, and usable via bb list and bb mcp"
    (p/let [_ (vscode/commands.executeCommand "joyride.startMcpServer")
            workspace-root (some-> vscode/workspace.workspaceFolders first .-uri .-fsPath)
            window (mcp-client/wait-for-window+ workspace-root 20)
            server-name (:serverName window)
            window-id (:windowId window)
            init-out (mcp-client/run-bb+ ["mcp" "initialize"
                                          "--server-name" server-name
                                          "--window-id" window-id])
            init (mcp-client/parse-envelope init-out)
            call-out (mcp-client/run-bb+ ["mcp" "tools/call"
                                          "--server-name" server-name
                                          "--window-id" window-id
                                          "--name" "joyride_evaluate_code"
                                          "--args" "-"]
                                         "{\"code\":\"(+ 1 2)\",\"who\":\"gb-test-engineer\"}\n")
            call (mcp-client/parse-envelope call-out)
            call-text (string/join " " (map :text (get-in call [:result :content])))
            port-file (get-in window [:mcp :portFilePath])]
      (is (some? (:windowId window)) "bb list shows this Joyride window")
      (is (= "127.0.0.1" (get-in window [:mcp :host])))
      (is (some? (get-in window [:mcp :port])) "listed window has an MCP port")
      (is (string? port-file) "port file path is present")
      (is (fs/existsSync port-file) (str "port file exists: " port-file))
      (is (true? (:ok init)) "bb mcp initialize connects")
      (is (true? (:ok call)) "bb mcp tools/call succeeds")
      (is (string/includes? call-text "3")
          (str "joyride_evaluate_code returns 3, got: " call-text)))))
