(ns integration-test.reader-conditionals
  (:require [cljs.test :refer [deftest is]]))

(deftest reader-conditionals
  (is (= #?(:joyride :joyride :cljs :cljs :default :default) :joyride))
  (is (= #?(:cljs :cljs :default :default) :cljs))
  (is (= #?(:default :default) :default)))