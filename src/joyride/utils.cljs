(ns joyride.utils
  (:require ["fdir" :refer [fdir]]
            ["vscode" :as vscode]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [joyride.db :as db]
            [promesa.core :as p]))

(defn jsify [clj-thing]
  (clj->js clj-thing))

(defn cljify [js-thing]
  (js->clj js-thing :keywordize-keys true))

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

(defn info [& xs]
  (vscode/window.showInformationMessage (str/join " " (mapv str xs))))

(defn warn [& xs]
  (vscode/window.showWarningMessage (str/join " " (mapv str xs))))

(defn error [& xs]
  (vscode/window.showErrorMessage (str/join " " (mapv str xs))))

(def ^{:dynamic true
       :doc "Should the Joyride output channel be revealed after `say`?
             Default: `true`"}
  *show-when-said?* false)

(defn sayln [message]
  (let [channel ^js (:output-channel @db/!app-db)]
    (.appendLine channel message)
    (when *show-when-said?*
      (.show channel true))))

(defn say [message]
  (let [channel ^js (:output-channel @db/!app-db)]
    (.append channel message)
    (when *show-when-said?*
      (.show channel true))))

(defn say-error [message]
  (sayln (str "ERROR: " message)))

(defn say-result
  ([result]
   (say-result nil result))
  ([message result]
   (let [prefix (if (empty? message)
                  "=> "
                  (str message "\n=> "))]
     (.append ^js (:output-channel @db/!app-db) prefix)
     (sayln (with-out-str (pprint/pprint result))))))

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
                (->> (cljify files)
                     (map #(vscode/Uri.file %))
                     (sort-by #(.-fsPath ^js %)))))))
