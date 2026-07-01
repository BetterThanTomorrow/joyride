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
     :server/host (.get config "host")}))

(defn- set-server-running-context! [running?]
  (when-contexts/set-context! ::when-contexts/joyride.isMcpServerRunning running?))

(defn- build-lifecycle-config [^js context]
  (lifecycle/create-config
   (merge (read-mcp-config)
          {:vscode/extension-context context
           :cursor/server-name "joyride"
           :cursor/script-relative-path "dist/joyride-mcp-server.js"
           :mcp/on-request (partial requests/handle-request {:extension-context context})
           :mcp/on-log (fn [level & args]
                         (apply js/console.log (str "[MCP " (name level) "]") args))
           :lifecycle/port-file-uri+ (fn [^js ctx _opts]
                                       (when-let [storage-uri (.-storageUri ctx)]
                                         (vscode/Uri.joinPath storage-uri "mcp-server" "port")))
           :lifecycle/request-port (fn [_ctx _opts] 0)
           :lifecycle/wrapper-path (fn [^js ctx _server-info]
                                     (path/join (.-extensionPath ctx) "dist" "joyride-mcp-server.js"))
           :lifecycle/on-running-changed (fn [running? _server-info]
                                          (set-server-running-context! running?))})))

;; Settings are read once into a static config (see plan Decision Q6):
;; changes take effect on the next activation, matching prior behavior.
(defn- lifecycle-config! [^js context]
  (or (:mcp/lifecycle-config @db/!app-db)
      (let [config (build-lifecycle-config context)]
        (swap! db/!app-db assoc :mcp/lifecycle-config config)
        config)))

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
   (lifecycle/maybe-start!+ (lifecycle-config! context) (lifecycle-state))))

(defn start! [^js context]
  (update-lifecycle-state!+
   (lifecycle/start!+ (lifecycle-config! context) (lifecycle-state))))

(defn stop!
  ([] (stop! nil {:lifecycle/silent? true}))
  ([^js context] (stop! context {:lifecycle/silent? false}))
  ([^js context opts]
   (update-lifecycle-state!+
    (lifecycle/stop!+ (lifecycle-config! context) (lifecycle-state) opts))))
