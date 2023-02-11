(ns joyride.scripts-handler
  (:require ["/joyride/vscode.js" :as vscode]
            ["path" :as path]
            [joyride.config :as conf]
            [joyride.constants :as const]
            [joyride.db :as db]
            [joyride.getting-started :as getting-started]
            [joyride.sci :as jsci]
            [joyride.utils :as utils :refer [cljify jsify]]
            [promesa.core :as p]
            [sci.core :as sci]
            [clojure.string :as str]))

(defn find-script-uris+
  "Returns a Promise that resolves to JS array of `vscode/Uri`s
   for the scripts files found in `base-path`/`script-folder-path`
   
   Will use `vscode/workspace` API for it if there is a workspace
   root, otherwise it uses direct filesystem access. (Probably means
   it is only Remote friendly in the case with a workspace root.)"
  [base-path script-folder-path]
  (if vscode/workspace.rootPath
    (p/let [glob (vscode/RelativePattern. base-path (path/join script-folder-path "**" "*.{cljs,js}"))
            script-uris (p/->> (vscode/workspace.findFiles glob)
                               cljify
                               (sort-by #(.-fsPath ^js %)))]
      (jsify script-uris))
    (p/let [crawl-path (path/join base-path script-folder-path)
            glob "**/*.cljs"
            script-uris (utils/find-fs-files+ crawl-path glob)]
      (jsify script-uris))))

(defn strip-abs-scripts-path
  "Strips the `scripts` path away from an absolute path to a script.
   I.e. everything from the root up to, and including the `scripts`
   directory for the section (User or Workspace) in question.
   Used for the label in the scripts menus."
  [abs-scripts-path abs-path]
  (subs abs-path (count (str abs-scripts-path path/sep))))

(defn script-uri->file-info [abs-scripts-path ^js uri]
  (let [abs-path (.-fsPath uri)
        section-path (strip-abs-scripts-path abs-scripts-path abs-path)]
    {:uri uri
     :absolute-path abs-path
     :relative-path section-path}))

(defn script-uris->file-infos+ [abs-scripts-path script-uris]
  (p/let [file-infos (map (partial script-uri->file-info abs-scripts-path)
                          script-uris)]
    file-infos))

(defn file-info->menu-item [file-infos]
  (map (fn [file-info]
         (assoc file-info :label (:relative-path file-info)))
       file-infos))

(defn- show-script-picker'+
  [title file-infos more-menu-items]
  (p/let [file-items (into [] (file-info->menu-item file-infos))
          menu-items (into file-items
                           (into [{:kind vscode/QuickPickItemKind.Separator}]
                                 more-menu-items))
          script-info (vscode/window.showQuickPick (jsify menu-items) #js {:title title})]
    (cljify script-info)))

(defn show-script-picker+
  "Shows a menu with scripts to the user.
   Returns the picked item as a map with keys:
   `:uri`, `:absolute-path`, `:relative-path`
   Where `:relative-path` is relative to the `base-path`"
  [{:keys [title more-menu-items]} base-path scripts-path]
  (-> (p/let [script-uris (find-script-uris+ base-path scripts-path)
              abs-scripts-path (path/join base-path scripts-path)
              file-infos (script-uris->file-infos+ abs-scripts-path script-uris)
              picked-script (show-script-picker'+ title file-infos more-menu-items)]
        picked-script)))

(defn handle-script-menu-selection+
  [menu-conf+ script-fn+ base-path scripts-path]
  (p/let [{:keys [title] :as menu-conf} menu-conf+
          pick (show-script-picker+ menu-conf base-path scripts-path)]
    (when pick
      (let [relative-path (:relative-path pick)
            function (:function pick)]
        (cond
          relative-path (script-fn+ title base-path scripts-path relative-path)
          function (function))))))

(defn- cljs-snippet-requiring-js [abs-path]
  (str "(require '[\"module\" :as module])
        (let [req (module/createRequire \"/\")
              resolved (.resolve req \"" abs-path "\")]
          (aset (.-cache req) resolved js/undefined)
          (req resolved))"))

(defn run-script+
  ([menu-conf+ base-path scripts-path]
   (handle-script-menu-selection+ menu-conf+ run-script+ base-path scripts-path))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)
               code (if (.endsWith script-path ".js")
                      (cljs-snippet-requiring-js abs-path)
                      (utils/vscode-read-uri+ script-uri))]
         #_(vscode/window.showInformationMessage (str code))
         (swap! db/!app-db assoc :invoked-script abs-path)
         (sci/with-bindings {sci/file abs-path}
           (jsci/eval-string code)))
       (p/handle (fn [result error]
                   (swap! db/!app-db assoc :invoked-script nil)
                   (if error
                     (binding [utils/*show-when-said?* true]
                       (utils/say-error (str title " Failed: " script-path " " (.-message error))))
                     (do (utils/say-result (str script-path " evaluated.") result)
                         result)))))))

(defn open-script+
  ([menu-conf+ base-path scripts-path]
   (handle-script-menu-selection+ menu-conf+ open-script+ base-path scripts-path))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)]
         (p/-> (vscode/workspace.openTextDocument script-uri)
               (vscode/window.showTextDocument
                #js {:preview false, :preserveFocus false})))
       (p/catch (fn [error]
                  (binding [utils/*show-when-said?* true]
                    (utils/say-error (str title " Failed: " script-path " " (.-message error)))))))))

(defn run-or-open-workspace-script-args [menu-conf-or-title+]
  [menu-conf-or-title+
   (:workspace-root-path @db/!app-db)
   conf/workspace-scripts-path])

(defn run-or-open-user-script-args [menu-conf-or-title]
  [menu-conf-or-title
   conf/user-config-path
   conf/user-scripts-path])

(defn menu-label-with-icon [label icon]
  (str "$(" icon ")" (:space const/glyph-chars) label))

(defn function-menu-item [{:keys [label function description icon]}]
  {:label (menu-label-with-icon label icon)
   :description description
   :function function})

(declare open-user-script+)
(declare run-user-script+)
(declare open-workspace-script+)
(declare run-workspace-script+)

(def open-workspace-script-menu-item (function-menu-item
                                      {:label "Open Workspace Script..."
                                       :icon "go-to-file"
                                       :function #'open-workspace-script+}))

(def open-user-script-menu-item (function-menu-item
                                 {:label "Open User Script..."
                                  :icon "go-to-file"
                                  :function #'open-user-script+}))

(def run-workspace-script-menu-item (function-menu-item
                                     {:label "Run Workspace Script..."
                                      :icon "play"
                                      :function #'run-workspace-script+}))

(def run-user-script-menu-item (function-menu-item
                                {:label "Run User Script..."
                                 :icon "play"
                                 :function #'run-user-script+}))

(defn workspace-menu-conf+ [title more-menu-items create-activate-fn create-hello-fn]
  (p/let [script-uris (cljify (find-script-uris+ (:workspace-root-path @db/!app-db)
                                                 conf/workspace-scripts-path))
          scripts (map (fn [^js uri]
                         (->> uri
                              (.-fsPath)
                              (strip-abs-scripts-path (conf/workspace-abs-scripts-path))))
                       script-uris)
          create-hello-script? (and create-hello-fn
                                    (or (empty? scripts)
                                        (= scripts '("activate.cljs"))))]
    {:title title
     :more-menu-items (cond-> []
                        create-activate-fn (conj {:label (menu-label-with-icon
                                                          "Create Workspace workspace_activate.cljs"
                                                          "plus")
                                                  :function create-activate-fn})
                        create-hello-script? (conj {:label (menu-label-with-icon
                                                            "Create Workspace hello_joyride_workspace_script.cljs"
                                                            "plus")
                                                    :function create-hello-fn})
                        :always (into more-menu-items))}))

(defn run-workspace-script+
  ([]
   (apply run-script+
          (run-or-open-workspace-script-args
           (p/let [create-activate-fn+ (getting-started/maybe-create-workspace-activate-fn+)
                   create-hello-fn+ (getting-started/maybe-create-workspace-hello-fn+)]
             (workspace-menu-conf+ "Run Workspace Script..."
                                   [open-workspace-script-menu-item
                                    run-user-script-menu-item]
                                   create-activate-fn+
                                   create-hello-fn+)))))
  ([script]
   (apply run-script+ (conj (run-or-open-workspace-script-args "Run") script))))

(defn run-user-script+
  ([]
   (apply run-script+ (run-or-open-user-script-args
                       {:title "Run User Script..."
                        :more-menu-items [open-user-script-menu-item
                                          run-workspace-script-menu-item]})))
  ([script]
   (apply run-script+ (conj (run-or-open-user-script-args "Run") script))))

(defn open-workspace-script+
  ([]
   (apply open-script+
          (run-or-open-workspace-script-args
           (p/let [create-activate-fn+ (getting-started/maybe-create-workspace-activate-fn+)
                   create-hello-fn+ (getting-started/maybe-create-workspace-hello-fn+)]
             (workspace-menu-conf+ "Open Workspace Script..."
                                   [run-workspace-script-menu-item
                                    open-user-script-menu-item]
                                   create-activate-fn+
                                   create-hello-fn+)))))
  ([script]
   (apply open-script+ (conj (run-or-open-workspace-script-args "Open") script))))

(defn open-user-script+
  ([]
   (apply open-script+ (run-or-open-user-script-args
                        {:title "Open User Script..."
                         :more-menu-items [run-user-script-menu-item
                                           open-workspace-script-menu-item]})))
  ([script]
   (apply open-script+ (conj (run-or-open-user-script-args "Open") script))))
