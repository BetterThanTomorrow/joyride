(ns joyride.lm.eval.validation-test
  "Pure unit tests for bracket validation logic"
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as string]
            [joyride.lm.eval.validation :as validation]))

(deftest validate-brackets-balanced-code
  (testing "Balanced code passes validation"
    (let [result (validation/validate-brackets "(+ 1 2 3)")]
      (is (:valid? result)
          "Balanced code should be valid")
      (is (nil? (:error result))
          "No error for balanced code"))))

(deftest validate-brackets-unbalanced-code
  (testing "Unbalanced brackets fail validation with balanced code provided"
    (let [result (validation/validate-brackets "(+ 1 2")]
      (is (not (:valid? result))
          "Unbalanced code should be invalid")
      (is (string? (:error result))
          "Error message should be present")
      (is (string/includes? (:error result) "unbalanced brackets")
          "Error message should mention unbalanced brackets")
      (is (string/includes? (:error result) "balanced-code")
          "Error should reference the balanced-code field")
      (is (string/includes? (:error result) "re-evaluate using that code")
          "Error should provide recovery guidance")
      (is (= "(+ 1 2)" (:balanced-code result))
          "Balanced code should be provided"))))

(deftest validate-brackets-bad-indentation
  (testing "Bad indentation that changes semantics fails validation with balanced code"
    (let [code "(defn f [x]\n  {:foo 1\n  :bar 2})"
          result (validation/validate-brackets code)]
      (is (not (:valid? result))
          "Semantically ambiguous code should be invalid")
      (is (clojure.string/includes? (:error result) "bracket balancer would change")
          "Error should explain semantic change risk")
      (is (string? (:balanced-code result))
          "Balanced code should be provided")
      (is (clojure.string/includes? (:balanced-code result) "{:foo 1}")
          "Balanced code should show the semantic transformation"))))

(deftest validate-brackets-multiple-forms
  (testing "Multiple balanced forms pass validation"
    (let [code "(def x 1)\n(def y 2)\n(+ x y)"
          result (validation/validate-brackets code)]
      (is (:valid? result)
          "Multiple balanced forms should be valid"))))

(deftest validate-brackets-nested-structures
  (testing "Nested balanced structures pass validation"
    (let [code "{:foo {:bar {:baz 1}}}"
          result (validation/validate-brackets code)]
      (is (:valid? result)
          "Nested structures should be valid when balanced"))))

(deftest validate-brackets-malformed-code
  (testing "Malformed brackets get parinfer error message"
    (let [code "[(][]})]"
          result (validation/validate-brackets code)]
      (is (not (:valid? result))
          "Malformed code should be invalid")
      (is (string/includes? (:error result) "malformed brackets")
          "Error should mention malformed brackets")
      (is (string/includes? (:error result) "Unmatched close-paren")
          "Error should include parinfer error message")
      (is (nil? (:balanced-code result))
          "Balanced code should NOT be provided for malformed code"))))

(deftest validate-brackets-parinfer-error-structure
  (let [malformed-code "[(][]})]"
        result (validation/validate-brackets malformed-code)]
    (testing "Malformed code returns structured parinfer error"
      (is (false? (:valid? result)))
      (is (string/includes? (:error result) "malformed brackets"))
      (is (some? (:parinfer-error result)))
      (is (= "Unmatched close-paren." (get-in result [:parinfer-error :message])))
      (is (= 1 (get-in result [:parinfer-error :line])))
      (is (= 2 (get-in result [:parinfer-error :column])))
      (is (nil? (:balanced-code result))))))
