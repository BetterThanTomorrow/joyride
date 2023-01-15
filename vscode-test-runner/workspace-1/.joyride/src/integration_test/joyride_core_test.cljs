(ns integration-test.joyride-core-test
  (:require [cljs.test :refer [testing is]]
            [joyride.core :as joy]
            [promesa.core :as p]
            ["ext://betterthantomorrow.joyride" :as ns-required-extension-api]
            [integration-test.macros :refer [deftest-async]]
            ["vscode" :as vscode]))

#_{:clj-kondo/ignore [:duplicate-require]}
(require '["ext://betterthantomorrow.joyride" :as top-level-required-extension-api])

(deftest-async js-keys-of-ns-required-extension-api
  (testing "keys of ns-required extension"
    (p/let [required-joy-api-keys (joy/js-keys ns-required-extension-api)]
      (is (.includes required-joy-api-keys "toString")))))

(deftest-async js-keys-of-top-level-required-extension-api
  (testing "keys of top-level required extension"
    (p/let [required-joy-api-keys (joy/js-keys top-level-required-extension-api)]
      (is (.includes required-joy-api-keys "toString")))))

(deftest-async js-keys-of-extension
  (testing "keys of vscode grabbed extension" 
    (p/let [joy-ext (vscode/extensions.getExtension "betterthantomorrow.joyride")
            joy-ext-keys (joy/js-keys joy-ext)]
      (is (.includes joy-ext-keys "isActive")))))

(comment
  ;; TODO: Is this a bug?
  (= #js []
     (js-keys ns-required-extension-api))
  :rcf)
