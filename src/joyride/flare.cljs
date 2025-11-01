(ns joyride.flare
  "Joyride Flares - WebView panel and sidebar view creation"
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.panel :as panel]
   [joyride.flare.sidebar :as sidebar]
   [promesa.core :as p]
   [joyride.config :as config]))

(defn- current-api-flares
  "Return the current active flares in API shape"
  []
  (-> (:flares @db/!app-db)
      (update-vals #(select-keys % [:view :message-handler]))))

;; TODO Handle namespaced keys (Using `:keyword-fn` probably)
(defn post-message!+
  "Send a message from extension to flare webview.

   Args:
   - flare-key: The key of the flare to send message to
   - message: The message data to send (will be serialized to JSON)

   Returns: the `postMessage` promise"
  [flare-key message]
  (let [flare-data (get (:flares @db/!app-db) flare-key)
        ^js view (:view flare-data)]
    (.postMessage (.-webview view) (clj->js message :keyword-fn #(subs (str %) 1)))))

(defn- default-local-resource-roots
  "Returns default local resource roots: workspace folders + extension dir + joyride user dir"
  []
  (let [workspace-roots (if vscode/workspace.workspaceFolders
                          (into [] (map #(.-uri %) vscode/workspace.workspaceFolders))
                          [])
        extension-uri (.-extensionUri ^js (db/extension-context))
        joyride-user-uri (vscode/Uri.file (config/user-abs-joyride-path))]
    (into [] (concat workspace-roots [extension-uri joyride-user-uri]))))

(defn- normalize-file-option [file-path-or-uri]
  (cond
    (.-scheme file-path-or-uri) file-path-or-uri
    (.isAbsolute path file-path-or-uri) (vscode/Uri.file file-path-or-uri)
    :else (if (and vscode/workspace.workspaceFolders
                   (> (.-length vscode/workspace.workspaceFolders) 0))
            (let [workspace-uri (.-uri (first vscode/workspace.workspaceFolders))]
              (vscode/Uri.joinPath workspace-uri file-path-or-uri))
            (throw (ex-info "Relative file paths require an open workspace. Please use an absolute path or open a workspace folder"
                            {:file-path file-path-or-uri})))))

(defn- normalize-flare-options
  [options]
  (let [k (:key options :anonymous)
        provided-webview-opts (js->clj (:webview-options options) :keywordize-keys true)
        default-local-roots (default-local-resource-roots)
        local-roots (or (:localResourceRoots provided-webview-opts)
                        default-local-roots)
        webview-options (merge {:enableScripts true
                                :localResourceRoots local-roots}
                               provided-webview-opts)]
    (merge {:title "Flare"
            :reveal? true
            :column js/undefined
            :preserve-focus? true
            :icon :flare/icon-default}
           options
           {:key k
            :sidebar-slot (when (sidebar/sidebar-keys k) (sidebar/key->sidebar-slot k))
            :webview-options (clj->js webview-options)}
           (when (:file options)
             {:file (normalize-file-option (:file options))}))))

(defn flare!+
  "Create a WebView panel or sidebar view with the given options.

   Options:
   - :html - HTML content string OR Hiccup data structure
   - :url - URL to display in iframe
   - :file - A string path to a HTML file in the workspace
   - :title - Panel/view title (default: 'WebView')
   - :key - Identifier for reusing panels. Use :sidebar-1 through :sidebar-5 for sidebar views
   - :icon - Icon for panel tab. String (path/URL) or map {:light \"...\" :dark \"...\"}
   - :column - vscode.ViewColumn (default: js/undefined)
   - :reveal? - Whether to reveal the panel when created or reused (default: true)
   - :preserve-focus? - Whether to preserve focus when revealing the panel (default: true)
   - :webview-options - A map with vscode WebviewPanelOptions & WebviewOptions for the webview (default: {:enableScripts true})
   - :message-handler - Function to handle messages from webview. Receives message object.

   Returns: {key view} where key is the flare key and view is the created panel or sidebar view."
  [options]
  (p/let [flare-options (normalize-flare-options options)
          {:keys [sidebar-slot]} flare-options
          view (if sidebar-slot
                 (sidebar/create-view!+ flare-options)
                 (panel/create-view!+ flare-options))]
    {(:key flare-options) view}))

(defn close!
  "Close/dispose a flare panel by key"
  [flare-key]
  (if (get (:flares @db/!app-db) flare-key)
    (let [sidebar? (contains? sidebar/sidebar-keys flare-key)]
      (if sidebar?
        (sidebar/close! flare-key)
        (panel/close! flare-key)))
    false))

(defn close-all!
  "Close all active flare panels"
  []
  (let [flare-keys (keys (:flares @db/!app-db))]
    (doseq [flare-key flare-keys]
      (close! flare-key))
    (count flare-keys)))

(defn get-flare
  "Get a flare by its key, returning only the view and optional message handler when active."
  [flare-key]
  (get (current-api-flares) flare-key))

(defn ls
  "List all currently active flares keyed by flare key."
  []
  (current-api-flares))
