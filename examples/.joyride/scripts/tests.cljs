(ns tests 
  (:require [clojure.test :as t :refer [deftest is]]))

(deftest foo
  (is (= 4 5)))

(t/run-tests 'tests)
