(ns integration-test.require-extension-test
  (:require [cljs.test :refer [testing is]]
            [promesa.core :as p]
            ["ext://betterthantomorrow.joyride" :as ns-required-extension]
            [integration-test.macros :refer [deftest-async]]))

#_{:clj-kondo/ignore [:duplicate-require]}
(require '["ext://betterthantomorrow.joyride" :as top-level-required-extension])

(deftest-async ns-require-extension
  (testing "ns form did require the Joyride extension"
    (p/let [answer (ns-required-extension/runCode "42")]
      (is (= 42 answer)))))

(deftest-async top-level-require-extension
  (testing "Requires the Joyride extension"
    (p/let [question (top-level-required-extension/runCode "42")]
      (is (= 42 question)))))

(require '[rewrite-clj.node])