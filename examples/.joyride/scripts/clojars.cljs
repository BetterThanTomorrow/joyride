(ns clojars
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))


(defn lift-clojars-results
  "A clojars json response is an object containing
   the actual results vector at key 'results'. This
   function just lifts up the vector. Expects
   a keywodized map."
  [{:keys [results] :or {results nil}}]
  results)


(defn search-clojars
  "Search clojars for the given term. The count of results will be
   capped by the API at 24. Returns a promise containing a vector of the results,
   where a single result is a map like
   
   {:created 1747047578708
    :description nil
    :group_name \"metosin\"
    :jar_name \"malli\"
    :version \"0.18.0\"}
  "
  [term]
  (let [uri-encoded-term (js->clj (js/encodeURI term))]
    (-> (js/fetch (str "https://clojars.org/search?format=json&q=" uri-encoded-term))
        (p/then #(.json %))
        (p/then #(js->clj % :keywordize-keys true))
        (p/then #(lift-clojars-results %))
        (p/catch #(nil)))))


(comment
  (p/let [result (search-clojars "malli")]
    (println result)))


(defn clojars-result->QuickPickItem
  "Turns a single clojars result into a VSCode QuickPickItem."
  [{:keys [description group_name jar_name version]}]
  #js {:label (str "" group_name "/" jar_name " " "{:mvn/version " "\"" version "\"}")
       :detail description})


(defn clojars-results->QuickPickItems
  "Turns a vector of clojars results as provided by search-clojars
   into a JavaScript array of QuickPickItems."
  [clojars-results]
  (clj->js
   (mapv clojars-result->QuickPickItem clojars-results)))


(defn insert-at-cursor-position
  "Inserts the given text at the cursor position of
   the active text editor."
  [text]
  (when-let [editor (.-activeTextEditor vscode/window)]
    (when-let [position (-> editor
                            (.-selection)
                            (.-active))]
      (.edit editor (fn [builder]
                      (.insert builder position text))))
    nil))


(comment
  (insert-at-cursor-position "hello"))


(defn ask-for-input
  "The main function of the script. Asks for a search term, queries
   clojars, displays the results and inserts the selected result
   at cursor position."
  []
  (p/let [input (vscode/window.showInputBox #js {:title "Search clojars"
                                                 :placeHolder "E.g. malli or reitit or ring/ring"})]
    (when input
      (p/let [clojars-results (search-clojars input)]
        (when clojars-results
          (p/let [picked-item (vscode/window.showQuickPick (clojars-results->QuickPickItems clojars-results))
                  pick-text (js->clj picked-item :keywordize-keys true)]
            (insert-at-cursor-position (:label pick-text))))))))


(comment
  (ask-for-input))


(when (= (joyride/invoked-script) joyride/*file*)
  (ask-for-input))
