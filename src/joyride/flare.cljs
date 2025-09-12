(ns joyride.flare
  "Joyride Flares - WebView panel and sidebar view creation"
  (:require
   ["vscode" :as vscode]
   [clojure.string :as str]
   [clojure.edn :as edn]
   [joyride.flare.sidebar-provider :as sidebar]
   [replicant.dom :as replicant]))

;; Panel registry for key-based reuse
(defonce !flare-panels (atom {}))
(defonce !flare-sidebar-views (atom {}))

(defn hiccup?
  "Check if data is a Hiccup structure (vector starting with keyword)"
  [data]
  (and (vector? data)
       (keyword? (first data))))

(defn render-hiccup
  "Render Hiccup data structure to HTML string using Replicant"
  [hiccup-data]
  (try
    ;; Use Replicant's DOM rendering for server-side HTML generation
    ;; This should be the correct API for Hiccup-to-HTML conversion
    (str (replicant/render hiccup-data))
    (catch js/Error e
      (throw (ex-info "Failed to render Hiccup data"
                      {:hiccup hiccup-data
                       :error (.-message e)})))))

(defn generate-iframe-content
  "Create iframe wrapper following Calva's approach"
  [url title]
  (str "<!DOCTYPE html>
<html>
<head>
    <meta charset=\"UTF-8\">
    <title>" (or title "WebView") "</title>
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
    ;; URL content - create iframe wrapper
    (:url content-data)
    (generate-iframe-content (:url content-data) title)

    ;; HTML content - check if it's Hiccup or HTML string
    (:html content-data)
    (let [html-content (:html content-data)]
      (if (hiccup? html-content)
        (render-hiccup html-content)
        html-content))

    ;; Default case
    :else
    (throw (ex-info "Invalid flare content: must specify either :html or :url"
                    {:content content-data}))))

(defn create-webview-panel!
  "Create or reuse a WebView panel based on options"
  [{:keys [key title column opts reveal]
    :or {title "WebView"
         column vscode/ViewColumn.Beside
         opts {:enableScripts true}
         reveal true}
    :as options}]
  (let [panel-key (or key (gensym "flare-panel-"))
        existing-panel (get @!flare-panels panel-key)]

    ;; If panel exists and is not disposed, reuse it
    (if (and existing-panel (not (.-disposed existing-panel)))
      (do
        (when reveal
          (.reveal existing-panel column))
        existing-panel)

      ;; Create new panel
      (let [panel (vscode/window.createWebviewPanel
                   "joyride.flare"
                   title
                   column
                   (clj->js opts))]

        ;; Set up disposal handling - onDidDispose is on the panel, not webview
        (.onDidDispose panel
                       #(swap! !flare-panels dissoc panel-key))

        ;; Store in registry
        (swap! !flare-panels assoc panel-key panel)

        panel))))

(defn update-panel-content!
  "Update the HTML content of a WebView panel"
  [panel content-data title]
  (let [html-content (render-content content-data title)]
    (set! (.-html (.-webview panel)) html-content)))

(defn flare!
  "Create a WebView panel or sidebar view with the given options.

   Options:
   - :html - HTML content string OR Hiccup data structure
   - :url - URL to display in iframe
   - :title - Panel/view title (default: 'WebView')
   - :key - Identifier for reusing panels
   - :reload - Force reload even if content unchanged (default: false)
   - :reveal - Show/focus the panel (default: true)
   - :column - VS Code ViewColumn (default: vscode.ViewColumn.Beside)
   - :opts - WebView options (default: {:enableScripts true})
   - :sidebar-panel? - Display in sidebar vs separate panel (default: false)

   Content Examples:
   - HTML string: {:html \"<h1>Hello</h1>\"}
   - Hiccup data: {:html [:div [:h1 \"Hello\"] [:p \"World\"]]}
   - URL: {:url \"https://example.com\"}

   Returns:
   - {:panel <webview-panel> :type :panel} for panels
   - {:view <webview-view> :type :sidebar} for sidebar views"
  [options]
  (let [{:keys [sidebar-panel?] :as opts} options]
    (if sidebar-panel?
      ;; TODO: Implement sidebar view creation
      (throw (ex-info "Sidebar panels not yet implemented" {:options options}))

      ;; Create regular panel
      (let [panel (create-webview-panel! opts)]
        (update-panel-content! panel opts (:title opts "WebView"))
        {:panel panel :type :panel}))))

(defn process-flare-request!
  "Process a flare request from tagged literal or function call"
  [flare-data]
  (try
    (flare! flare-data)
    (catch js/Error e
      (js/console.error "Failed to process flare request:" (.-message e) flare-data)
      (throw e))))
