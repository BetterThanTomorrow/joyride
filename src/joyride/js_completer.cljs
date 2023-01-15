(ns joyride.js-completer
  (:require [promesa.core :as p]
            ["repl" :as node-repl]
            ["vscode" :as vscode]))

(defn complete+
  "Returns a promise resolving to the completion results of `s` given a JavaScript `context`."
  [s ^js context]
  (p/create (fn [resolve reject]
              (let [repl (.start node-repl)]
                (.completer repl
                            s
                            (fn [error result]
                              (if-not error
                                (resolve (->> result
                                              first
                                              js->clj
                                              (remove empty?)))
                                (reject error))
                              (.close repl))
                            (.assign js/Object (.-context repl) context))))))

(defn js-keys+
  "Returns a promise resolving to all keys of `obj` as a Javascript array."
  [^js obj]
  (p/let [o-name (name (gensym))
          prefix (str o-name ".")
          completions+ (complete+ prefix (clj->js {o-name obj}))]
    (->> completions+
         (map (fn [completion]
                (subs completion (count prefix))))
         clj->js)))

(comment
  (p/let [keys+ (js-keys+ (vscode/extensions.getExtension "betterthantomorrow.joyride"))]
    (def keys+ keys+))

  (p/let [completions+ (complete+ "ext." #js {:ext (vscode/extensions.getExtension "betterthantomorrow.joyride")})]
    (def completions+ completions+))
  :rcf)

