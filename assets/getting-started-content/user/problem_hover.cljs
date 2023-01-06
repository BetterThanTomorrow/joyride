(ns problem-hover
  (:require ["vscode" :as vscode]))

;; Adding diagnostics info to the top of the hover to get it above the fold

(defonce !problems (atom {}))

(defn on-changed-diagnostics [event]
  (doseq [uri (.-uris event)]
    (swap! !problems assoc (.-fsPath uri) (vscode/languages.getDiagnostics uri))))

(defn- provide-hover [document position]
  (let [hover (vscode/MarkdownString.)
        problems (->> (get @!problems (-> document .-uri .-fsPath))
                      (keep (fn [problem]
                              (let [range (.-range problem)]
                                (when (.contains range position)
                                  problem)))))]
    (doseq [problem problems]
      (.appendCodeblock hover (str (.-message problem)
                                   "; " (.-source problem)
                                   (when (.-code problem)
                                     (str "(" (.-code problem) ")")))
                        ; highlight hover as 'ini', because works
                        "ini"))
    (new vscode/Hover #js [hover])))

(defn register-diagnostics-handler! []
  (vscode/languages.onDidChangeDiagnostics on-changed-diagnostics))

(defn register-provider! []
  ; Use "*" instead of "clojure" to add this to all file types
  (vscode/languages.registerHoverProvider "clojure" #js {:provideHover provide-hover}))

(comment
  foo
  (remove 1 2 3)
  :rcf)

