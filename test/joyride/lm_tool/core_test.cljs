(ns joyride.lm-tool.core-test
  "Tests for LM tool core functionality"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as string]
            [joyride.lm-tool.core :as core]))

(deftest test-format-confirmation-message
  (testing "Confirmation message formatting returns structured data"
    (let [code "(+ 1 2 3)"
          ns "user"
          wait-for-promise? false
          result (core/format-confirmation-message code ns wait-for-promise?)]
      (is (map? result))
      (is (= :confirmation (:type result)))
      (is (= "Run Joyride Code" (:title result)))
      (is (= code (:code result)))
      (is (= ns (:ns result)))
      (is (= wait-for-promise? (:wait-for-promise? result)))
      (is (string? (:description result))))))

(deftest test-format-result-message
  (testing "Result message formatting returns structured data"
    (let [result "6"
          stdout "Hello from stdout!"
          stderr ""
          formatted (core/format-result-message result stdout stderr)]
      (is (map? formatted))
      (is (= :success (:type formatted)))
      (is (= result (:result formatted)))
      (is (= stdout (:stdout formatted)))
      (is (= stderr (:stderr formatted))))))

(deftest test-format-error-message
  (testing "Error message formatting returns structured data"
    (let [error "Could not resolve symbol: unknown-fn"
          code "(unknown-fn 1)"
          stdout "Some output"
          stderr "Warning message"
          formatted (core/format-error-message error code stdout stderr)]
      (is (map? formatted))
      (is (= :error (:type formatted)))
      (is (= error (:error formatted)))
      (is (= code (:code formatted)))
      (is (= stdout (:stdout formatted)))
      (is (= stderr (:stderr formatted))))))

(deftest test-validate-input
  (testing "Input validation"
    (is (:valid? (core/validate-input {:code "(+ 1 2)" :ns "user" :wait-for-promise? false})))
    (is (not (:valid? (core/validate-input {:code "(+ 1 2)" :ns "user" :wait-for-promise? ""}))))
    (is (not (:valid? (core/validate-input {:code nil :ns "user"}))))
    (is (not (:valid? (core/validate-input {:code "" :ns "user"}))))
    (is (not (:valid? (core/validate-input {:code "(+ 1 2)" :ns nil}))))))

(deftest test-extract-input-data
  (testing "Input data extraction"
    (let [input #js {:code "(+ 1 2)" :namespace "test"}
          result (core/extract-input-data input)]
      (is (= "(+ 1 2)" (:code result)))
      (is (= "test" (:ns result))))

    (let [input #js {:code "(+ 1 2)"}  ; No namespace
          result (core/extract-input-data input)]
      (is (= "(+ 1 2)" (:code result)))
      (is (= "user" (:ns result))))))

(deftest test-markdown-conversion-functions
  (testing "Confirmation message to markdown conversion"
    (let [confirmation-data {:type :confirmation
                             :title "Execute ClojureScript Code"
                             :code "(+ 1 2 3)"
                             :ns "user"
                             :description "Test description"}
          markdown (core/confirmation-message->markdown confirmation-data)]
      (is (string? markdown))
      (is (string/includes? markdown "Execute the following ClojureScript"))
      (is (string/includes? markdown "(+ 1 2 3)"))
      (is (string/includes? markdown "user"))))

  (testing "Error message to markdown conversion"
    (let [error-data {:type :error
                      :error "Could not resolve symbol"
                      :code "(unknown-fn)"
                      :stdout "Some output"
                      :stderr "Warning"}
          markdown (core/error-message->markdown error-data)]
      (is (string? markdown))
      (is (string/includes? markdown "Error executing ClojureScript"))
      (is (string/includes? markdown "Could not resolve symbol"))
      (is (string/includes? markdown "(unknown-fn)"))
      (is (string/includes? markdown "Some output")))))
