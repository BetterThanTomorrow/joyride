(ns joyride.extension-host-tests
  "Tests for `joyride.extension` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.extension-host-tests)`."
  (:require [cljs.test :refer [async deftest is use-fixtures]]
            [clojure.string :as str]
            [joyride.extension :as extension]
            [joyride.integrations.calva :as calva]
            [joyride.output :as output]
            [joyride.sci :as jsci]))

(def ^:private !calva-state (atom nil))

(use-fixtures :once
  {:before (fn []
             (reset! !calva-state calva/state)
             (set! calva/state (constantly :absent)))
   :after (fn []
            (set! calva/state @!calva-state))})

(defn- run-code-err-lines+
  "Runs `code` with Run Code and resolves to the error lines it appends to Joyride Output."
  [code]
  (let [!lines (atom [])
        eval-err output/append-line-eval-err!
        other-err output/append-line-other-err!
        reveal output/reveal!
        restore! (fn []
                   (set! output/append-line-eval-err! eval-err)
                   (set! output/append-line-other-err! other-err)
                   (set! output/reveal! reveal))]
    (set! output/append-line-eval-err! (fn ([m] (swap! !lines conj m)) ([m _] (swap! !lines conj m))))
    (set! output/append-line-other-err! (fn ([m] (swap! !lines conj m)) ([m _] (swap! !lines conj m))))
    (set! output/reveal! (fn [] nil))
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
   ["(throw #js {:message 42})" "#js {:message 42}"]
   ["(js/Promise.reject #js {:message 42})" "#js {:message 42}"]
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

(defn- echoed-results+
  "Evaluates each of `codes` with `joyride.sci/eval-string` and resolves to the results it echoes to Joyride Output."
  [codes]
  (let [!echoed (atom [])
        append-eval-result output/append-eval-result!
        append-promise-result output/append-promise-result!]
    (set! output/append-eval-result! (fn ([m] (swap! !echoed conj m)) ([m _] (swap! !echoed conj m))))
    (set! output/append-promise-result! (fn ([m] (swap! !echoed conj (str "#<Promise " m ">"))) ([m _] (swap! !echoed conj (str "#<Promise " m ">")))))
    (-> (js/Promise.all (into-array (map jsci/eval-string codes)))
        (.then (fn [_] @!echoed))
        (.finally (fn []
                    (set! output/append-eval-result! append-eval-result)
                    (set! output/append-promise-result! append-promise-result))))))

(deftest promise-echo-within-result-limits
  (async done
    (-> (echoed-results+ ["(vec (range 30))"
                          "(js/Promise.resolve (vec (range 100)))"
                          "(js/Promise.resolve [[[[[[[[[1]]]]]]]]])"
                          "(js/Promise.resolve (range))"])
        (.then (fn [echoed]
                 (is (= ["[0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29]"
                         "#<Promise [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 ...]>"
                         "#<Promise [[[[[[[#]]]]]]]>"
                         "#<Promise (0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 ...)>"]
                        echoed)
                     "A value that isn't a promise echoes in full, and a promise's value within the result limits")))
        (.catch (fn [e] (is (nil? e) "The evaluations settle")))
        (.finally done))))

(defn- terminal-lines+
  "Evaluates each of `codes` with `joyride.sci/eval-string` and resolves to the lines it appends to Joyride Output, without colors."
  [codes]
  (let [!lines (atom [])
        append-line output/append-line!]
    (set! output/append-line! #(swap! !lines conj (str/replace % #"\u001b\[[0-9;]*m" "")))
    (-> (js/Promise.all (into-array (map jsci/eval-string codes)))
        (.then (fn [_] @!lines))
        (.finally #(set! output/append-line! append-line)))))

(deftest promise-echo-on-one-line
  (async done
    (-> (terminal-lines+ ["(js/Promise.resolve [1 2])"
                          "(js/Promise.resolve {:a 1})"
                          "(js/Promise.resolve 42)"])
        (.then (fn [lines]
                 (is (= ["#<Promise [1 2]>" "#<Promise {:a 1}>" "#<Promise 42>"]
                        lines)
                     "A promise's echo keeps its closing > with the value")))
        (.catch (fn [e] (is (nil? e) "The evaluations settle")))
        (.finally done))))
