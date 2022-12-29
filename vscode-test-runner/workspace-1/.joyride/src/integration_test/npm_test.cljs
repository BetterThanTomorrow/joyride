(ns integration-test.npm-test
  (:require [cljs.test :refer [deftest testing is]]
            ["random-animal-name" :as random-animal-name]))


(deftest npm
  (testing "Yields a random animal name"
    (is (not= "Hungry @pappapez"
              (random-animal-name)))))



