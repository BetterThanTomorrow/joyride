(ns workspace-activate
  (:require [integration-test.db :as db]
            ["vscode" :as vscode]
            [promesa.core :as p]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def symbol-1 :symbol-1)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn fn-1 []
  :fn-1)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ws-root []
  (first vscode/workspace.workspaceFolders))

;; Wait for the waiter promise to be created and then immediately resolve it
(defn- activated! []
  (if-let [waiter (:ws-activate-waiter @db/!state)]
    (do
      (println "Runner: workspace-activate script done, resolving waiter promise")
      (p/resolve! waiter))
    (do
      (println "Runner: workspace-activate script done, but no waiter promise created yet")
      (js/setTimeout activated! 10))))

(activated!)