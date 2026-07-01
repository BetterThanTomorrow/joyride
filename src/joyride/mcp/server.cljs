(ns joyride.mcp.server
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.requests :as requests]
   [promesa.core :as p]
   [vscode-mcp.cursor :as mcp-cursor]
   [vscode-mcp.policy :as mcp-policy]
   [vscode-mcp.server :as mcp-server]))

(defn read-mcp-config []
  (let [config (vscode/workspace.getConfiguration "joyride.mcp")]
    {:auto-start? (.get config "autoStartServer" false)
     :auto-register? (.get config "autoRegisterCursor" true)
     :host (.get config "host")}))

(defn port-file-present? [server-info]
  (when-let [^js port-file-uri (:server/port-file-uri server-info)]
    (let [fs-path (.-fsPath port-file-uri)]
      (boolean (and (seq fs-path) (.existsSync fs fs-path))))))

(defn- start-server!+ [^js context {:keys [host]}]
  (let [storage-uri (.-storageUri context)
        port-file-uri (when storage-uri (vscode/Uri.joinPath storage-uri "mcp-server" "port"))]
    (mcp-server/start-server!+ {:server/host host
                                :server/request-port 0
                                :server/port-file-uri port-file-uri
                                :mcp/on-request (partial requests/handle-request {:extension-context context})
                                :mcp/on-log (fn [level & args]
                                              (apply js/console.log (str "[MCP " (name level) "]") args))})))

(defn maybe-start! [^js context]
  (let [{:keys [auto-start? auto-register? host]} (read-mcp-config)
        cursor-available? (mcp-cursor/cursor-mcp-available?)
        wrapper-path (path/join "dist" "joyride-mcp-server.js")]
    (if (mcp-policy/should-auto-start? {:mcp/auto-start? auto-start?
                                        :mcp/auto-register? auto-register?
                                        :mcp/cursor-available? cursor-available?})
      (-> (start-server!+ context {:host host})
          (p/then (fn [server-info]
                    (swap! db/!app-db assoc :mcp/server-info server-info)
                    (when (mcp-policy/should-register-with-cursor? {:mcp/auto-register? auto-register?
                                                                    :mcp/cursor-available? cursor-available?
                                                                    :mcp/port-file-present? (port-file-present? server-info)})
                      (mcp-cursor/register-and-reload-mcp-client!+
                       {:cursor/server-name "joyride"
                        :vscode/extension-context context
                        :cursor/script-relative-path wrapper-path
                        :server/port-file-uri (:server/port-file-uri server-info)
                        :server/host host}))
                    server-info))
          (p/catch (fn [e]
                     (js/console.error "Failed to start Joyride MCP server:" (.-message e)))))
      (p/resolved nil))))

(defn stop! []
  (when-let [server-info (:mcp/server-info @db/!app-db)]
    (-> (mcp-cursor/unregister-mcp-server!+ {:cursor/server-name "joyride"})
        (p/then (fn [_]
                  (mcp-server/stop-server!+ server-info)))
        (p/then (fn [_]
                  (swap! db/!app-db dissoc :mcp/server-info)))
        (p/catch (fn [e]
                   (js/console.error "Failed to stop Joyride MCP server:" (.-message e)))))))
