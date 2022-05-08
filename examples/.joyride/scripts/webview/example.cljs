(ns webview.example
  (:require ["path" :as path]
            ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(defn main []
  (p/let [panel (vscode/window.createWebviewPanel
                 "My webview!" "Scittle"
                 vscode/ViewColumn.One
                 #js {:enableScripts true})
          uri (vscode/Uri.file (path/join vscode/workspace.rootPath
                                          ".joyride" "scripts" "webview"
                                          "page.html"))
          data (vscode/workspace.fs.readFile uri)
          html (.decode (js/TextDecoder. "utf-8") data)]
    (set! (.. panel -webview -html) (str html))))

(when (= (joyride/get-invoked-script) joyride/*file*)
  (main))

;; live demo here: https://twitter.com/borkdude/status/1519607386218053632
