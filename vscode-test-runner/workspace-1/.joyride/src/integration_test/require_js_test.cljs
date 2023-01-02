(ns integration-test.require-js-test
  (:require [cljs.test :refer [deftest testing is async]]
            [promesa.core :as p]
            ["../js-file" :as js-file]
            [require-subdir-cljs-requiring-js]
            ["vscode" :as vscode]))

(deftest require-js-file
  (testing "Can require js file directly"
    (is (= 42
           js-file/fortytwo)))
  (testing "Can require cljs from subdir which requires  js file"
    (is (= 42
           require-subdir-cljs-requiring-js/fortytwo))))

(deftest script-require-js
  (testing "Requires js file from script"
    (async done
           (p/let [script-result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "require_js_script.cljs")]
             (is (= 42
                    script-result)))
           (done))))

(deftest script-require-cljs-requiring-js
  (testing "Requires js file from script requiring a cljs namespace"
    (async done
           (p/let [script-result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "require_cljs_requiring_js_script.cljs")]
             (is (= 42
                    script-result)))
           (done))))

(deftest script-require-cljs-requiring-js-from-subdir
  (testing "Requires js file from script requiring a cljs namespace in a subdir from the js file"
    (async done
           (p/let [script-result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "require_cljs_requiring_js_from_subdir_script.cljs")]
             (is (= 42
                    script-result)))
           (done))))
