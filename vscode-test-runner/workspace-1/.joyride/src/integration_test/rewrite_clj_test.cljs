(ns integration-test.rewrite-clj-test
  (:require
   [cljs.test :refer [deftest is]]
   [clojure.set]
   #_[rewrite-clj.node :as n]
   #_[rewrite-clj.parser :as p]))

(deftest rewrite-clj-test
  #_(is (= :list (-> (p/parse-string "(+ 1 2 3)")
                   (n/tag)))))
