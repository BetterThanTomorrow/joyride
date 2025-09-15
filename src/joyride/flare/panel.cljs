(ns joyride.flare.panel
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]
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
  [^js webview-view flare-options]
  (let [{:keys [key title icon message-handler sidebar? webview-options]} flare-options
        ^js webview (.-webview webview-view)]
    (set! (.-options webview) webview-options)

    (set! (.-title webview-view) title)

    (when (and icon (not sidebar?))
      (set! (.-iconPath webview-view) (resolve-icon-path icon)))

    (let [storage-key (if sidebar? :flare-sidebar :flare-panels)
          storage-path [storage-key key]]
      (when-let [existing-data (get-in @db/!app-db storage-path)]
        (when-let [^js old-disposable (:message-handler-disposable existing-data)]
          (.dispose old-disposable)))

      (when (and message-handler webview)
        (let [new-disposable (.onDidReceiveMessage webview message-handler)]
          (swap! db/!app-db assoc-in (conj storage-path :message-handler-disposable) new-disposable))))

    (update-panel-content! webview-view flare-options)))

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