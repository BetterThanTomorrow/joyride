(ns joyride.flare.sidebar
  (:require
   ["vscode" :as vscode]
   [joyride.db :as db]
   [joyride.flare.panel :as panel]
   [joyride.when-contexts :as when-contexts]))

(def sidebar-keys #{:sidebar-1 :sidebar-2 :sidebar-3 :sidebar-4 :sidebar-5})

(defn- sidebar-slot->key [slot]
  (keyword (str "sidebar-" slot)))

(defn key->sidebar-slot [k]
  (let [key-str (name k)]
    (js/parseInt (subs key-str 8))))

(defn- store-sidebar-view!
  [slot ^js webview-view]
  (let [sidebar-key (sidebar-slot->key slot)
        dispose-listener (get-in @db/!app-db [:flare-sidebar-views slot :dispose-listener])]
    (when dispose-listener
      (.dispose ^js dispose-listener))
    (let [new-dispose-listener (.onDidDispose webview-view
                                              (fn []
                                                (when-let [^js handler (:message-handler (get (:flares @db/!app-db) sidebar-key))]
                                                  (.dispose handler))
                                                (swap! db/!app-db update :flares dissoc sidebar-key)
                                                (swap! db/!app-db update-in [:flare-sidebar-views slot] dissoc :webview-view)
                                                (swap! db/!app-db update-in [:flare-sidebar-views slot] dissoc :dispose-listener)
                                                (swap! db/!app-db update-in [:flare-sidebar-views slot] dissoc :pending-flare)
                                                (when-contexts/set-flare-content-context! slot false)))]
      (swap! db/!app-db assoc-in [:flare-sidebar-views slot :webview-view] webview-view)
      (swap! db/!app-db assoc-in [:flare-sidebar-views slot :dispose-listener] new-dispose-listener))))

(defn- create-flare-webview-provider!
  "Create WebView provider for sidebar flare views"
  [slot]
  (letfn [(resolve-webview-view
            [^js webview-view]
            (store-sidebar-view! slot webview-view)

            (if-let [pending-flare (get-in @db/!app-db [:flare-sidebar-views slot :pending-flare])]
              (let [{:keys [key options]} pending-flare
                    sidebar-data (get (:flares @db/!app-db) key)]
                (swap! db/!app-db update-in [:flare-sidebar-views slot] dissoc :pending-flare)
                (when-let [^js disposable (:message-handler sidebar-data)]
                  (.dispose disposable))
                (swap! db/!app-db assoc-in [:flares key] {:view webview-view})
                (panel/update-view-with-options! webview-view options))
              (when (= slot 1) ; Only show default content in slot 1
                (set! (.-html (.-webview webview-view))
                      "<h3>Joyride Flare</h3><p>No flare content yet. Create a flare using <code>flare!</code> with <code>:key :sidebar-1</code>. See <a href=\"https://github.com/BetterThanTomorrow/joyride/blob/master/examples/.joyride/src/flares_examples.cljs\">some examples</a>.</p>")
                (swap! db/!app-db assoc-in [:flares :sidebar-1] {:view webview-view}))))]
    #js {:resolveWebviewView resolve-webview-view}))

#_(let [flare-disposables (flare-sidebar/register-flare-providers!)]
    (doseq [flare-disposable flare-disposables]
      (swap! db/!app-db update :disposables conj flare-disposable)
      (.push (.-subscriptions ^js extension-context) flare-disposable)))

(defn- register-flare-provider!
  "Register a flare webview provider with VS Code"
  [slot]
  (if (get-in @db/!app-db [:flare-sidebar-views slot :provider-registered?])
    (get-in @db/!app-db [:flare-sidebar-views slot :provider-disposable])
    (let [provider (create-flare-webview-provider! slot)
          provider-id (str "joyride.flare-" slot)
          disposable (vscode/window.registerWebviewViewProvider
                      provider-id
                      provider
                      #js {:webviewOptions #js {:retainContextWhenHidden true}})]
      (swap! db/!app-db update :disposables conj disposable)
      (.push (.-subscriptions ^js (:extension-context @db/!app-db)) disposable)
      (swap! db/!app-db assoc-in [:flare-sidebar-views slot :provider-registered?] true)
      (swap! db/!app-db assoc-in [:flare-sidebar-views slot :provider-disposable] disposable)
      disposable)))

(defn create-sidebar-view!
  "Create or reuse a sidebar panel"
  [{:keys [sidebar-slot key reveal? preserve-focus?] :as flare-options}]
  (when-contexts/set-flare-content-context! sidebar-slot true)
  (register-flare-provider! sidebar-slot)
  (let [sidebar-data (get (:flares @db/!app-db) key)
        view (get-in @db/!app-db [:flare-sidebar-views sidebar-slot :webview-view] :pending)]
    (if (= view :pending)
      (do
        (swap! db/!app-db assoc-in [:flare-sidebar-views sidebar-slot :pending-flare]
               {:key key :options flare-options})
        (when reveal?
          (vscode/commands.executeCommand (str "joyride.flare-" sidebar-slot ".focus")
                                          preserve-focus?))
        :pending)
      (let [existing-handler (:message-handler sidebar-data)]
        (when existing-handler
          (.dispose ^js existing-handler))
        (swap! db/!app-db assoc-in [:flares key] {:view view})
        (if view
          (do
            (panel/update-view-with-options! view flare-options)
            (when reveal?
              (.show view preserve-focus?))
            view)
          (do
            (swap! db/!app-db update-in [:flare-sidebar-views sidebar-slot] dissoc :webview-view)
            (swap! db/!app-db update :flares dissoc key)
            (create-sidebar-view! flare-options)))))))

(defn close! [flare-key]
  (let [flare-data (get (:flares @db/!app-db) flare-key)
        ^js view (:view flare-data)
        ^js message-handler (:message-handler flare-data)
        slot (key->sidebar-slot flare-key)]
    (when message-handler
      (.dispose message-handler))
    (swap! db/!app-db update :flares dissoc flare-key)
    (when slot
      (when-contexts/set-flare-content-context! slot false))
    (if (and view (not (.-disposed view)))
      (do
        (.dispose view)
        true)
      false)))