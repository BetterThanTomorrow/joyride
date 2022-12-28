(ns integration-test.workspace-activate-test
  (:require [cljs.test :refer [deftest testing is]]
            ["path" :as path]))

(deftest ws-activate 
  (testing "Workspace activation script defines a symbol"
    (is (= :symbol-1
           #_{:clj-kondo/ignore [:unresolved-namespace]}
           workspace-activate/symbol-1)))
  
  (testing "Workspace activation script defines a function"
    (is (= :fn-1
           #_{:clj-kondo/ignore [:unresolved-namespace]}
           (workspace-activate/fn-1))))
  
  (testing "Workspace activation script finds workspace root"
    (is (= "workspace-1"
           #_{:clj-kondo/ignore [:unresolved-namespace]}
           (-> (workspace-activate/ws-root)
               .-uri
               .-fsPath
               path/basename)))))