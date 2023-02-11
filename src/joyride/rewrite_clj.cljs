(ns joyride.rewrite-clj
  (:require [rewrite-clj.node]
            [rewrite-clj.parser]
            [rewrite-clj.zip]
            [sci.core :as sci]
            [sci.ctx-store :as store]))

(def rzns (sci/create-ns 'rewrite-clj.zip))
(def rewrite-clj-zip-ns (sci/copy-ns rewrite-clj.zip rzns))

(def rpns (sci/create-ns 'rewrite-clj.parser))
(def rewrite-clj-parser-ns (sci/copy-ns rewrite-clj.parser rpns))

(def rnns (sci/create-ns 'rewrite-clj.node))
(def rewrite-clj-node-ns (sci/copy-ns rewrite-clj.node rnns))

(sci/add-namespace! (store/get-ctx) 'rewrite-clj.zip rewrite-clj-zip-ns)
(sci/add-namespace! (store/get-ctx) 'rewrite-clj.parser rewrite-clj-parser-ns)
(sci/add-namespace! (store/get-ctx) 'rewrite-clj.node rewrite-clj-node-ns)
