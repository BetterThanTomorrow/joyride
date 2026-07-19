;;
;; Copyright (c) Huahai Yang. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns ^:no-doc editscript.diff.a-star
  (:require [editscript.edit :as e]
            [editscript.util.pairing :as pa]
            [editscript.util.index :as i]
            [editscript.util.common :as co]
            #?(:cljs [goog.math.Long :refer [getMaxValue]]))
  #?(:clj (:import [clojure.lang Keyword]
                   [java.io Writer]
                   [java.lang Comparable]
                   [editscript.util.index Node])
	 :cljr  (:import [clojure.lang Keyword]
                   [editscript.util.index Node])))

#?(:clj (set! *warn-on-reflection* true))
#?(:cljr (set! *warn-on-reflection* true))
#?(:clj (set! *unchecked-math* :warn-on-boxed))

;; diffing

(defn- coord-hash [a b] (co/szudzik (i/get-order a) (i/get-order b)))

#?(:clj
   (deftype Coord [^Node a
                   ^Node b]
     ;; Java's native hash is too slow,
     ;; overriding hashCode significantly speeds things up
     Object
     #?@(:clj [(hashCode [_] (coord-hash a b))])
     (equals [_ that]
       (and (= (i/get-order a) (i/get-order (.-a ^Coord that)))
            (= (i/get-order b) (i/get-order (.-b ^Coord that)))))
     (toString [_]
       (str "[" (i/get-value a) "," (i/get-value b) "]"))

     #?@(:bb []
         :clj [Comparable
               (compareTo [this that]
                          (- (.hashCode this) (.hashCode that)))]))
   :cljr
   (deftype Coord [^Node a
                   ^Node b]
     ;; Java's native hash is too slow,
     ;; overriding hashCode significantly speeds things up
     Object
     (GetHashCode [_] (coord-hash a b))
     (Equals  [_ that]
       (and (= (i/get-order a) (i/get-order (.-a ^Coord that)))
            (= (i/get-order b) (i/get-order (.-b ^Coord that)))))
     (ToString [_]
       (str "[" (i/get-value a) "," (i/get-value b) "]"))

     IComparable
     (CompareTo [this that]
       (- (.GetHashCode this) (.GetHashCode that))))

   :cljs
   (deftype Coord [^Node a
                   ^Node b]
     IHash
     (-hash [_] (coord-hash a b))

     IEquiv
     (-equiv [_ that]
       (and (= (i/get-order a) (i/get-order (.-a ^Coord that)))
            (= (i/get-order b) (i/get-order (.-b ^Coord that)))))

     IComparable
     (-compare [this that]
       (- (-hash this) (-hash that)))))

(defn- get-coord [^Coord coord] [(.-a coord) (.-b coord)])

(defprotocol IStep
  (operator [this] "Operator to try")
  (current [this] "Starting pair of nodes")
  (neighbor [this] "Destination pair of nodes"))

(deftype Step [^Keyword op
               ^Coord cur
               ^Coord nbr]
  IStep
  (operator [_] op)
  (current [_] cur)
  (neighbor [_] nbr))

#?(:clj
   (defmethod print-method Step
     [x ^Writer writer]
     (print-method {:op  (operator x)
                    :cur (current x)
                    :nbr (neighbor x)}
                   writer)))

(defn- get-step
  [step]
  ((juxt operator current neighbor) step))

(defprotocol IState
  (get-came [this] "Get the local succession map")
  (set-came [this came] "Set the local succession map")
  (get-open [this] "Get the open priority queue")
  (set-open [this open] "Set the open priority queue")
  (get-g [this] "Get the g cost map")
  (set-g [this g] "Set the g cost map"))

(deftype State [^:unsynchronized-mutable came
                ^:unsynchronized-mutable open
                ^:unsynchronized-mutable g]
  IState
  (get-came [_] came)
  (set-came [this c] (set! came c) this)
  (get-open [_] open)
  (set-open [this o] (set! open o) this)
  (get-g [_] g)
  (set-g [this got] (set! g got) this))

