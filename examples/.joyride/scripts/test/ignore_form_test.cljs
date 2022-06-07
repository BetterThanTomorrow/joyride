(ns test.ignore-form-test
  (:require ["vscode" :as vsode]
            [promesa.core :as p]
            [ignore-form :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest foo
  (testing "Equality"
    (is (= "1" 1))))
