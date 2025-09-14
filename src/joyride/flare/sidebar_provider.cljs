(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]))

(defn get-sidebar-view
  "Get the current sidebar webview view from app-db"
  []
  (get-in @db/!app-db [:flare-sidebar-state :webview-view]))

(defn get-last-content
  "Get the last content from app-db"
  []
  (get-in @db/!app-db [:flare-sidebar-state :last-content]))

(defn create-flare-webview-provider
  "Create WebView provider for sidebar flare views"
  [^js extension-context]
  (letfn [(resolve-webview-view [^js webview-view]
            (swap! db/!app-db assoc-in [:flare-sidebar-state :webview-view] webview-view)

            (set! (.-options (.-webview webview-view))
                  (clj->js {:enableScripts true
                            :localResourceRoots [(.-extensionUri extension-context)]}))

            ;; Initialize with stored content or default
            (if-let [content (get-last-content)]
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
  (if-let [view (get-sidebar-view)]
    view
    (do
      (when reveal?
        (vscode/commands.executeCommand "joyride.flare.focus"))
      :pending)))