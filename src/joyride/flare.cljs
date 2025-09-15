(ns joyride.flare
  "Joyride Flares - WebView panel and sidebar view creation"
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.panel :as panel]
   [joyride.flare.sidebar-provider :as sidebar]))

(defn post-message!
  "Send a message from extension to flare webview.

   Args:
   - flare-key: The key of the flare to send message to
   - message: The message data to send (will be serialized to JSON)

   Returns: a map with the .postMessage promises for the :panel or :sidebar flares matching the key"
  [flare-key message]
  (let [panel-data (get (:flare-panels @db/!app-db) flare-key)
        sidebar-data (get (:flare-sidebar @db/!app-db) flare-key)]
    (cond-> {}
      (and panel-data (not (.-disposed ^js (:view panel-data))))
      (assoc :panel
             (let [^js webview (.-webview ^js (:view panel-data))]
               (.postMessage webview (clj->js message))))

      (and sidebar-data (:view sidebar-data))
      (assoc :sidebar
             (let [^js webview (.-webview ^js (:view sidebar-data))]
               (.postMessage webview (clj->js message)))))))

(defn- normalize-flare-options
  [options]
  (merge {:key (or (:key options)
                   (keyword "flare" (str "flare-" (gensym))))
          :title "Flare"
          :reveal? true
          :column js/undefined
          :preserve-focus? true
          :icon :flare/icon-default
          :sidebar? (or (:sidebar-panel? options) ; Calva uses :sidebar-panel?
                        false)}                   ; accept it without ceremony
         options
         {:webview-options (or (clj->js (:webview-options options))
                               #js {:enableScripts true})}))

(defn flare!
  "Create a WebView panel or sidebar view with the given options.

   Options:
   - :html - HTML content string OR Hiccup data structure
   - :url - URL to display in iframe
   - :title - Panel/view title (default: 'WebView')
   - :key - Identifier for reusing panels
   - :icon - Icon for panel tab. String (path/URL) or map {:light \"...\" :dark \"...\"}
   - :column - vscode.ViewColumn (default: js/undefined)
   - :reveal? - Whether to reveal the panel when created or reused (default: true)
   - :preserve-focus? - Whether to preserve focus when revealing the panel (default: true)
   - webview-options - JS object vscode WebviewPanelOptions & WebviewOptions for the webview (default: {:enableScripts true})
   - :message-handler - Function to handle messages from webview. Receives message object.
   - :sidebar? - Display in sidebar vs separate panel (default: false)

   Returns: {:panel <webview-panel> :type :panel} or {:view <webview-view> :type :sidebar}"
  [options]
  (let [flare-options (normalize-flare-options options)
        {:keys [sidebar? key reveal? preserve-focus?]} flare-options]
    (if sidebar?
      (let [sidebar-data (get (:flare-sidebar @db/!app-db) key)
            view (sidebar/ensure-sidebar-view!)]
        (if (= view :pending)
          (do
            ;; Store the flare options for when view becomes available
            (swap! db/!app-db assoc-in [:flare-sidebar-state :pending-flare]
                   {:key key :options flare-options})
            (when reveal?
              (vscode/commands.executeCommand "joyride.flare.focus"
                                              preserve-focus?))
            {:sidebar :pending})
          (do
            (when-let [^js disposable (:message-handler sidebar-data)]
              (.dispose disposable))
            (swap! db/!app-db assoc :flare-sidebar
                   {key {:view view}})
            (panel/update-view-with-options! view flare-options)
            (when reveal?
              (.show view preserve-focus?))
            {:sidebar view})))
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
    false))

(defn close-all!
  "Close all active flare panels"
  []
  (let [active-panels (:flare-panels @db/!app-db)]
    (doseq [[key _panel-data] active-panels]
      (close! key))
    (count active-panels)))

(defn get-flares
  "Get a flare by its key"
  [flare-key]
  (let [panel-data (get (:flare-panels @db/!app-db) flare-key)
        ^js panel-view (:view panel-data)
        sidebar-data (get (:flare-sidebar @db/!app-db) flare-key)
        ^js sidebar-view (:view sidebar-data)]
    (cond-> {}
      (and panel-view (not (.-disposed panel-view)))
      (assoc :panel panel-data)

      (and sidebar-view (not (.-disposed sidebar-view)))
      (assoc :sidebar sidebar-data))))

(defn ls ; TODO: Include sidebar panels
  "List all currently active flare panels"
  []
  {:panels (->> (:flare-panels @db/!app-db)
                (filter (fn [[_key panel-data]]
                          (not (.-disposed ^js (:view panel-data)))))
                (into {}))
   :sidebar (->> (:flare-sidebar @db/!app-db)
                 (filter (fn [[_key sidebar-data]]
                           (not (.-disposed ^js (:view sidebar-data)))))
                 (into {}))})