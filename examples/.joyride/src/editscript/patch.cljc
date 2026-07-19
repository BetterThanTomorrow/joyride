;;
;; Copyright (c) Huahai Yang. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns ^:no-doc editscript.patch
  (:require [editscript.edit :as e]
            [editscript.util.common :as c]
            [clojure.string :as s]))

#?(:clj (set! *warn-on-reflection* true))
#?(:cljr (set! *warn-on-reflection* true))
#?(:clj (set! *unchecked-math* :warn-on-boxed))

(defn vget
  [x p]
  (case (e/get-type x)
    (:map :vec :set) (get x p)
    :lst             (nth x p)))

(defn- vdelete
  [x p]
  (case (e/get-type x)
    ;;NB, there is a special case where dissoc has no effect:
    ;;if p is ##NaN, then p cannot be found in x, for (= ##NaN ##NaN) is false!
    :map (dissoc x p)
    :vec (into (subvec x 0 p) (subvec x (inc ^long p)))
    :set (disj x p)
    :lst (->> (split-at p x)
              (#(concat (nth % 0) (next (nth % 1))))
              (apply list))))

(defn- vadd
  [x p v]
  (case (e/get-type x)
    :map (assoc x p v)
    :vec (if (= p (count x))
           (conj x v)
           (into (conj (subvec x 0 p) v) (subvec x p)))
    :set (conj x v)
    :lst (->> (split-at p x)
              (#(concat (nth % 0) (conj (nth % 1) v)))
              (apply list))))

(defn- sreplace
  [x edits level]
  (let [x  (c/transform-str x level)
        sf (if (= level :character) subs subvec)
        i  (volatile! 0)
        ss (persistent!
             (reduce
               (fn [ss e]
                 (cond
                   (integer? e)     (let [s (sf x @i (+ ^long @i ^long e))]
                                      (vswap! i (partial + e))
                                      (conj! ss s))
                   (= (nth e 0) :-) (do (vswap! i (partial + (nth e 1))) ss)
                   (= (nth e 0) :r) (let [s (nth e 1)]
                                      (vswap! i (partial + (count s)))
                                      (conj! ss s))
                   (= (nth e 0) :+) (conj! ss (nth e 1))))
               (transient [])
               edits))]
    (case level
      :character (apply str ss)
      :word      (s/join " " (flatten ss))
      :line      (s/join "\n" (flatten ss)))))

(defn- vreplace
  [x p v]
  (case (e/get-type x)
    :map (assoc x p v)
    :vec (assoc x p v)
    :set (-> x (disj p) (conj v))
    :lst (->> (split-at p x)
              (#(concat (nth % 0) (conj (rest (nth % 1)) v)))
              (apply list))))

(defn- valter
  [x p o v]
  (case o
    :-  (vdelete x p)
    :+  (vadd x p v)
    :r  (vreplace x p v)
    :s  (vreplace x p (sreplace (vget x p) v :character))
    :sw (vreplace x p (sreplace (vget x p) v :word))
    :sl (vreplace x p (sreplace (vget x p) v :line))))

(defn patch*
  [old [path op value]]
  (letfn [(up [x p o v]
            (let [[f & r] p]
              (if r
                (valter x f :r (up (vget x f) r o v))
                (if (seq p)
                  (valter x f o v)
                  (case o
                    :s  (sreplace x v :character)
                    :sw (sreplace x v :word)
                    :sl (sreplace x v :line)
                    v)))))]
    (up old path op value)))
