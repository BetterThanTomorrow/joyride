(ns funcool.promesa
  (:require [clj-kondo.hooks-api :as api]))

(defn plet
  ;; this effectively rewrites the let to `(let [[bind1 bind2] [(expr-1) (expr-2)] ,,,)
  ;; making referring to other binding syms produce an error
  ;; thanks to @NoahBogart on Slack for this
  [{:keys [:node]}]
  (let [[_plet binds & body] (:children node)
        new-bind-syms (api/vector-node (take-nth 2 (:children binds)))
        new-bind-exprs (api/vector-node (take-nth 2 (rest (:children binds))))
        new-node (api/list-node
                  (list* (api/token-node 'clojure.core/let)
                         (api/vector-node [new-bind-syms new-bind-exprs])
                         body))]
    {:node new-node}))
