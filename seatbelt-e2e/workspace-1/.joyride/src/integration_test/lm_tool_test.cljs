(ns integration-test.lm-tool-test
  "The Language Model Tool, reached through Joyride's MCP server, with the promise printer collision simulated."
  (:require
   [cljs.test :refer [async is testing use-fixtures]]
   [clojure.string :as string]
   [integration-test.collision :as collision]
   [integration-test.macros :refer [deftest-async]]
   [integration-test.mcp-client :as mcp-client]
   [promesa.core :as p]
   ["vscode" :as vscode]))

(def !window (atom nil))

(use-fixtures :once
  {:before #(async done
                   (-> (p/let [window (mcp-client/start-server+)]
                         (reset! !window window))
                       (p/catch (fn [e] (js/console.error "MCP server didn't start:" e)))
                       (p/finally (fn [_ _] (done)))))
   :after #(async done
                  (-> (vscode/commands.executeCommand "joyride.stopMcpServer")
                      (p/finally (fn [_ _] (done)))))})

(defn- evaluate+
  "Evaluates `code` with the tool, under the simulated collision, with `opts` as further tool arguments."
  ([code] (evaluate+ code {}))
  ([code opts]
   (collision/with-collision+
     #(mcp-client/evaluate+ @!window (merge {:code code :who "lm-tool-test"} opts)))))

(defn- root-print-fn+
  "Resolves to SCI's root `*print-fn*`, read outside the evaluation's own bindings."
  []
  (p/then (p/delay 0) (fn [_] *print-fn*)))

(deftest-async awaited-promise
  (testing "An awaited promise's value comes back as #<Promise …>"
    (p/let [result (evaluate+ "(js/Promise.resolve [1 (js/Promise.resolve 2)])" {:awaitResult true})]
      (is (= "success" (:type result)) (str "Succeeds: " result))
      (is (= "#<Promise [1 #<js/Promise[~]>]>" (:result result))
          "The value prints, with the promise inside it"))))

(deftest-async awaited-value-that-is-not-a-promise
  (testing "An awaited value that isn't a promise prints as itself"
    (p/let [result (evaluate+ "(+ 1 2)" {:awaitResult true})]
      (is (= "3" (:result result)) (str "Prints as itself: " result)))))

(deftest-async fire-and-forget-promises
  (testing "A promise that isn't awaited gives the not awaited result, also when it rejects"
    (p/let [resolving (evaluate+ "(js/Promise.resolve 1)")
            rejecting (evaluate+ "(js/Promise.reject (js/Error. \"late\"))")]
      (is (string/includes? (:result resolving) "fire-and-forget")
          (str "Not awaited: " resolving))
      (is (string/includes? (:result rejecting) "fire-and-forget")
          (str "Not awaited, also when it rejects later: " rejecting)))))

(deftest-async results-within-limits
  (testing "Long and circular values print within the result limits"
    (p/let [long-result (evaluate+ "(range 100)")
            circular (evaluate+ "(let [o #js {:a 1}] (set! (.-self o) o) o)")]
      (is (string/ends-with? (:result long-result) "...)")
          (str "A long collection is cut: " long-result))
      (is (= "success" (:type circular))
          (str "A circular object prints: " circular))
      (is (string/starts-with? (str (:result circular)) "#js {:a 1, :self #js {:a 1")
          "It prints down to a limited depth"))))

(deftest-async code-runs-once
  (testing "Awaited code runs exactly once"
    (set! (.-joyrideLmToolRuns js/globalThis) 0)
    (p/let [_ (evaluate+ "(do (set! (.-joyrideLmToolRuns js/globalThis) (inc (.-joyrideLmToolRuns js/globalThis))) (js/Promise.resolve :ran))"
                         {:awaitResult true})]
      (is (= 1 (.-joyrideLmToolRuns js/globalThis))
          "The code ran once"))))

(deftest-async awaited-rejections
  (testing "Awaited rejections come back as errors, with output from before them, and leave printing in place"
    (p/let [print-fn-before (root-print-fn+)
            error-rejection (evaluate+ "(do (println \"before\") (js/Promise.reject (js/Error. \"boom\")))" {:awaitResult true})
            string-rejection (evaluate+ "(js/Promise.reject \"just a string\")" {:awaitResult true})
            nil-rejection (evaluate+ "(js/Promise.reject nil)" {:awaitResult true})
            print-fn-after (root-print-fn+)]
      (is (= ["error" "boom" "before\n"]
             ((juxt :type :error :stdout) error-rejection))
          (str "An Error rejection, with output: " error-rejection))
      (is (= "just a string" (:error string-rejection))
          (str "A string rejection: " string-rejection))
      (is (= "nil" (:error nil-rejection))
          (str "A nil rejection: " nil-rejection))
      (is (identical? print-fn-before print-fn-after)
          "Printing goes where it went before"))))
