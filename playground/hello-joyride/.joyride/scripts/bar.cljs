(ns bar
  (:require [foo]
            ["vscode" :as vscode]))

(vscode/window.showInformationMessage (str (foo/x2 42)))