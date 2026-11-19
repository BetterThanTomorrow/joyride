(ns integration-test.collision
  "Simulates another extension's build replacing promesa's promise printer on `Promise.prototype`."
  (:require [promesa.core :as p]))

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

(defn- restore-printers!
  "Puts the `originals` back on `proto`. Throws when `proto` was changed while the collision was simulated."
  [proto originals breaker names-before]
  (let [untouched? (and (every? #(identical? breaker (aget proto %)) (keys originals))
                        (= names-before (set (js/Object.getOwnPropertyNames proto))))]
    (doseq [[k v] originals]
      (aset proto k v))
    (when-not untouched?
      (throw (js/Error. "Promise.prototype was changed while the collision was simulated")))))

(defn with-collision+
  "Calls `f`, which returns a promise, with every promise printer on `Promise.prototype` replaced
   by one that throws the error another extension's printer throws, for at most 10 seconds.
   Restores the printers when the promise settles, and rejects when `Promise.prototype` was
   changed meanwhile, or when no printer is found, since nothing would be simulated."
  [f]
  (let [proto (.-prototype js/Promise)
        names-before (set (js/Object.getOwnPropertyNames proto))
        originals (into {} (map (juxt identity #(aget proto %))) (promise-printer-props))
        breaker (fn [_ _] (throw (js/Error. collision-error)))]
    (if (empty? originals)
      (p/rejected (js/Error. "No promise printer found on Promise.prototype, so the collision can't be simulated"))
      (do
        (doseq [k (keys originals)]
          (aset proto k breaker))
        (-> (p/do (f))
            (p/timeout 10000)
            (p/finally (fn [_ _]
                         (restore-printers! proto originals breaker names-before))))))))

(defn printer-found?
  "Returns true when promesa's promise printer is on `Promise.prototype`."
  []
  (boolean (seq (promise-printer-props))))
