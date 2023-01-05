(ns integration-test.user-activate-test
  (:require [cljs.test :refer [deftest testing is]]
            ["path" :as path]))

(deftest user-activate 
  (testing "User activation script is required"
    (is #_{:clj-kondo/ignore [:unresolved-namespace]}
     (= #'user-activate/!db
        ((ns-publics 'user-activate) '!db))))
  
  (testing "my-lib is required"
    (is (seq
         (ns-publics 'my-lib)))))