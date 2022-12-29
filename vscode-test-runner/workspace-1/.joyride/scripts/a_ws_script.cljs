(ns a-ws-script
  (:require [promesa.core :as p]
            ["vscode" :as vscode]))

(p/do!
 (vscode/window.showInformationMessage "Hello" "OK")
 (p/delay 2000))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def symbol-1 :a-ws-script)

symbol-1