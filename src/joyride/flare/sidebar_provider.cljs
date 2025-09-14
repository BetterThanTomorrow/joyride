(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]))

(defn get-sidebar-view
  "Get the current sidebar webview view from app-db"
  []
  (get-in @db/!app-db [:flare-sidebar-state :webview-view]))

(defn create-flare-webview-provider
  "Create WebView provider for sidebar flare views"
  []
  (letfn [(resolve-webview-view
           [^js webview-view]
           (swap! db/!app-db assoc-in [:flare-sidebar-state :webview-view] webview-view)

           (set! (.-html (.-webview webview-view))
                 "<h3>Joyride Flare</h3><p>No flare content yet. Create a flare using <code>flare!</code> function. See <a href=\"https://github.com/BetterThanTomorrow/joyride/blob/master/examples/.joyride/src/flares_examples.cljs\">some examples</a>.</p>")

           (swap! db/!app-db assoc-in [:flare-sidebar :default]
                  {:view webview-view :message-handler-disposable nil}))]
    #js {:resolveWebviewView resolve-webview-view}))

(defn register-flare-provider!
  "Register the flare webview provider with VS Code"
  []
  (let [provider (create-flare-webview-provider)
        disposable (vscode/window.registerWebviewViewProvider
                    "joyride.flare"
                    provider
                    #js {:webviewOptions #js {:retainContextWhenHidden true}})]
    disposable))

(defn ensure-sidebar-view!
  "Ensure sidebar view is available and reveal if needed"
  [reveal?]
  (if-let [^js view (get-sidebar-view)]
    view
    (do
      (when reveal?
        (vscode/commands.executeCommand "joyride.flare.focus"))
      :pending)))