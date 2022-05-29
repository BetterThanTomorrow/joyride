(ns joyride.sci
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]
            [goog.object :as gobject]
            [joyride.db :as db]
            [joyride.config :as conf]
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


(defn- active-extension? [namespace]
  (let [[extension-name _module-name] (str/split namespace #"\$")
        extension (vscode/extensions.getExtension extension-name)]
    (and extension
         (.-isActive extension))))

(defn- extension-module [namespace]
  (let [[extension-name module-name] (str/split namespace #"\$")
        extension (vscode/extensions.getExtension extension-name)]
    (when extension
      (when-let [exports (.-exports extension)]
        [module-name
         (if module-name
           (let [path (str/split module-name #"\.")]
             (apply gobject/getValueByKeys exports path))
           exports)]))))

(comment
  (js-keys extension)
  (re-find #"^.*(?=\$)" "betterthantomorrow.calva$v0")
  (let [[extension module] (str/split "betterthantomorrow.calva$v0" #"\$")]
    [extension module])
  (extension-module "betterthantomorrow.calva$v0")
  (extension-module "borkdude.clj-kondo")
  (extension-module "redhat.vscode-yaml")
  (extension-module "sysoev.stylus")
  )

(def !ctx
  (volatile!
   (sci/init {:classes {'js goog/global
                        :allow :all}
              :namespaces (assoc
                           (:namespaces pconfig/config)
                           'joyride.core {'*file* sci/file
                                          'extension-context (sci/copy-var db/extension-context joyride-ns)
                                          'invoked-script (sci/copy-var db/invoked-script joyride-ns)
                                          'output-channel (sci/copy-var db/output-channel joyride-ns)})
              :load-fn (fn [{:keys [namespace opts]}]
                         (cond
                           (symbol? namespace)
                           (source-script-by-ns namespace)
                           (string? namespace) ;; node built-in or npm library
                           (cond 
                             (= "vscode" namespace)
                             (do (sci/add-class! @!ctx 'vscode vscode)
                                 (sci/add-import! @!ctx (symbol (str @sci/ns)) 'vscode (:as opts))
                                 {:handled true})

                             (active-extension? namespace)
                             (let [[module-name module] (extension-module namespace)]
                               (sci/add-class! @!ctx (symbol namespace) module)
                               (sci/add-import! @!ctx (symbol (str @sci/ns)) (symbol namespace) (:as opts))
                               {:handled true})

                             :else
                             (let [mod (js/require namespace)
                                   ns-sym (symbol namespace)]
                               (sci/add-class! @!ctx ns-sym mod)
                               (sci/add-import! @!ctx (symbol (str @sci/ns)) ns-sym
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
