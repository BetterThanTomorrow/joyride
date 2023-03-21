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

(defn get-arrow-clause [zl]
  (let [lhs (z/sexpr zl)
        arrow (-> zl z/right z/string)
        rhs (-> zl z/right z/right z/sexpr)]
    [lhs arrow rhs]))

(defn main []
  (log "port-arrow-form started")
  (-> (p/let [editor ^js vscode/window.activeTextEditor
              selection (e/current-selection)
              source (str "[ " (e/current-selection-text) " ]")
              zloc (z/of-string source)
              [lhs arrow rhs] (get-arrow-clause (z/down zloc))]
        (e/delete-range! editor selection)
        (e/insert-text!+ (str `(~'is (~'= ~rhs ~lhs)))))))

(when (= (joyride/invoked-script) joyride/*file*)
  (main))
