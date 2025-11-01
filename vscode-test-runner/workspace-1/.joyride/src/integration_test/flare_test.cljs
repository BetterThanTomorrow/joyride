(ns integration-test.flare-test
  (:require
   ["vscode" :as vscode]
   [cljs.test :refer [deftest is testing]]
   [joyride.flare :as flare]
   [promesa.core :as p]
   [integration-test.macros :refer [deftest-async]]))

(deftest-async namespaced-keywords-in-messages
  (testing "post-message!+ handles namespaced keywords by dropping the namespace"
    (p/let [;; Create a flare that pongs the ping
            ;; and a message handler that captures the pong
            received-message (atom nil)
            message-handler (fn [msg]
                              (reset! received-message (js->clj msg :keywordize-keys true)))
            message {:msg/type "test"
                     :foo 42
                     :bar "42"
                     :msg/data {:user/name "Alice"
                                :user/id 123
                                :nested {:action/name "click"}}}

            _ (def received-message received-message)
            _ (def message-handler message-handler)
            _ (def message message)

            ;; Create a flare with the message handler
            _ (flare/flare!+ {:key :test-namespaced-keywords
                              :html [:div
                                     [:h1 "Test"]
                                     [:script
                                      "const vscode = acquireVsCodeApi();
                                       function sendMessage(data) {
                                         vscode.postMessage(data);
                                         console.log('Sent message:', type, data);
                                       }
                                       window.addEventListener('message', event => {
                                        console.log(event.data)
                                        vscode.postMessage(event.data);
                                       });"]]
                              :title "Test"
                              :message-handler message-handler})

            ;; Wait a bit for the flare to be created
            _ (p/delay 100)

            ;; Send a message with namespaced keywords
            _ (flare/post-message!+ :test-namespaced-keywords
                                    message)

            ;; Wait for the message to be received
            _ (p/delay 100)
            ]



      ;; Verify that the namespaces were dropped in the JavaScript conversion
      (is (= message @received-message)
          "The message survives the roundtrip intact")
      ;; Clean up
      (flare/close! :test-namespaced-keywords))))

(comment
  @received-message
  :rcf)
