(ns integration-test.tap-test
  (:require [cljs.test :refer [deftest testing is async]]
            [promesa.core :as p]))

(def !tap (atom []))

(defn tap-fn! [x]
  (swap! !tap conj x))

(defn tap>+ [x]
  (p/create (fn [resolve _reject]
              (let [result (tap> x)]
                (js/setTimeout (fn [] (resolve result)) 10)))))

(deftest tap
  (testing "taps to our atom"
    (async done
           (-> (p/do!
                (add-tap tap-fn!)
                (reset! !tap [])
                (p/let [result+ (tap>+ :tapped)]
                  (is (boolean? result+))
                  (is (= [:tapped] @!tap))
                  (remove-tap tap-fn!)
                  (reset! !tap [])
                  result+))
               (p/finally
                 done)))))
