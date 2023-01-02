(ns require-subdir-cljs-requiring-js
  (:require [subdir.require-js-from-subdir]))

(def fortytwo subdir.require-js-from-subdir/fortytwo)

(comment
  fortytwo
  :rcf)

fortytwo