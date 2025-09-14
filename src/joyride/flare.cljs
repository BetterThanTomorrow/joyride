(ns joyride.flare
  "Joyride Flares - WebView panel and sidebar view creation"
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.sidebar-provider :as sidebar]
   [replicant.string :as replicant]))

(defn resolve-icon-path
  "Convert icon specification to VS Code Uri or themed icon object"
  [icon-spec]
  (let [ext-uri (.-extensionUri ^js (db/extension-context))
        resolve-path (fn [path]
                       (if (or (.startsWith path "http://") (.startsWith path "https://"))
                         (vscode/Uri.parse path)
                         (vscode/Uri.file path)))]
    (cond
      (= icon-spec :flare/icon-default)
      (vscode/Uri.joinPath ext-uri "assets" "j-icon.svg")

      ;; String path - absolute path or URL
      (string? icon-spec)
      (resolve-path icon-spec)

      ;; Map with :light and :dark - absolute paths or URLs
      (and (map? icon-spec) (:light icon-spec) (:dark icon-spec))
      #js {:light (resolve-path (:light icon-spec))
           :dark (resolve-path (:dark icon-spec))}

      ;; Already a Uri - pass through
      :else icon-spec)))

(defn render-hiccup
  "Render Hiccup data structure to HTML string using Replicant.
   Handles :script and :style tags so that they are not escaped, by using :innerHTML."
  [hiccup-data]
  (letfn [(process-hiccup [data]
            (cond
              (and (vector? data)
                   (contains? #{:script :style} (first data))
                   (> (count data) 1))
              (let [[tag attrs & content] data
                    safe-attrs (if (map? attrs)
                                 (assoc attrs :innerHTML (apply str content))
                                 {:innerHTML (apply str (cons attrs content))})]
                [tag safe-attrs])

              (vector? data)
              (mapv process-hiccup data)

              :else
              data))]
    (try
      (let [processed-hiccup (process-hiccup hiccup-data)]
        (replicant/render processed-hiccup))
      (catch js/Error e
        (throw (ex-info (str "Failed to render Hiccup data " (.-message e))
                        {:hiccup hiccup-data
                         :processed (process-hiccup hiccup-data)
                         :error (.-message e)}))))))

(defn generate-iframe-content
  "Create iframe wrapper following Calva's approach"
  [url title]
  (str "<!DOCTYPE html>
<html>
<head>
    <meta charset=\"UTF-8\">
    <title>" (or title "Flare") "</title>
    <style>
        body, html {
            margin: 0;
            padding: 0;
            height: 100%;
            overflow: hidden;
        }
        iframe {
            width: 100%;
            height: 100%;
            border: none;
        }
    </style>
</head>
<body>
    <iframe src=\"" url "\" sandbox=\"allow-scripts allow-same-origin allow-forms allow-modals allow-orientation-lock allow-pointer-lock allow-presentation allow-top-navigation\"></iframe>
</body>
</html>"))

(defn render-content
  "Handle different content types and generate appropriate HTML"
  [flare-options]
  (cond
    (:url flare-options)
    (generate-iframe-content (:url flare-options) (:title flare-options))

    (:html flare-options)
    (let [html-content (:html flare-options)]
      (if (vector? html-content)
        (render-hiccup html-content)
        html-content))

    :else
    (throw (ex-info "Invalid flare content: must specify either :html or :url"
                    {:content flare-options}))))

(defn update-panel-content!
  "Update the HTML content of a WebView panel or sidebar view"
  [^js webview-container options]
  (let [^js webview (.-webview webview-container)
        html-content (render-content options)]
    (when webview
      (set! (.-html webview) html-content))))

(defn update-panel-with-options!
  "Update an existing panel or sidebar view with all provided options"
  [^js webview-container flare-options]
  (let [{:keys [key title icon message-handler sidebar?]} flare-options
        ^js webview (.-webview webview-container)]

    (when title
      (set! (.-title webview-container) title))

    (when (and icon (not sidebar?))
      (set! (.-iconPath webview-container) (resolve-icon-path icon)))

    (let [storage-key (if sidebar? :flare-sidebar :flare-panels)
          storage-path [storage-key key]]
      (when-let [existing-data (get-in @db/!app-db storage-path)]
        (when-let [^js old-disposable (:message-handler-disposable existing-data)]
          (.dispose old-disposable)))

      (when (and message-handler webview)
        (let [new-disposable (.onDidReceiveMessage webview message-handler)]
          (swap! db/!app-db assoc-in (conj storage-path :message-handler-disposable) new-disposable))))

    (update-panel-content! webview-container flare-options)))

(defn create-webview-panel!
  "Create or reuse a WebView panel based on options"
  [{:keys [key title column webview-options reveal? preserve-focus?]
    :as flare-options}]
  (let [existing-panel-data (get (:flare-panels @db/!app-db) key)
        ^js existing-panel (:panel existing-panel-data)]

    (if (and existing-panel (not (.-disposed existing-panel)))
      (do
        (when reveal?
          (.reveal existing-panel column preserve-focus?))
        (update-panel-with-options! existing-panel flare-options)
        existing-panel)
      (let [panel (vscode/window.createWebviewPanel
                   "joyride.flare"
                   title
                   #js {:viewColumn column
                        :preserveFocus preserve-focus?}
                   webview-options)]

        (.onDidDispose panel
                       #(swap! db/!app-db update :flare-panels dissoc key))

        (swap! db/!app-db assoc-in [:flare-panels key]
               {:panel panel :message-handler-disposable nil})

        (update-panel-with-options! panel flare-options)
        panel))))

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
      (and panel-data (not (.-disposed ^js (:panel panel-data))))
      (assoc :panel
             (let [^js webview (.-webview ^js (:panel panel-data))]
               (.postMessage webview (clj->js message))))

      (and sidebar-data (:view sidebar-data))
      (assoc :sidebar
             (let [^js webview (.-webview ^js (:view sidebar-data))]
               (.postMessage webview (clj->js message)))))))

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
  (let [flare-options (merge {:key (or (:key options)
                                       (keyword "flare" (str "flare-" (gensym))))
                              :title "Flare"
                              :reveal? true
                              :webview-options {:enableScripts true}
                              :column js/undefined
                              :preserve-focus? true
                              :icon :flare/icon-default
                              :sidebar? false}
                             options)
        {:keys [sidebar? key reveal?]} flare-options]
    (if sidebar?
      (let [view (sidebar/ensure-sidebar-view! reveal?)]
        (if (= view :pending)
          {:view :pending :type :sidebar}
          (do
            (swap! db/!app-db assoc-in [:flare-sidebar key]
                   {:view view :message-handler-disposable nil})
            (update-panel-with-options! view (assoc flare-options :sidebar? true))
            {:view view :type :sidebar})))
      (let [panel (create-webview-panel! flare-options)]
        {:panel panel :type :panel}))))

