(ns integration-test.nrepl-start-stop-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(deftest-async start-and-stop-nrepl-server
  (testing "Starts and stops the nrepl server"
    (p/let [port (vscode/commands.executeCommand "joyride.startNReplServer")
            stopped-server (js/JSON.stringify (vscode/commands.executeCommand "joyride.stopNReplServer"))
            _ (js/console.log stopped-server)]
      (is (number? port))
      (is (some? stopped-server)))))
