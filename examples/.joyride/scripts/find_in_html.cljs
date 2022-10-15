(ns find-in-html
  (:require ["posthtml-parser" :as parser]
            [z-joylib.hickory.select :as hs]))
(comment
  (-> "<div>
       <label class=\"hw\">hello</label>
       <ul id=\"foo\">
         <li class=\"hw\">Hello World</li>
       </ul>
    </div>"
      parser/parser
      (js->clj :keywordize-keys true)
      first
      (->> (hs/select (hs/attr :class #(= "hw" %)))
           (mapv #(-> % (get-in [:content 0])))))
  )