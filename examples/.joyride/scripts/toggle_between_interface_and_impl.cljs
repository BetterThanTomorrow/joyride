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
            [joyride.core :as joyride]))

(defn ^:async navigate-to
  [path]
  (try
    (let [doc (await (vscode/workspace.openTextDocument path))]
      (await (vscode/window.showTextDocument doc)))
    (catch :default e
      (vscode/window.showInformationMessage (str "Error: " e)))))

(defn make-relative-path
  [file-path]
  (let [editor ^js vscode/window.activeTextEditor
        doc-uri ^js (.. editor -document -uri)
        workspace-root-path (.. (vscode/workspace.getWorkspaceFolder doc-uri) -uri -path)]
    (path/relative workspace-root-path file-path)))

(defn make-absolute-path
  [relative-path]
  (let [editor ^js vscode/window.activeTextEditor
        doc-uri ^js (.. editor -document -uri)
        workspace-root-path (.. (vscode/workspace.getWorkspaceFolder doc-uri) -uri -path)]
    (str workspace-root-path path/sep relative-path)))

(defn ^:async find-candidate-impls
  [file-name]
  (let [extension (path/extname file-name)
        file-path (path/dirname file-name)
        editor ^js vscode/window.activeTextEditor
        doc-uri ^js (.. editor -document -uri)
        workspace-root-path (.. (vscode/workspace.getWorkspaceFolder doc-uri) -uri -path)
        rel (make-relative-path file-path)
        rel-pattern (vscode/RelativePattern. workspace-root-path (str rel path/sep "**" path/sep "*" extension))]
    (await (vscode/workspace.findFiles rel-pattern "**/interface.clj" 10))))

(defn ^:async interface->impl
  [file-name]
  (let [extension (path/extname file-name)]
    (if (str/ends-with? file-name (str "interface" extension))
      (let [candidates (await (find-candidate-impls file-name))]
        (case (count candidates)
          1
          (await (navigate-to (first candidates)))

          0
          (vscode/window.showInformationMessage "Not sure what to show in this case")

          (let [rel-candidates (mapv (fn [c] (make-relative-path (.-path c))) candidates)
                selection (await (vscode/window.showQuickPick (clj->js rel-candidates) #js {:title "Select implementation file"}))
                abs-selection (make-absolute-path selection)]
            (println (pr-str abs-selection))
            (await (navigate-to abs-selection)))))
      (await (navigate-to (str/replace file-name "/interface/" "/"))))))

(defn ^:async impl->interface
  [file-name]
  (let [file-path (path/dirname file-name)
        base-name (path/basename file-name)
        extension (path/extname file-name)
        interface-file (path/resolve file-path (str "interface" extension))]
    (try
      (let [doc (await (vscode/workspace.openTextDocument interface-file))]
        (await (vscode/window.showTextDocument doc)))
      (catch :default _
        (let [interface-in-folder (path/resolve file-path (str "interface" path/sep base-name))]
          (await (navigate-to interface-in-folder)))))))

(defn ^:async main
  []
  (let [editor ^js vscode/window.activeTextEditor
        file-name (.. editor -document -fileName)]
    (if (str/index-of file-name "interface")
      (await (interface->impl file-name))
      (await (impl->interface file-name)))))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))
