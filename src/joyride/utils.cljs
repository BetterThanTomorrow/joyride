(ns joyride.utils)

(defn jsify [clj-thing]
  (clj->js clj-thing))

(defn cljify [js-thing]
  (js->clj js-thing :keywordize-keys true))