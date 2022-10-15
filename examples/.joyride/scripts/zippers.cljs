(ns zippers
  (:require [clojure.zip :as zip]))

;; example from: https://clojuredocs.org/clojure.zip/next#example-5ceef576e4b0ca44402ef735

(defn zip-walk [f z]
  (if (zip/end? z)
    (zip/root z)
    (recur f (zip/next (f z)))))

(comment
  (->> [1 2 [3 4]]
       zip/vector-zip
       (zip-walk
        (fn [loc]
          (if (zip/branch? loc)
            loc
            (zip/edit loc * 2))))) ; => [2 4 [6 8]]
  )