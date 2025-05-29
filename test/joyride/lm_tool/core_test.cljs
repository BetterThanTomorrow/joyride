(ns joyride.lm-tool.core-test
  "Tests for LM tool core functionality"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string]
            [joyride.lm-tool.core :as core]))

(deftest test-format-confirmation-message
  (testing "Confirmation message formatting returns structured data"
    (let [code "(+ 1 2 3)"
          namespace "user"
          result (core/format-confirmation-message code namespace)]
      (is (map? result))
      (is (= :confirmation (:type result)))
      (is (= "Execute ClojureScript Code" (:title result)))
      (is (= code (:code result)))
      (is (= namespace (:namespace result)))
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
      (is (= "user" (:namespace result))))))

(deftest test-markdown-conversion-functions
  (testing "Confirmation message to markdown conversion"
    (let [confirmation-data {:type :confirmation
                            :title "Execute ClojureScript Code"
                            :code "(+ 1 2 3)"
                            :namespace "user"
                            :description "Test description"}
          markdown (core/confirmation-message->markdown confirmation-data)]
      (is (string? markdown))
      (is (clojure.string/includes? markdown "Execute the following ClojureScript"))
      (is (clojure.string/includes? markdown "(+ 1 2 3)"))
      (is (clojure.string/includes? markdown "user"))))

  (testing "Result message to markdown conversion"
    (let [result-data {:type :success
                      :result "6"
                      :stdout "Hello world!"
                      :stderr ""}
          markdown (core/result-message->markdown result-data)]
      (is (string? markdown))
      (is (clojure.string/includes? markdown "Evaluation result"))
      (is (clojure.string/includes? markdown "6"))
      (is (clojure.string/includes? markdown "Hello world!"))))

  (testing "Error message to markdown conversion"
    (let [error-data {:type :error
                     :error "Could not resolve symbol"
                     :code "(unknown-fn)"
                     :stdout "Some output"
                     :stderr "Warning"}
          markdown (core/error-message->markdown error-data)]
      (is (string? markdown))
      (is (clojure.string/includes? markdown "Error executing ClojureScript"))
      (is (clojure.string/includes? markdown "Could not resolve symbol"))
      (is (clojure.string/includes? markdown "(unknown-fn)"))
      (is (clojure.string/includes? markdown "Some output")))))
