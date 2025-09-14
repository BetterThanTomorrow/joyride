(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]))

(defonce !flare-webview-view (atom nil))
(defonce !last-content (atom nil))

(defn create-flare-webview-provider
  "Create WebView provider for sidebar flare views"
  [^js extension-context]
  (letfn [(resolve-webview-view [^js webview-view]
            (reset! !flare-webview-view webview-view)

            (set! (.-options (.-webview webview-view))
                  (clj->js {:enableScripts true
                            :localResourceRoots [(.-extensionUri extension-context)]}))

            ;; Initialize with stored content or default
            (if-let [content @!last-content]
              (do
                (set! (.-html (.-webview webview-view)) (:html content))
                (when (:title content)
                  (set! (.-title webview-view) (:title content))))
              (set! (.-html (.-webview webview-view))
                    "<h3>Joyride Flare</h3><p>No flare content yet. Create a flare using <code>flare!</code> function.</p>"))

            ;; Initialize database entry for message handler tracking
            (swap! db/!app-db assoc-in [:flare-sidebar :default]
                   {:view webview-view :message-handler-disposable nil}))]
    #js {:resolveWebviewView resolve-webview-view}))

(defn register-flare-provider!
  "Register the flare webview provider with VS Code"
  [extension-context]
  (let [provider (create-flare-webview-provider extension-context)
        disposable (vscode/window.registerWebviewViewProvider "joyride.flare" provider)]
    disposable))

(defn ensure-sidebar-view!
  "Ensure sidebar view is available and reveal if needed"
  [reveal?]
  (if-let [view @!flare-webview-view]
    view
    (do
      (when reveal?
        (vscode/commands.executeCommand "joyride.flare.focus"))
      :pending)))