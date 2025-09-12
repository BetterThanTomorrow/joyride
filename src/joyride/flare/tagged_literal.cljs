(ns joyride.flare.tagged-literal
  (:require
   [joyride.flare :as flare]))

(defn joyride-flare-reader
  "Tagged literal reader for #joyride/flare"
  [flare-data]
  (flare/process-flare-request! flare-data))
