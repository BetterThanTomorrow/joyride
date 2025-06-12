(ns integration-test.joyride-core-test
  (:require
   ["../non-enumerable-props.js" :as non-enumerable]
   ["ext://betterthantomorrow.joyride" :as ns-required-extension-api]
   ["path" :as path]
   ["process" :as process]
   ["vscode" :as vscode]
   [cljs.test :refer [deftest is testing]]
   [clojure.set]
   [joyride.core :as joy]))

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

(deftest user-joyride-dir
  (is (= (path/join (aget process/env "VSCODE_JOYRIDE_USER_CONFIG_PATH") "joyride")
         joy/user-joyride-dir
         "joyride.core/user-joyride-dir is defined and points to the right directory")))

(comment
  ;; TODO: Is this a bug?
  (= #js []
     (js-properties))
  )
