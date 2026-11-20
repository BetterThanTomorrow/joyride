(ns joyride.extension-host-tests
  "Tests for `joyride.extension` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.extension-host-tests)`."
  (:require [cljs.test :refer [async deftest is]]
            [joyride.extension :as extension]
            [joyride.output :as output]))

(defn- run-code-err-lines+
  "Runs `code` with Run Code and resolves to the error lines it appends to Joyride Output."
  [code]
  (let [!lines (atom [])
        eval-err output/append-line-eval-err!
        other-err output/append-line-other-err!
        show-terminal output/show-terminal!
        restore! (fn []
                   (set! output/append-line-eval-err! eval-err)
                   (set! output/append-line-other-err! other-err)
                   (set! output/show-terminal! show-terminal))]
    (set! output/append-line-eval-err! #(swap! !lines conj %))
    (set! output/append-line-other-err! #(swap! !lines conj %))
    (set! output/show-terminal! (fn [& _] nil))
    (-> (extension/run-code+ code)
        (.then (constantly nil) (constantly nil))
        (.then #(js/Promise. (fn [resolve] (js/setTimeout resolve 20))))
        (.then (fn [_] @!lines))
        (.finally restore!))))

(def ^:private failures
  [["(js/Promise.reject nil)" "nil"]
   ["(js/Promise.reject (js/Error. \"boom\"))" "boom"]
   ["(throw (ex-info \"sync\" {:a 1}))" "sync"]
   ["(throw \"a string\")" "a string"]
   ["(inc" "EOF while reading, expected ) to match ( at [1,1]"]])

(deftest run-code-prints-each-failure-once
  (async done
    (-> (reduce (fn [acc [code expected]]
                  (.then acc (fn [_]
                               (.then (run-code-err-lines+ code)
                                      (fn [lines]
                                        (is (= [expected] lines)
                                            (str code " appends one error line")))))))
                (js/Promise.resolve nil)
                failures)
        (.finally done))))
