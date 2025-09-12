(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]))

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

            (if-let [content @!last-content]
              (do
                (set! (.-html (.-webview webview-view)) (:html content))
                (when (:title content)
                  (set! (.-title webview-view) (:title content))))
              (set! (.-html (.-webview webview-view))
                    "<h3>Joyride Flare</h3><p>No flare content yet. Create a flare using <code>flare!</code> function or <code>#joyride/flare</code> tagged literal.</p>")))]
    #js {:resolveWebviewView resolve-webview-view}))

(defn register-flare-provider!
  "Register the flare webview provider with VS Code"
  [extension-context]
  (let [provider (create-flare-webview-provider extension-context)
        disposable (vscode/window.registerWebviewViewProvider "joyride.flare" provider)]
    disposable))

(defn update-sidebar-flare!
  "Update the content of the sidebar flare view"
  [html-content & {:keys [title reveal] :or {reveal true}}]
  (reset! !last-content {:html html-content :title title})

  (if-let [^js view @!flare-webview-view]
    ;; View is resolved, update immediately
    (do
      (set! (.-html (.-webview view)) html-content)
      (when title
        (set! (.-title view) title))
      view)
    ;; View not resolved yet, reveal it to trigger resolution
    (do
      (when reveal
        (vscode/commands.executeCommand "joyride.flare.focus"))
      ;; Return a placeholder that indicates content is queued
      :pending)))