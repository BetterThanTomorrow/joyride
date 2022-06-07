(ns joyride.sci
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]
            [goog.object :as gobject]
            [joyride.db :as db]
            [joyride.config :as conf]
            [sci.configs.clojure.test :as ct-config]
            [sci.configs.funcool.promesa :as pconfig]
            [sci.core :as sci]))

(sci/alter-var-root sci/print-fn (constantly *print-fn*))

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
                                  [(conf/workspace-abs-scripts-path) (conf/user-abs-scripts-path)]))]
    (when path-to-load
      {:file ns-path
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

(def !ctx
  (volatile!
   (sci/init {:classes {'js goog/global
                        :allow :all}
              :namespaces (merge
                           (:namespaces pconfig/config)
                           (:namespaces ct-config/config)
                           {'joyride.core {'*file* sci/file
                                           'extension-context (sci/copy-var db/extension-context joyride-ns)
                                           'invoked-script (sci/copy-var db/invoked-script joyride-ns)
                                           'output-channel (sci/copy-var db/output-channel joyride-ns)}})
              :load-fn (fn [{:keys [ns libname opts]}]
                         (cond
                           (symbol? libname)
                           (source-script-by-ns libname)
                           (string? libname) ;; node built-in or npm library
                           (cond
                             (= "vscode" libname)
                             (do (sci/add-class! @!ctx 'vscode vscode)
                                 (sci/add-import! @!ctx ns 'vscode (:as opts))
                                 {:handled true})

                             (active-extension? libname)
                             (let [module (extension-module libname)
                                   munged-ns (symbol (munge libname))
                                   refer (:refer opts)]
                               (sci/add-class! @!ctx munged-ns module)
                               (sci/add-import! @!ctx ns munged-ns (:as opts))
                               (when refer
                                 (doseq [sym refer]
                                   (let [prop (gobject/get module sym)
                                         sub-sym (symbol (str munged-ns "$" sym))]
                                     (sci/add-class! @!ctx sub-sym prop)
                                     (sci/add-import! @!ctx ns sub-sym sym))))
                               {:handled true})

                             :else
                             (let [mod (js/require libname)
                                   ns-sym (symbol libname)]
                               (sci/add-class! @!ctx ns-sym mod)
                               (sci/add-import! @!ctx ns ns-sym
                                                (or (:as opts)
                                                    ns-sym))
                               {:handled true}))))})))

(def !last-ns (volatile! @sci/ns))

(defn eval-string [s]
  (sci/binding [sci/ns @!last-ns]
    (let [rdr (sci/reader s)]
      (loop [res nil]
        (let [form (sci/parse-next @!ctx rdr)]
          (if (= :sci.core/eof form)
            (do
              (vreset! !last-ns @sci/ns)
              res)
            (recur (sci/eval-form @!ctx form))))))))
