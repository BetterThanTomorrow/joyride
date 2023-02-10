(ns integration-test.rewrite-clj-test
  (:require
   [cljs.test :refer [deftest is]]
   [clojure.set]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as p]))

(deftest rewrite-clj-test
  (is (= :list (-> (p/parse-string "(+ 1 2 3)")
                   (n/tag)))))
