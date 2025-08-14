(ns joyride.balance
  (:require ["parinfer" :as parinfer]))

(defn infer-parens
  [code]
  (some-> (parinfer/indentMode code #js {:partialResult true})
          (js->clj :keywordize-keys true)))
