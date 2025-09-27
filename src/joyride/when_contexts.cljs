(ns joyride.when-contexts
  (:require ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.vscode-utils :as vscode-utils]
            [promesa.core :as p]))

(defonce ^:private !db (atom {:contexts {::joyride.isActive false
                                         ::joyride.isNReplServerRunning false
                                         ::joyride.userActivateScriptExists false
                                         ::joyride.userHelloScriptExists false
                                         ::joyride.workspaceActivateScriptExists false
                                         ::joyride.workspaceHelloScriptExists false
                                         ::joyride.sidebar-1.flare.hasContent false
                                         ::joyride.sidebar-2.flare.hasContent false
                                         ::joyride.sidebar-3.flare.hasContent false
                                         ::joyride.sidebar-4.flare.hasContent false
                                         ::joyride.sidebar-5.flare.hasContent false}}))

(defn set-context! [k v]
  (swap! !db assoc-in [:contexts k] v)
  (vscode/commands.executeCommand "setContext" (name k) v))

(defn context [k]
  (get-in @!db [:contexts (if (string? k)
                            (keyword (str "joyride.when-contexts/" k))
                            k)]))

(defn update-script-contexts! []
  (p/let [user-activate-exists? (vscode-utils/path-or-uri-exists?+
                                 (vscode-utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))
          user-hello-exists? (vscode-utils/path-or-uri-exists?+
                              (vscode-utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))
          ;; Workspace contexts - only check if workspace exists
          ws-scripts-path (conf/workspace-abs-scripts-path)
          ws-activate-exists? (when ws-scripts-path
                                (vscode-utils/path-or-uri-exists?+
                                 (vscode-utils/path->uri ws-scripts-path ["workspace_activate.cljs"])))
          ws-hello-exists? (when ws-scripts-path
                             (vscode-utils/path-or-uri-exists?+
                              (vscode-utils/path->uri ws-scripts-path ["hello_joyride_workspace_script.cljs"])))]
    ;; Update all contexts
    (set-context! ::joyride.userActivateScriptExists user-activate-exists?)
    (set-context! ::joyride.userHelloScriptExists user-hello-exists?)
    (set-context! ::joyride.workspaceActivateScriptExists (boolean ws-activate-exists?))
    (set-context! ::joyride.workspaceHelloScriptExists (boolean ws-hello-exists?))))

(defn set-flare-content-context!
  "Set the hasContent context for a flare slot"
  [slot has-content?]
  (case slot
    1 (set-context! ::joyride.sidebar-1.flare.hasContent has-content?)
    2 (set-context! ::joyride.sidebar-2.flare.hasContent has-content?)
    3 (set-context! ::joyride.sidebar-3.flare.hasContent has-content?)
    4 (set-context! ::joyride.sidebar-4.flare.hasContent has-content?)
    5 (set-context! ::joyride.sidebar-5.flare.hasContent has-content?)
    (throw (ex-info "Invalid flare slot" {:slot slot}))))

(defn initialize-flare-contexts!
  "Initialize all flare content contexts, except slot 1, to false"
  []
  (set-flare-content-context! 1 true)
  (doseq [slot (range 2 6)]
    (set-flare-content-context! slot false)))

(comment
  ;; Interactive testing and development

  ;; Check current context state
  @!db

  ;; Test the update function
  (update-script-contexts!)

  ;; Check specific contexts
  (context ::joyride.userActivateScriptExists)
  (context "joyride.userActivateScriptExists")
  (context ::joyride.userHelloScriptExists)
  (context "joyride.userHelloScriptExists")
  (context ::joyride.workspaceActivateScriptExists)
  (context ::joyride.workspaceHelloScriptExists)

  ;; Manual context testing
  (set-context! ::joyride.userActivateScriptExists true)
  (set-context! ::joyride.userActivateScriptExists false)

  ;; Check file paths being used
  [(conf/user-abs-scripts-path)
   (conf/workspace-abs-scripts-path)]

  ;; Test path->uri helper
  (vscode-utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"])

  ;; Test file existence manually
  (p/let [it-exists? (vscode-utils/path-or-uri-exists?+
                      (vscode-utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))]
    (def it-exists? it-exists?))

;; Reset all script contexts to false
  (do
    (set-context! ::joyride.userActivateScriptExists false)
    (set-context! ::joyride.userHelloScriptExists false)
    (set-context! ::joyride.workspaceActivateScriptExists false)
    (set-context! ::joyride.workspaceHelloScriptExists false)
    @!db)

  (context "joyride.isActive"))