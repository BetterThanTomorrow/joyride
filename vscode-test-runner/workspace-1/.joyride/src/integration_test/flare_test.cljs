(ns integration-test.flare-test
  (:require
   ["vscode" :as vscode]
   [cljs.test :refer [deftest is testing]]
   [joyride.flare :as flare]
   [promesa.core :as p]
   [integration-test.macros :refer [deftest-async]]))

(deftest-async namespaced-keywords-in-messages
  (testing "post-message!+ handles namespaced keywords by dropping the namespace"
    (p/let [;; Create a test flare with a message handler that captures the received message
            received-message (atom nil)
            message-handler (fn [msg]
                              (reset! received-message (js->clj msg :keywordize-keys true)))
            
            ;; Create a flare with the message handler
            _ (flare/flare!+ {:key :test-namespaced-keywords
                              :html "<h1>Test Flare</h1>"
                              :title "Test"
                              :message-handler message-handler})
            
            ;; Wait a bit for the flare to be created
            _ (p/delay 100)
            
            ;; Send a message with namespaced keywords
            _ (flare/post-message!+ :test-namespaced-keywords
                                    {:msg/type "test"
                                     :msg/data {:user/name "Alice"
                                               :user/id 123
                                               :nested {:action/name "click"}}})
            
            ;; Wait for the message to be received
            _ (p/delay 100)]
      
      ;; Verify that the namespaces were dropped in the JavaScript conversion
      (is (= "test" (get @received-message "type"))
          "Namespaced keyword :msg/type should become 'type'")
      
      (is (= "Alice" (get-in @received-message ["data" "name"]))
          "Namespaced keyword :user/name should become 'name'")
      
      (is (= 123 (get-in @received-message ["data" "id"]))
          "Namespaced keyword :user/id should become 'id'")
      
      (is (= "click" (get-in @received-message ["data" "nested" "name"]))
          "Nested namespaced keyword :action/name should become 'name'")
      
      ;; Clean up
      (flare/close! :test-namespaced-keywords))))

(deftest-async plain-keywords-still-work
  (testing "post-message!+ still works correctly with plain (non-namespaced) keywords"
    (p/let [;; Create a test flare with a message handler that captures the received message
            received-message (atom nil)
            message-handler (fn [msg]
                              (reset! received-message (js->clj msg :keywordize-keys true)))
            
            ;; Create a flare with the message handler
            _ (flare/flare!+ {:key :test-plain-keywords
                              :html "<h1>Test Flare</h1>"
                              :title "Test"
                              :message-handler message-handler})
            
            ;; Wait a bit for the flare to be created
            _ (p/delay 100)
            
            ;; Send a message with plain keywords (no namespaces)
            _ (flare/post-message!+ :test-plain-keywords
                                    {:type "test"
                                     :data {:name "Bob"
                                           :id 456}})
            
            ;; Wait for the message to be received
            _ (p/delay 100)]
      
      ;; Verify that plain keywords work as expected
      (is (= "test" (get @received-message "type"))
          "Plain keyword :type should become 'type'")
      
      (is (= "Bob" (get-in @received-message ["data" "name"]))
          "Plain keyword :name should become 'name'")
      
      (is (= 456 (get-in @received-message ["data" "id"]))
          "Plain keyword :id should become 'id'")
      
      ;; Clean up
      (flare/close! :test-plain-keywords))))

(deftest-async mixed-keywords-in-messages
  (testing "post-message!+ handles a mix of plain and namespaced keywords correctly"
    (p/let [;; Create a test flare with a message handler that captures the received message
            received-message (atom nil)
            message-handler (fn [msg]
                              (reset! received-message (js->clj msg :keywordize-keys true)))
            
            ;; Create a flare with the message handler
            _ (flare/flare!+ {:key :test-mixed-keywords
                              :html "<h1>Test Flare</h1>"
                              :title "Test"
                              :message-handler message-handler})
            
            ;; Wait a bit for the flare to be created
            _ (p/delay 100)
            
            ;; Send a message with a mix of plain and namespaced keywords
            _ (flare/post-message!+ :test-mixed-keywords
                                    {:type "mixed"
                                     :msg/source "test"
                                     :data {:name "Charlie"
                                           :user/role "admin"}})
            
            ;; Wait for the message to be received
            _ (p/delay 100)]
      
      ;; Verify the conversion
      (is (= "mixed" (get @received-message "type"))
          "Plain keyword :type should become 'type'")
      
      (is (= "test" (get @received-message "source"))
          "Namespaced keyword :msg/source should become 'source'")
      
      (is (= "Charlie" (get-in @received-message ["data" "name"]))
          "Plain keyword :name should become 'name'")
      
      (is (= "admin" (get-in @received-message ["data" "role"]))
          "Namespaced keyword :user/role should become 'role'")
      
      ;; Clean up
      (flare/close! :test-mixed-keywords))))