(defn close!
  "Close/dispose a flare panel by key"
  [flare-key]
  (if-let [panel-data (get (:flare-panels @db/!app-db) flare-key)]
    (let [^js panel (:panel panel-data)]
      (if (.-disposed panel)
        false
        (do
          (when-let [^js disposable (:message-handler-disposable panel-data)]
            (.dispose disposable))
          (.dispose panel)
          true)))
    false))

(defn ls
  "List all currently active flare panels"
  []
  (->> (:flare-panels @db/!app-db)
       (filter (fn [[_key panel-data]] (not (.-disposed ^js (:panel panel-data)))))
       (into {})))

(defn close-all!
  "Close all active flare panels"
  []
  (let [active-panels (ls)]
    (doseq [[_key panel-data] active-panels]
      (let [^js panel (:panel panel-data)]
        (when (not (.-disposed panel))
          (when-let [^js disposable (:message-handler-disposable panel-data)]
            (.dispose disposable))
          (.dispose panel))))
    (swap! db/!app-db assoc :flare-panels {})
    (count active-panels)))

(defn get-flare
  "Get a flare by its key"
  [flare-key]
  (when-let [panel-data (get (:flare-panels @db/!app-db) flare-key)]
    (let [^js panel (:panel panel-data)]
      (when (not (.-disposed panel))
        {:panel panel :type :panel}))))
