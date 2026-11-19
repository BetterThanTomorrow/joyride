(ns integration-test.promise-results-test
  (:require [cljs.test :refer [is testing]]
            [clojure.string :as string]
            [integration-test.collision :as collision]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(defn- run-code+ [code]
  (vscode/commands.executeCommand "joyride.runCode" code))

(deftest-async collision-is-simulated
  (testing "The promise printer can be found and replaced"
    (is (collision/printer-found?)
        "promesa's promise printer is on Promise.prototype")
    (collision/with-collision+
      #(p/let [outcome (-> (run-code+ "(pr-str (js/Promise.resolve 1))")
                           (p/then (constantly :printed))
                           (p/catch (constantly :threw)))]
         (is (= :threw outcome)
             "Printing a promise throws under the simulated collision")))))

(deftest-async run-code-resolves-promise-results
  (testing "Run Code returns what a promise resolves to, with the collision in place"
    (collision/with-collision+
      #(p/let [value (run-code+ "(js/Promise.resolve 42)")
               nested (run-code+ "[(js/Promise.resolve 1) {:k (js/Promise.resolve 2)}]")]
         (is (= 42 value)
             "A promise result resolves to its value")
         (is (= 2 (count nested))
             "A value holding promises is returned")
         (is (instance? js/Promise (first nested))
             "The promise in the value is returned as a promise")))))

(deftest-async echo-of-settled-promise-succeeds
  (testing "Echoing a settled promise's value leaves no unhandled rejection"
    (let [!unhandled (atom [])
          listener (fn [reason _] (swap! !unhandled conj (str reason)))]
      (.on js/process "unhandledRejection" listener)
      (-> (collision/with-collision+
            #(p/let [_ (run-code+ "(js/Promise.resolve [(js/Promise.resolve 1) #js {:x (js/Promise.resolve 2)}])")
                     _ (p/delay 100)]
               (is (empty? @!unhandled)
                   (str "No unhandled rejections, got: " @!unhandled))))
          (p/finally (fn [_ _]
                       (.off js/process "unhandledRejection" listener)))))))

(deftest-async run-code-rejects-with-rejected-promise
  (testing "Run Code rejects when the promise rejects"
    (collision/with-collision+
      #(p/let [outcome (-> (run-code+ "(js/Promise.reject (js/Error. \"boom\"))")
                           (p/then (constantly "resolved"))
                           (p/catch (fn [e] (str (.-message e)))))]
         (is (string/includes? outcome "boom")
             "The rejection reaches the caller")))))