(defn- get-state
  [state]
  ((juxt get-came get-open get-g) state))

(defn- access-g
  [g cur]
  (get g cur #?(:clj Long/MAX_VALUE
                :cljr Int64/MaxValue
                :cljs (getMaxValue))))

(declare diff*)

(defn- compute-cost
  [^Coord cur came g op opts]
  (let [^long gc (access-g g cur)]
    (case op
      :=      gc
      ;; delete only cost 1, for not including deleted data in script
      :-      (inc gc)
      ;; these cost the size of included data, plus 1
      (:a :i) (let [sb (i/get-size (.-b cur))]
                (+ gc (inc ^long sb)))
      :r      (+ gc ^long (diff* (.-a cur) (.-b cur) came opts)))))

(defn- heuristic
  "A simplistic but optimistic estimate of the cost to reach goal when at (x y).

  For nested structure, multiple deletion may be merged into one.
  Also, because addition/replacement requires new value to be present in
  editscript, whereas deletion does not, we assign estimate differently. "
  ^long [type cur end [gx gy]]
  (case type
    (:map :set) 0
    (:vec :lst) (let [[na nb] (get-coord cur)
                      [ra rb] (get-coord end)
                      x       (if (identical? ra na) gx (i/get-order na))
                      y       (if (identical? rb nb) gy (i/get-order nb))
                      dy      (- ^long gy ^long y)
                      dx      (- ^long gx ^long x)]
                  (cond
                    (== dx 0) dy
                    (== dy 0) 1
                    (> dx dy) 3
                    (< dx dy) (- dy dx)
                    :else     2))))

