(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]))

(defonce !flare-webview-view (atom nil))

(defn create-flare-webview-provider
  "Create WebView provider for sidebar flare views"
  [extension-context]
  (letfn [(resolve-webview-view [^js webview-view]
            (reset! !flare-webview-view webview-view)
            ;; Set default content
            (set! (.-html (.-webview webview-view))
                  "<h3>Joyride Flare</h3><p>No flare content yet. Create a flare using <code>flare!</code> function or <code>#joyride/flare</code> tagged literal.</p>"))]

    ;; Return the provider object with the required interface
    #js {:resolveWebviewView resolve-webview-view}))

(defn register-flare-provider!
  "Register the flare webview provider with VS Code"
  [extension-context]
  (let [provider (create-flare-webview-provider extension-context)
        disposable (vscode/window.registerWebviewViewProvider "joyride.flare" provider)]
    disposable))

(defn update-sidebar-flare!
  "Update the content of the sidebar flare view"
  [html-content]
  (when-let [^js view @!flare-webview-view]
    (set! (.-html (.-webview view)) html-content)
    view))