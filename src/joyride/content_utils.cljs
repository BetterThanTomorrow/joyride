(ns joyride.content-utils
  (:require
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.config :as conf]
   [joyride.utils :as utils]
   [joyride.when-contexts :as when-contexts]
   [promesa.core :as p]))

(def ^:private github-base-url
  "https://raw.githubusercontent.com/BetterThanTomorrow/joyride/master")

(defn- source-uri-to-github-url
  "Convert a local source URI to a GitHub raw URL"
  [^js source-uri]
  (let [fs-path (.-fsPath source-uri)
        assets-marker "assets/getting-started-content"
        assets-index (.indexOf fs-path assets-marker)]
    (when (>= assets-index 0)
      (let [relative-path (.substring fs-path assets-index)]
        (str github-base-url "/" relative-path)))))

(def ^:private fetch-timeout-ms 10000)

(defn- fetch-content-from-github
  "Fetch content from GitHub with timeout and error handling"
  [github-url]
  (let [controller (js/AbortController.)
        signal (.-signal controller)
        timeout-id (js/setTimeout #(.abort controller) fetch-timeout-ms)]
    (-> (p/let [response (js/fetch github-url #js {:signal signal})
                _ (when-not (.-ok response)
                    (throw (js/Error. (str "GitHub fetch failed: " (.-status response)))))
                arrayBuffer (.arrayBuffer response)
                uint8Array (js/Uint8Array. arrayBuffer)]
          {:type "success"
           :content uint8Array
           :source "github"})
        (p/catch
         (fn [error]
           (js/console.warn "Failed to fetch content from GitHub" (.-message error))
           {:type "error"
            :message (str "Failed to fetch from GitHub: " (.-message error))}))
        (p/finally (fn [] (js/clearTimeout timeout-id))))))

(defn update-script-contexts!
  "Updates VS Code context variables based on current script file existence"
  []
  (p/let [user-activate-exists? (utils/path-or-uri-exists?+
                                 (utils/path->uri (conf/user-abs-scripts-path) ["user_activate.cljs"]))
          user-hello-exists? (utils/path-or-uri-exists?+
                              (utils/path->uri (conf/user-abs-scripts-path) ["hello_joyride_user_script.cljs"]))
          ws-scripts-path (conf/workspace-abs-scripts-path)
          ws-activate-exists? (when ws-scripts-path
                                (utils/path-or-uri-exists?+
                                 (utils/path->uri ws-scripts-path ["workspace_activate.cljs"])))
          ws-hello-exists? (when ws-scripts-path
                             (utils/path-or-uri-exists?+
                              (utils/path->uri ws-scripts-path ["hello_joyride_workspace_script.cljs"])))]
    (when-contexts/set-context! ::when-contexts/joyride.userActivateScriptExists user-activate-exists?)
    (when-contexts/set-context! ::when-contexts/joyride.userHelloScriptExists user-hello-exists?)
    (when-contexts/set-context! ::when-contexts/joyride.workspaceActivateScriptExists ws-activate-exists?)
    (when-contexts/set-context! ::when-contexts/joyride.workspaceHelloScriptExists ws-hello-exists?)))

(defn default-content-uri [sub-path]
  (utils/path->uri (utils/extension-path) (concat ["assets" "getting-started-content"] sub-path)))

(defn create-content-file+ [source-uri ^js destination-uri]
  (js/console.info "Creating " ^String (.-fsPath destination-uri))
  (p/let [github-url (source-uri-to-github-url source-uri)
          github-result (when github-url
                          (fetch-content-from-github github-url))
          _ (vscode/workspace.fs.createDirectory (->> destination-uri
                                                      .-fsPath
                                                      path/dirname
                                                      vscode/Uri.file))]
    (if (and github-result (= (:type github-result) "success"))
      ;; GitHub fetch succeeded, write the content
      (p/do (vscode/workspace.fs.writeFile destination-uri (:content github-result))
            (update-script-contexts!)
            destination-uri)
      ;; Fallback to current behavior (copy from local assets)
      (p/do (vscode/workspace.fs.copy source-uri destination-uri)
            (update-script-contexts!)
            destination-uri))))

(defn maybe-create-content+
  "Copies `source-uri` to `destination-uri` if the latter does not exist.
   Returns nil if `destination-uri` already exists."
  [source-uri destination-uri]
  (p/let [exists?+ (utils/path-or-uri-exists?+ destination-uri)]
    (when-not exists?+
      (create-content-file+ source-uri destination-uri))))

(defn maybe-create-user-readme+ []
  (maybe-create-content+ (default-content-uri ["user" "README.md"])
                         (utils/path->uri (conf/user-abs-joyride-path) ["README.md"])))

(defn maybe-create-workspace-config+ []
  (maybe-create-content+ (default-content-uri ["workspace" "deps.edn"])
                         (utils/path->uri (conf/workspace-abs-joyride-path) ["deps.edn"])))

(defn maybe-create-user-config+ []
  (maybe-create-content+ (default-content-uri ["user" "deps.edn"])
                         (utils/path->uri (conf/user-abs-joyride-path) ["deps.edn"])))

(defn maybe-create-user-gitignore+ []
  (maybe-create-content+ (default-content-uri ["user" ".gitignore"])
                         (utils/path->uri (conf/user-abs-joyride-path) [".gitignore"])))

(defn maybe-create-user-workspace-activate+ []
  (maybe-create-content+ (default-content-uri ["user" ".joyride" "scripts" "workspace_activate.cljs"])
                         (utils/path->uri (conf/user-abs-joyride-path) [".joyride" "scripts" "workspace_activate.cljs"])))

(defn maybe-create-user-copilot-instructions+ []
  (maybe-create-content+ (default-content-uri ["user" ".github" "copilot-instructions.md"])
                         (utils/path->uri (conf/user-abs-joyride-path) [".github" "copilot-instructions.md"])))

(defn maybe-create-user-project+ []
  (p/do
    (vscode/workspace.fs.createDirectory (utils/path->uri (conf/user-abs-joyride-path) ["sripts"]))
    (vscode/workspace.fs.createDirectory (utils/path->uri (conf/user-abs-joyride-path) ["src"]))
    (maybe-create-user-readme+)
    (maybe-create-user-config+)
    (maybe-create-user-gitignore+)
    (maybe-create-user-workspace-activate+)
    (maybe-create-user-copilot-instructions+)))