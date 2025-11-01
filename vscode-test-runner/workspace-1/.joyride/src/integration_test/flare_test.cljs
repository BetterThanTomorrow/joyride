(ns integration-test.flare-test
  "Integration tests for Joyride Flares functionality, specifically testing namespaced keyword handling"
  (:require
   ["vscode" :as vscode]
   [cljs.test :refer [deftest is testing]]
   [joyride.flare :as flare]
   [promesa.core :as p]
   [integration-test.macros :refer [deftest-async]]))

(deftest-async post-message-with-namespaced-keywords
  (testing "post-message!+ correctly handles namespaced keywords in messages"
    (p/let [received-messages (atom [])
            ;; Create a flare with a message handler that captures messages
            _ (flare/flare!+ {:key :test-namespaced-keys
                              :html [:div
                                     [:h1 "Test Flare"]
                                     [:script {:type "text/javascript"}
                                      "
                                      window.addEventListener('message', event => {
                                        const vscode = acquireVsCodeApi();
                                        vscode.postMessage({type: 'echo', data: event.data});
                                      });
                                      "]]
                              :webview-options {:enableScripts true}
                              :message-handler (fn [msg]
                                                 (swap! received-messages conj msg))})
            ;; Give the webview time to initialize
            _ (p/delay 500)
            
            ;; Send a message with namespaced keywords
            _ (flare/post-message!+ :test-namespaced-keys
                                    {:action/type "update"
                                     :user/name "Alice"
                                     :user/age 30
                                     :data {:nested/key "value"}})
            
            ;; Wait for the message to be processed
            _ (p/delay 500)]
      
      ;; Verify we received an echo message
      (is (> (count @received-messages) 0)
          "Should have received at least one message")
      
      ;; If we received an echo, verify the keys are preserved correctly
      (when (pos? (count @received-messages))
        (let [echoed-msg (first @received-messages)
              msg-data (.-data echoed-msg)]
          ;; In JavaScript, the namespaced keywords should be preserved as strings with slash
          (is (some? (aget msg-data "action/type"))
              "Namespaced keyword :action/type should be accessible as 'action/type'")
          (is (= "update" (aget msg-data "action/type"))
              "Value of namespaced key should be preserved")
          (is (some? (aget msg-data "user/name"))
              "Namespaced keyword :user/name should be accessible as 'user/name'")
          (is (= "Alice" (aget msg-data "user/name"))
              "Value of :user/name should be 'Alice'")
          (is (= 30 (aget msg-data "user/age"))
              "Value of :user/age should be 30")))
      
      ;; Clean up
      (flare/close! :test-namespaced-keys))))

(deftest-async post-message-with-mixed-keywords
  (testing "post-message!+ handles both namespaced and non-namespaced keywords"
    (p/let [received-messages (atom [])
            _ (flare/flare!+ {:key :test-mixed-keys
                              :html [:div
                                     [:h1 "Test Mixed Keys"]
                                     [:script {:type "text/javascript"}
                                      "
                                      window.addEventListener('message', event => {
                                        const vscode = acquireVsCodeApi();
                                        vscode.postMessage({type: 'received', original: event.data});
                                      });
                                      "]]
                              :webview-options {:enableScripts true}
                              :message-handler (fn [msg]
                                                 (swap! received-messages conj msg))})
            _ (p/delay 500)
            
            ;; Send message with mixed keyword types
            _ (flare/post-message!+ :test-mixed-keys
                                    {:type "command"
                                     :action/name "save"
                                     :data {:value 42}
                                     :meta/timestamp 1234567890})
            
            _ (p/delay 500)]
      
      (is (> (count @received-messages) 0)
          "Should have received at least one message")
      
      (when (pos? (count @received-messages))
        (let [msg (first @received-messages)
              original (.-original msg)]
          ;; Non-namespaced keywords should work as before
          (is (some? (aget original "type"))
              "Non-namespaced keyword :type should be accessible")
          (is (= "command" (aget original "type"))
              "Value of :type should be 'command'")
          
          ;; Namespaced keywords should be preserved
          (is (some? (aget original "action/name"))
              "Namespaced keyword :action/name should be accessible")
          (is (= "save" (aget original "action/name"))
              "Value of :action/name should be 'save'")
          (is (some? (aget original "meta/timestamp"))
              "Namespaced keyword :meta/timestamp should be accessible")
          (is (= 1234567890 (aget original "meta/timestamp"))
              "Value of :meta/timestamp should be correct")))
      
      (flare/close! :test-mixed-keys))))

(comment
  ;; Manual REPL testing
  (require '[joyride.flare :as flare])
  (require '[promesa.core :as p])
  
  ;; Test basic namespaced keyword handling
  (p/let [_ (flare/flare!+ {:key :manual-test
                            :html [:div
                                   [:h1 "Manual Test"]
                                   [:div#output "Waiting for message..."]
                                   [:script {:type "text/javascript"}
                                    "
                                    window.addEventListener('message', event => {
                                      const output = document.getElementById('output');
                                      const data = event.data;
                                      output.innerHTML = '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
                                      
                                      // Log the keys to console for inspection
                                      console.log('Received message with keys:', Object.keys(data));
                                      console.log('action/type =', data['action/type']);
                                      console.log('user/name =', data['user/name']);
                                    });
                                    "]]
                            :webview-options {:enableScripts true}})]
    (flare/post-message!+ :manual-test
                          {:action/type "test"
                           :user/name "Bob"
                           :user/email "bob@example.com"
                           :regular-key "value"}))
  
  ;; Clean up
  (flare/close! :manual-test)
  
  :rcf)
