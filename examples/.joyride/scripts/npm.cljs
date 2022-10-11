(ns npm
  (:require ["moment" :as moment]))

(.format (moment) "dddd")

(require '["axios" :as axios]
         '[promesa.core :as p])

(def result (atom nil))
(->
 (axios.get "https://clojure.org")
 (p/then #(reset! result %)))

@result