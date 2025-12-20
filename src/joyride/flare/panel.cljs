(ns joyride.flare.panel
  (:require
   ["fs" :as fs]
   ["vscode" :as vscode]
   [clojure.edn :as edn]
   [clojure.string]
   [joyride.config :as config]
   [joyride.db :as db]
   [joyride.html.file-paths :as file-paths]
   [joyride.html.to-hiccup :as h2h]
   [joyride.vscode-utils :as vscode-utils]
   [replicant.string :as replicant]))

(defn resolve-icon-path
  "Convert icon specification to VS Code Uri or themed icon object"
  [icon-spec]
  (let [ext-uri (.-extensionUri ^js (db/extension-context))
        assets-uri (vscode/Uri.joinPath ext-uri "assets")
        resolve-path (fn [path]
                       (if (or (.startsWith path "http://") (.startsWith path "https://"))
                         (vscode/Uri.parse path)
                         (vscode/Uri.file (vscode-utils/as-workspace-abs-path path))))]
    (cond
      (= icon-spec :flare/icon-default)
      #js {:light (vscode/Uri.joinPath assets-uri "j-icon-light.svg")
           :dark (vscode/Uri.joinPath assets-uri "j-icon-dark.svg")}

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
        (throw (ex-info (str "An error occurred while rendering Hiccup data to HTML. "
                             "Please check your Hiccup structure. "
                             "Original error: " (.-message e))
                        {:hiccup hiccup-data
                         :processed (process-hiccup hiccup-data)
                         :error (.-message e)}))))))

(defn- url-with-cache-bust
  "Add cache-busting query parameter to force iframe reload"
  [url]
  (let [separator (if (clojure.string/includes? url "?") "&" "?")]
    (str url separator "_joyride_cb=" (.now js/Date))))

(defn generate-iframe-content
  "Create iframe wrapper following Calva's approach.
   Adds cache-busting parameter to force reload on repeated calls."
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
    <iframe src=\"" (url-with-cache-bust url) "\" sandbox=\"allow-scripts allow-same-origin allow-forms allow-modals allow-orientation-lock allow-pointer-lock allow-presentation allow-top-navigation-by-user-activation\"></iframe>
</body>
</html>"))

(defn- expand-path-templates
  "Expands template placeholders in path strings to absolute paths.
   Supported templates:
   - {joyride/user-dir} → User's Joyride directory (~/.config/joyride)
   - {joyride/extension-dir} → Joyride extension installation directory
   - {joyride/workspace-dir} → Current workspace root directory"
  [path-str]
  (-> path-str
      (clojure.string/replace "{joyride/user-dir}" (config/user-abs-joyride-path))
      (clojure.string/replace "{joyride/extension-dir}"
                              (.-path (.-extensionUri ^js (db/extension-context))))
      (clojure.string/replace "{joyride/workspace-dir}"
                              (or vscode/workspace.rootPath ""))))

(defn- ->webview-uri-str
  "Transforms a path or a uri to a webview local resource uri string.
   Expands {joyride/*} templates before resolution."
  [^js webview path]
  (->> path
       expand-path-templates
       vscode-utils/as-workspace-abs-path
       vscode/Uri.file
       (.asWebviewUri webview)
       str))

(defn- render-content
  "Handle different content types and generate appropriate HTML"
  [^js webview flare-options]
  (let [file-paths-transformer (partial ->webview-uri-str webview)]
    (cond
      (:file flare-options)
      (let [^js file-uri (:file flare-options)
            file-path (.-fsPath file-uri)
            file-content (fs/readFileSync file-path "utf8")
            hiccup-content (try (edn/read-string file-content)
                                (catch :default _e))]
        (if (vector? hiccup-content)
          (-> (file-paths/transform-file-paths-in-hiccup file-paths-transformer hiccup-content)
              render-hiccup)
          (-> (h2h/html->hiccup file-content {:transform-file-paths file-paths-transformer})
              render-hiccup)))

      (:url flare-options)
      (generate-iframe-content (:url flare-options) (:title flare-options))

      (:html flare-options)
      (let [html-content (:html flare-options)]
        (if (vector? html-content)
          (-> (file-paths/transform-file-paths-in-hiccup file-paths-transformer html-content)
              render-hiccup)
          (-> (h2h/html->hiccup html-content {:transform-file-paths file-paths-transformer})
              render-hiccup)))

      :else
      (throw (ex-info "Missing flare content"
                      {:missing ":html, :url, or :file"})))))

(defn update-view-content!
  "Update the HTML content of a WebView panel or sidebar view"
  [^js webview-view options]
  (let [^js webview (.-webview webview-view)
        html-content (render-content webview options)]
    (when webview
      (set! (.-html webview) html-content))))

(defn update-view-with-options!
  "Update an existing panel or sidebar view with all provided options"
  [^js webview-view flare-options]
  (let [{:keys [key title icon message-handler sidebar-slot webview-options]} flare-options
        ^js webview (.-webview webview-view)]
    (set! (.-options webview) webview-options)

    (set! (.-title webview-view) title)

    (when (and icon (not sidebar-slot))
      (set! (.-iconPath webview-view) (resolve-icon-path icon)))

    (let [storage-path [:flares key]]
      (when-let [existing-data (get-in @db/!app-db storage-path)]
        (when-let [^js old-disposable (:message-handler existing-data)]
          (.dispose old-disposable)))

      (when (and message-handler webview)
        (let [new-disposable (.onDidReceiveMessage webview message-handler)]
          (swap! db/!app-db assoc-in (conj storage-path :message-handler) new-disposable))))

    (update-view-content! webview-view flare-options)))

(defn create-view!+
  "Create or reuse a WebView panel"
  [{:keys [key title column webview-options reveal? preserve-focus?]
    :as flare-options}]
  (let [existing-panel-data (get (:flares @db/!app-db) key)
        ^js existing-panel (:view existing-panel-data)]

    (if (and existing-panel (not (.-disposed existing-panel)))
      (do
        (when reveal?
          (.reveal existing-panel column preserve-focus?))
        (update-view-with-options! existing-panel flare-options)
        (js/Promise.resolve existing-panel))
      (let [panel (vscode/window.createWebviewPanel
                   "joyride.flare"
                   title
                   #js {:viewColumn column
                        :preserveFocus preserve-focus?}
                   webview-options)]

        (.onDidDispose panel
                       (fn []
                         (when-let [^js message-handler (:message-handler (get (:flares @db/!app-db) key))]
                           (.dispose message-handler))
                         (swap! db/!app-db update :flares dissoc key)))

        (swap! db/!app-db assoc-in [:flares key]
               {:view panel})

        (update-view-with-options! panel flare-options)
        (js/Promise.resolve panel)))))

(defn close! [flare-key]
  (let [flare-data (get (:flares @db/!app-db) flare-key)
        ^js view (:view flare-data)
        ^js message-handler (:message-handler flare-data)]
    (if (and view (not (.-disposed view)))
      (do
        (when message-handler
          (.dispose message-handler))
        (.dispose view)
        (swap! db/!app-db update :flares dissoc flare-key)
        true)
      false)))
