(ns joyride.mcp.server
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.requests :as requests]
   [promesa.core :as p]
   [vscode-mcp.cursor :as mcp-cursor]
   [vscode-mcp.server :as mcp-server]))

(defn start! [^js context]
  (let [wrapper-path (path/join "dist" "joyride-mcp-server.js")
        storage-uri (.-storageUri context)
        port-file-uri (when storage-uri (vscode/Uri.joinPath storage-uri "mcp-server" "port"))]
    (-> (mcp-server/start-server!+ {:on-request (partial requests/handle-request {:extension-context context})
                                    :port 0
                                    :port-file-uri port-file-uri
                                    :on-log (fn [level & args]
                                              (apply js/console.log (str "[MCP " (name level) "]") args))})
        (p/then (fn [server-info]
                  (swap! db/!app-db assoc :mcp/server-info server-info)
                  (mcp-cursor/register-and-reload-mcp-client!+
                   "joyride"
                   context
                   wrapper-path
                   (:server/port-file-uri server-info))))
        (p/catch (fn [e]
                   (js/console.error "Failed to start Joyride MCP server:" (.-message e)))))))

(defn stop! []
  (when-let [server-info (:mcp/server-info @db/!app-db)]
    (-> (mcp-cursor/unregister-mcp-server!+ "Joyride MCP")
        (p/then (fn [_]
                  (mcp-server/stop-server!+ server-info)))
        (p/then (fn [_]
                  (swap! db/!app-db dissoc :mcp/server-info)))
        (p/catch (fn [e]
                   (js/console.error "Failed to stop Joyride MCP server:" (.-message e)))))))
