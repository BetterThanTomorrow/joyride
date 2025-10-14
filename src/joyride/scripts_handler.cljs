(ns joyride.scripts-handler
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.constants :as const]
            [joyride.db :as db]
            [joyride.getting-started :as getting-started]
            [joyride.output :as output]
            [joyride.sci :as jsci]
            [joyride.utils :refer [cljify jsify]]
            [joyride.vscode-utils :as utils]
            [promesa.core :as p]
            [sci.core :as sci]
            [clojure.string :as string]))

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
  (let [quoted-path (string/replace abs-path #"\\" "\\\\")]
    (str "(require '[\"module\" :as module])
        (let [req (module/createRequire \"/\")
              resolved (.resolve req \"" quoted-path "\")]
          (aset (.-cache req) resolved js/undefined)
          (js/require resolved))")))

(defn run-script+
  ([menu-conf+ base-path scripts-path]
   (handle-script-menu-selection+ menu-conf+ run-script+ base-path scripts-path))
  ([title base-path scripts-path script-path]
   (-> (p/let [abs-path (path/join base-path scripts-path script-path)
               script-uri (vscode/Uri.file abs-path)
               code (if (.endsWith script-path ".js")
                      (cljs-snippet-requiring-js abs-path)
                      (utils/vscode-read-uri+ script-uri))
               workspace-root (:workspace-root-path @db/!app-db)
               script-kind (cond
                             (= base-path conf/user-config-path) "user"
                             (and workspace-root (= base-path workspace-root)) "workspace"
                             :else nil)
               message (if script-kind
                         (str "Evaluating " script-kind " script: " script-path)
                         (str "Evaluating script: " script-path))]
         (output/append-line-other-out message)
         (swap! db/!app-db assoc :invoked-script abs-path)
         (sci/with-bindings {sci/file abs-path}
           (binding [jsci/*echo-eval-code?* false]
             (jsci/eval-string code))))
       (p/handle (fn [result error]
                   (swap! db/!app-db assoc :invoked-script nil)
                   (if error
                     (let [message (or (ex-message error) (.-message error) (str error))
                           headline (str title " Failed: " script-path " " message)]
                       (output/append-line-other-err headline)
                       (.then (vscode/window.showErrorMessage (str title " error") "Reveal output terminal")
                              (output/show-terminal)))
                     result))))))

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
                  (let [message (or (ex-message error) (.-message error) (str error))
                        headline (str title " Failed: " script-path " " message)]
                    (output/append-line-other-err headline)
                    (.then (vscode/window.showErrorMessage (str title " error") "Reveal output terminal")
                           (output/show-terminal))))))))

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

(defn script-template
  "Returns a script template with the given namespace"
  [the-ns]
  (str "(ns " the-ns "
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
  [the-ns]
  (str "(ns " the-ns ")"))

(defn parse-namespace-and-filename
  "Converts user input to namespace and file path.
   Handles both namespace-style (my.lib) and path-style (my/lib) inputs.
   Sanitizes dashes to underscores for file paths, dots to slashes (except final .cljs)"
  [input]
  (when-not (string/blank? input)
    (let [;; Remove .cljs extension if present to process the base name
          base-name (if (.endsWith input ".cljs")
                      (subs input 0 (- (count input) 5))
                      input)
        ;; Split on path separators and dots to get segments
          segments (-> base-name
                       (string/split #"[/\\.]"))
        ;; Create namespace (dots between segmen
          the-ns (string/join "." segments)
        ;; Create file path (slashes between segments, dashes become underscores)
          file-path-segments (map #(string/replace % "-" "_") segments)
          file-path (str (apply path/join file-path-segments) ".cljs")]
      (if (and (seq segments)
               (not (some string/blank? segments)))
        {:the-ns the-ns
         :file-path file-path}
        (throw (ex-info (str "Invalid namespace or file path: `" input "`") input))))))

(defn create-file-safely+
  "Creates a file only if it doesn't already exist.
   Returns a Promise that rejects if the file already exists."
  [^js file-uri content]
  (p/let [exists? (utils/path-or-uri-exists?+ file-uri)]
    (if exists?
      (p/rejected (ex-info (str "File already exists: " (.-fsPath file-uri))
                           {:file-path (.-fsPath file-uri)}))
      (vscode/workspace.fs.writeFile file-uri (.encode (js/TextEncoder.) content)))))

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
    (when-let [parsed (parse-namespace-and-filename input)]
      (let [{:keys [the-ns file-path]} parsed
            template (case file-type
                       :scripts (script-template the-ns)
                       (src-template the-ns))
            base-path (case file-type
                        :scripts (conf/user-abs-scripts-path)
                        (conf/user-abs-src-path))
            full-path (path/join base-path file-path)
            file-uri (vscode/Uri.file full-path)]
        (p/-> (p/do (vscode/workspace.fs.createDirectory (vscode/Uri.file (path/dirname full-path)))
                    (create-file-safely+ file-uri template))
              (p/then (fn [_]
                        (p/-> (vscode/workspace.openTextDocument file-uri)
                              (vscode/window.showTextDocument
                               #js {:preview false :preserveFocus false})))))))))

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
   :function #(create-and-open-user-file+ :scripts)
   :detail (str "Scripts can be run with the **Run User Script** command. Will be created in `" (conf/user-abs-scripts-path) "/`.")})

(def create-user-src-file-menu-item
  {:label (menu-label-with-icon "Create User Source File..." "plus")
   :function #(create-and-open-user-file+ :src)
   :detail (str "Will be created in `" (conf/user-abs-src-path) "/`.")})

(defn open-user-joyride-directory+
  "Opens the user Joyride directory in a new VS Code window"
  []
  (-> (p/let [joyride-path (conf/user-abs-joyride-path)
              joyride-uri (vscode/Uri.file joyride-path)]
        (vscode/commands.executeCommand "vscode.openFolder" joyride-uri true))
      (p/catch (fn [error]
                 (output/append-line-other-err (str "Failed to open User Joyride directory: " (.-message error)))
                 (.then (vscode/window.showErrorMessage "Failed to open User Joyride directory: " "Reveal output")
                        (output/show-terminal))))))

(def open-user-joyride-directory-menu-item
  {:label (menu-label-with-icon "Open User Joyride Directory in New Window" "folder")
   :function #'open-user-joyride-directory+
   :detail (str "Opens `" (conf/user-abs-joyride-path) "/` in a new VS Code window.")})

(defn run-user-script+
  ([]
   (apply run-script+
          (run-or-open-user-script-args
           (user-menu-conf+ "Run User Script..."
                            [open-user-script-menu-item
                             run-workspace-script-menu-item
                             create-user-script-menu-item
                             create-user-src-file-menu-item
                             open-user-joyride-directory-menu-item]
                            getting-started/maybe-create-user-activate-script+
                            getting-started/maybe-create-user-hello-script+))))
  ([script]
   (apply run-script+ (conj (run-or-open-user-script-args "Run") script))))

(defn open-user-script+
  ([]
   (handle-user-script-menu-selection+
    (user-scripts-and-src-menu-conf+ "Open User Script..."
                                     [run-user-script-menu-item
                                      open-workspace-script-menu-item
                                      create-user-script-menu-item
                                      create-user-src-file-menu-item
                                      open-user-joyride-directory-menu-item]
                                     getting-started/maybe-create-user-activate-script+
                                     getting-started/maybe-create-user-hello-script+)
    open-script+))
  ([script]
   (apply open-script+ (conj (run-or-open-user-script-args "Open") script))))
