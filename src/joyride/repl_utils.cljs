(ns joyride.repl-utils
  (:require
   [clojure.string :as str]
   [goog.object :as gobject]
   [sci.core :as sci]))

(defn the-sci-ns [ctx ns-sym]
  (sci/eval-form ctx (list 'clojure.core/the-ns (list 'quote ns-sym))))

(defn format [fmt-str x]
  (str/replace fmt-str "%s" x))

(defn fully-qualified-syms [ctx ns-sym]
  (let [syms (sci/eval-string* ctx (format "(keys (ns-map '%s))" ns-sym))
        sym-strs (map #(str "`" %) syms)
        sym-expr (str "[" (str/join " " sym-strs) "]")
        syms (sci/eval-string* ctx sym-expr)
        syms (remove #(str/starts-with? (str %) "nbb.internal") syms)]
    syms))

(defn match [_alias->ns ns->alias query [sym-ns sym-name qualifier]]
  (let [pat (re-pattern query)]
    (or (when (and (= :unqualified qualifier) (re-find pat sym-name))
          [sym-ns sym-name])
        (when sym-ns
          (or (when (re-find pat (str (get ns->alias (symbol sym-ns)) "/" sym-name))
                [sym-ns (str (get ns->alias (symbol sym-ns)) "/" sym-name)])
              (when (re-find pat (str sym-ns "/" sym-name))
                [sym-ns (str sym-ns "/" sym-name)]))))))

(defn- instance-completions [instance prefix]
  (let [props (loop [obj instance
                     props []]
                (if obj
                  (recur (js/Object.getPrototypeOf obj)
                         (into props (js/Object.getOwnPropertyNames obj)))
                  props))
        completions (map (fn [k]
                           [nil (str prefix k)]) props)]
    completions))

(defn instance-properties
  "Returns properties of `instance`, attempting to correspond with what `node:repl.completer` would return."
  [instance]
  (->> (instance-completions instance nil)
       (map second)
       (remove #{"__lookupGetter__" "__defineSetter__"
                 "__lookupSetter__" "__defineGetter__"})))

(defn ns-imports->completions [sci-ctx query-ns query]
  (let [[_ns-part name-part] (str/split query #"/")
        resolved (sci/eval-string* sci-ctx
                                   (pr-str `(let [resolved# (resolve '~query-ns)]
                                              (when-not (var? resolved#)
                                                resolved#))))]
    (when resolved
      (when-let [[prefix imported] (if name-part
                                     (let [ends-with-dot? (str/ends-with? name-part ".")
                                           fields (str/split name-part #"\.")
                                           fields (if ends-with-dot?
                                                    fields
                                                    (butlast fields))]
                                       [(str query-ns "/" (when (seq fields)
                                                            (let [joined (str/join "." fields)]
                                                              (str joined "."))))
                                        (apply gobject/getValueByKeys resolved
                                               fields)])
                                     [(str query-ns "/") resolved])]
        (instance-completions imported prefix)))))

(defn handle-complete* [{ns-str :ns
                         :keys [sci-ctx]
                         :as request}]
  (try
    (let [sci-ns (when ns-str
                   (the-sci-ns sci-ctx (symbol ns-str)))]
      (sci/binding [sci/ns (or sci-ns @sci/ns)]
        (if-let [query (or (:symbol request)
                           (:prefix request))]
          (let [has-namespace? (str/includes? query "/")
                query-ns (when has-namespace? (some-> (str/split query #"/")
                                                      first symbol))
                from-current-ns (fully-qualified-syms sci-ctx (sci/eval-string* sci-ctx "(ns-name *ns*)"))
                from-current-ns (map (fn [sym]
                                       [(namespace sym) (name sym) :unqualified])
                                     from-current-ns)
                alias->ns (sci/eval-string* sci-ctx "(let [m (ns-aliases *ns*)] (zipmap (keys m) (map ns-name (vals m))))")
                ns->alias (zipmap (vals alias->ns) (keys alias->ns))
                from-aliased-nss (doall (mapcat
                                         (fn [alias]
                                           (let [ns (get alias->ns alias)
                                                 syms (sci/eval-string* sci-ctx (format "(keys (ns-publics '%s))" ns))]
                                             (map (fn [sym]
                                                    [(str ns) (str sym) :qualified])
                                                  syms)))
                                         (keys alias->ns)))
                all-namespaces (->> (sci/eval-string* sci-ctx "(all-ns)")
                                    (map (fn [ns]
                                           [(str ns) nil :qualified])))
                from-imports (when has-namespace? (ns-imports->completions sci-ctx query-ns query))
                fully-qualified-names (when-not from-imports
                                        (when has-namespace?
                                          (let [ns (get alias->ns query-ns query-ns)
                                                syms (sci/eval-string* sci-ctx (format "(and (find-ns '%s)
                                                                                         (keys (ns-publics '%s)))"
                                                                                       ns))]
                                            (map (fn [sym]
                                                   [(str ns) (str sym) :qualified])
                                                 syms))))
                svs (concat from-current-ns from-aliased-nss all-namespaces fully-qualified-names)
                completions (keep (fn [entry]
                                    (match alias->ns ns->alias query entry))
                                  svs)
                completions (concat completions from-imports)
                completions (->> (map (fn [[namespace name]]
                                        (cond-> {"candidate" (str name)}
                                          namespace (assoc "ns" (str namespace))))
                                      completions)
                                 distinct vec)]
            {"completions" completions
             "status" ["done"]})
          {"status" ["done"]})))
    (catch :default e
      (js/console.warn e)
      {"completions" []
       "status" ["done"]})))
