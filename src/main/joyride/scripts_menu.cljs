(ns joyride.scripts-menu
  (:require ["path" :as path]
            [joyride.util :refer [jsify cljify]]
            ["vscode" :as vscode]
            [promesa.core :as p]))

;; TODO: Pick these from settings
(def user-scripts-path "/Users/pez/.joyride/scripts")
(def workspace-scripts-path ".joyride/scripts")

(defn find-script-uris+ [script-folder-path]
  (let [glob (path/join script-folder-path "*.cljs")]
    (p/let [script-uris (vscode/workspace.findFiles glob)]
      script-uris)))

(defn strip-base-path [base-path abs-path]
  (subs abs-path (count (str base-path path/sep))))

(defn script-uri->file-info [base-path uri]
  (let [abs-path (.-fsPath uri)
        script (strip-base-path base-path abs-path)]
    {:uri uri
     :script script}))

(defn script-uris->file-infos+ [base-path script-uris]
  (p/let [file-infos (map (partial script-uri->file-info base-path)
                          script-uris)]
    file-infos))

(defn file-info->menu-item [section file-infos]
  (map (fn [file-info]
         (merge file-info
                {:label section
                 :description (:script file-info)}))
       file-infos))

(defn ask-user-to-select-script+
  "Shows a menu with scripts to the user.
   Returns the `vscode.Uri` of the picked item"
  ;; TODO: Add user scripts to the menu
  [section file-infos]
  (p/let [menu-items (jsify (file-info->menu-item section file-infos))
          script-info (vscode/window.showQuickPick menu-items #js {:title "Run Script"})]
    (:uri (cljify script-info))))

(comment
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

  (p/let [picked-script (ask-user-to-select-script+ "Workspace" file-infos)]
    (def picked-script picked-script)
    picked-script)

  )