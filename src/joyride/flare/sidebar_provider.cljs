(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.panel :as panel]))

(defn get-sidebar-view
  "Get the sidebar webview view for a specific slot from app-db"
  [slot]
  (get-in @db/!app-db [:flare-sidebar-views slot :webview-view]))

(defn create-flare-webview-provider
  "Create WebView provider for sidebar flare views"
  [slot]
  (letfn [(resolve-webview-view
           [^js webview-view]
           (swap! db/!app-db assoc-in [:flare-sidebar-views slot :webview-view] webview-view)

           (if-let [pending-flare (get-in @db/!app-db [:flare-sidebar-views slot :pending-flare])]
             (let [{:keys [key options]} pending-flare
                   sidebar-data (get (:flare-sidebars @db/!app-db) key)]
               (swap! db/!app-db update-in [:flare-sidebar-views slot] dissoc :pending-flare)
               (when-let [^js disposable (:message-handler sidebar-data)]
                 (.dispose disposable))
               (swap! db/!app-db assoc-in [:flare-sidebars key] {:view webview-view})
               (panel/update-view-with-options! webview-view options))
             (when (= slot 1) ; Only show default content in slot 1
               (set! (.-html (.-webview webview-view))
                     "<h3>Joyride Flare</h3><p>No flare content yet. Create a flare using <code>flare!</code> with <code>:key :sidebar-1</code>. See <a href=\"https://github.com/BetterThanTomorrow/joyride/blob/master/examples/.joyride/src/flares_examples.cljs\">some examples</a>.</p>")
               (swap! db/!app-db assoc-in [:flare-sidebars :sidebar-1] {:view webview-view}))))]
    #js {:resolveWebviewView resolve-webview-view}))

(defn register-flare-providers!
  "Register all 5 flare webview providers with VS Code"
  []
  (for [slot (range 1 6)]
    (let [provider (create-flare-webview-provider slot)
          provider-id (str "joyride.flare-" slot)
          disposable (vscode/window.registerWebviewViewProvider
                      provider-id
                      provider
                      #js {:webviewOptions #js {:retainContextWhenHidden true}})]
      disposable)))

(defn ensure-sidebar-view!
  "Ensure sidebar view is available for the given slot"
  [slot]
  (if-let [^js view (get-sidebar-view slot)]
    view
    :pending))