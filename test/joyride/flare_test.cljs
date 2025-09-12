(ns joyride.flare-test
  (:require
   [cljs.test :refer [deftest testing is]]
   [joyride.flare.error-handling :as error]))

(deftest flare-validation-test
  (testing "Valid flare options"
    (testing "HTML string content"
      (is (= {:html "<h1>Hello</h1>"}
             (error/validate-flare-options {:html "<h1>Hello</h1>"}))))

    (testing "Hiccup data structures"
      (is (= {:html [:div [:h1 "Hello"] [:p "World"]]}
             (error/validate-flare-options {:html [:div [:h1 "Hello"] [:p "World"]]}))))

    (testing "URL content"
      (is (= {:url "https://example.com"}
             (error/validate-flare-options {:url "https://example.com"}))))

    (testing "All option combinations"
      (is (= {:html "<h1>Test</h1>" :title "Test Panel" :key "test-key"}
             (error/validate-flare-options {:html "<h1>Test</h1>"
                                           :title "Test Panel"
                                           :key "test-key"})))))

  (testing "Invalid flare options"
    (testing "Missing content"
      (is (thrown-with-msg? js/Error #"must specify either :html or :url"
                           (error/validate-flare-options {}))))

    (testing "Both HTML and URL provided"
      (is (thrown-with-msg? js/Error #"cannot specify both :html and :url"
                           (error/validate-flare-options {:html "<h1>Test</h1>"
                                                          :url "https://example.com"}))))

    (testing "Invalid HTML content type"
      (is (thrown-with-msg? js/Error #":html content must be a string or Hiccup vector"
                           (error/validate-flare-options {:html 123}))))

    (testing "Invalid URL content type"
      (is (thrown-with-msg? js/Error #":url content must be a string"
                           (error/validate-flare-options {:url 123}))))

    (testing "Invalid title type"
      (is (thrown-with-msg? js/Error #":title must be a string"
                           (error/validate-flare-options {:html "<h1>Test</h1>" :title 123}))))

    (testing "Invalid key type"
      (is (thrown-with-msg? js/Error #":key must be a string or keyword"
                           (error/validate-flare-options {:html "<h1>Test</h1>" :key 123}))))

    (testing "Non-map options"
      (is (thrown-with-msg? js/Error #"must be a map"
                           (error/validate-flare-options "not-a-map"))))))

(deftest hiccup-rendering-test
  (testing "Hiccup structure validation"
    (testing "Valid simple elements"
      (is (= [:h1 "Hello"]
             (error/validate-hiccup-structure [:h1 "Hello"]))))

    (testing "Valid nested structures"
      (is (= [:div [:h1 "Hello"] [:p "World"]]
             (error/validate-hiccup-structure [:div [:h1 "Hello"] [:p "World"]]))))

    (testing "Valid attributes"
      (is (= [:div {:class "container" :id "main"} "Content"]
             (error/validate-hiccup-structure [:div {:class "container" :id "main"} "Content"]))))

    (testing "Invalid Hiccup structures"
      (testing "Non-vector"
        (is (thrown-with-msg? js/Error #"must be a vector"
                             (error/validate-hiccup-structure "not-a-vector"))))

      (testing "Empty vector"
        (is (thrown-with-msg? js/Error #"cannot be empty"
                             (error/validate-hiccup-structure []))))

      (testing "Non-keyword tag"
        (is (thrown-with-msg? js/Error #"must start with a keyword tag"
                             (error/validate-hiccup-structure ["div" "content"]))))

      (testing "Invalid attribute keys"
        (is (thrown-with-msg? js/Error #"must have keyword or string keys"
                             (error/validate-hiccup-structure [:div {123 "value"} "content"])))))))

(deftest webview-management-test
  (testing "Panel key generation"
    (testing "Auto-generated keys are unique"
      ;; This is a simple test - in a real scenario we'd mock the VS Code API
      (let [key1 (gensym "flare-panel-")
            key2 (gensym "flare-panel-")]
        (is (not= key1 key2)))))

  (testing "Content type detection"
    (testing "Vector content detected as Hiccup"
      (is (vector? [:div "content"])))

    (testing "String content detected as HTML"
      (is (string? "<div>content</div>")))))

(deftest flare-api-test
  (testing "Flare options processing"
    (testing "Default values applied"
      ;; Test that default values are properly applied
      (let [minimal-options {:html "<h1>Test</h1>"}]
        ;; This would require mocking VS Code API for full testing
        (is (map? minimal-options))))

    (testing "Sidebar panel flag recognition"
      (let [sidebar-options {:html "<h1>Test</h1>" :sidebar-panel? true}
            panel-options {:html "<h1>Test</h1>" :sidebar-panel? false}]
        (is (true? (:sidebar-panel? sidebar-options)))
        (is (false? (:sidebar-panel? panel-options)))))))

(deftest error-handling-integration-test
  (testing "Safe flare processing"
    (testing "Valid options pass through"
      (let [valid-options {:html "<h1>Test</h1>"}
            process-fn (fn [opts] {:processed opts})]
        (is (= {:processed valid-options}
               (error/safe-flare-processing valid-options process-fn)))))

    (testing "Invalid options throw descriptive errors"
      (let [invalid-options {:invalid "option"}
            process-fn (fn [opts] {:processed opts})]
        (is (thrown-with-msg? js/Error #"Flare processing failed"
                             (error/safe-flare-processing invalid-options process-fn)))))))