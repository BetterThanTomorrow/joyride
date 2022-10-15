(ns html-to-hiccup
  (:require ["posthtml-parser" :as parser]
            [clojure.walk :as walk]))

(comment
  (-> "<label for=\"hw\">Foo</label><ul id=\"foo\"><li>Hello</li></ul>"
      (parser/parser)
      (js->clj :keywordize-keys true)
      (->> (into [:div])
           (walk/postwalk
            (fn [{:keys [tag attrs content] :as element}]
              (if tag
                (into [(keyword tag) (or attrs {})] content)
                element))))) ;=> [:div [:label {:for "hw"} "Foo"] [:ul {:id "foo"} [:li {} "Hello"]]]
  )