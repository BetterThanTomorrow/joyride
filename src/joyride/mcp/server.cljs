(ns joyride.mcp.server
  (:require
   ["os" :as os]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.requests :as requests]
   [joyride.when-contexts :as when-contexts]
   [promesa.core :as p]
   [vscode-mcp.core :as vscode-mcp]
   [vscode-mcp.cursor :as vscode-mcp-cursor]))

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

(defn- lifecycle-state []
  (or (:mcp/lifecycle-state @db/!app-db) (vscode-mcp/init-state)))

(defn sync-cursor-mcp-when-contexts!
  "Refreshes Cursor MCP when-contexts from current lifecycle state and settings."
  []
  (let [lifecycle (lifecycle-state)
        cursor-available? (vscode-mcp-cursor/cursor-mcp-available?)
        server-running? (vscode-mcp/running? lifecycle)
        cursor-registered? (and server-running?
                                (vscode-mcp/cursor-registered? lifecycle))]
    (when-contexts/set-context! ::when-contexts/joyride.isCursorMcpAvailable cursor-available?)
    (when-contexts/set-context! ::when-contexts/joyride.mcpServerRegisteredWithCursor cursor-registered?)))

(defn- build-lifecycle-config [^js context]
  (let [mcp-config (read-mcp-config)]
    (vscode-mcp/create-config
     (merge mcp-config
            {:vscode/extension-context context
             :cursor/server-name "joyride"
             :manual-setup/extension-name "Joyride"
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
                                             (set-server-running-context! running?)
                                             (sync-cursor-mcp-when-contexts!))
             :lifecycle/on-cursor-registered (fn [_result]
                                               (sync-cursor-mcp-when-contexts!))
             :lifecycle/on-cursor-registration-failed (fn [_failure]
                                                        (sync-cursor-mcp-when-contexts!))}))))

(defn- update-lifecycle-state!+ [state+]
  (p/then state+
          (fn [state]
            (swap! db/!app-db assoc :mcp/lifecycle-state state)
            (sync-cursor-mcp-when-contexts!)
            state)))

(defn- register-failure-message [{:keys [reason]}]
  (case reason
    :cursor-api-unavailable
    "Cursor MCP registration API is not available in this editor."

    :registration-failed
    "Could not register Joyride MCP server with Cursor."

    "Could not register Joyride MCP server with Cursor."))

(defn server-running? []
  (vscode-mcp/running? (lifecycle-state)))

(defn init-cursor-mcp-when-contexts! []
  (sync-cursor-mcp-when-contexts!))

(defn maybe-start! [^js context]
  (update-lifecycle-state!+
   (vscode-mcp/maybe-start!+ (build-lifecycle-config context) (lifecycle-state) true)))

(defn start! [^js context]
  (update-lifecycle-state!+
   (vscode-mcp/start!+ (build-lifecycle-config context) (lifecycle-state) false)))

(defn register-with-cursor! [^js context]
  (p/let [result (vscode-mcp/register-with-cursor!+
                   (build-lifecycle-config context)
                   (lifecycle-state))
          _ (when-not (:ok result)
              (vscode/window.showInformationMessage
               (register-failure-message result)))]
    (update-lifecycle-state!+ (p/resolved (:state result)))))

(defn stop!
  ([] (stop! nil {:lifecycle/silent? true}))
  ([^js context] (stop! context {:lifecycle/silent? false}))
  ([^js context stop-options]
   (update-lifecycle-state!+
    (vscode-mcp/stop!+ (build-lifecycle-config context) (lifecycle-state) stop-options))))
