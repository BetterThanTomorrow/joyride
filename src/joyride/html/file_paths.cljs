(ns joyride.html.file-paths
  (:require [clojure.string :as string]))

(def ^:private file-path-attr-keys
  #{:action
    :background
    :data
    :formaction
    :href
    :poster
    :src
    :xlink:href})

(defn- looks-like-file-path?
  [s]
  (let [candidate (some-> s string/trim)]
    (boolean (and candidate
                  (not (string/blank? candidate))
                  (not (string/starts-with? candidate "#"))
                  (not (string/starts-with? candidate "?"))
                  (not (re-find #"^[a-zA-Z][a-zA-Z0-9+.-]*:" candidate))
                  (not (string/starts-with? candidate "//"))))))

(defn- transform-path-if-needed
  [transform value]
  (if (and transform (string? value))
    (let [trimmed (string/trim value)]
      (if (looks-like-file-path? trimmed)
        (transform trimmed)
        value))
    value))

(defn- transform-srcset
  [transform value]
  (if (and transform (string? value))
    (->> (string/split value #",")
         (map string/trim)
         (remove string/blank?)
         (map (fn [entry]
                (let [[path descriptor] (string/split entry #"\\s+" 2)
                      updated (transform-path-if-needed transform path)]
                  (if descriptor
                    (str updated " " descriptor)
                    updated))))
         (string/join ", "))
    value))

(defn- transform-style-string
  [transform value]
  (if (and transform (string? value))
    (.replace value (js/RegExp. "url\\((['\"]?)([^'\")]+)\\1\\)" "gi")
              (fn [match quote path]
                (let [quote (or quote "")
                      trimmed (string/trim path)
                      updated (transform-path-if-needed transform trimmed)]
                  (if (= updated trimmed)
                    match
                    (str "url(" quote updated quote ")")))))
    value))

(defn- transform-style
  [transform style]
  (cond
    (not transform) style
    (string? style) (transform-style-string transform style)
    (map? style) (into {}
                       (map (fn [[k v]]
                              [k (if (string? v)
                                   (transform-style-string transform v)
                                   v)]))
                       style)
    :else style))

(defn transform-file-attrs
  [transform attrs]
  (if (and attrs transform)
    (reduce-kv (fn [acc k v]
                 (assoc acc k
                        (cond
                          (= k :style) (transform-style transform v)
                          (= k :srcset) (transform-srcset transform v)
                          (file-path-attr-keys k) (transform-path-if-needed transform v)
                          :else v)))
               {} attrs)
    attrs))

(defn transform-file-paths-in-hiccup
  "Apply `transform` to file path strings within the provided hiccup form."
  [transform hiccup]
  (letfn [(walk [node]
            (cond
              (vector? node)
              (let [[tag & body] node
                    [attrs body] (if (map? (first body))
                                   [(first body) (rest body)]
                                   [nil body])
                    transformed-attrs (when attrs (transform-file-attrs transform attrs))
                    transformed-children (map walk body)]
                (into [tag]
                      (concat (when transformed-attrs [transformed-attrs])
                              transformed-children)))

              (seq? node)
              (if (= 'comment (first node))
                node
                (map walk node))

              :else node))]
    (if (and transform hiccup)
      (walk hiccup)
      hiccup)))
