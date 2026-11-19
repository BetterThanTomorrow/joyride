(ns joyride.printing-test
  (:require [cljs.pprint :as pp]
            [cljs.test :refer [deftest is]]
            [joyride.printing :as printing]))

(defn- promise-printer-props
  "Returns the names of the promise printer methods on `Promise.prototype`, found by their output text."
  []
  (let [proto (.-prototype js/Promise)]
    (filterv (fn [k]
               (let [v (aget proto k)]
                 (and (fn? v)
                      (.includes (str v) "js/Promise[~]"))))
             (js/Object.getOwnPropertyNames proto))))

(defn- with-collision
  "Calls `f` with every promise printer on `Promise.prototype` replaced by one that throws
   the error another extension's printer throws, and restores them afterwards."
  [f]
  (let [proto (.-prototype js/Promise)
        originals (into {} (map (juxt identity #(aget proto %))) (promise-printer-props))]
    (doseq [k (keys originals)]
      (aset proto k (fn [_ _] (throw (js/Error. "No protocol method IWriter.-write defined for type object: [object Object]")))))
    (try
      (f)
      (finally
        (doseq [[k v] originals]
          (aset proto k v))))))

(defn- circular-array []
  (let [a (js/Array. 10)]
    (dotimes [i 10] (aset a i a))
    a))

(deftest collision-simulation
  (is (seq (promise-printer-props))
      "The promise printer is found on Promise.prototype")
  (is (thrown? js/Error (with-collision #(pr-str (js/Promise.resolve 1))))
      "pr-str throws on a promise under the simulated collision"))

(deftest value->str-promises
  (let [p (js/Promise.resolve 1)]
    (with-collision
      (fn []
        (is (= "#<js/Promise[~]>" (printing/value->str p {}))
            "A top-level promise prints like promesa prints it")
        (is (= "[[1 #<js/Promise[~]> {:k #<js/Promise[~]>}] #js {:x #<js/Promise[~]>}]"
               (printing/value->str [[1 p {:k p}] #js {:x p}] {}))
            "Promises nested in collections and #js objects print")
        (is (= "#object[cljs.core.Atom {:val #<js/Promise[~]>}]"
               (printing/value->str (atom p) {}))
            "A promise in an atom prints")))))

(deftest value->str-printing
  (is (= "[\"s\" :k nil]" (printing/value->str ["s" :k nil] {}))
      "Strings stay quoted")
  (is (= (pr-str {:a [1 2 3] :b #{1}}) (printing/value->str {:a [1 2 3] :b #{1}} {}))
      "Values without promises print as pr-str prints them"))

(deftest value->str-limits
  (is (= "(0 1 2 ...)" (printing/value->str (range 10) {:print-length 3}))
      "The length limit applies")
  (is (= "[[#]]" (printing/value->str [[[1]]] {:print-level 2}))
      "The depth limit applies")
  (is (= (pr-str (range 30)) (printing/value->str (range 30) {:print-length 0 :print-level 0}))
      "Limits of 0 mean no limit")
  (is (= (pr-str (range 30)) (printing/value->str (range 30) {:print-length nil :print-level nil}))
      "Limits of nil mean no limit"))

(deftest value->str-fallback
  (let [circular-obj (let [o #js {:a 1}] (set! (.-self o) o) o)]
    (is (= "#js {:a 1, :self #js {:a 1, :self #js #}}"
           (printing/value->str circular-obj {:fallback-level 2}))
        "A circular object prints down to the fallback depth")
    (is (= 7 (count (re-seq #":self" (printing/value->str circular-obj {}))))
        "The fallback depth defaults to 7")
    (is (> 10010 (count (printing/value->str (circular-array) {:fallback-level 0})))
        "The fallback print is capped in size")))

(deftest error-message-of-thrown-values
  (is (= "boom" (printing/error-message (js/Error. "boom")))
      "An error gives its message")
  (is (= "boom" (printing/error-message "boom"))
      "A string is the message")
  (is (= "nil" (printing/error-message nil))
      "nil prints as nil")
  (is (= "{:a 1}" (printing/error-message {:a 1}))
      "Other values print"))

(deftest pprint-dispatch-printing
  (let [p (js/Promise.resolve 1)
        pprint (fn [x] (with-out-str (pp/write x :dispatch printing/pprint-dispatch)))]
    (is (= (with-out-str (pp/write {:a [1 2 3] :b "s" :c #{1}}))
           (pprint {:a [1 2 3] :b "s" :c #{1}}))
        "Clojure data pretty prints as with the default dispatch")
    (with-collision
      (fn []
        (is (= "[1 #<js/Promise[~]> {:k #<js/Promise[~]>}]" (pprint [1 p {:k p}]))
            "Promises in Clojure collections print")
        (is (= "[#js {:x #<js/Promise[~]>}\n #object[cljs.core.Atom {:val #<js/Promise[~]>}]]"
               (pprint [#js {:x p} (atom p)]))
            "Promises in #js objects and atoms print")))))
