(ns babashka-fs
  (:require [babashka.fs :as fs]
            [joyride.core :as joyride]))

;; babashka.fs path helpers and with-temp-dir — built into Joyride.
;; See https://github.com/babashka/fs/blob/master/API.md

(defn demo-paths
  []
  (let [here (fs/cwd)]
    {:cwd (str here)
     :cwd-exists? (fs/exists? here)
     :readme? (fs/exists? (fs/path here "README.md"))
     :file-name (fs/file-name (fs/path here "README.md"))}))

(defn demo-temp-dir
  []
  (fs/with-temp-dir [d nil]
    (let [f (fs/path d "hello.txt")]
      (fs/create-file f)
      {:temp-dir (str d)
       :file (str f)
       :exists? (fs/exists? f)})))

(defn main
  []
  (println "paths:" (demo-paths))
  (println "temp:" (demo-temp-dir)))

(comment
  (fs/exists? ".")
  (fs/with-temp-dir [d nil]
    (println "temp dir: " d)
    (fs/create-file (fs/path d "smoke.txt"))
    (fs/exists? (fs/path d "smoke.txt")))
  :rcf)

(when (= (joyride/invoked-script) joyride/*file*)
  (main))
