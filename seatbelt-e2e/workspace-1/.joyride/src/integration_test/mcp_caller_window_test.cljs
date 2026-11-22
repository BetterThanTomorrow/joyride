(ns integration-test.mcp-caller-window-test
  "The caller's window. `bb list`, `bb mcp` and the stdio wrapper with no port file find the
   window they run in, as a client that an extension starts does: under this window's
   extension host. The window's terminals carry its windowId."
  (:require
   [cljs.test :refer [is testing]]
   [clojure.string :as string]
   [integration-test.macros :refer [deftest-async]]
   [integration-test.mcp-client :as mcp-client]
   [joyride.core :as joyride]
   [promesa.core :as p]
   ["child_process" :as cp]
   ["fs" :as fs]
   ["os" :as os]
   ["path" :as path]
   ["vscode" :as vscode]))

(def ^:private window-id-env "VSCODE_MCP_WINDOW_ID")

(defn- this-window-token!
  "A fresh token in this extension host's global scope. An evaluation that returns it ran in
   this window."
  []
  (let [token (str "caller-window-" (random-uuid))]
    (set! (.-joyrideCallerWindowToken js/globalThis) token)
    token))

(def ^:private token-code
  "js/globalThis.joyrideCallerWindowToken")

(defn- process-env []
  (into {} (map vec) (js/Object.entries js/process.env)))

(defn- parse-json-lines [out]
  (->> (string/split-lines out)
       (keep #(try
                (js->clj (js/JSON.parse %) :keywordize-keys true)
                (catch :default _ nil)))))

