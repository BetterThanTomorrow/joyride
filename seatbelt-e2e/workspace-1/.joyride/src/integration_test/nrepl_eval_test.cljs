(ns integration-test.nrepl-eval-test
  (:require [cljs.test :refer [testing is use-fixtures async]]
            [integration-test.macros :refer [deftest-async]]
            ["nrepl-client" :as nrepl-client]
            [promesa.core :as p]
            ["vscode" :as vscode]
            ["util" :as util]))

(def !state (atom {:client nil
                   :client-eval nil}))

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
                                client (.connect nrepl-client  #js {:port port})
                                client-eval (util/promisify client.eval)]
                          (swap! !state assoc :client client :client-eval client-eval)
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
             (.end (:client @!state))
             (p/do (vscode/commands.executeCommand "joyride.stopNReplServer")
                   (done))))})

(deftest-async evaluate
  (testing "Evaluate returns in `:value`"
    (p/create (fn [resolve _reject]
                (p/let [messages ((:client-eval @!state) "42")
                        answer (from-nrepl-messages messages "value")]
                  (is (= "42"
                         answer))
                  (resolve))))))

(deftest-async client-print-out
  (testing "nRepl server prints evaluated prints to `:out`"
    (p/create (fn [resolve _reject]
                (p/let [messages ((:client-eval @!state) "(println 42)")
                        answer (from-nrepl-messages messages "out")]
                  (is (= "42\n"
                         answer))
                  (resolve))))))

(deftest-async print-out
  (testing "While the nRepl server is connected `println` prints to message stream `:out`"
    (p/create (fn [resolve _reject]
                ;; The nrepl-client seems to lack a way to get out-of-band prints
                ;; we're using the raw Buffer, looking for our printed value in it
                (.once (:client @!state) "data"
                       (fn [data]
                         (is (= (re-find #"printed 42" (.toString data))
                                "printed 42"))
                         (resolve)))
                (println "printed 42")))))
