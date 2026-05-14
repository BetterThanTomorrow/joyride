(ns integration-test.nrepl-start-stop-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

;; https://github.com/BetterThanTomorrow/joyride/issues/146
;; We should be able to start and stop the server in quick succession
;; without running into any ”Server already started” errors
;; (Though I failed creating a not-thrown? assertion, so just testing that we get a result for now.)

(defn- start-stop-server+ []
  (p/let [port (vscode/commands.executeCommand "joyride.startNReplServer")
          stopped-server (vscode/commands.executeCommand "joyride.stopNReplServer")] 
    (is (number? port))
    (is (some? stopped-server))))

(deftest-async start-and-stop-nrepl-server-1
  (testing "Starts and stops the nrepl server 1"
    (start-stop-server+)))

(deftest-async start-and-stop-nrepl-server-2
  (testing "Starts and stops the nrepl server 2"
    (start-stop-server+)))

(deftest-async start-and-stop-nrepl-server-3
  (testing "Starts and stops the nrepl server 3"
    (start-stop-server+)))

(deftest-async start-and-stop-nrepl-server-4
  (testing "Starts and stops the nrepl server 4"
    (start-stop-server+)))

(deftest-async start-and-stop-nrepl-server-5
  (testing "Starts and stops the nrepl server 5"
    (start-stop-server+)))