(ns clojure-symbols
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]))

;; Finds `MARK`ers in the files and creates navigational symbols for them
;; Adapted from an example by https://github.com/maxrothman
;; NB: Will create a separate Outline tree, we know of no way to
;; blend it with some existing Outline tree.

(.appendLine (joyride/output-channel) "clojure-symbols loading...")

(defn- line-seq [document]
  (map #(.lineAt document %) (range (.-lineCount document))))

;; Example `MARK`er
;;; MARK Hello

(def ^:private key-type
  (->> vscode/SymbolKind
       js->clj
       (some #(when (= "Key" (second %)) (first %)))))

(def ^:private mark #"\s*;;; MARK (.*)")

(defn- provide-symbols [document]
  (if (< (.-lineCount document) 5000)
    (->> (line-seq document)
         (map-indexed #(vector %1 (-> (re-matches mark (.-text %2)) second)))
         (filter (comp some? second))
         (map #(vscode/SymbolInformation.
                (second %) ;Symbol name
                key-type   ;Symbol kind
                ""         ;Container name
                (vscode/Location. (.-uri document)
                                  (vscode/Position. (first %) 0))))
         clj->js)
    []))

(def ^:private symbol-provider
  #js {"provideDocumentSymbols" provide-symbols})

(defn register-provider! []
  (vscode/languages.registerDocumentSymbolProvider "clojure" symbol-provider))