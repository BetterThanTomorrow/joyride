(ns integration-test.joyride-core-test
  (:require
   ["../non-enumerable-props.js" :as non-enumerable]
   ["ext://betterthantomorrow.joyride" :as ns-required-extension-api]
   ["path" :as path]
   ["process" :as process]
   ["vscode" :as vscode]
   [cljs.test :refer [deftest is testing]]
   [clojure.set]
   [clojure.string]
   [joyride.core :as joy]
   [promesa.core :as p]
   [integration-test.macros :refer [deftest-async]]))

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
         joy/user-joyride-dir)
      "joyride.core/user-joyride-dir is defined and points to the right directory"))

(deftest-async slurp-relative-path
  (testing "slurp can read a file using relative path from workspace root"
    (p/let [content (joy/slurp ".joyride/etc/test-data.txt")]
      (is (clojure.string/includes? content "Hello from Joyride slurp test!"))
      (is (clojure.string/includes? content "åäö 🎉")))))

(deftest-async slurp-absolute-path
  (testing "slurp can read a file using absolute path"
    (p/let [workspace-root vscode/workspace.rootPath
            absolute-path (path/join workspace-root ".joyride/etc/test-data.txt")
            content (joy/slurp absolute-path)]
      (is (clojure.string/includes? content "Hello from Joyride slurp test!"))
      (is (clojure.string/includes? content "multiple lines")))))

(deftest-async load-file-relative-path
  (testing "load-file can evaluate a file using relative path from workspace root"
    (p/let [_ (joy/load-file ".joyride/etc/test_data.cljs")]
      (is (= :load-file-success @(resolve 'test-data/test-symbol)))
      (is (= "Hello from load-file!" (:message @(resolve 'test-data/test-data))))
      (is (= 42 (:number @(resolve 'test-data/test-data))))
      (is (fn? @(resolve 'test-data/test-function))))))

(deftest-async load-file-absolute-path
  (testing "load-file can evaluate a file using absolute path"
    (p/let [workspace-root vscode/workspace.rootPath
            absolute-path (path/join workspace-root ".joyride/etc/test_data.cljs")
            _ (joy/load-file absolute-path)]
      (is (= :load-file-success @(resolve 'test-data/test-symbol)))
      (is (= [1 2 3] (:vector @(resolve 'test-data/test-data)))))))


;; These ns leakage tests do not guard against the error they are intended to guard against
;; TManual tests at the repl is the only things that works for now.

#_(deftest-async load-file-should-not-change-ns
    (testing "load-file should not change current *ns* (expected to fail until fixed)"
      (let [before (str *ns*)]
        (p/let [_ (joy/load-file ".joyride/etc/test_data.cljs")
                after (str *ns*)]
          (is (= before after) (str "Namespace shouldn't change, was: " before ", now: " after))))))

#_(deftest-async load-file-should-not-change-ns-across-evals
    (testing "load-file should not persistently change *ns* across separate evals"
      (let [before (str *ns*)]
        (p/let [_ (joy/load-file ".joyride/etc/test_data.cljs")
                reported-ns (vscode/commands.executeCommand "joyride.runCode" "(str *ns*)")]
          (is (= before (str reported-ns))
              (str "Namespace should not leak when loading a file: was: " before ", now: " reported-ns))))))

(comment
  ;; TODO: Is this a bug?
  (= #js []
     (js-properties)))
