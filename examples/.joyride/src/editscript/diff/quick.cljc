;;
;; Copyright (c) Huahai Yang. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns ^:no-doc editscript.diff.quick
  (:require [clojure.set :as set]
            [editscript.edit :as e]
            [editscript.util.common :as c
             #?@(:cljs [:include-macros true])]))

#?(:clj (set! *warn-on-reflection* true))
#?(:clj (set! *unchecked-math* :warn-on-boxed))
#?(:cljr (set! *warn-on-reflection* true))

(declare diff*)

(defn- diff-map
  [script path a b opts]
  (reduce-kv
    (fn [_ ka va]
      (let [path' (conj path ka)]
        (if (contains? b ka)
          (diff* script path' va (get b ka) opts)
          (diff* script path' va (e/nada) opts))))
    nil
    a)
  (reduce-kv
    (fn [_ kb vb]
      (when-not (contains? a kb)
        (diff* script (conj path kb) (e/nada) vb opts)))
    nil
    b))

(defn- diff-vec
  "Adjust the indices to have a correct editscript"
  [script path a b opts]
  (let [edits (c/vec-edits a b opts)]
    (if (= edits :timeout)
      (e/replace-data script path b)
      (reduce
        (fn [[^long ia ^long ia' ^long ib] op]
          (case op
            :- (do (diff* script (conj path ia') (get a ia) (e/nada) opts)
                   [(inc ia) ia' ib])
            :+ (do (diff* script (conj path ia') (e/nada) (get b ib) opts)
                   [ia (inc ia') (inc ib)])
            :r (do (diff* script (conj path ia') (get a ia) (get b ib) opts)
                   [(inc ia) (inc ia') (inc ib)])
            [(+ ia ^long op) (+ ia' ^long op) (+ ib ^long op)]))
        (transient [0 0 0])
        edits))))

(defn- diff-set
  [script path a b opts]
  (doseq [va (set/difference a b)]
    (diff* script (conj path va) va (e/nada) opts))
  (doseq [vb (set/difference b a)]
    (diff* script (conj path vb) (e/nada) vb opts)))

(defn- diff-lst
  [script path a b opts]
  (diff-vec script path (vec a) (vec b) opts))

(defn- diff-val
  [script path _ b]
  (if (= (e/get-type b) :nil)
    (e/delete-data script path)
    (e/replace-data script path b)))

(defn diff*
  [script path a b {:keys [str-diff]
                    :or   {str-diff :none}
                    :as   opts}]
  (when-not (= a b)
    (case (e/get-type a)
      :nil (e/add-data script path b)
      :map (c/coll-case a b script path :map #'diff-map opts)
      :vec (c/coll-case a b script path :vec #'diff-vec opts)
      :set (c/coll-case a b script path :set #'diff-set opts)
      :lst (c/coll-case a b script path :lst #'diff-lst opts)
      :str (if (= str-diff :none)
             (diff-val script path a b)
             (c/coll-case a b script path :str
                          #'editscript.util.common/diff-str opts))
      :val (diff-val script path a b))))

(defn diff
  "Create an EditScript that represents the difference between `b` and `a`
  This algorithm is fast, but it does not attempt to generate an EditScript
  that is minimal in size"
  ([a b]
   (diff a b nil))
  ([a b opts]
   (let [script (e/edits->script [])]
     (diff* script [] a b opts)
     script)))
