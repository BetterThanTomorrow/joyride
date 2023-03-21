(ns port-arrow-form
  (:require ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]
            [rewrite-clj.zip :as z]
            [util.editor :as e]))

;; A small helper for porting midje to clojure.test, which demos
;; several useful techniques for manipulating Clojure code.

;; This code relies on the active selection

(defn log [x]
  (.appendLine (joyride.core/output-channel) (str x)))

(defn get-arrow-clause
  "Make a pseudo-form since we're just using this for parsing."
  [text]
  (let [zloc (z/down (z/of-string (str "[ " text " ]")))
        lhs (z/sexpr zloc)
        arrow (-> zloc z/right z/string)
        rhs (-> zloc z/right z/right z/sexpr)]
    [lhs arrow rhs]))

(defn main []
  (log "port-arrow-form started")
  (-> (p/let [[lhs _ rhs] (get-arrow-clause (e/current-selection-text))]
        (e/replace-range! (str `(~'is (~'= ~rhs ~lhs)))))))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))
