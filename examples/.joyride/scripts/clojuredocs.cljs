(ns clojuredocs
  (:require ["ext://betterthantomorrow.calva$v0" :refer [ranges]]
            ["vscode" :as vscode]
            [clojure.string :as string]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(defn clojuredocs-url [symbol-string]
  (let [qualified-symbol-string (some->> symbol-string
                                         symbol
                                         resolve
                                         symbol
                                         str)]
    (str "https://clojuredocs.org/"
         ;; clean up ? ! &
         (-> qualified-symbol-string
             (string/replace "?" "%3f")
             (string/replace "!" "%21")
             (string/replace "&" "%26")))))

(defn lookup-current-form-or-selection []
  (try (let [[_ lookup] (ranges.currentForm)
              url (clojuredocs-url lookup)]
         (if url
           (vscode/commands.executeCommand "simpleBrowser.show" url)
           (vscode/window.showInformationMessage
            (str "clojuredocs.cljs, can't resolve: " lookup))))
      (catch :default e
        (println (str "Clojuredocs lookup error: " e)))))

(when (= (joyride/invoked-script) joyride/*file*)
  (lookup-current-form-or-selection))

