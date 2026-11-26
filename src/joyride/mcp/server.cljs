(ns joyride.mcp.server
  (:require
   ["os" :as os]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.mcp.registry :as registry]
   [joyride.mcp.requests :as requests]
   [joyride.when-contexts :as when-contexts]
   [promesa.core :as p]
   [vscode-mcp.core :as vscode-mcp]
   [vscode-mcp.cursor :as vscode-mcp-cursor]))

(defn- read-mcp-config []
  (let [config (vscode/workspace.getConfiguration "joyride.mcp")]
    {:mcp/auto-start? (.get config "autoStartServer" false)
     :mcp/auto-register? (.get config "autoRegisterCursor" true)
     :mcp/auto-register-eca? (.get config "autoRegisterEca" true)
     :registry/enabled? (registry/enabled? (.get config "enableRegistry" true))
     :server/host (.get config "host")
     :server/request-port (.get config "port" 0)}))

(defn- server-state
  "The MCP server's state, as vscode-mcp reported it last: plain data."
  []
  (:mcp/server-state @db/!app-db))

(defn sync-cursor-mcp-when-contexts!
  "Sets the MCP when-contexts from the server's state."
  []
  (let [{:server/keys [running?]
         :lifecycle/keys [registered-name]} (server-state)]
    (when-contexts/set-context! ::when-contexts/joyride.isMcpServerRunning (boolean running?))
    (when-contexts/set-context! ::when-contexts/joyride.isCursorMcpAvailable (vscode-mcp-cursor/cursor-mcp-available?))
    (when-contexts/set-context! ::when-contexts/joyride.mcpServerRegisteredWithCursor (boolean (and running?
                                                                                                    registered-name)))))

(defn- server-changed!
  "Takes the server's state, as vscode-mcp reports it when it changes."
  [server-state]
  (swap! db/!app-db assoc :mcp/server-state server-state)
  (sync-cursor-mcp-when-contexts!))

(defn- build-lifecycle-config [^js context]
  (vscode-mcp/create-config
   (merge (read-mcp-config)
          {:vscode/extension-context context
           :server/name "joyride"
           :wrapper/script-relative-path "dist/joyride-mcp-server.js"
           :manual-setup/extension-name "Joyride"
           :lifecycle/wrapper-install-dir (path/join (os/homedir) ".config" "joyride")
           :registry/custom-data (registry/compact-data)
           :mcp/on-request (partial requests/handle-request {:extension-context context})
           :mcp/on-log (fn [level & args]
                         (apply js/console.log (str "[MCP " (name level) "]") args))
           :mcp/on-server-changed server-changed!})))

(defn- register-failure-message [{:keys [reason]}]
  (case reason
    :cursor-api-unavailable
    "Cursor MCP registration API is not available in this editor."

    "Could not register Joyride MCP server with Cursor."))

(defn server-running? []
  (boolean (:server/running? (server-state))))

(defn init-cursor-mcp-when-contexts! []
  (sync-cursor-mcp-when-contexts!))

(defn maybe-start! [^js context]
  (vscode-mcp/maybe-start!+ (build-lifecycle-config context)))

(defn start! [^js context]
  (vscode-mcp/start!+ (build-lifecycle-config context)))

(defn register-with-cursor! [^js context]
  (p/let [server-state (vscode-mcp/register-with-cursor!+ (build-lifecycle-config context))]
    (when-let [registration-error (:lifecycle/registration-error server-state)]
      (vscode/window.showInformationMessage (register-failure-message registration-error)))
    server-state))

(defn stop!
  ([] (stop! nil {:lifecycle/silent? true}))
  ([^js context] (stop! context {:lifecycle/silent? false}))
  ([^js context stop-options]
   (vscode-mcp/stop!+ (build-lifecycle-config context) stop-options)))
