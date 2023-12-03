(ns joyride.sci
  (:require
   ["fs" :as fs]
   ["module" :as module]
   ["path" :as path]
   ["vscode" :as vscode]
   [clojure.string :as str]
   [clojure.zip]
   [goog.object :as gobject]
   [joyride.config :as conf]
   [joyride.db :as db]
   [joyride.repl-utils :as repl-utils]
   [sci.configs.cljs.test :as cljs-test-config]
   [sci.configs.cljs.pprint :as cljs-pprint-config]
   [sci.configs.funcool.promesa :as promesa-config]
   [sci.core :as sci]
   [sci.ctx-store :as store]
   [rewrite-clj.node]
   [rewrite-clj.parser]
   [rewrite-clj.zip]))

(sci/enable-unrestricted-access!) ;; allows mutating and set!-ing all vars from inside SCI
(sci/alter-var-root sci/print-fn (constantly *print-fn*))
(sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*))

(def joyride-ns (sci/create-ns 'joyride.core nil))

(defn ns->path [namespace]
  (-> (str namespace)
      (munge)
      (str/replace  "." "/")
      (str ".cljs")))

(defn source-script-by-ns [namespace]
  (let [ns-path (ns->path namespace)
        path-if-exists (fn [search-path]
                         (let [file-path (path/join search-path ns-path)]
                           (when (fs/existsSync file-path)
                             file-path)))
        ;; workspace first, then user - the and is a nil check for no workspace
        path-to-load (first (keep #(and % (path-if-exists %))
                                  [(conf/workspace-abs-src-path)
                                   (conf/workspace-abs-scripts-path)
                                   (conf/user-abs-src-path)
                                   (conf/user-abs-scripts-path)]))]
    (when path-to-load
      {:file ns-path
       :path-to-load path-to-load
       :source (str (fs/readFileSync path-to-load))})))

(def ^:private extension-namespace-prefix "ext://")

(defn- extract-extension-name [namespace]
  (subs namespace (count extension-namespace-prefix)))

(defn- active-extension? [namespace]
  (when (.startsWith namespace extension-namespace-prefix)
    (let [[extension-name _module-name] (.split (extract-extension-name namespace) "$")
          extension (vscode/extensions.getExtension extension-name)]
      (and extension
           (.-isActive extension)))))

(defn- extension-module [namespace]
  (let [[extension-name module-name] (.split (extract-extension-name namespace) "$")
        extension (vscode/extensions.getExtension extension-name)]
    (when extension
      (when-let [exports (.-exports extension)]
        (if module-name
          (gobject/getValueByKeys exports (.split module-name "."))
          exports)))))

(defn require* [from-ns lib {:keys [reload]}]
  (let [from-path (if (.startsWith lib "/")
                    ""
                    (:path-to-load (source-script-by-ns from-ns)))
        req (module/createRequire (path/resolve (or from-path "./script.cljs")))
        resolved (.resolve req lib)]
    (when reload
      (aset (.-cache req) resolved js/undefined))
    (js/require resolved)))

(def zns (sci/create-ns 'clojure.zip nil))

(def zip-namespace
  (sci/copy-ns clojure.zip zns))

(def rzns (sci/create-ns 'rewrite-clj.zip))
(def rewrite-clj-zip-ns (sci/copy-ns rewrite-clj.zip rzns))

(def rpns (sci/create-ns 'rewrite-clj.parser))
(def rewrite-clj-parser-ns (sci/copy-ns rewrite-clj.parser rpns))

(def rnns (sci/create-ns 'rewrite-clj.node))
(def rewrite-clj-node-ns (sci/copy-ns rewrite-clj.node rnns))

(def core-namespace (sci/create-ns 'clojure.core nil))

(def joyride-code
  {'*file* sci/file
   'extension-context (sci/copy-var db/extension-context joyride-ns)
   'invoked-script (sci/copy-var db/invoked-script joyride-ns)
   'output-channel (sci/copy-var db/output-channel joyride-ns)
   'js-properties repl-utils/instance-properties})

(store/reset-ctx!
 (sci/init {:classes {'js (doto goog/global
                            (aset "require" js/require))
                      :allow :all}
            :namespaces {'clojure.core {'IFn (sci/copy-var IFn core-namespace)
                                        'tap> (sci/copy-var tap> core-namespace)
                                        'add-tap (sci/copy-var remove-tap core-namespace)
                                        'remove-tap (sci/copy-var remove-tap core-namespace)}
                         'clojure.zip zip-namespace
                         'cljs.test cljs-test-config/cljs-test-namespace
                         'cljs.pprint cljs-pprint-config/cljs-pprint-namespace
                         'promesa.core promesa-config/promesa-namespace
                         'promesa.protocols promesa-config/promesa-protocols-namespace
                         'joyride.core joyride-code
                         'rewrite-clj.zip rewrite-clj-zip-ns
                         'rewrite-clj.parser rewrite-clj-parser-ns
                         'rewrite-clj.node rewrite-clj-node-ns}
            :ns-aliases {'clojure.test 'cljs.test}
            :load-fn (fn [{:keys [ns libname opts]}]
                       (cond
                         (symbol? libname)
                         (source-script-by-ns libname)
                         (string? libname) ;; node built-in or npm library
                         (cond
                           (= "vscode" libname)
                           (do (sci/add-class! (store/get-ctx) 'vscode vscode)
                               (sci/add-import! (store/get-ctx) ns 'vscode (:as opts))
                               {:handled true})

                           (active-extension? libname)
                           (let [module (extension-module libname)
                                 munged-ns (symbol (munge libname))
                                 refer (:refer opts)]
                             (sci/add-class! (store/get-ctx) munged-ns module)
                             (sci/add-import! (store/get-ctx) ns munged-ns (:as opts))
                             (when refer
                               (doseq [sym refer]
                                 (let [prop (gobject/get module sym)
                                       sub-sym (symbol (str munged-ns "$" sym))]
                                   (sci/add-class! (store/get-ctx) sub-sym prop)
                                   (sci/add-import! (store/get-ctx) ns sub-sym sym))))
                             {:handled true})

                           :else
                           (let [mod (require* ns libname opts)
                                 ns-sym (symbol libname)]
                             (sci/add-class! (store/get-ctx) ns-sym mod)
                             (sci/add-import! (store/get-ctx) ns ns-sym
                                              (or (:as opts)
                                                  ns-sym))
                             {:handled true}))))}))

(def !last-ns (volatile! @sci/ns))

(defn eval-string [s]
  (sci/binding [sci/ns @!last-ns]
    (let [rdr (sci/reader s)]
      (loop [res nil]
        (let [form (sci/parse-next (store/get-ctx) rdr)]
          (if (= :sci.core/eof form)
            (do
              (vreset! !last-ns @sci/ns)
              res)
            (recur (sci/eval-form (store/get-ctx) form))))))))
