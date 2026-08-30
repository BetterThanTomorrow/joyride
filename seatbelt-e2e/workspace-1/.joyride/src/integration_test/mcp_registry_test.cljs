(ns integration-test.mcp-registry-test
  (:require
   [cljs.test :refer [is testing]]
   [clojure.string :as string]
   [integration-test.macros :refer [deftest-async]]
   [promesa.core :as p]
   ["child_process" :as cp]
   ["fs" :as fs]
   ["os" :as os]
   ["path" :as path]
   ["vscode" :as vscode]))

(def registry-home
  (path/join (os/homedir) ".config" "vscode-mcp" "registry"))

(def bb-bin
  (or (first (filter fs/existsSync
                     ["/opt/homebrew/bin/bb"
                      "/usr/local/bin/bb"
                      (path/join (os/homedir) ".local" "bin" "bb")]))
      "bb"))

(defn- run-bb+
  ([args] (run-bb+ args nil))
  ([args stdin]
   (p/create
    (fn [resolve reject]
      (let [proc (cp/spawn bb-bin (clj->js args) #js {:cwd registry-home})
            out (atom "")
            err (atom "")]
        (.on (.-stdout proc) "data" (fn [d] (swap! out str (str d))))
        (.on (.-stderr proc) "data" (fn [d] (swap! err str (str d))))
        (.on proc "error" (fn [e] (reject e)))
        (.on proc "close"
             (fn [code]
               (if (zero? code)
                 (resolve @out)
                 (reject (js/Error. (str "bb exit " code "\n" @out "\n" @err))))))
        (when stdin
          (.write (.-stdin proc) stdin)
          (.end (.-stdin proc))))))))

(defn- parse-windows [stdout]
  (if (string/blank? stdout)
    []
    (js->clj (js/JSON.parse stdout) :keywordize-keys true)))

(defn- parse-envelope [stdout]
  (js->clj (js/JSON.parse stdout) :keywordize-keys true))

(defn- this-joyride-window [windows workspace-root]
  (let [base (and workspace-root (path/basename workspace-root))]
    (some (fn [w]
            (when (and (= "joyride" (:serverName w))
                       (let [root (:workspaceRoot w)]
                         (or (= workspace-root root)
                             (and base root (string/ends-with? root base)))))
              w))
          windows)))

(defn- wait-for-window+ [workspace-root tries]
  (-> (p/let [out (run-bb+ ["list" "--json"])
              match (this-joyride-window (parse-windows out) workspace-root)]
        (if match
          match
          (if (pos? tries)
            (p/then (p/delay 250)
                    (fn [_] (wait-for-window+ workspace-root (dec tries))))
            (throw (js/Error. (str "Joyride window not in bb list for " workspace-root "\n" out))))))
      (p/catch (fn [e]
                 (if (pos? tries)
                   (p/then (p/delay 250)
                           (fn [_] (wait-for-window+ workspace-root (dec tries))))
                   (throw e))))))

(deftest-async registry-entry-usable-via-bb-mcp
  {:after (vscode/commands.executeCommand "joyride.stopMcpServer")}
  (testing "MCP registry entry is discoverable, connectable, and usable via bb list and bb mcp"
    (p/let [_ (vscode/commands.executeCommand "joyride.startMcpServer")
            workspace-root vscode/workspace.rootPath
            window (wait-for-window+ workspace-root 20)
            server-name (:serverName window)
            window-id (:windowId window)
            init-out (run-bb+ ["mcp" "initialize"
                               "--server-name" server-name
                               "--window-id" window-id])
            init (parse-envelope init-out)
            call-out (run-bb+ ["mcp" "tools/call"
                               "--server-name" server-name
                               "--window-id" window-id
                               "--name" "joyride_evaluate_code"
                               "--args" "-"]
                              "{\"code\":\"(+ 1 2)\",\"who\":\"gb-test-engineer\"}\n")
            call (parse-envelope call-out)
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
