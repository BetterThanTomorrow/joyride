(ns joyride.mcp.server
  (:require
   ["os" :as os]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.requests :as requests]
   [joyride.when-contexts :as when-contexts]
   [promesa.core :as p]
   [vscode-mcp.core :as vscode-mcp]))

(defn- read-mcp-config []
  (let [config (vscode/workspace.getConfiguration "joyride.mcp")]
    {:mcp/auto-start? (.get config "autoStartServer" false)
     :mcp/auto-register? (.get config "autoRegisterCursor" true)
     :server/host (.get config "host")
     :server/request-port (.get config "port" 0)}))

(defn- get-workspace-root-uri-or-nil []
  (some-> vscode/workspace.workspaceFolders
          first
          .-uri))

(defn- get-server-dir+ [ctx-or-base-uri]
  (let [base (cond
               (instance? vscode/Uri ctx-or-base-uri) ctx-or-base-uri
               (get-workspace-root-uri-or-nil) (get-workspace-root-uri-or-nil)
               :else (.-globalStorageUri ^js ctx-or-base-uri))]
    (vscode/Uri.joinPath base ".joyride" "mcp-server")))

(defn- get-port-file-uri+ [ctx-or-base-uri]
  (vscode/Uri.joinPath (get-server-dir+ ctx-or-base-uri) "port"))

(defn- get-cursor-port-file-uri [instance-slug]
  (vscode/Uri.file (path/join (os/tmpdir) "joyride-mcp-server" instance-slug "port")))

(defn- set-server-running-context! [running?]
  (when-contexts/set-context! ::when-contexts/joyride.isMcpServerRunning running?))

(defn- build-lifecycle-config [^js context]
  (let [mcp-config (read-mcp-config)]
    (vscode-mcp/create-config
     (merge mcp-config
            {:vscode/extension-context context
             :cursor/server-name "joyride"
             :cursor/script-relative-path "dist/joyride-mcp-server.js"
             :mcp/on-request (partial requests/handle-request {:extension-context context})
             :mcp/on-log (fn [level & args]
                           (apply js/console.log (str "[MCP " (name level) "]") args))
             :lifecycle/port-file-uri+ (fn [^js ctx {:lifecycle/keys [cursor-mode? instance-slug]}]
                                         (if cursor-mode?
                                           (get-cursor-port-file-uri instance-slug)
                                           (get-port-file-uri+ ctx)))
             :lifecycle/request-port (fn [_ctx {:lifecycle/keys [cursor-mode?]}]
                                       (if cursor-mode?
                                         0
                                         (:server/request-port (read-mcp-config))))
             :lifecycle/wrapper-path (fn [^js ctx _server-info]
                                       (path/join (.-extensionPath ctx) "dist" "joyride-mcp-server.js"))
             :lifecycle/on-running-changed (fn [running? _server-info]
                                             (set-server-running-context! running?))}))))

(defn- lifecycle-state []
  (or (:mcp/lifecycle-state @db/!app-db) (vscode-mcp/init-state)))

(defn- update-lifecycle-state!+ [state+]
  (p/then state+ (fn [state]
                   (swap! db/!app-db assoc :mcp/lifecycle-state state)
                   state)))

(defn server-running? []
  (vscode-mcp/running? (lifecycle-state)))

(defn maybe-start! [^js context]
  (update-lifecycle-state!+
   (vscode-mcp/maybe-start!+ (build-lifecycle-config context) (lifecycle-state) true)))

(defn start! [^js context]
  (update-lifecycle-state!+
   (vscode-mcp/start!+ (build-lifecycle-config context) (lifecycle-state) false)))

(defn stop!
  ([] (stop! nil {:lifecycle/silent? true}))
  ([^js context] (stop! context {:lifecycle/silent? false}))
  ([^js context {:lifecycle/keys [silent?]}]
   (update-lifecycle-state!+
    (vscode-mcp/stop!+ (build-lifecycle-config context) (lifecycle-state) silent?))))
