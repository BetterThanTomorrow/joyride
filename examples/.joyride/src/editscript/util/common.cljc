;;
;; Copyright (c) Huahai Yang. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns ^:no-doc editscript.util.common
  (:refer-clojure :exclude [slurp])
  (:require [editscript.edit :as e]
            [clojure.string :as s]))

#?(:clj (set! *warn-on-reflection* true))
#?(:clj (set! *unchecked-math* :warn-on-boxed))

(defn szudzik
  "Szudzik's paring function"
  [^long x ^long y]
  (if (> y x)
    (+ x (* y y))
    (+ x y (* x x))))

(defmacro coll-case
  [a b script path type diff-fn opts]
  `(case (e/get-type ~b)
     :nil  (e/delete-data ~script ~path)
     ~type (~diff-fn ~script ~path ~a ~b ~opts)
     (e/replace-data ~script ~path ~b)))

(defn current-time
  ^long []
  #?(:clj (System/currentTimeMillis) 
     :cljr (.ToUnixTimeMilliseconds (DateTimeOffset/Now))
  :cljs (.getTime (js/Date.))))

(defn- vec-edits*
  "Based on 'Wu, S. et al., 1990, An O(NP) Sequence Comparison Algorithm,
  Information Processing Letters, 35:6, p317-23.'

  A greedy algorithm, attempting to get to the furthest points with a given
  number of edits. Very fast. However, it does not have replacement operations,
  so it is not very useful for nested trees. It can also only do unit cost for
  addition and deletion. "
  [a b n m timeout]
  (let [^long n n
        ^long m m
        delta   (- n m)
        snake   (fn [^long k ^long x]
                  (loop [x x y (- x k)]
                    (let [ax (get a x) by (get b y)]
                      (if (and (< x n)
                               (< y m)
                               (= (type ax) (type by))
                               (= ax by))
                        (recur (inc x) (inc y))
                        x))))
        fp-fn   (fn [fp ^long k]
                  (let [[dk-1 vk-1] (get fp (dec k) [-1 []])
                        dk-1        (inc ^long dk-1)
                        [dk+1 vk+1] (get fp (inc k) [-1 []])
                        x           (max dk-1 ^long dk+1)
                        ^long sk    (snake k x)
                        ops         (let [es (if (> dk-1 ^long dk+1)
                                               (conj vk-1 :-)
                                               (conj vk+1 :+))]
                                      (if (> sk x)
                                        (conj es (- sk x))
                                        es))]
                    (assoc! fp k [sk ops])))
        begin   (current-time)]
    (loop [p 0 fp (transient {})]
      (let [fp (loop [k (* -1 p) fp fp]
                 (if (< k delta)
                   (recur (inc k) (fp-fn fp k))
                   fp))
            fp (loop [k (+ delta p) fp fp]
                 (if (< delta k)
                   (recur (dec k) (fp-fn fp k))
                   fp))
            fp (fp-fn fp delta)]
        (cond
          (and timeout (< ^long timeout (- (current-time) begin)))
          :timeout
          (= n (nth (get fp delta) 0))
          (-> (persistent! fp) (get delta) (#(nth % 1)) rest)
          :else
          (recur (inc p) fp))))))

(defn- swap-ops
  [edits]
  (mapv (fn [op] (case op :+ :- :- :+ op)) edits))

(defn min+plus->replace
  "Aggressively turn :- and :+ into replacements."
  [v]
  (into []
        (comp
          (partition-by integer?)
          (mapcat
            (fn [coll]
              (let [m (nth coll 0)]
                (if (or (integer? m) (= 1 (count coll)))
                  coll
                  (let [p       (if (= m :-) :+ :-)
                        [ms ps] (split-with #(= % m) coll)
                        mc      (count ms)
                        pc      (count ps)
                        delta   (#?(:cljr Math/Abs :default Math/abs) (- mc pc))
                        rs      (repeat (- (max mc pc) delta) :r)]
                    (cond
                      (< mc pc) (concat rs (repeat delta p))
                      (= mc pc) rs
                      :else     (concat (repeat delta m) rs))))))))
        v))

(defn vec-edits
  [a b {:keys [vec-timeout]
        :or   {vec-timeout 1000}}]
  (let [a (vec a)
        b (vec b)
        n (count a)
        m (count b)
        e (if (< n m)
            (vec-edits* b a m n vec-timeout)
            (vec-edits* a b n m vec-timeout))]
    (if (= e :timeout)
      e
      (min+plus->replace (if (< n m) (swap-ops e) e)))))

(defn- group-strs
  [edits b level]
  (let [sf (if (= level :character) subs subvec)
        i  (volatile! 0)]
    (into []
          (comp
            (partition-by identity)
            (mapcat
              (fn [coll]
                (let [x (nth coll 0)
                      c (count coll)]
                  (cond
                    (integer? x) (do (vswap! i (partial + x)) coll)
                    (= :- x)     [[x c]]
                    (= :r x)     (let [s (sf b @i (+ ^long @i c))]
                                   (vswap! i (partial + c))
                                   [[x s]])
                    (= :+ x)     (let [s (sf b @i (+ ^long @i c))]
                                   (vswap! i (partial + c))
                                   [[x s]]))))))
          edits)))

(defn transform-str
  [s level]
  (case level
    :character s
    :word      (vec (s/split s #" "))
    :line      (vec (s/split-lines s))
    (throw (ex-info "Unknown string diff level" {:str-diff level}))))

(defn diff-str
  [script path a b {:keys [str-change-limit str-diff]
                    :or   {str-change-limit 0.2}
                    :as   opts}]
  (let [a'    (transform-str a str-diff)
        b'    (transform-str b str-diff)
        edits (vec-edits a' b' opts)]
    (if (= edits :timeout)
      (e/replace-data script path b)
      (let [ca        (count a')
            unchanged (double (transduce (filter integer?) + edits))]
        (if (and (< 0 str-change-limit 1.0)
                 (< (* ca (- 1.0 ^double str-change-limit)) unchanged))
          (let [edits' (group-strs edits b' str-diff)]
            (e/replace-str script path edits' str-diff))
          (e/replace-data script path b'))))))

#?(:clj (defmacro vslurp
          [file]
          (clojure.core/slurp file)))
