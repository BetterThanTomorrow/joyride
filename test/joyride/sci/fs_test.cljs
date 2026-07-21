(ns joyride.sci.fs-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [clojure.string]
   [joyride.sci.fs :as sci-fs]
   [sci.core :as sci]))

(defn- fs-ctx
  []
  (sci/init {:namespaces {'babashka.fs sci-fs/fs-namespace}}))

(deftest babashka-fs-available-test
  (testing "exists? works via require in isolated SCI ctx"
    (let [ctx (fs-ctx)
          result (sci/eval-string* ctx "(require '[babashka.fs :as fs]) (fs/exists? \".\")")]
      (is (true? result)))))

(deftest with-temp-dir-test
  (testing "creates file inside temp dir and cleans up without :keep"
    (let [ctx (fs-ctx)
          result (sci/eval-string*
                  ctx
                  "(require '[babashka.fs :as fs])
                   (let [!path (atom nil)]
                     (fs/with-temp-dir [d nil]
                       (let [f (fs/path d \"unit-test.txt\")]
                         (fs/create-file f)
                         (reset! !path (str f))
                         (assert (fs/exists? f))))
                     {:existed-after? (fs/exists? @!path)
                      :path @!path})")]
      (is (map? result))
      (is (false? (:existed-after? result)))
      (is (string? (:path result)))
      (is (not (clojure.string/blank? (:path result)))))))
