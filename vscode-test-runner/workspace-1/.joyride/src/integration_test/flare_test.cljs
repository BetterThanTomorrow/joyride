(ns integration-test.flare-test
  (:require
   [cljs.test :refer [is testing]]
   [joyride.flare :as flare]
   [promesa.core :as p]
   [integration-test.macros :refer [deftest-async]]))

(deftest-async namespaced-keywords-in-messages
  (testing "post-message!+ handles namespaced keywords"
    (let [ready? (p/deferred)
          received-message (p/deferred)]
      (p/let [message-handler (fn [msg]
                                (if (= true msg)
                                  (p/resolve! ready? true)
                                  (p/resolve! received-message (js->clj msg :keywordize-keys true))))
              message {:msg/type "test",
                       :foo 42,
                       :bar "42",
                       :msg/data {:user/name "Alice", :user/id 123, :nested {:action/name "click"}}}
              ;; This flare echoes messages back
              _ (flare/flare!+
                 {:key :test/namespaced-keywords,
                  :html [:div [:h1 "Test"]
                         [:script
                          "const vscode = acquireVsCodeApi();
                           vscode.postMessage(true)
                           window.addEventListener('message', event => {
                             vscode.postMessage(event.data);
                           });"]],
                  :title "Test",
                  :message-handler message-handler})
              _ ready?
              ;; We need to wait a tiny bit to be be ready for realz
              _ (p/delay 200)
              _ (flare/post-message!+ :test/namespaced-keywords message)
              ;; We need to wait a tick before closing the flare
              _ (p/delay 0)
              _ (flare/close! :test/namespaced-keywords)
              resolved-message received-message]
        (is (= message resolved-message)
            "The message survives the roundtrip intact")))))
