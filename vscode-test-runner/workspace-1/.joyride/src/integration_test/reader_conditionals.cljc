(ns integration-test.reader-conditionals
  (:require [cljs.test :refer [deftest is]]))

(deftest reader-conditionals
  (is (= :joyride #?(:joyride :joyride :cljs :cljs :default :default)))
  (is (= :cljs #?(:cljs :cljs :default :default)))
  (is (= :default #?(:default :default))))