(ns joyride.scripts-handler-test
  (:require [clojure.test :refer [testing is deftest]]
            [joyride.scripts-handler :as sh]))

(deftest handle-script-menu-selection+
  (testing ""
    (is (= nil
           (sh/handle-script-menu-selection+ nil nil nil nil))))) ;; I need your help to define the parameters :D
