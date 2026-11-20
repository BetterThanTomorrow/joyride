(ns integration-test.nrepl-eval-test
  (:require [cljs.test :refer [testing is use-fixtures async]]
            [integration-test.collision :as collision]
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

(defn- send+
  "Sends the nREPL message `msg` and resolves to the response messages."
  [msg]
  (p/create (fn [resolve reject]
              (.send (:client @!state) (clj->js msg)
                     (fn [err messages]
                       (if err
                         (reject err)
                         (resolve messages)))))))

(deftest-async values-holding-promises
  (testing "nREPL returns values holding promises, with the collision in place"
    (collision/with-collision+
      #(p/let [plain (send+ {:op "eval"
                             :code "[(js/Promise.resolve 1) #js {:x (js/Promise.resolve 2)}]"})
               pretty (send+ {:op "eval"
                              :code "[(js/Promise.resolve 1) (atom (js/Promise.resolve 2))]"
                              "nrepl.middleware.print/print" "cider.nrepl.pprint/pprint"
                              "nrepl.middleware.print/options" {:right-margin 120}})
               awaited (send+ {:op "eval"
                               :code "(js/Promise.resolve [1 (js/Promise.resolve 2)])"})]
         (is (= "[#<js/Promise[~]> #js {:x #<js/Promise[~]>}]"
                (from-nrepl-messages plain "value"))
             "Promises in a value and in a #js object print")
         (is (= "[#<js/Promise[~]> #object[cljs.core.Atom {:val #<js/Promise[~]>}]]"
                (from-nrepl-messages pretty "value"))
             "Pretty printing prints promises in a value and in an atom")
         (is (= "#<Promise [1 #<js/Promise[~]>]>"
                (from-nrepl-messages awaited "value"))
             "An awaited promise's value prints, with the promise inside it")))))

(deftest-async errors-holding-promises
  (testing "nREPL finishes errors whose data holds promises, with the collision in place"
    (collision/with-collision+
      #(p/let [thrown (send+ {:op "eval"
                              :code "(throw (ex-info \"thrown boom\" {:p (js/Promise.resolve 1)}))"})
               rejected (send+ {:op "eval"
                                :code "(js/Promise.reject (ex-info \"rejected boom\" {:p (js/Promise.resolve 1)}))"})]
         (is (re-find #"thrown boom" (from-nrepl-messages thrown "ex"))
             "A thrown error is reported")
         (is (re-find #"rejected boom" (from-nrepl-messages rejected "ex"))
             "A rejection is reported")))))

(deftest-async rejection-with-nil
  (testing "nREPL reports a rejection with nil as nil"
    (p/let [rejected (send+ {:op "eval"
                             :code "(js/Promise.reject nil)"})]
      (is (= "nil" (from-nrepl-messages rejected "ex"))
          "The error text is nil"))))
