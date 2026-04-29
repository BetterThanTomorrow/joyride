(ns npm
  (:require ["moment" :as moment]))

(.format (moment) "dddd")

(require '["axios" :as axios])

(def result (atom nil))
((^:async fn []
   (reset! result (await (axios.get "https://clojure.org")))))

@result