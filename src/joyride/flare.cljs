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
  "Render Hiccup data structure to HTML string using Replicant"
  [hiccup-data]
  (try
    (replicant/render hiccup-data)
    (catch js/Error e
      (throw (ex-info (str "Failed to render Hiccup data " (.-message e))
                      {:hiccup hiccup-data
                       :error (.-message e)})))))

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
  [content-data title]
  (cond
    (:url content-data)
    (generate-iframe-content (:url content-data) title)

    (:html content-data)
    (let [html-content (:html content-data)]
      (if (vector? html-content)
        (render-hiccup html-content)
        html-content))

    :else
    (throw (ex-info "Invalid flare content: must specify either :html or :url"
                    {:content content-data}))))

(defn create-webview-panel!
  "Create or reuse a WebView panel based on options"
  [{:keys [key title column opts reveal icon]
    :or {title "WebView"
         column vscode/ViewColumn.Beside
         opts {:enableScripts true}
         reveal true
         icon :flare/icon-default}}]
  (let [panel-key (or key (keyword "joyride.flare" (str "flare-" (gensym))))
        ^js existing-panel (get (db/flare-panels) panel-key)]

    (if (and existing-panel (not (.-disposed existing-panel)))
      (do
        (when reveal
          (.reveal existing-panel column))
        existing-panel)

      (let [panel (vscode/window.createWebviewPanel
                   "joyride.flare"
                   title
                   column
                   (clj->js opts))]

        ;; Set icon (default or custom)
        (when icon
          (set! (.-iconPath panel) (resolve-icon-path icon)))

        (.onDidDispose panel
                       #(swap! db/!app-db update :flare-panels dissoc panel-key))

        (swap! db/!app-db assoc-in [:flare-panels panel-key] panel)

        panel))))

(defn update-panel-content!
  "Update the HTML content of a WebView panel"
  [^js panel content-data title]
  (let [html-content (render-content content-data title)]
    (set! (.-html (.-webview panel)) html-content)))

(defn flare!
  "Create a WebView panel or sidebar view with the given options.

   Options:
   - :html - HTML content string OR Hiccup data structure
   - :url - URL to display in iframe
   - :title - Panel/view title (default: 'WebView')
   - :key - Identifier for reusing panels
   - :icon - Icon for panel tab. String (path/URL) or map {:light \"...\" :dark \"...\"}
   - :sidebar-panel? - Display in sidebar vs separate panel (default: false)

   Examples:
   - {:html [:h1 \"Hello\"] :icon \"https://example.com/icon.png\"}
   - {:icon {:light \"https://light.png\" :dark \"https://dark.png\"}}

   Returns: {:panel <webview-panel> :type :panel} or {:view <webview-view> :type :sidebar}"
  [options]
  (let [{:keys [sidebar-panel?] :as opts} options]
    (if sidebar-panel?
      (let [html-content (render-content opts (:title opts "Flare"))
            title (:title opts "Flare")
            reveal (:reveal opts true)
            result (sidebar/update-sidebar-flare! html-content :title title :reveal reveal)]
        (if (= result :pending)
          {:view :pending :type :sidebar}
          {:view result :type :sidebar}))
      (let [panel (create-webview-panel! opts)]
        (update-panel-content! panel opts (:title opts "WebView"))
        {:panel panel :type :panel}))))

;; Programmatic flare control APIs



(defn close!
  "Close/dispose a flare panel by key"
  [flare-key]
  (if-let [^js panel (get (db/flare-panels) flare-key)]
    (if (.-disposed panel)
      false
      (do (.dispose panel) true))
    false))

(defn ls
  "List all currently active flare panels"
  []
  (->> (db/flare-panels)
       (filter (fn [[_key ^js panel]] (not (.-disposed panel))))
       (into {})))

(defn close-all!
  "Close all active flare panels"
  []
  (let [active-panels (ls)]
    (doseq [[_key ^js panel] active-panels]
      (when (not (.-disposed panel))
        (.dispose panel)))
    (swap! db/!app-db assoc :flare-panels {})
    (count active-panels)))

(defn get-flare
  "Get a flare by its key"
  [flare-key]
  (when-let [^js panel (get (db/flare-panels) flare-key)]
    (when (not (.-disposed panel))
      {:panel panel :type :panel})))
