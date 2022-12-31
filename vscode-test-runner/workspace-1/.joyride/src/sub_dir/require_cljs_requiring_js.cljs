(ns sub-dir.require-cljs-requiring-js
  (:require [sub-dir.require-js]))

(def fortytwo sub-dir.require-js/fortytwo)

(comment
  fortytwo
  :rcf)

fortytwo