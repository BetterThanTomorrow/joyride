(ns integration-test.babashka-fs-test
  (:require
   [babashka.fs :as fs]
   [cljs.test :refer [deftest is testing]]))

(deftest babashka-fs-exists-test
  (is (true? (fs/exists? "."))))

(deftest babashka-fs-with-temp-dir-test
  (testing "temp file exists inside with-temp-dir and is gone after"
    (let [!path (atom nil)]
      (fs/with-temp-dir [d nil]
        (let [f (fs/path d "e2e-smoke.txt")]
          (fs/create-file f)
          (reset! !path (str f))
          (is (true? (fs/exists? f)))))
      (is (false? (fs/exists? @!path))))))