(defn- explore
  [type end came goal state step opts]
  (let [[came' open g] (get-state state)
        [op cur nbr]   (get-step step)
        tmp-g          (compute-cost cur came g op opts)]
    (if (>= ^long tmp-g ^long (access-g g nbr))
      state
      (doto state
        (set-came (assoc! came' nbr [cur op]))
        (set-open (assoc open nbr
                         (+ ^long tmp-g ^long (heuristic type nbr end goal))))
        (set-g (assoc! g nbr tmp-g))))))

(defn- next-node
  [na ra]
  (or (i/get-next na) ra))

(defn- vec-frontier
  [end cur]
  (let [[ra rb] (get-coord end)
        [na nb] (get-coord cur)
        a=b     (= (i/get-value na) (i/get-value nb))
        x=gx    (identical? na ra)
        x<gx    (not x=gx)
        y<gy    (not (identical? nb rb))
        na'     (next-node na ra)
        nb'     (next-node nb rb)]
    (if (and x<gx y<gy a=b)
      [(->Step := cur (->Coord na' nb'))]
      (cond-> []
        x<gx            (conj (->Step :- cur (->Coord na' nb)))  ; delete
        (and x<gx y<gy) (conj (->Step :r cur (->Coord na' nb'))) ; replace
        (and x=gx y<gy) (conj (->Step :a cur (->Coord na nb')))  ; append
        (and x<gx y<gy) (conj (->Step :i cur (->Coord na nb'))))))) ; insert

(defn- map-frontier
  [^Coord init end cur]
  (let [[ra rb] (get-coord end)
        [na nb] (get-coord cur)
        ka      (i/get-key na)
        kb      (i/get-key nb)]
    (if (identical? na ra)
      ;; testing keys of b
      [(->Step (if (contains? (i/get-value ra) kb) := :a)
               cur (->Coord ra (next-node nb rb)))]
      (let [va  (i/get-value na)
            vb  (i/get-value nb)
            mb  (i/get-value rb)
            na' (next-node na ra)
            cb  (i/get-children rb)]
        (if (identical? na' ra)
          ;; transition point from testing keys of a to that of b
          (let [startb (->Coord ra (.-b init))
                enda   (->Coord na (cb ka))]
            (if (contains? mb ka)
              (if (= ka kb)
                [(->Step (if (= va vb) := :r) cur startb)]
                [(->Step := cur enda)
                 (->Step :r enda startb)])
              [(->Step :- cur startb)]))
          ;; testing keys of a
          [(if (contains? mb ka)
             (if (= ka kb)
               (->Step (if (= va vb) := :r)
                       cur (->Coord na' (or (cb (i/get-key na')) nb)))
               (->Step := cur (->Coord na (cb ka))))
             (->Step :- cur (->Coord na' nb)))])))))

(defn- frontier
  [type init end cur]
  (case type
    (:vec :lst) (vec-frontier end cur)
    (:map :set) (map-frontier init end cur)))

(defn- A*
  [type ra rb came opts]
  (let [end  (->Coord ra rb)
        init (->Coord (i/get-first ra) (i/get-first rb))
        goal [(i/get-order ra) (i/get-order rb)]]
    (loop [state (->State (transient {})
                          (pa/priority-map init (heuristic type init end goal))
                          (transient {init 0}))]
      (let [[came' open _] (get-state state)]
        (if (empty? open)
          (throw (ex-info "A* diff fails to find a solution" {:ra ra :rb rb}))
          (let [[cur cost] (peek open)]
            (if (= cur end)
              (do (vswap! came assoc end (persistent! came'))
                  cost)
              (recur (reduce
                       #(explore type end came goal %1 %2 opts)
                       (set-open state (pop open))
                       (frontier type init end cur))))))))))

(defn- vec-fn
  [node]
  (let [v (i/get-value node)]
    (if (= :vec (e/get-type v))
      v
      (vec v))))

(defn- use-quick
  [ra rb came opts]
  (let [edits (co/vec-edits (vec-fn ra) (vec-fn rb) opts)]
    (if (= edits :timeout)
      edits
      (loop [[op & ops] edits
             na         (i/get-first ra)
             nb         (i/get-first rb)
             m          (transient {})
             cost       0]
        (if op
          (let [na' (next-node na ra)
                nb' (next-node nb rb)
                cur (->Coord na nb)
                sb  (i/get-size nb)]
            (if (integer? op)
              (recur (if (> ^long op 1) `[~(dec ^long op) ~@ops] ops)
                     na' nb'
                     (assoc! m (->Coord na' nb') [cur :=])
                     (long cost))
              (case op
                :- (recur ops na' nb
                          (assoc! m (->Coord na' nb) [cur op])
                          (inc (long cost)))
                :+ (recur ops na nb'
                          (assoc! m (->Coord na nb')
                                  [cur (if (identical? na ra) :a :i)])
                          (+ (long cost) 1 (long sb)))
                :r (recur ops na' nb'
                          (assoc! m (->Coord na' nb') [cur op])
                          (+ (long cost) 1 (long sb))))))
          (let [root (->Coord ra rb)]
            (vswap! came assoc root (persistent! m))
            cost))))))

(defn- diff*
  ^long [ra rb came opts]
  (let [sa     ^long (i/get-size ra)
        sb     ^long (i/get-size rb)
        va     (i/get-value ra)
        vb     (i/get-value rb)
        typea  (e/get-type va)
        update #(vswap! came assoc (->Coord ra rb) {})]
    (cond
      ;; both are leaves, skip or replace
      (= 1 sa sb)
      (do (update)
          (if (= va vb)
            0
            2))
      ;; one of them is leaf, replace
      (or (= 1 sa) (= 1 sb))
      (do (update)
          (inc ^long sb))
      ;; non-empty coll with same type, drill down
      (= typea (e/get-type vb))
      (if (= va vb)
        (do (update) 0)
        (let [r (inc ^long sb)
              a (if (and (#{:vec :lst} typea)
                         (let [cc+1 #(-> % i/get-children count inc)]
                           (or (= sa (cc+1 ra)) (= sb (cc+1 rb)))))
                  ;; vec or lst contains leaves only, safe to use quick algo.
                  (let [res (use-quick ra rb came opts)]
                    (if (= res :timeout) (inc r) res))
                  ;; otherwise run A*
                  (A* typea ra rb came opts))]
          (if (< r ^long a)
            (do (update) r)
            a)))
      ;; types differ, can only replace
      :else
      (do (update)
          (inc ^long sb)))))

;; generating editscript

(defn- index-key?
  [node]
  (#{:vec :lst} (-> node i/get-value e/get-type)))

(defn- adjust-delete-insert
  [trie op root path]
  (if (= op :=)
    path
    (loop [newp []
           prev []
           node root
           left path]
      (if (seq left)
        (let [[k & ks] left
              ^long d  (get-in @trie (conj prev :delta) 0)]
          (recur (conj newp (if (index-key? node) (+ ^long k d) k))
                 (conj prev k)
                 ((i/get-children node) k)
                 ks))
        (if (index-key? (i/get-parent node))
          (let [seen    (conj (if (seq path) (pop path) path) :delta)
                ^long d (get-in @trie seen 0)]
            (vswap! trie assoc-in seen (case op :- (dec d) :i (inc d) d))
            newp)
          newp)))))

(defn- adjust-append
  [trie op na nb path path']
  (if (= op :a)
    (if (index-key? na)
      (conj path' (let [seen    (conj path :delta)
                        ^long d (get-in @trie seen 0)]
                    (vswap! trie assoc-in seen (inc d))
                    (+ d (-> na i/get-children count))))
      (conj path' (i/get-key nb)))
    path'))

(defn- convert-path
  [trie op roota na nb path]
  (->> path
       (adjust-delete-insert trie op roota)
       (adjust-append trie op na nb path)))

(defn- write-script
  [steps roota script {:keys [str-diff]
                       :or   {str-diff :none}
                       :as   opts}]
  (reduce
    (fn [trie [op na nb]]
      (let [path (convert-path trie op roota na nb (i/get-path na))
            va   (i/get-value na)
            vb   (i/get-value nb)]
        (case op
          :-      (e/delete-data script path)
          :r      (if (and (= :str (e/get-type va) (e/get-type vb))
                           (not= str-diff :none))
                    (co/diff-str script path va vb opts)
                    (e/replace-data script path vb))
          (:a :i) (e/add-data script path vb)
          nil)
        trie))
    (volatile! {:delta 0})
    steps))

(defn- trace*
  [came cur steps]
  (if-let [m (came cur)]
    (if (seq m)
      (loop [v (m cur)]
        (if v
          (let [[prev op] v
                [na nb]   (get-coord prev)]
            (if (and (came prev) (= op :r))
              (trace* came prev steps)
              (vswap! steps conj [op na nb]))
            (recur (m prev)))
          steps))
      (let [[ra rb] (get-coord cur)]
        (vswap! steps conj [(if (= (i/get-value ra) (i/get-value rb)) := :r)
                            ra rb])
        steps))
    steps))

(defn- trace
  ([came cur]
   @(trace* came cur (volatile! '())))
  ([came ^Coord cur script opts]
   (-> (trace came cur)
       (write-script (.-a cur) script opts))))

(defn diff
  "Create an EditScript that represents the minimal difference between `b` and `a`"
  ([a b]
   (diff a b nil))
  ([a b opts]
   (let [script (e/edits->script [])]
     (when-not (= a b)
       (let [roota (i/index a)
             rootb (i/index b)
             came  (volatile! {})
             cost  (diff* roota rootb came opts)]
         ;; #?(:clj (let [total          (* (get-size roota) (get-size rootb))
         ;;               ^long explored (reduce + (map count (vals @came)))]
         ;;           (printf "cost is %d, explored %d of %d - %.1f%%\n"
         ;;                   cost explored total
         ;;                   (* 100 (double (/ explored total))))))
         (trace @came (->Coord roota rootb) script opts)
         script))
     script)))
