(ns integration-test.nrepl-eval-test
  (:require [cljs.test :refer [testing is use-fixtures async]]
            [integration-test.macros :refer [deftest-async]]
            ["nrepl-client" :as nrepl-client]
            [promesa.core :as p]
            ["vscode" :as vscode]
            ["util" :as util]))

(defn- from-nrepl-messages [messages k]
  (.reduce messages
           (fn [answer message]
             (if (aget message k)
               (str answer (aget message k))
               answer))
           ""))

(use-fixtures :once
  {:before
   #(async done 
           (p/do!
            (p/create (fn [resolve reject]
                        (p/let [port (vscode/commands.executeCommand "joyride.startNReplServer")
                                client' (.connect nrepl-client  #js {:port port})
                                eval' (util/promisify client'.eval)]
                          (def client client')
                          (def eval eval')
                          (.on client "error"
                               (fn [error]
                                 (js/console.error "Error connecting to nREPL server:" error)
                                 (reject error)))
                          (.once client "connect"
                                 (fn [] (resolve))))))
            (done)))
   :after
   #(async done
           (do
             (.end client)
             (p/do (vscode/commands.executeCommand "joyride.stopNReplServer")
                   (done))))})

(deftest-async evaluate
  (testing "Evaluate returns in `:value`"
    (p/create (fn [resolve _reject]
                (p/let [messages (eval "42")
                        answer (from-nrepl-messages messages "value")]
                  (is (= "42"
                         answer))
                  (resolve))))))

(deftest-async print-out
  (testing "nRepl server prints to `:out`"
    (p/create (fn [resolve _reject]
                (p/let [messages (eval "(println 42)")
                        answer (from-nrepl-messages messages "out")]
                  (is (= "42\n"
                         answer))
                  (resolve))))))
