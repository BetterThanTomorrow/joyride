(ns joyride.mcp.registry-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [joyride.mcp.registry :as registry]))

(deftest enabled?-test
  (testing "on by default"
    (is (registry/enabled? true))
    (is (registry/enabled? nil)))
  (testing "off only when false"
    (is (not (registry/enabled? false)))))

(deftest compact-data-test
  (testing "does not set envelope keys the writer owns"
    (let [data (registry/compact-data)]
      (is (empty? (select-keys data [:name :workspaceRoot :workspaceFolder
                                     :serverName :windowId :mcp]))))))
