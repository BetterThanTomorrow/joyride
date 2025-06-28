(ns joyride.scripts-handler
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]
            [joyride.config :as conf]
            [joyride.constants :as const]
            [joyride.db :as db]
            [joyride.getting-started :as getting-started]
            [joyride.sci :as jsci]
            [joyride.utils :as utils :refer [cljify jsify]]
            [promesa.core :as p]
            [sci.core :as sci]))

(defn find-script-uris+
  "Returns a Promise that resolves to JS array of `vscode.Uri`s
   for the scripts files found in `base-path`/`script-folder-path`

   Will use `vscode/workspace` API for it if there is a workspace
   root, otherwise it uses direct filesystem access. (Probably means
   it is only Remote friendly in the case with a workspace root.)"
  [base-path script-folder-path]
  (if vscode/workspace.rootPath
    (p/let [glob (vscode/RelativePattern. base-path (path/join script-folder-path "**" "*.{cljs,cljc,js}"))
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
          (js/require resolved))"))

(defn run-script+
  ([menu-conf+ base-path scripts-path]
   (handle-script-menu-selection+ menu-conf+ run-script+ base-path scripts-path))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)
               code (if (.endsWith script-path ".js")
                      (cljs-snippet-requiring-js abs-path)
                      (utils/vscode-read-uri+ script-uri))]
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
          create-activate-script? (and create-activate-fn
                                       (not (some #(= % "workspace_activate.cljs") scripts)))
          create-hello-script? (and create-hello-fn
                                    (or (empty? scripts)
                                        (= scripts '("workspace_activate.cljs"))))]
    {:title title
     :more-menu-items (cond-> []
                        create-activate-script? (conj {:label (menu-label-with-icon
                                                               "Create Workspace Script workspace_activate.cljs"
                                                               "plus")
                                                       :function create-activate-fn})
                        create-hello-script? (conj {:label (menu-label-with-icon
                                                            "Create Workspace Script hello_joyride_workspace_script.cljs"
                                                            "plus")
                                                    :function create-hello-fn})
                        :always (into more-menu-items))}))

(defn user-menu-conf+ [title more-menu-items create-activate-fn create-hello-fn]
  (p/let [script-uris (cljify (find-script-uris+ conf/user-config-path
                                                 conf/user-scripts-path))
          scripts (map (fn [^js uri]
                         (->> uri
                              (.-fsPath)
                              (strip-abs-scripts-path (conf/user-abs-scripts-path))))
                       script-uris)
          create-activate-script? (and create-activate-fn
                                       (not (some #(= % "user_activate.cljs") scripts)))
          create-hello-script? (and create-hello-fn
                                    (or (empty? scripts)
                                        (= scripts '("user_activate.cljs"))))]
    {:title title
     :more-menu-items (cond-> []
                        create-activate-script? (conj {:label (menu-label-with-icon
                                                               "Create User Script user_activate.cljs"
                                                               "plus")
                                                       :function create-activate-fn})
                        create-hello-script? (conj {:label (menu-label-with-icon
                                                            "Create User Script hello_joyride_user_script.cljs"
                                                            "plus")
                                                    :function create-hello-fn})
                        :always (into more-menu-items))}))

(defn find-user-scripts-and-src-uris+
  "Returns a Promise that resolves to a map with :scripts and :src keys,
   each containing arrays of file URIs for their respective directories"
  []
  (p/let [scripts-uris (find-script-uris+ conf/user-config-path conf/user-scripts-path)
          src-uris (find-script-uris+ conf/user-config-path (path/join conf/user-joyride-path "src"))]
    {:scripts scripts-uris
     :src src-uris}))

(defn user-files->sectioned-file-infos+
  "Converts the user-files map to file-infos with section information"
  [user-files]
  (p/let [scripts-infos (when (seq (:scripts user-files))
                          (script-uris->file-infos+ (conf/user-abs-scripts-path) (:scripts user-files)))
          src-infos (when (seq (:src user-files))
                      (script-uris->file-infos+ (conf/user-abs-src-path) (:src user-files)))]
    {:scripts (map #(assoc % :section :scripts) (or scripts-infos []))
     :src (map #(assoc % :section :src) (or src-infos []))}))

(defn create-sectioned-menu-items
  "Creates menu items with section separators for scripts and src files"
  [sectioned-file-infos more-menu-items]
  (let [scripts-items (file-info->menu-item (:scripts sectioned-file-infos))
        src-items (file-info->menu-item (:src sectioned-file-infos))]
    (cond-> []
      (seq scripts-items) (-> (conj {:label "scripts/" :kind vscode/QuickPickItemKind.Separator})
                              (into scripts-items))
      (seq src-items) (-> (conj {:label "src/" :kind vscode/QuickPickItemKind.Separator})
                          (into src-items))
      :always (-> (conj {:kind vscode/QuickPickItemKind.Separator})
                  (into more-menu-items)))))

(defn show-user-script-picker+
  "Shows a menu with both user scripts and src files to the user, organized in sections.
   Returns the picked item as a map with keys:
   `:uri`, `:absolute-path`, `:relative-path`, `:section`"
  [{:keys [title more-menu-items]}]
  (p/let [user-files (find-user-scripts-and-src-uris+)
          sectioned-file-infos (user-files->sectioned-file-infos+ user-files)
          menu-items (create-sectioned-menu-items sectioned-file-infos more-menu-items)
          script-info (vscode/window.showQuickPick (jsify menu-items) #js {:title title})]
    (cljify script-info)))

(defn handle-user-script-menu-selection+
  "Handles user script menu selection for files that can be in scripts or src directories"
  [menu-conf+ script-fn+]
  (p/let [{:keys [title] :as menu-conf} menu-conf+
          pick (show-user-script-picker+ menu-conf)]
    (when pick
      (let [relative-path (:relative-path pick)
            section (:section pick)
            function (:function pick)]
        (cond
          relative-path (case section
                          :scripts (script-fn+ title conf/user-config-path conf/user-scripts-path relative-path)
                          :src (script-fn+ title conf/user-config-path (path/join conf/user-joyride-path "src") relative-path)
                          ;; fallback for backwards compatibility
                          (script-fn+ title conf/user-config-path conf/user-scripts-path relative-path))
          function (function))))))

(defn user-scripts-and-src-menu-conf+
  "User menu configuration that includes both scripts and src files"
  [title more-menu-items create-activate-fn create-hello-fn]
  (p/let [user-files (find-user-scripts-and-src-uris+)
          all-scripts (concat (:scripts user-files) (:src user-files))
          scripts (map (fn [^js uri]
                         (->> uri
                              (.-fsPath)
                              path/basename))
                       all-scripts)
          create-activate-script? (and create-activate-fn
                                       (not (some #(= % "user_activate.cljs") scripts)))
          create-hello-script? (and create-hello-fn
                                    (or (empty? scripts)
                                        (= scripts '("user_activate.cljs"))))]
    {:title title
     :more-menu-items (cond-> []
                        create-activate-script? (conj {:label (menu-label-with-icon
                                                               "Create User Script user_activate.cljs"
                                                               "plus")
                                                       :function create-activate-fn})
                        create-hello-script? (conj {:label (menu-label-with-icon
                                                            "Create User Script hello_joyride_user_script.cljs"
                                                            "plus")
                                                    :function create-hello-fn})
                        :always (into more-menu-items))}))

(defn parse-namespace-and-filename
  "Converts user input to namespace and file path.
   Handles both namespace-style (my.lib) and path-style (my/lib) inputs.
   Sanitizes dashes to underscores for file paths, dots to slashes (except final .cljs)"
  [input]
  (let [;; Remove .cljs extension if present to process the base name
        base-name (if (.endsWith input ".cljs")
                    (subs input 0 (- (count input) 5))
                    input)
        ;; Split on path separators and dots to get segments
        segments (-> base-name
                     (str/split #"[/\\.]"))
        ;; Create namespace (dots between segments, dashes preserved)
        namespace (str/join "." segments)
        ;; Create file path (slashes between segments, dashes become underscores)
        file-path-segments (map #(str/replace % "-" "_") segments)
        file-path (str (path/join file-path-segments) ".cljs")]
    {:namespace namespace
     :file-path file-path}))

(defn script-template
  "Returns a script template with the given namespace"
  [namespace]
  (str "(ns " namespace "
  (:require [\"vscode\" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]
            :reload))

(defn- main []
  (println \"Hello World\"))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))"))

(defn src-template
  "Returns a source file template with the given namespace"
  [namespace]
  (str "(ns " namespace ")"))

(defn create-and-open-user-file+
  "Creates a new user file (script or src) and opens it in the editor"
  [file-type]
  (p/let [input (vscode/window.showInputBox
                 #js {:title (str "Create User `" (name file-type) "/` File")
                      :placeHolder "E.g. the-thing, or the_thing.cljs"
                      :prompt (str "Enter a namespace or file path. Will be created in, and opeed from: `"
                                   (case file-type
                                     :scripts (conf/user-abs-scripts-path)
                                     (conf/user-abs-src-path))
                                   "/`.")})]
    (when input
      (let [{:keys [namespace file-path]} (parse-namespace-and-filename input)
            template (case file-type
                       :scripts (script-template namespace)
                       (src-template namespace))
            base-path (case file-type
                        :scripts (conf/user-abs-scripts-path)
                        (conf/user-abs-src-path))
            full-path (path/join base-path file-path)
            file-uri (vscode/Uri.file full-path)]
        (p/do (vscode/workspace.fs.createDirectory (vscode/Uri.file (path/dirname full-path)))
              (vscode/workspace.fs.writeFile file-uri (.encode (js/TextEncoder.) template))
              (p/-> (vscode/workspace.openTextDocument file-uri)
                    (vscode/window.showTextDocument
                     #js {:preview false :preserveFocus false})))))))

(defn run-workspace-script+
  ([]
   (apply run-script+
          (run-or-open-workspace-script-args
           (workspace-menu-conf+ "Run Workspace Script..."
                                 [open-workspace-script-menu-item
                                  run-user-script-menu-item]
                                 getting-started/maybe-create-workspace-activate-script+
                                 getting-started/maybe-create-workspace-hello-script+))))
  ([script]
   (apply run-script+ (conj (run-or-open-workspace-script-args "Run") script))))

(defn run-user-script+
  ([]
   (apply run-script+
          (run-or-open-user-script-args
           (user-menu-conf+ "Run User Script..."
                            [open-user-script-menu-item
                             run-workspace-script-menu-item]
                            getting-started/maybe-create-user-activate-script+
                            getting-started/maybe-create-user-hello-script+))))
  ([script]
   (apply run-script+ (conj (run-or-open-user-script-args "Run") script))))

(defn open-workspace-script+
  ([]
   (apply open-script+
          (run-or-open-workspace-script-args
           (workspace-menu-conf+ "Open Workspace Script..."
                                 [run-workspace-script-menu-item
                                  open-user-script-menu-item]
                                 getting-started/maybe-create-workspace-activate-script+
                                 getting-started/maybe-create-workspace-hello-script+))))
  ([script]
   (apply open-script+ (conj (run-or-open-workspace-script-args "Open") script))))

(def create-user-script-menu-item
  {:label (menu-label-with-icon "Create User Script..." "plus")
   :function #(create-and-open-user-file+ :scripts)})

(def create-user-src-file-menu-item
  {:label (menu-label-with-icon "Create User Source File..." "plus")
   :function #(create-and-open-user-file+ :src)})

(defn open-user-script+
  ([]
   (handle-user-script-menu-selection+
    (user-scripts-and-src-menu-conf+ "Open User Script..."
                                     [run-user-script-menu-item
                                      open-workspace-script-menu-item
                                      create-user-script-menu-item
                                      create-user-src-file-menu-item]
                                     getting-started/maybe-create-user-activate-script+
                                     getting-started/maybe-create-user-hello-script+)
    open-script+))
  ([script]
   (apply open-script+ (conj (run-or-open-user-script-args "Open") script))))