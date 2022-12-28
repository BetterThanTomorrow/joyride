(ns workspace-activate
  (:require ["vscode" :as vscode]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def symbol-1 :symbol-1)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn fn-1 []
  :fn-1)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ws-root []
  (first vscode/workspace.workspaceFolders))