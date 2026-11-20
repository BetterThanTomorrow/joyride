(ns joyride.printing-test
  (:require [cljs.pprint :as pp]
            [cljs.test :refer [deftest is testing]]
            [joyride.printing :as printing]
            [promesa.core]))

(def ^:private collision-error "No protocol method IWriter.-write defined for type object: [object Object]")

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
   the error another extension's printer throws, and restores them afterwards.
   Throws when `f` changed `Promise.prototype`."
  [f]
  (let [proto (.-prototype js/Promise)
        names-before (set (js/Object.getOwnPropertyNames proto))
        originals (into {} (map (juxt identity #(aget proto %))) (promise-printer-props))
        breaker (fn [_ _] (throw (js/Error. collision-error)))]
    (doseq [k (keys originals)]
      (aset proto k breaker))
    (try
      (f)
      (finally
        (let [untouched? (and (every? #(identical? breaker (aget proto %)) (keys originals))
                              (= names-before (set (js/Object.getOwnPropertyNames proto))))]
          (doseq [[k v] originals]
            (aset proto k v))
          (when-not untouched?
            (throw (js/Error. "Promise.prototype was changed while the collision was simulated"))))))))

(defn- circular-array []
  (let [a (js/Array. 10)]
    (dotimes [i 10] (aset a i a))
    a))

(defrecord Point [x y])

(def ^:private samples
  [(range 30)
   {1 :a 2 :b 3 :c}
   #{1 2 3}
   #js {:a 1 :b #js [1 2]}
   #js [1 "s" nil]
   (js/Date. 0)
   inc
   (->Point 1 2)
   #"re.*x"
   (uuid "00000000-0000-0000-0000-000000000001")
   ##NaN
   "esc\"aped\nline"
   (vec (range 60))
   {:k (vec (repeat 12 "a-long-string"))}])

(deftest collision-simulation
  (is (seq (promise-printer-props))
      "The promise printer is found on Promise.prototype")
  (is (thrown? js/Error (with-collision #(pr-str (js/Promise.resolve 1))))
      "pr-str throws on a promise under the simulated collision")
  (is (thrown-with-msg? js/Error #"Promise.prototype was changed"
        (with-collision #(aset (.-prototype js/Promise) "joyride_test_prop" 1)))
      "A change to Promise.prototype during the simulation is caught")
  (js-delete (.-prototype js/Promise) "joyride_test_prop"))

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

(deftest value->str-prints-like-pr-str
  (doseq [x samples]
    (is (= (pr-str x) (printing/value->str x {}))
        (str "Prints like pr-str: " (pr-str x))))
  (is (= "[\"s\" :k nil]" (printing/value->str ["s" :k nil] {}))
      "Strings stay quoted"))

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

(deftest value->str-never-throws
  (let [o #js {}]
    (js/Object.defineProperty o "bad" #js {:enumerable true
                                           :get (fn [] (throw (js/Error. "nope")))})
    (is (= "#<unprintable: nope>" (printing/value->str o {}))
        "A value that can't be printed gives a marker")))

(deftest error-message-of-thrown-values
  (is (= "boom" (printing/error-message (js/Error. "boom")))
      "An error gives its message")
  (is (= "boom" (printing/error-message "boom"))
      "A string is the message")
  (is (= "nil" (printing/error-message nil))
      "nil prints as nil")
  (is (= "{:a 1}" (printing/error-message {:a 1}))
      "Other values print")
  (is (= "thrown string" (printing/error-message (ex-info nil {} "thrown string")))
      "An error without a message gives its cause's message")
  (is (= "the cause" (printing/error-message (ex-info "" {} (js/Error. "the cause"))))
      "An error with an empty message gives its cause's message")
  (is (= "#js {:message 42}" (printing/error-message #js {:message 42}))
      "A value whose message is a number prints")
  (is (= "#js {:message #js {}}" (printing/error-message #js {:message #js {}}))
      "A value whose message is an object prints")
  (is (= "#js {:message 42}" (printing/error-message (ex-info 42 {} #js {:message 42})))
      "An error whose message is a number gives its cause"))

(defn- pprint-str [x]
  (with-out-str (pp/write x :dispatch printing/pprint-dispatch)))

(deftest pprint-dispatch-prints-like-default-dispatch
  (doseq [x samples]
    (is (= (with-out-str (pp/write x)) (pprint-str x))
        (str "Pretty prints like the default dispatch: " (pr-str x))))
  (testing "With print limits bound"
    (doseq [[length level] [[3 2] [0 nil]]]
      (binding [*print-length* length
                *print-level* level]
        (doseq [x [(range 30) [[[1]]] {:k (vec (repeat 12 "a-long-string"))} #js {:a 1} (atom 1)]]
          (is (= (with-out-str (pp/write x)) (pprint-str x))
              (str "Pretty prints like the default dispatch at length " length ": " (pr-str x))))))))

(deftest pprint-dispatch-promises
  (let [p (js/Promise.resolve 1)]
    (with-collision
      (fn []
        (is (= "[1 #<js/Promise[~]> {:k #<js/Promise[~]>}]" (pprint-str [1 p {:k p}]))
            "Promises in Clojure collections print")
        (is (= "[#js {:x #<js/Promise[~]>}\n #object[cljs.core.Atom {:val #<js/Promise[~]>}]]"
               (pprint-str [#js {:x p} (atom p)]))
            "Promises in #js objects and atoms print")))
    (is (= "[#js [1 2 ...]]" (binding [*print-length* 2] (pprint-str [#js [1 2 3 4]])))
        "The print length reaches #js values")))
