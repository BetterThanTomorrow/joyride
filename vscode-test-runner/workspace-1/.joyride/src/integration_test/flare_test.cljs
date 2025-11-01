(ns integration-test.flare-test
  (:require
   [cljs.test :refer [is testing]]
   [joyride.flare :as flare]
   [promesa.core :as p]
   [integration-test.macros :refer [deftest-async]]))

(deftest-async namespaced-keywords-in-messages
  (testing "post-message!+ handles namespaced keywords"
    (p/let [received-message (atom nil)
            message-handler (fn [msg]
                              (reset! received-message (js->clj msg :keywordize-keys true)))
            message {:msg/type "test"
                     :foo 42
                     :bar "42"
                     :msg/data {:user/name "Alice"
                                :user/id 123
                                :nested {:action/name "click"}}}
            ;; This flare simply pongs the ping
            _ (flare/flare!+ {:key :test-namespaced-keywords
                              :html [:div
                                     [:h1 "Test"]
                                     [:script
                                      "const vscode = acquireVsCodeApi();
                                       window.addEventListener('message', event => {
                                        vscode.postMessage(event.data);
                                       });"]]
                              :title "Test"
                              :message-handler message-handler})

            ;; Wait a bit for the flare to be created
            _ (p/delay 200)
            _ (flare/post-message!+ :test-namespaced-keywords
                                    message)
            ;; Wait for the message to be received (next tick?)
            _ (p/delay 0)]

      (is (= message @received-message)
          "The message survives the roundtrip intact")
      ;; Clean up
      (flare/close! :test-namespaced-keywords))))
