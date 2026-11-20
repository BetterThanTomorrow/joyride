(ns joyride.printing
  "Prints evaluation results without throwing, and without dispatching through `Promise.prototype`."
  (:require [cljs.pprint :as pp]
            [goog.string :as gstring]))

(def ^:private default-print-length 25)
(def ^:private default-print-level 7)
(def ^:private fallback-max-chars 10000)

(defn- limit
  "Returns `n` when it is a positive number, otherwise nil, which means no limit."
  [n]
  (when (and (number? n) (pos? n)) n))

(defn promise-alt-impl
  "Print `:alt-impl` that writes a native promise as `#<js/Promise[~]>` and hands other values to the normal printer."
  [obj writer opts]
  (if (instance? js/Promise obj)
    (-write writer "#<js/Promise[~]>")
    ((:fallback-impl opts) obj writer opts)))

(defn- print-opts
  "Returns printer options from the current print bindings, with `promise-alt-impl`."
  []
  {:flush-on-newline *flush-on-newline*
   :readably *print-readably*
   :meta *print-meta*
   :dup *print-dup*
   :print-length *print-length*
   :alt-impl promise-alt-impl})

(deftype ^:private CappedWriter [^js sb cap ^:mutable written]
  IWriter
  (-write [_ s]
    (let [room (- cap written)]
      (if (<= (count s) room)
        (do (.append sb s)
            (set! written (+ written (count s))))
        (do (.append sb (subs s 0 room))
            (set! written cap)
            (throw (ex-info "Print size cap reached" {::capped true}))))))
  (-flush [_] nil))

(defn- pr-str-limited
  [x print-length print-level]
  (binding [*print-length* (limit print-length)
            *print-level* (limit print-level)]
    (pr-str-with-opts [x] (print-opts))))

(defn- pr-str-capped
  "Prints `x` with both limits and at most `fallback-max-chars` characters, ending a cut print with `...`."
  [x print-length print-level]
  (let [sb (gstring/StringBuffer.)]
    (binding [*print-length* print-length
              *print-level* print-level]
      (try
        (pr-seq-writer [x] (CappedWriter. sb fallback-max-chars 0) (print-opts))
        (str sb)
        (catch :default e
          (if (::capped (ex-data e))
            (str sb "...")
            (throw e)))))))

(defn value->str
  "Returns `x` printed readably, with native promises as `#<js/Promise[~]>`. Never throws.
   A limit of nil or 0 means no limit. When printing throws, `x` is printed again with the
   fallback limits, defaulting to 25 items and depth 7, and capped in size. When that
   throws too, returns `#<unprintable: <message>>`."
  [x {:keys [print-length print-level fallback-length fallback-level]}]
  (try
    (pr-str-limited x print-length print-level)
    (catch :default _
      (try
        (pr-str-capped x
                       (or (limit fallback-length) default-print-length)
                       (or (limit fallback-level) default-print-level))
        (catch :default e
          (str "#<unprintable: " (or (some-> e .-message) (str e)) ">"))))))

(defn error-message
  "Returns the message of the thrown or rejected value `e`: an error's message when it is a non-empty
   string, else its cause's message, a string as is, or anything else printed."
  [e]
  (let [message (some-> e .-message)]
    (cond
      (and (string? message) (seq message)) message
      (ex-cause e) (error-message (ex-cause e))
      (string? e) e
      :else (value->str e {}))))

(defn pprint-dispatch
  "`cljs.pprint` dispatch that pretty prints Clojure collections, and prints every other value the
   way `pr` does within the current print bindings, with native promises as `#<js/Promise[~]>`.
   When that printing throws, the value is printed with `value->str`."
  [x]
  (if (coll? x)
    (pp/simple-dispatch x)
    (-write *out* (try
                    (pr-str-with-opts [x] (print-opts))
                    (catch :default _
                      (value->str x {:print-length *print-length*
                                     :print-level *print-level*}))))))
