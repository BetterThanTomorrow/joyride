(ns joyride.lm-tool.core-test
  "Tests for LM tool core functionality"
  (:require [cljs.test :refer [deftest is testing]]
            [joyride.lm-tool.core :as core]))

(deftest test-format-confirmation-message
  (testing "Confirmation message formatting"
    (let [code "(+ 1 2 3)"
          namespace "user"
          result (core/format-confirmation-message code namespace)]
      (is (string? result))
      (is (clojure.string/includes? result "Execute the following ClojureScript"))
      (is (clojure.string/includes? result code))
      (is (clojure.string/includes? result namespace)))))

(deftest test-format-result-message
  (testing "Result message formatting"
    (let [result "6"
          formatted (core/format-result-message result)]
      (is (string? formatted))
      (is (clojure.string/includes? formatted "Evaluation result"))
      (is (clojure.string/includes? formatted result)))))

(deftest test-format-error-message
  (testing "Error message formatting"
    (let [error "Could not resolve symbol: unknown-fn"
          code "(unknown-fn 1)"
          formatted (core/format-error-message error code)]
      (is (string? formatted))
      (is (clojure.string/includes? formatted "Error executing ClojureScript"))
      (is (clojure.string/includes? formatted error))
      (is (clojure.string/includes? formatted code)))))

(deftest test-validate-input
  (testing "Input validation"
    (is (:valid? (core/validate-input {:code "(+ 1 2)" :namespace "user"})))
    (is (not (:valid? (core/validate-input {:code nil :namespace "user"}))))
    (is (not (:valid? (core/validate-input {:code "" :namespace "user"}))))
    (is (not (:valid? (core/validate-input {:code "(+ 1 2)" :namespace nil}))))))

(deftest test-extract-input-data
  (testing "Input data extraction"
    (let [input #js {:code "(+ 1 2)" :namespace "test"}
          result (core/extract-input-data input)]
      (is (= "(+ 1 2)" (:code result)))
      (is (= "test" (:namespace result))))

    (let [input #js {:code "(+ 1 2)"}  ; No namespace
          result (core/extract-input-data input)]
      (is (= "(+ 1 2)" (:code result)))
      (is (= "user" (:namespace result))))))  ; Should default to "user"
