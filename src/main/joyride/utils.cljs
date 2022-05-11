(ns joyride.utils
  (:require ["vscode" :as vscode]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [joyride.db :as db]
            [promesa.core :as p]))

(defn jsify [clj-thing]
  (clj->js clj-thing))

(defn cljify [js-thing]
  (js->clj js-thing :keywordize-keys true))

(defn path-exists?+ [path]
  (-> (p/let [uri (vscode/Uri.file path)
              stat (vscode/workspace.fs.stat uri)])
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

(defn say [message]
  (let [channel ^js (:output-channel @db/!app-db)]
    (.appendLine channel message)
    (when *show-when-said?*
      (.show channel true))))

(defn say-error [message]
  (say (str "ERROR: " message)))

(defn say-result
  ([result]
   (say-result nil result))
  ([message result]
   (let [prefix (if (empty? message)
                  "=> "
                  (str message "\n=> "))]
     (.append ^js (:output-channel @db/!app-db) prefix)
     (say (with-out-str (pprint/pprint result))))))