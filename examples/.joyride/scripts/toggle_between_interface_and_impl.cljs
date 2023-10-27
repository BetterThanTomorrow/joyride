(ns toggle-between-interface-and-impl
  "This is a Joyride script that is intended for Polylith users. You can use it
   to switch between the interface and implementation of a component. Note: not
   all cases work, specifically when you're in `interface.clj`, this script does
   not yet know how to handle that. (Some ideas: display a list of candidates that
   could be the matching implementation, if there is only one candidate just open
   it right away, look at the list of requires in the ns and do something clever, etc.)

   Just like CTRL-SHIFT-T can toggle between test and impl (in Calva), I bound
   this script to CTRL-SHIFT-I using this keybinding config:

   {
     \"key\": \"ctrl+shift+i\",
     \"command\": \"joyride.runWorkspaceScript\",
     \"args\": \"toggle_between_interface_and_impl.cljs\",
     \"when\": \"editorTextFocus && editorLangId =~ /clojure|scheme|lisp/\"
   }"
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(defn interface->impl
  [file-name]
  (let [extension (path/extname file-name)]
    (if (str/ends-with? file-name (str "interface" extension))
      (vscode/window.showInformationMessage "Not sure what to show in this case")
      (-> (vscode/workspace.openTextDocument (str/replace file-name "/interface/" "/"))
          (vscode/window.showTextDocument)
          (p/catch #(vscode/window.showInformationMessage (str "Error: " %)))))))

(defn impl->interface
  [file-name]
  (p/let [file-path (path/dirname file-name)
          base-name (path/basename file-name)
          extension (path/extname file-name)
          interface-file (path/resolve file-path (str "interface" extension))]
    ;; First try if there is a matching file called "interface.clj"
    (-> (vscode/workspace.openTextDocument interface-file)
        (vscode/window.showTextDocument)
        (p/catch (fn [_]
                   ;; Then try .../interface/<name>.clj
                   (let [interface-in-folder (path/resolve file-path (str "interface" path/sep base-name))]
                     (-> (vscode/workspace.openTextDocument interface-in-folder)
                         (vscode/window.showTextDocument)
                         (p/catch #(vscode/window.showInformationMessage (str "Error: " %))))))))))

(defn main
  []
  (p/let [editor ^js vscode/window.activeTextEditor
          file-name (.. editor -document -fileName)]
    (if (str/index-of file-name "interface")
      (interface->impl file-name)
      (impl->interface file-name))))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))

(comment
  (main)
  )
