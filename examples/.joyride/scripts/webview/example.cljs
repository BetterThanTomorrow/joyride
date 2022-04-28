(ns webview.example
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]))

(def panel
  (vscode/window.createWebviewPanel
   "My webview!" "Scittle"
   vscode/ViewColumn.One
   #js {:enableScripts true}))

(def html (fs/readFileSync (path/join vscode/workspace.rootPath
                                      ".joyride" "scripts" "webview"
                                      "page.html")))

(set! (.. panel -webview -html) (str html))

;; live demo here: https://twitter.com/borkdude/status/1519607386218053632
