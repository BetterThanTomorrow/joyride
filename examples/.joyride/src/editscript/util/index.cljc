;;
;; Copyright (c) Huahai Yang. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns editscript.util.index
  (:require [editscript.edit :as e]
            #?(:cljs [goog.math.Long :refer [getMaxValue]]))
  #?(:clj (:import [clojure.lang PersistentVector]
                   [java.io Writer])
				   :cljr (:import [clojure.lang PersistentVector])) )

;; indexing

(defprotocol INode
  (get-path [this] "Get the path to the node from root")
  (get-value [this] "Get the actual data")
  (get-children [this] "Get all children node in a map")
  (add-child [this node] "Add a child node")
  (get-key [this] "Get the key of this node")
  (get-parent [this] "Get the parent node")
  (get-first [this] "Get the first child node")
  (get-last [this] "Get the last child node")
  (get-next [this] "Get the next sibling node")
  (set-next [this node] "Set the next sibling node")
  (set-order [this o] "Set the traversal order of this node")
  (get-order [this] "Get the order of this node in traversal")
  (get-size [this] "Get the size of sub-tree, used to estimate cost")
  (set-size [this s] "Set the size of sub-tree"))

(deftype Node [^PersistentVector path
               value
               parent
               ^:unsynchronized-mutable children
               ^:unsynchronized-mutable first
               ^:unsynchronized-mutable last
               ^:unsynchronized-mutable next
               ^:unsynchronized-mutable index
               ^:unsynchronized-mutable ^long order
               ^:unsynchronized-mutable ^long size]
  INode
  (get-path [_] path)
  (get-key [this] (-> this get-path peek))
  (get-value [_] value)
  (get-parent [_] parent)
  (get-children [_] children)
  (get-first [_] first)
  (get-last [_] last)
  (get-next [_] next)
  (set-next [_ n] (set! next n))
  (get-order [_] order)
  (set-order [this o] (set! order (long o)) this)
  (get-size [_] size)
  (set-size [this s] (set! size (long s)) this)
  (add-child [_ node]
    (set! children (assoc children (get-key node) node))
    (when last (set-next last node))
    (when-not first (set! first node))
    (set! last node)
    node))

#?(:clj
   (defmethod print-method Node
     [x ^Writer writer]
     (print-method {:value    (get-value x)
                    :order    (get-order x)
                    :children (get-children x)}
                   writer)))

(declare index*)

(defn- associative-children
  "map and vector are associative"
  [order path data parent]
  (reduce-kv
    (fn [_ k v]
      (index* order (conj path k) v parent))
    nil
    data))

(defn- set-children
  "set is a map of keys to themselves"
  [order path data parent]
  (doseq [x data]
    (index* order (conj path x) x parent)))

(defn- list-children
  "add index as key"
  [order path data parent]
  (reduce
    (fn [i x]
      (index* order (conj path i) x parent)
      (inc ^long i))
    0
    data))

(defn- inc-order
  "order value reflects the size of elements"
  [order ^long size]
  (vswap! order (fn [o] (+ size ^long o))))

(defn- index-collection
  [type order path data parent]
  (let [node (->Node path data parent {} nil nil nil 0 0 1)]
    (add-child parent node)
    (case type
      (:map :vec) (associative-children order path data node)
      :set        (set-children order path data node)
      :lst        (list-children order path data node))
    (let [^long cs (->> (get-children node) vals (map get-size) (reduce +))
          size     (+ (long (get-size node)) cs)]
      (doto node
        (set-order @order)
        (set-size size))
      (inc-order order size))
    node))

(defn- index-value
  [order path data parent]
  (let [node (->Node path data parent nil nil nil nil 0 @order 1)]
    (add-child parent node)
    (inc-order order 1)
    node))

(defn- index*
  [order path data parent]
  (let [type (e/get-type data)]
    (if (or (= type :val) (= type :str))
      (index-value order path data parent)
      (index-collection type order path data parent))))

(defn index
  "Traverse data to build an indexing tree of Nodes,
  compute path, sizes of sub-trees, siblings, etc. for each Node.
  This takes little time"
  [data]
  (let [order (volatile! 0)]
    (index* order [] data (->Node [] ::dummy nil {} nil nil nil 0 -1 0))))
