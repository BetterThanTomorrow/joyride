(ns joyride.mcp.server
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.requests :as requests]
   [joyride.when-contexts :as when-contexts]
   [promesa.core :as p]
   [vscode-mcp.cursor :as mcp-cursor]
   [vscode-mcp.policy :as mcp-policy]
   [vscode-mcp.server :as mcp-server]
   [vscode-mcp.stdio-config :as stdio-config]))

(defn read-mcp-config []
  (let [config (vscode/workspace.getConfiguration "joyride.mcp")]
    {:auto-start? (.get config "autoStartServer" false)
     :auto-register? (.get config "autoRegisterCursor" true)
     :host (.get config "host")}))

(defn server-running? []
  (boolean (:mcp/server-info @db/!app-db)))

(defn- set-server-running-context! [running?]
  (when-contexts/set-context! ::when-contexts/joyride.isMcpServerRunning running?))

(defn- wrapper-script-path [^js context]
  (path/join (.-extensionPath context) "dist" "joyride-mcp-server.js"))

(defn port-file-present? [server-info]
  (when-let [^js port-file-uri (:server/port-file-uri server-info)]
    (let [fs-path (.-fsPath port-file-uri)]
      (boolean (and (seq fs-path) (.existsSync fs fs-path))))))

(defn copy-command-strings [wrapper-path server-info]
  (let [{:server/keys [assigned-port ^js port-file-uri host]} server-info
        port-file-path (some-> port-file-uri .-fsPath)]
    {:port-command (stdio-config/stdio-command-string "node" wrapper-path (str assigned-port) host)
     :port-file-command (stdio-config/stdio-command-string "node" wrapper-path port-file-path host)}))

(defn- start-server!+ [^js context {:keys [host]}]
  (let [storage-uri (.-storageUri context)
        port-file-uri (when storage-uri (vscode/Uri.joinPath storage-uri "mcp-server" "port"))]
    (mcp-server/start-server!+ {:server/host host
                                :server/request-port 0
                                :server/port-file-uri port-file-uri
                                :mcp/on-request (partial requests/handle-request {:extension-context context})
                                :mcp/on-log (fn [level & args]
                                              (apply js/console.log (str "[MCP " (name level) "]") args))})))

(defn- show-server-started-message!+ [^js context server-info]
  (let [{:server/keys [assigned-port port-note]} server-info
        wrapper-path (wrapper-script-path context)
        {:keys [port-command port-file-command]} (copy-command-strings wrapper-path server-info)
        message (str port-note " MCP socket server started on port: " assigned-port)]
    (p/let [button (vscode/window.showInformationMessage
                    message
                    "Copy command + port"
                    "Copy command + port-file")]
      (case button
        "Copy command + port"
        (vscode/env.clipboard.writeText port-command)

        "Copy command + port-file"
        (vscode/env.clipboard.writeText port-file-command)

        nil))))

(defn- register-with-cursor-if-needed!+ [context server-info config]
  (let [{:keys [auto-register? host]} config
        cursor-available? (mcp-cursor/cursor-mcp-available?)]
    (if (mcp-policy/should-register-with-cursor? {:mcp/auto-register? auto-register?
                                                  :mcp/cursor-available? cursor-available?
                                                  :mcp/port-file-present? (port-file-present? server-info)})
      (-> (mcp-cursor/register-and-reload-mcp-client!+
           {:cursor/server-name "joyride"
            :vscode/extension-context context
            :cursor/script-relative-path "dist/joyride-mcp-server.js"
            :server/port-file-uri (:server/port-file-uri server-info)
            :server/host host})
          (p/then (fn [result]
                    (when (:ok result)
                      (swap! db/!app-db assoc :mcp/cursor-registered? true))
                    result)))
      (p/resolved nil))))

(defn- start-server-internal!+ [context {:keys [host silent? register?]}]
  (let [config (read-mcp-config)]
    (-> (start-server!+ context {:host host})
        (p/then (fn [server-info]
                  (swap! db/!app-db assoc :mcp/server-info server-info)
                  (set-server-running-context! true)
                  (p/do!
                   (when register?
                     (register-with-cursor-if-needed!+ context server-info config))
                   (when-not silent?
                     (show-server-started-message!+ context server-info))
                   server-info)))
        (p/catch (fn [e]
                   (js/console.error "Failed to start Joyride MCP server:" (.-message e)))))))

(defn maybe-start! [^js context]
  (let [{:keys [auto-start? auto-register? host]} (read-mcp-config)
        cursor-available? (mcp-cursor/cursor-mcp-available?)]
    (if (mcp-policy/should-auto-start? {:mcp/auto-start? auto-start?
                                        :mcp/auto-register? auto-register?
                                        :mcp/cursor-available? cursor-available?})
      (start-server-internal!+ context {:host host :silent? true :register? auto-register?})
      (p/resolved nil))))

(defn start! [^js context]
  (if (server-running?)
    (p/do (vscode/window.showInformationMessage "MCP server is already running")
          nil)
    (let [{:keys [auto-register? host]} (read-mcp-config)]
      (start-server-internal!+ context {:host host :silent? false :register? auto-register?}))))

(defn stop!
  ([] (stop! nil {:silent? true}))
  ([^js context] (stop! context {}))
  ([^js _context {:keys [silent?]}]
   (if-let [server-info (:mcp/server-info @db/!app-db)]
     (let [cursor-registered? (:mcp/cursor-registered? @db/!app-db)]
       (-> (if cursor-registered?
             (mcp-cursor/unregister-mcp-server!+ {:cursor/server-name "joyride"})
             (p/resolved true))
           (p/then (fn [_] (mcp-server/stop-server!+ server-info)))
           (p/then (fn [_]
                     (swap! db/!app-db dissoc :mcp/server-info :mcp/cursor-registered?)
                     (set-server-running-context! false)
                     (when-not silent?
                       (vscode/window.showInformationMessage "MCP server stopped"))))
           (p/catch (fn [e]
                      (js/console.error "Failed to stop Joyride MCP server:" (.-message e))))))
     (p/resolved nil))))
