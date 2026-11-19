(ns integration-test.mcp-client
  "Reaches Joyride's MCP server the way external agents do, through `bb list` and `bb mcp`."
  (:require
   [clojure.string :as string]
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

(defn run-bb+
  ([args] (run-bb+ args nil))
  ([args stdin]
   (p/create
    (fn [resolve reject]
      (fs/mkdirSync registry-home #js {:recursive true})
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

(defn parse-envelope [stdout]
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

(defn wait-for-window+ [workspace-root tries]
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

(defn start-server+
  "Starts Joyride's MCP server and resolves to this window's `bb list` entry."
  []
  (p/let [_ (vscode/commands.executeCommand "joyride.startMcpServer")
          workspace-root (some-> vscode/workspace.workspaceFolders first .-uri .-fsPath)]
    (wait-for-window+ workspace-root 20)))

(defn evaluate+
  "Calls `joyride_evaluate_code` in `window` with the tool arguments `args`, and resolves to the
   tool result's text parsed as JSON, keywordized, or to `{:text …}` when the text isn't JSON."
  [{:keys [serverName windowId]} args]
  (p/let [out (run-bb+ ["mcp" "tools/call"
                        "--server-name" serverName
                        "--window-id" windowId
                        "--name" "joyride_evaluate_code"
                        "--args" "-"]
                       (str (js/JSON.stringify (clj->js args)) "\n"))
          text (string/join (map :text (get-in (parse-envelope out) [:result :content])))]
    (try
      (js->clj (js/JSON.parse text) :keywordize-keys true)
      (catch :default _
        {:text text}))))