(defn- run-wrapper+
  "Runs the stdio wrapper at `wrapper-path` with no port argument, in the environment `env`,
   and writes the JSON-RPC `requests` to it. Resolves to `{:responses {id response} :stderr …}`
   once every request is answered, or after `timeout-ms`."
  [wrapper-path env requests timeout-ms]
  (p/create
   (fn [resolve _reject]
     (let [proc (cp/spawn "node" #js [wrapper-path] #js {:env (clj->js env)})
           out (atom "")
           err (atom "")
           wanted (set (map :id requests))
           answers #(into {} (map (juxt :id identity)) (parse-json-lines @out))
           !timer (atom nil)
           finish! (fn []
                     (js/clearTimeout @!timer)
                     (.kill proc)
                     (resolve {:responses (answers) :stderr @err}))]
       (reset! !timer (js/setTimeout finish! timeout-ms))
       (.on (.-stdout proc) "data"
            (fn [d]
              (swap! out str (str d))
              (when (every? (set (keys (answers))) wanted)
                (finish!))))
       (.on (.-stderr proc) "data" (fn [d] (swap! err str (str d))))
       (.on proc "error" (fn [e] (swap! err str (.-message e)) (finish!)))
       (doseq [request requests]
         (.write (.-stdin proc) (str (js/JSON.stringify (clj->js request)) "\n")))))))

(defn- tool-text [response]
  (string/join " " (map :text (get-in response [:result :content]))))

(defn- terminal-writing-env-var
  "Options for a terminal that writes the value of `env-var` in its environment to
   `out-file`: `cmd.exe` on Windows, `/bin/sh` elsewhere."
  [env-var out-file]
  (if (= "win32" js/process.platform)
    #js {:name "caller window test"
         :shellPath "cmd.exe"
         :shellArgs #js ["/d" "/c" (str "echo %" env-var "%>\"" out-file "\"")]}
    #js {:name "caller window test"
         :shellPath "/bin/sh"
         :shellArgs #js ["-c" (str "printf %s \"$" env-var "\" > " out-file)]}))

(defn- wait-for-file-text+
  "The text of `file` once it has some, or nil after `tries` waits of 250 ms."
  [file tries]
  (let [text (when (fs/existsSync file) (fs/readFileSync file "utf8"))]
    (if (or (seq text) (zero? tries))
      text
      (p/then (p/delay 250) (fn [_] (wait-for-file-text+ file (dec tries)))))))

(defn- log [& xs]
  (apply js/console.log "[caller-window-test]" xs))

(defn- this-window-listed? [out workspace-root]
  (let [root (some-> workspace-root fs/realpathSync)]
    (some #(and (= "joyride" (:serverName %))
                (= root (some-> (:workspaceRoot %) fs/realpathSync)))
          (mcp-client/parse-envelope out))))

(defn- wait-until-stopped+
  "Resolves once this window's Joyride entry is gone from `bb list`, or after `tries` waits."
  [workspace-root tries]
  (p/let [out (mcp-client/run-bb+ ["list" "--all" "--json"])]
    (when (and (this-window-listed? out workspace-root) (pos? tries))
      (p/then (p/delay 250) (fn [_] (wait-until-stopped+ workspace-root (dec tries)))))))

(defn- fresh-server+
  "Stops Joyride's MCP server, waits until its entry is gone, and starts it again. The
   previous test's stop isn't awaited, and a start while it runs would find the server running."
  []
  (let [workspace-root (some-> vscode/workspace.workspaceFolders first .-uri .-fsPath)]
    (p/let [_ (log "stopping")
            _ (vscode/commands.executeCommand "joyride.stopMcpServer")
            _ (wait-until-stopped+ workspace-root 40)
            _ (log "starting")
            window (mcp-client/start-server+)]
      (log "started" (:windowId window))
      window)))

(defn- within+
  "`promise`, or a rejection naming `what` after `ms`."
  [what ms promise]
  (p/race [promise
           (p/then (p/delay ms) (fn [_] (throw (js/Error. (str what " took more than " ms " ms")))))]))

(deftest-async bb-finds-the-caller-window
  (testing "bb list and bb mcp, run under this window's extension host, need no --window-id"
    (p/let [window (within+ "starting the server" 30000 (fresh-server+))
            token (this-window-token!)
            _ (log "bb list")
            listed (within+ "bb list" 30000
                            (p/-> (mcp-client/run-bb+ ["list" "--json"])
                                  mcp-client/parse-envelope))
            listed-all (within+ "bb list --all" 30000
                                (p/-> (mcp-client/run-bb+ ["list" "--all" "--json"])
                                      mcp-client/parse-envelope))
            _ (log "bb mcp")
            call (within+ "bb mcp" 30000
                          (p/-> (mcp-client/run-bb+ ["mcp" "tools/call"
                                                     "--server-name" "joyride"
                                                     "--name" "joyride_evaluate_code"
                                                     "--args" "-"]
                                                    (str (js/JSON.stringify #js {:code token-code
                                                                                 :who "gb-test-engineer"})
                                                         "\n"))
                                mcp-client/parse-envelope))]
      (is (seq listed) "bb list lists the caller's window")
      (is (every? #(= (:windowId window) (:windowId %)) listed)
          (str "bb list lists only the caller's window " (:windowId window) ", got "
               (pr-str (map :windowId listed))))
      (is (every? #(= [:wrapperPath] (keys (:mcp %))) listed)
          (str "bb list gives the caller's window the wrapper and no port file, got "
               (pr-str (map :mcp listed))))
      (is (some #(= (:windowId window) (:windowId %)) listed-all)
          "bb list --all lists the caller's window too")
      (is (true? (:ok call)) (str "bb mcp without --window-id succeeds: " (pr-str call)))
      (is (string/includes? (tool-text call) token)
          (str "bb mcp without --window-id evaluates in this window, got: " (tool-text call))))))

(deftest-async wrapper-finds-the-caller-window
  (testing "The stdio wrapper with no port file, started under this window's extension host, connects to this window"
    (p/let [window (within+ "starting the server" 30000 (fresh-server+))
            token (this-window-token!)
            wrapper-path (get-in window [:mcp :wrapperPath])
            _ (log "wrapper" wrapper-path)
            {:keys [responses stderr]}
            (run-wrapper+ wrapper-path
                          (process-env)
                          [{:jsonrpc "2.0" :id 1 :method "initialize" :params {}}
                           {:jsonrpc "2.0" :id 2 :method "tools/call"
                            :params {:name "joyride_evaluate_code"
                                     :arguments {:code token-code :who "gb-test-engineer"}}}]
                          ;; A cold PowerShell process query took 45 s in a Windows VM
                          60000)]
      (is (string? wrapper-path) "the registry entry names the installed wrapper")
      (is (= "joyride" (get-in responses [1 :result :serverInfo :name]))
          (str "the wrapper answers initialize from Joyride, stderr: " stderr))
      (is (string/includes? (tool-text (get responses 2)) token)
          (str "the wrapper evaluates in this window, got: " (pr-str (get responses 2))
               " stderr: " stderr)))))

(deftest-async wrapper-fails-outside-every-window
  (testing "The stdio wrapper with no port file and nothing that names a window answers with no-window"
    (p/let [window (within+ "starting the server" 30000 (fresh-server+))
            env (dissoc (process-env)
                        "VSCODE_CRASH_REPORTER_PROCESS_TYPE" "VSCODE_PID" window-id-env)
            {:keys [responses stderr]}
            (run-wrapper+ (get-in window [:mcp :wrapperPath])
                          env
                          [{:jsonrpc "2.0" :id 1 :method "initialize" :params {}}]
                          15000)
            error (get-in responses [1 :error])]
      (is (= -32001 (:code error))
          (str "initialize gets an error response, got: " (pr-str responses) " stderr: " stderr))
      (is (string/includes? (str (:message error)) "No window found for joyride")
          (str "the error says no window was found, got: " (:message error))))))

(deftest-async terminals-carry-the-window-id
  {:after (vscode/commands.executeCommand "joyride.stopMcpServer")}
  (testing "The window's terminals have VSCODE_MCP_WINDOW_ID set to its windowId"
    (p/let [window (within+ "starting the server" 30000 (fresh-server+))
            mutator (.get (.-environmentVariableCollection (joyride/extension-context))
                          window-id-env)
            out-file (path/join (os/tmpdir) (str "joyride-terminal-window-id-" (random-uuid)))
            terminal (vscode/window.createTerminal (terminal-writing-env-var window-id-env out-file))
            _ (log "terminal")
            text (wait-for-file-text+ out-file 40)]
      (.dispose terminal)
      (is (= (:windowId window) (some-> mutator .-value))
          "Joyride's terminal environment collection names this window")
      (is (= (:windowId window) (some-> text string/trim))
          (str "a new terminal has " window-id-env " set to this window's id, got: " (pr-str text))))))
