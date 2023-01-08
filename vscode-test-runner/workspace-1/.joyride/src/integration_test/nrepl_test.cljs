(ns integration-test.nrepl-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            ["nrepl-client" :as nrepl-client]
            [promesa.core :as p]
            ["vscode" :as vscode]
            ["util" :as util]))

(deftest-async start-and-stop-nrepl-server
  (testing "Starts and stops the nrepl server"
    (p/let [port (vscode/commands.executeCommand "joyride.startNReplServer")
            _ (is (number? port))
            client (.connect nrepl-client  #js {:port port})
            eval (util/promisify client.eval)]
      (p/create (fn [resolve reject]
                  (.on client "error"
                       (fn [error]
                         (js/console.error "Error connecting to nREPL server:" error)
                         (reject error)))
                  (.once client "connect"
                         (fn []
                           ;; TODO: Inside this p/let `println` stops working
                           ;;       nothing is printed
                           ;;       js/console.log works
                           (p/let [messages (eval "42")
                                   ;; TODO: Couldn't make this work with `js->clj`
                                   ;;       This is why js `.reduce` is used
                                   answer (.reduce messages
                                                   (fn [answer message]
                                                     (if (.-value message)
                                                       (str answer (.-value message))
                                                       answer))
                                                   "")]
                             (is (= "42"
                                    answer))
                             (.end client)
                             (p/let [something (vscode/commands.executeCommand "joyride.stopNReplServer")]
                               (is (some? something))
                               (resolve))))))))))
