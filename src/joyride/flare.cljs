(ns joyride.flare
  "Joyride Flares - WebView panel and sidebar view creation"
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.panel :as panel]
   [joyride.flare.sidebar-provider :as sidebar]
   [joyride.when-contexts :as when-contexts]))

(defn post-message!
  "Send a message from extension to flare webview.

   Args:
   - flare-key: The key of the flare to send message to
   - message: The message data to send (will be serialized to JSON)

   Returns: a map with the .postMessage promises for the :panel or :sidebar flares matching the key"
  [flare-key message]
  (let [panel-data (get (:flare-panels @db/!app-db) flare-key)
        sidebar-data (get (:flare-sidebars @db/!app-db) flare-key)]
    (cond-> {}
      (and panel-data (not (.-disposed ^js (:view panel-data))))
      (assoc :panel
             (let [^js webview (.-webview ^js (:view panel-data))]
               (.postMessage webview (clj->js message))))

      (and sidebar-data (:view sidebar-data))
      (assoc :sidebar
             (let [^js webview (.-webview ^js (:view sidebar-data))]
               (.postMessage webview (clj->js message)))))))

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

(def ^:private sidebar-keys #{:sidebar-1 :sidebar-2 :sidebar-3 :sidebar-4 :sidebar-5})

(defn- key->sidebar-slot [k]
  (let [key-str (name k)]
    (js/parseInt (subs key-str 8))))

(defn- normalize-flare-options
  [options]
  (let [k (:key options ::anonymous)]
    (merge options
           {:key k
            :title "Flare"
            :reveal? true
            :column js/undefined
            :preserve-focus? true
            :icon :flare/icon-default
            :sidebar-slot (when (sidebar-keys k) (key->sidebar-slot k))}
           {:webview-options (or (clj->js (:webview-options options))
                                 #js {:enableScripts true})}
           (when (:file options)
             {:file (normalize-file-option (:file options))}))))

(defn flare!
  "Create a WebView panel or sidebar view with the given options.

   Options:
   - :html - HTML content string OR Hiccup data structure
   - :url - URL to display in iframe
   - :title - Panel/view title (default: 'WebView')
   - :key - Identifier for reusing panels. Use :sidebar-1 through :sidebar-5 for sidebar views
   - :icon - Icon for panel tab. String (path/URL) or map {:light \"...\" :dark \"...\"}
   - :column - vscode.ViewColumn (default: js/undefined)
   - :reveal? - Whether to reveal the panel when created or reused (default: true)
   - :preserve-focus? - Whether to preserve focus when revealing the panel (default: true)
   - webview-options - JS object vscode WebviewPanelOptions & WebviewOptions for the webview (default: {:enableScripts true})
   - :message-handler - Function to handle messages from webview. Receives message object.

   Returns: {:panel <webview-panel>} or {:sidebar <webview-view>}"
  [options]
  (let [flare-options (normalize-flare-options options)
        {:keys [sidebar-slot key reveal? preserve-focus?]} flare-options]
    (if sidebar-slot
      (do
        (when-contexts/set-flare-content-context! sidebar-slot true)
        (let [sidebar-data (get (:flare-sidebars @db/!app-db) key)
              view (sidebar/ensure-sidebar-view! sidebar-slot)]
          (if (= view :pending)
            (do
            ;; Store the flare options for when view becomes available
              (swap! db/!app-db assoc-in [:flare-sidebar-views sidebar-slot :pending-flare]
                     {:key key :options flare-options})
              (when reveal?
                (vscode/commands.executeCommand (str "joyride.flare-" sidebar-slot ".focus")
                                                preserve-focus?))
              {:sidebar :pending})
            (do
              (when-let [^js disposable (:message-handler sidebar-data)]
                (.dispose disposable))
              (swap! db/!app-db assoc-in [:flare-sidebars key] {:view view})
              (panel/update-view-with-options! view flare-options)
              (when reveal?
                (.show view preserve-focus?))
              {:sidebar view}))))
      (let [panel (panel/create-webview-panel! flare-options)]
        {:panel panel}))))

(defn close!
  "Close/dispose a flare panel by key"
  [flare-key]
  (if-let [panel-data (get (:flare-panels @db/!app-db) flare-key)]
    (let [^js panel (:view panel-data)]
      (if (.-disposed panel)
        false
        (do
          (when-let [^js disposable (:message-handler panel-data)]
            (.dispose disposable))
          (.dispose panel)
          (swap! db/!app-db update :flare-panels dissoc flare-key)
          true)))
    ;; Check if it's a sidebar flare
    (if-let [sidebar-data (get (:flare-sidebars @db/!app-db) flare-key)]
      (let [^js view (:view sidebar-data)]
        (if (.-disposed view)
          false
          (do
            (when-let [^js disposable (:message-handler sidebar-data)]
              (.dispose disposable))
            ;; For sidebar views, clear content instead of disposing
            (set! (.-html (.-webview view)) "")
            (swap! db/!app-db update :flare-sidebars dissoc flare-key)
            ;; Update when context if this was a sidebar slot
            (when (sidebar-keys flare-key)
              (when-contexts/set-flare-content-context! (key->sidebar-slot flare-key) false))
            true)))
      false)))

(defn close-all!
  "Close all active flare panels"
  []
  (let [active-panels (into (:flare-panels @db/!app-db) (:flare-sidebars @db/!app-db))]
    (doseq [[key _panel-data] active-panels]
      (close! key))
    (count active-panels)))

(defn get-flares
  "Get a flare by its key"
  [flare-key]
  (let [panel-data (get (:flare-panels @db/!app-db) flare-key)
        ^js panel-view (:view panel-data)
        sidebar-data (get (:flare-sidebars @db/!app-db) flare-key)
        ^js sidebar-view (:view sidebar-data)]
    (cond-> {}
      (and panel-view (not (.-disposed panel-view)))
      (assoc :panel panel-data)

      (and sidebar-view (not (.-disposed sidebar-view)))
      (assoc :sidebar sidebar-data))))

(defn ls
  "List all currently active flare panels and sidebar panels"
  []
  {:panels (->> (:flare-panels @db/!app-db)
                (filter (fn [[_key panel-data]]
                          (not (.-disposed ^js (:view panel-data)))))
                (into {}))
   :sidebars (->> (:flare-sidebars @db/!app-db)
                  (filter (fn [[_key sidebar-data]]
                            (not (.-disposed ^js (:view sidebar-data)))))
                  (into {}))})