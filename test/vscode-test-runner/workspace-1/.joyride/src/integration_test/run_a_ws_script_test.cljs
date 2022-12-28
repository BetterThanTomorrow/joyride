(ns integration-test.run-a-ws-script-test
  (:require [cljs.test :refer [deftest testing is]]
            #_[promesa.core :as p]
            #_["vscode" :as vscode]))

(deftest run-a-ws-script
  (testing "Runs a workspace script"
    ;; TODO: This doesn't work, 0 assertions
    #_(p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")]
        (is (= :a-ws-script
               result)))

    (is #_{:clj-kondo/ignore [:unresolved-namespace]}
     (= :a-ws-script
        a-ws-script/symbol-1))))