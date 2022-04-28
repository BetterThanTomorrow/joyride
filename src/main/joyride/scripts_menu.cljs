(ns joyride.scripts-menu
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.settings :refer [workspace-scripts-path]]
            [joyride.utils :refer [jsify cljify]]
            [promesa.core :as p]))

(defn find-script-uris+ [script-folder-path]
  (let [glob (path/join script-folder-path "**" "*.cljs")]
    (p/let [script-uris (p/->> (vscode/workspace.findFiles glob)
                               cljify
                               (sort-by #(.-fsPath %)))]
      (jsify script-uris))))

(defn strip-base-path [base-path abs-path]
  (subs abs-path (count (str base-path path/sep))))

(defn script-uri->file-info [base-path ^js uri]
  (let [abs-path (.-fsPath uri)
        section-path (strip-base-path base-path abs-path)]
    {:uri uri
     :absolute-path abs-path
     :section-path section-path}))

(defn script-uris->file-infos+ [base-path script-uris]
  (p/let [file-infos (map (partial script-uri->file-info base-path)
                          script-uris)]
    file-infos))

(defn file-info->menu-item [section file-infos]
  (map (fn [file-info]
         (merge file-info
                (if section
                  {:label section
                   :description (:section-path file-info)}
                  {:label (:section-path file-info)})))
       file-infos))

(defn show-scripts-menu+
  "Shows a menu with scripts to the user.
   Returns the `vscode.Uri` of the picked item"
  ;; TODO: Add user scripts to the menu
  [title section file-infos]
  (p/let [menu-items (jsify (file-info->menu-item section file-infos))
          script-info (vscode/window.showQuickPick menu-items #js {:title title})]
    (cljify script-info)))

(defn show-workspace-scripts-menu+ []
  (-> (p/let [ws-folder-path vscode/workspace.rootPath
              script-uris (find-script-uris+ workspace-scripts-path)
              base-path (path/join ws-folder-path workspace-scripts-path)
              file-infos (p/->> (script-uris->file-infos+ base-path script-uris)
                                (map (fn [f-i]
                                       (assoc f-i :workspace-scripts-path workspace-scripts-path))))
              picked-script (show-scripts-menu+ "Run Workspace Script" nil #_"Workspace" file-infos)]
        picked-script)
      (p/handle (fn [result error]
                  (if error
                    (js/console.error "Selecting Workspace Script Failed: " (.-message error))
                    result)))))

(comment
  (p/let [picked-script (show-workspace-scripts-menu+)]
    (def picked-script picked-script)
    picked-script)

  (p/let [script-uris (find-script-uris+ (path/join ".joyride" "scripts"))]
    (def script-uris script-uris)
    script-uris)

  (def ws-folder-path (-> vscode/workspace.workspaceFolders
                          first
                          (.-uri)
                          (.-fsPath)))

  (p/let [base-path (path/join ws-folder-path workspace-scripts-path)
          file-infos (script-uris->file-infos+ base-path script-uris)]
    (def file-infos file-infos)
    file-infos)

  (p/let [picked-script (show-scripts-menu+ "Run Workspace Script" nil #_"Workspace" file-infos)]
    (def picked-script picked-script)
    picked-script)

  )
