(ns require-js-script
  (:require ["../src/js-file.js" :as js-file]))

(defn hello []
  (js-file/hello))

(def fortytwo js-file/fortytwo)

(comment
  (hello)
  fortytwo
  :rcf)

fortytwo