(ns integration-test.db
  (:require [cljs.test]))

(def !state (atom {:running nil
                   :ws-activated? false
                   :fail 0
                   :error 0}))

