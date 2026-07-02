(ns joyride.mcp.server
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.requests :as requests]
   [joyride.when-contexts :as when-contexts]
   [promesa.core :as p]
   [vscode-mcp.lifecycle :as lifecycle]))

(defn- read-mcp-config []
  (let [config (vscode/workspace.getConfiguration "joyride.mcp")]
    {:mcp/auto-start? (.get config "autoStartServer" false)
     :mcp/auto-register? (.get config "autoRegisterCursor" true)
     :server/host (.get config "host")
     :server/request-port (.get config "port" 0)}))

(defn- set-server-running-context! [running?]
  (when-contexts/set-context! ::when-contexts/joyride.isMcpServerRunning running?))

(defn- build-lifecycle-config [^js context]
  (let [mcp-config (read-mcp-config)]
    (lifecycle/create-config
     (merge mcp-config
            {:vscode/extension-context context
             :cursor/server-name "joyride"
             :cursor/script-relative-path "dist/joyride-mcp-server.js"
             :mcp/on-request (partial requests/handle-request {:extension-context context})
             :mcp/on-log (fn [level & args]
                           (apply js/console.log (str "[MCP " (name level) "]") args))
             :lifecycle/port-file-uri+ (fn [^js ctx _opts]
                                         (when-let [storage-uri (.-storageUri ctx)]
                                           (vscode/Uri.joinPath storage-uri "mcp-server" "port")))
             :lifecycle/request-port (fn [_ctx _opts] (:server/request-port (read-mcp-config)))
             :lifecycle/wrapper-path (fn [^js ctx _server-info]
                                       (path/join (.-extensionPath ctx) "dist" "joyride-mcp-server.js"))
             :lifecycle/on-running-changed (fn [running? _server-info]
                                             (set-server-running-context! running?))}))))

(defn- lifecycle-state []
  (or (:mcp/lifecycle-state @db/!app-db) (lifecycle/init-state)))

(defn- update-lifecycle-state!+ [state+]
  (p/then state+ (fn [state]
                   (swap! db/!app-db assoc :mcp/lifecycle-state state)
                   state)))

(defn server-running? []
  (lifecycle/running? (lifecycle-state)))

(defn maybe-start! [^js context]
  (update-lifecycle-state!+
   (lifecycle/maybe-start!+ (build-lifecycle-config context) (lifecycle-state))))

(defn start! [^js context]
  (update-lifecycle-state!+
   (lifecycle/start!+ (build-lifecycle-config context) (lifecycle-state))))

(defn stop!
  ([] (stop! nil {:lifecycle/silent? true}))
  ([^js context] (stop! context {:lifecycle/silent? false}))
  ([^js context opts]
   (update-lifecycle-state!+
    (lifecycle/stop!+ (build-lifecycle-config context) (lifecycle-state) opts))))
