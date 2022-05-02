(ns joyride.scripts-menu
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.config :as conf]
            [joyride.utils :refer [jsify cljify]]
            [promesa.core :as p]))

(defn find-script-uris+ [base-path script-folder-path]
  (let [glob (vscode/RelativePattern. base-path (path/join script-folder-path "**" "*.cljs"))]
    (p/let [script-uris (p/->> (vscode/workspace.findFiles glob)
                               cljify
                               (sort-by #(.-fsPath ^js %)))]
      (jsify script-uris))))

(comment
  (let [uris (find-script-uris+ vscode/workspace.rootPath conf/workspace-scripts-path)]
    (def uris uris)
    uris)

  (let [uris (find-script-uris+ conf/user-config-path conf/user-scripts-path)]
    (def uris uris)
    uris)
  (def glob (path/join conf/user-scripts-path "**" "*.cljs")))

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
