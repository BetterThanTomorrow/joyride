(ns build
  (:require [babashka.process :refer [shell]]))

(def initial-compiled? (atom false))

(defn esbuild-pass
  {:shadow.build/stage :flush}
  [build-state]
  #_#_#_#_(println "[:extension] bundling with esbuild")
  (when (= :dev (:shadow.build/mode build-state))
    (when-not @initial-compiled?
      (Thread/sleep 5000)
      (reset! initial-compiled? true)))
  (shell {:continue true}
         "node_modules/.bin/esbuild" "out/js/joyride.js"
         "--outfile=out/js/joyride.cjs" "--format=cjs"
         "--bundle"
         "--platform=node"
         "--external:vscode")
  (println "esbuild done.")
  build-state)
