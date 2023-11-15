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

(defn navigate-to
  [path]
  (-> (vscode/workspace.openTextDocument path)
      (vscode/window.showTextDocument)
      (p/catch #(vscode/window.showInformationMessage (str "Error: " %)))))

(defn make-relative-path
  [file-path]
  (p/let [editor ^js vscode/window.activeTextEditor
          doc-uri ^js (.. editor -document -uri)
          workspace-root-path (.. (vscode/workspace.getWorkspaceFolder doc-uri) -uri -path)]
    (path/relative workspace-root-path file-path)))

(defn make-absolute-path
  [relative-path]
  (p/let [editor ^js vscode/window.activeTextEditor
          doc-uri ^js (.. editor -document -uri)
          workspace-root-path (.. (vscode/workspace.getWorkspaceFolder doc-uri) -uri -path)]
    (str workspace-root-path path/sep relative-path)))

(defn find-candidate-impls
  [file-name]
  (p/let [extension (path/extname file-name)
          file-path (path/dirname file-name)
          editor ^js vscode/window.activeTextEditor
          doc-uri ^js (.. editor -document -uri)
          workspace-root-path (.. (vscode/workspace.getWorkspaceFolder doc-uri) -uri -path)
          rel (make-relative-path file-path)
          rel-pattern (vscode/RelativePattern. workspace-root-path (str rel path/sep "**" path/sep "*" extension))]
    (vscode/workspace.findFiles rel-pattern "**/interface.clj" 10)))

(defn interface->impl
  [file-name]
  (let [extension (path/extname file-name)]
    (if (str/ends-with? file-name (str "interface" extension))
      (p/let [candidates (find-candidate-impls file-name)]
        (case (count candidates)
          1
          (navigate-to (first candidates))

          0
          ;; Create new impl file?
          (vscode/window.showInformationMessage "Not sure what to show in this case")

          (p/let [rel-candidates (p/all (map (fn [c] (make-relative-path (.-path c))) candidates))
                  selection (vscode/window.showQuickPick (clj->js rel-candidates) #js {:title "Select implementation file"})
                  abs-selection (make-absolute-path selection)]
            (doto (joyride/output-channel) (.show true) (.append (pr-str abs-selection)))
            (navigate-to abs-selection))))
      (navigate-to (str/replace file-name "/interface/" "/")))))

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
                     (navigate-to interface-in-folder)))))))

(defn main
  []
  (p/let [editor ^js vscode/window.activeTextEditor
          file-name (.. editor -document -fileName)]
    (if (str/index-of file-name "interface")
      (interface->impl file-name)
      (impl->interface file-name))))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))
