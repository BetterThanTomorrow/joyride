(ns integration-test.joyride-core-test
  (:require [clojure.set]
            [cljs.test :refer [deftest is testing]]
            [joyride.core :as joy]
            ["ext://betterthantomorrow.joyride" :as ns-required-extension-api]
            ["vscode" :as vscode]
            ["../non-enumerable-props.js" :as non-enumerable]))

#_{:clj-kondo/ignore [:duplicate-require]}
(require '["ext://betterthantomorrow.joyride" :as top-level-required-extension-api])

(deftest js-properties
  (testing "keys of object with non-enumerable properties"
    (let [non-enumerable-keys (joy/js-properties non-enumerable/obj)]
      (is (every? (set non-enumerable-keys) #{"x" "y" "z"}))
      (is (= "toString" 
             (some #{"toString"} non-enumerable-keys)))))
  (testing "keys of ns-required extension"
    (let [required-joy-api-keys (joy/js-properties ns-required-extension-api)]
      (is (= "toString" 
             (some #{"toString"} required-joy-api-keys)))))
  (testing "keys of top-level required extension"
    (let [required-joy-api-keys (joy/js-properties top-level-required-extension-api)]
      (is (= "toString"
             (some #{"toString"} required-joy-api-keys)))))
  (testing "keys of vscode grabbed extension"
    (let [joy-ext (vscode/extensions.getExtension "betterthantomorrow.joyride")
          joy-ext-keys (joy/js-properties joy-ext)]
      (is (= "isActive"
             (some #{"isActive"} joy-ext-keys))))))

(comment
  ;; TODO: Is this a bug?
  (= #js []
     (js-properties))
  )
