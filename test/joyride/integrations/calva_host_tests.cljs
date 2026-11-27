(ns joyride.integrations.calva-host-tests
  "Tests for `joyride.integrations.calva` that need the Extension Development Host.
   Run them from the Extension Development Host REPL with
   `(cljs.test/run-tests 'joyride.integrations.calva-host-tests)`."
  (:require [cljs.test :refer [deftest is]]
            [joyride.integrations.calva :as calva]))

(defn- extension
  "Returns a stand-in for the Calva extension. Reading `exports` throws when it isn't `active?`, as VS Code's does."
  [active?]
  (let [ext #js {:isActive active?}]
    (js/Object.defineProperty ext "exports"
                              #js {:get (fn []
                                          (if active?
                                            #js {:v1 #js {:repl #js {:log identity}}}
                                            (throw (js/Error. "Extension 'betterthantomorrow.calva' is not known or not activated"))))})
    ext))

(deftest calva-state
  (is (= :absent (calva/state-of nil))
      "No extension is absent")
  (is (= :inactive (calva/state-of (extension false)))
      "An extension that hasn't started is inactive")
  (is (= :active (calva/state-of (extension true)))
      "A started extension is active"))

(deftest repl-api-reads-exports-only-when-active
  (is (nil? (calva/repl-api (extension false)))
      "An inactive Calva gives no API, without reading its exports")
  (is (nil? (calva/repl-api nil))
      "An absent Calva gives no API")
  (is (fn? (some-> (calva/repl-api (extension true)) .-log))
      "An active Calva gives its repl API"))
