(ns integration-test.npm-test
  (:require [cljs.test :refer [deftest testing is]]
            ["color-convert" :as convert]))

(deftest npm
  (testing "Can use required npm module"
    (is (= "7B2D43"
           (convert/rgb.hex 123 45 67)))))
