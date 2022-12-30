(ns integration-test.db
  (:require [cljs.test]
            [promesa.core :as p]))

(def !state (atom {:running nil
                   :ws-activate-waiter nil
                   :fail 0
                   :error 0}))

