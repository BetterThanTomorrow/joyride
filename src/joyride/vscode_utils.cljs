(ns joyride.vscode-utils
  (:require
   ["fdir" :refer [fdir]]
   ["path" :as path]
   ["vscode" :as vscode]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [joyride.config :as config]
   [joyride.db :as db]
   [joyride.utils :as utils]
   [promesa.core :as p]
   [joyride.output :as output]))

(defn path-or-uri-exists?+ [path-or-uri]
  (-> (p/let [uri (if (= (type "") (type path-or-uri))
                    (vscode/Uri.file path-or-uri)
                    path-or-uri)
              _stat (vscode/workspace.fs.stat uri)])
      (p/handle
       (fn [_r, e]
         (if e
           false
           true)))))

(defn vscode-read-uri+ [^js uri-or-path]
  (let [uri (if (string? uri-or-path)
              (vscode/Uri.file uri-or-path)
              uri-or-path)]
    (-> (p/let [_ (vscode/workspace.fs.stat uri)
                data (vscode/workspace.fs.readFile uri)
                decoder (js/TextDecoder. "utf-8")
                code (.decode decoder data)]
          code))))

(defn workspace-root []
  vscode/workspace.rootPath)

(defn info [title & xs]
  (output/append-line-other-err! (str title "\n" (str/join " " (mapv str xs))))
  (.then (vscode/window.showInformationMessage title "Reveal output terminal")
         (output/show-terminal!)))

(defn warn [title & xs]
  (output/append-line-other-err! (str title "\n" (str/join " " (mapv str xs))))
  (.then (vscode/window.showWarningMessage title "Reveal output terminal")
         (output/show-terminal!)))

(defn error [title & xs]
  (output/append-line-other-err! (str title "\n" (str/join " " (mapv str xs))))
  (.then (vscode/window.showErrorMessage (str/join " " (mapv str xs)) "Reveal output terminal")
         (output/show-terminal!)))

(defn extension-path []
  (-> ^js (:extension-context @db/!app-db)
      (.-extensionPath)))

(defonce glob-er (fdir.))

(defn find-fs-files+
  "Returns Uris for files on the filesystem in `crawl-path` matching `glob`.
   NB: Not remote friendly! Is not using `vscode/workspace` API."
  [crawl-path glob]
  (-> glob-er
      (.withBasePath)
      (.glob glob)
      (.crawl crawl-path)
      (.withPromise)
      (p/then (fn [files]
                (->> (utils/cljify files)
                     (map #(vscode/Uri.file %))
                     (sort-by #(.-fsPath ^js %)))))))

(defn path->uri
  "Creates a VS Code URI by joining a base path with sub-path components"
  [base-path sub-path]
  (apply (.-joinPath vscode/Uri) (vscode/Uri.file base-path) sub-path))

(defn as-workspace-abs-path
  "Returns the absolute path of file-path, assuming relative paths are relative to the worksspace."
  [file-path]
  (if (path/isAbsolute file-path)
    file-path
    (if-let [workspace-root (config/workspace-abs-path)]
      (path/join workspace-root file-path)
      file-path)))