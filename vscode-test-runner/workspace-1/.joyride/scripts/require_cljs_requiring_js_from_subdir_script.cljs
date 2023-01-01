(ns require-cljs-requiring-js-from-subdir-script
  (:require [subdir.require-js-from-subdir]))

(defn hello []
  (subdir.require-js-from-subdir/hello))

(def fortytwo subdir.require-js-from-subdir/fortytwo)

(comment
  (hello)
  fortytwo
  :rcf)

fortytwo