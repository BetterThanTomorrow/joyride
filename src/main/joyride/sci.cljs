(ns joyride.sci
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]
            [joyride.settings :refer [workspace-scripts-path]]
            [joyride.utils :as utils]
            [sci-configs.funcool.promesa :as pconfig]
            [sci.core :as sci]))

(sci/alter-var-root sci/print-fn (constantly *print-fn*))

(defn ns->path [namespace]
  (-> (str namespace)
      (munge)
      (str/replace  "." "/")
      (str ".cljs")))

(def !ctx (volatile!
           (sci/init {:classes {'js goog/global
                                :allow :all}
                      :namespaces (:namespaces pconfig/config)
                      :load-fn (fn [{:keys [namespace opts]}]
                                 (cond
                                   (symbol? namespace)
                                   {:source
                                    (let [path (ns->path namespace)]
                                      (str
                                       (fs/readFileSync
                                        (path/join
                                         (utils/workspace-root)
                                         workspace-scripts-path
                                         path))))}
                                   (string? namespace) ;; node built-in or npm library
                                   (if (= "vscode" namespace)
                                     (do (sci/add-class! @!ctx 'vscode vscode)
                                         (sci/add-import! @!ctx (symbol (str @sci/ns)) 'vscode (:as opts))
                                         {:handled true})
                                     (let [mod (js/require namespace)
                                           ns-sym (symbol namespace)]
                                       (sci/add-class! @!ctx ns-sym mod)
                                       (sci/add-import! @!ctx (symbol (str @sci/ns)) ns-sym
                                                        (or (:as opts)
                                                            ns-sym))
                                       {:handled true}))))})))

(defn eval-string [s]
  (sci/eval-string* @!ctx s))
