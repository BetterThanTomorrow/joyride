(ns require-cljs-requiring-js-script
  (:require [require-js]))

(defn hello []
  (require-js/hello))

(def fortytwo require-js/fortytwo)

(comment
  (hello)
  fortytwo
  :rcf)

fortytwo