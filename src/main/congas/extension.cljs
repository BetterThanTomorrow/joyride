(ns congas.extension
  (:require
   ["vscode" :as vscode]
   [clojure.string :as str]
   [sci-configs.funcool.promesa :as pconfig]
   [sci.core :as sci]
   [promesa.core :as p]))

(defn register-disposable [^js context ^js disposable]
  (.push (.-subscriptions context) disposable))

(defn debug [& xs]
  (apply vscode/window.showInformationMessage (into-array (mapv str xs))))

(def !ctx (volatile!
           (sci/init {:classes {'js goog/global
                                :allow :all}
                      :namespaces (:namespaces pconfig/config)
                      :load-fn (fn [{:keys [namespace opts]}]
                                 (when ;; assume npm library
                                     (string? namespace)
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

(defn eval-query []
  (p/let [input (vscode/window.showInputBox #js {:placeHolder "(require '[\"path\" :as path]) (path/resolve \".\")"
                                                 :prompt "Type one or more expressions to evaluate"})
          res (sci/eval-string* @!ctx input)]
    (vscode/window.showInformationMessage (str "The result: " res))))

(defn run-script [& script]
  (let [program (str/join "\n"
                          (map pr-str '[(require '["vscode" :as vscode])
                                        (vscode/window.showInformationMessage "Hello from SCI!")]))]
    (sci/eval-string* @!ctx
                      program
                      #_(fs/readFileSync (path/resolve ws-root ".congas/scripts/hello.cljs"))))
  (sci/eval-form @!ctx
                 '(do (require '[promesa.core :as p])
                      (p/do
                        (p/delay 2000)
                        (vscode/window.showInformationMessage "Hello from SCI again!!!!!!"))))
  (eval-query))

(defn- register-command []
  (vscode/commands.registerCommand "congas.runScript" run-script))

(defn- setup-command [^js context]
  (->> (register-command)
       (register-disposable context)))



                                        ; /Users/pez/Desktop/empty/congas/scripts/hello.cljs
(defn ^:export activate [^js context]
  (setup-command context))

(defn ^:export deactivate []
  )

(comment)
