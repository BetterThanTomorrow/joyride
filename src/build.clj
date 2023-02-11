(ns build
  (:require [babashka.process :refer [shell]]))

(defn esbuild-pass
  {:shadow.build/stage :flush}
  [build-state]
  (println "[:viewer] bundling wiht esbuild")
  (shell "node_modules/.bin/esbuild" "out/js/joyride.js"
         "--outfile=out/js/joyride.js" "--format=cjs"
         "--allow-overwrite"
         "--bundle"
         "--platform=node"
         "--external:vscode")
  (println "esbuild done.")
  build-state)
