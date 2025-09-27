(ns joyride.flare.sidebar-provider
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.panel :as panel]
   [joyride.when-contexts :as when-contexts]))

(defn- create-flare-webview-provider!
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
    (let [provider (create-flare-webview-provider! slot)
          provider-id (str "joyride.flare-" slot)
          disposable (vscode/window.registerWebviewViewProvider
                      provider-id
                      provider
                      #js {:webviewOptions #js {:retainContextWhenHidden true}})]
      disposable)))

(defn create-sidebar-view!
  "Create or reuse a sidebar panel"
  [{:keys [sidebar-slot key reveal? preserve-focus?] :as flare-options}]
  (when-contexts/set-flare-content-context! sidebar-slot true)
  (let [sidebar-data (get (:flare-sidebars @db/!app-db) key)
        view (get-in @db/!app-db [:flare-sidebar-views sidebar-slot :webview-view] :pending)]
    (if (= view :pending)
      (do
        ;; Store the flare options for when view becomes available
        (swap! db/!app-db assoc-in [:flare-sidebar-views sidebar-slot :pending-flare]
               {:key key :options flare-options})
        (when reveal?
          (vscode/commands.executeCommand (str "joyride.flare-" sidebar-slot ".focus")
                                          preserve-focus?))
        :pending)
      (do
        (when-let [^js disposable (:message-handler sidebar-data)]
          (.dispose disposable))
        (swap! db/!app-db assoc-in [:flare-sidebars key] {:view view})
        (panel/update-view-with-options! view flare-options)
        (when reveal?
          (.show view preserve-focus?))
        view))))
