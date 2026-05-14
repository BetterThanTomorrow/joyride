(ns subdir.require-cljs-requiring-js-from-subdir
  (:require [subdir.require-js-from-subdir]))

(def fortytwo subdir.require-js-from-subdir/fortytwo)

(comment
  fortytwo
  :rcf)

fortytwo