(ns joyride.scripts-menu
  (:require ["fast-glob" :as fast-glob]
            ["path" :as path]
            ["vscode" :as vscode]
            [joyride.utils :refer [cljify jsify]]
            [promesa.core :as p]))

(defn find-script-uris+
  "Returns a Promise that resolves to JS array of `vscode.Uri`s
   for the scripts files found in `base-path`/`script-folder-path`
   
   Will use `vscode/workspace` API for it if there is a workspace
   root, otherwise it uses direct filesystem access. (Probably means
   it is only Remote friendly in the case with a workspace root.)"
  [base-path script-folder-path]
  (if vscode/workspace.rootPath
    (p/let [glob (vscode/RelativePattern. base-path (path/join script-folder-path "**" "*.cljs"))
            script-uris (p/->> (vscode/workspace.findFiles glob)
                               cljify
                               (sort-by #(.-fsPath ^js %)))]
      (jsify script-uris))
    (p/let [glob (path/join base-path script-folder-path "**" "*.cljs")
            script-uris (p/->> (fast-glob glob #js {:dot true})
                               (map #(vscode/Uri.file %))
                               (sort-by #(.-fsPath ^js %)))]
      (jsify script-uris))))

(defn strip-abs-scripts-path [abs-scripts-path abs-path]
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
  [title file-infos]
  (p/let [menu-items (jsify (file-info->menu-item file-infos))
          script-info (vscode/window.showQuickPick menu-items #js {:title title})]
    (cljify script-info)))

(defn show-script-picker+
  "Shows a menu with scripts to the user.
   Returns the picked item as a map with keys:
   `:uri`, `:absolute-path`, `:relative-path`
   Where `:relative-path` is relative to the `base-path`"
  [title base-path scripts-path]
  (-> (p/let [script-uris (find-script-uris+ base-path scripts-path)
              abs-scripts-path (path/join base-path scripts-path)
              file-infos (script-uris->file-infos+ abs-scripts-path script-uris)
              picked-script (show-script-picker'+ title file-infos)]
        picked-script)
      (p/handle (fn [result error]
                  (if error
                    (js/console.error title "Failed:" (.-message error))
                    result)))))
