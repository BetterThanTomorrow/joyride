#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]])

(def vscode-test-runner-dir (fs/parent *file*))
(def source-workspace (fs/path vscode-test-runner-dir "workspace-1"))
(def temp-base (fs/path "/tmp" "joyride-test"))
(def temp-workspace (fs/path temp-base "workspace"))
(def temp-runner (fs/path temp-base "runner"))

(binding [*out* *err*]
  (println "Setting up isolated test environment...")
  (println "  Source workspace:" (str source-workspace))
  (println "  Source runner:" (str vscode-test-runner-dir))
  (println "  Target base:" (str temp-base))

  ;; Clean existing temp environment
  (when (fs/exists? temp-base)
    (println "  Cleaning existing environment...")
    (fs/delete-tree temp-base))

  ;; Copy workspace to temp
  (println "  Copying workspace...")
  (fs/copy-tree source-workspace temp-workspace)

  ;; Copy test runner files to temp
  (println "  Copying test runner...")
  (fs/create-dirs temp-runner)
  (doseq [file ["launch.js" "runTests.js"]]
    (fs/copy (fs/path vscode-test-runner-dir file)
             (fs/path temp-runner file)))

  ;; Create minimal package.json for test runner
  (spit (str (fs/path temp-runner "package.json"))
        (str "{\"dependencies\": {\"@vscode/test-electron\": \"^2.4.1\", \"minimist\": \"^1.2.8\"}}"))

  ;; Install workspace dependencies
  (println "  Installing workspace dependencies...")
  (shell {:dir (str temp-workspace)
          :out "/dev/null"}
         "npm install")

  ;; Install test runner dependencies
  (println "  Installing test runner dependencies...")
  (shell {:dir (str temp-runner)
          :out "/dev/null"}
         "npm install")

  (println "✓ Isolated test environment ready")
  (println "  Workspace:" (str temp-workspace))
  (println "  Runner:" (str temp-runner)))

;; Output the runner path to stdout for consumption by the test script
(println (str temp-runner))
