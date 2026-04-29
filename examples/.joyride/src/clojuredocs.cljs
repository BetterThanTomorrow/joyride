(ns clojuredocs
  (:require ["ext://betterthantomorrow.calva$v1" :refer [ranges repl]]
            ["vscode" :as vscode]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [joyride.core :as joyride]))

;; To use this un-ignore the relevant part in `user_activate.cljs`
;; Then register a keyboard shortcut something like this:
;;     {
;;         "command": "joyride.runCode",
;;         "args": "(clojuredocs/lookup-current-form-or-selection)",
;;         "key": "ctrl+alt+c d",
;;     },

(defn ^:async resolve-in-repl [symbol-string]
  (try
    (let [result (await (repl.evaluateCode
                         js/undefined
                         (str "(some->> \"" symbol-string "\""
                              "          symbol"
                              "          resolve"
                              "          symbol"
                              "          str)")))
          result-string (.-result result)
          qualified-symbol-string (edn/read-string result-string)]
      (or qualified-symbol-string
          (str "clojure.core/" symbol-string)))
    (catch :default _ ; We fall back on clojure.core/<symbol-string>
      (str "clojure.core/" symbol-string))))

(defn ^:async clojuredocs-url [symbol-string]
  (let [resolved (await (resolve-in-repl symbol-string))]
    (str "https://clojuredocs.org/"
         (-> resolved
             (string/replace "?" "%3f") ; clean up ? ! &
             (string/replace "!" "%21")
             (string/replace "&" "%26")))))

(defn ^:async lookup-current-form-or-selection []
  (try
    (let [[_ lookup] (await (ranges.currentForm))
          url (await (clojuredocs-url lookup))]
      (if url
        (vscode/commands.executeCommand "simpleBrowser.show" url)
        (vscode/window.showInformationMessage
         (str "clojuredocs.cljs, can't resolve: " lookup))))
    (catch :default e
      (println (str "Clojuredocs lookup error: " e)))))

(when (= (joyride/invoked-script) joyride/*file*)
  (lookup-current-form-or-selection))
