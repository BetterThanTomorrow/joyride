(ns integration-test.scripts-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            ["util" :as util]))

(def access (util/promisify fs.access))
(def read-file (util/promisify fs.readFile))
(def write-file (util/promisify fs.writeFile))
(def append-file (util/promisify fs.appendFile))
(def rm (util/promisify fs.rm))

(def ws-root-path (-> (first vscode/workspace.workspaceFolders)
                      .-uri
                      .-fsPath))

(deftest-async run-a-user-script
  (testing "Runs a user script"
    (p/let [_created (vscode/commands.executeCommand "joyride.createUserHelloScript")
            result (vscode/commands.executeCommand "joyride.runUserScript" "hello_joyride_user_script.cljs")]
      (is (= "🎸"
             result)))))

(deftest-async run-a-user-javascript-script
  (testing "Runs a user script written in JavaScript"
    (p/let [_created (vscode/commands.executeCommand "joyride.createUserHelloScript")
            result (vscode/commands.executeCommand "joyride.runUserScript" "hello_joyride_user_script.js")]
      (is (= "Hello World!"
             (.hello result))))))

(deftest-async run-a-ws-script
  (testing "Runs a workspace script"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")]
      (is (= :a-ws-script
             result)))))

(deftest-async run-a-ws-cljs-script-from-subdir
  (testing "Runs a workspace ClojureScript script from a subdirectory"
    (p/let [script "example/write_a_file.cljs"
            _ (vscode/commands.executeCommand "joyride.runWorkspaceScript" script)
            written-file (path/join ws-root-path "test-from-cljs-script.txt")
            content (read-file written-file #js {:encoding "utf8"})]
      (is (= "Written from a Workspace ClojureScript Script!"
             content))
      (rm written-file))))

(deftest-async run-a-ws-js-script-from-subdir
  (testing "Runs a workspace JavaScript script from a subdirectory"
    (p/let [script "example/write-a-file.js"
            _ (vscode/commands.executeCommand "joyride.runWorkspaceScript" script)
            written-file (path/join ws-root-path "test-from-js-script.txt")
            content (read-file written-file #js {:encoding "utf8"})]
      (is (= "Written from a Workspace JavaScript Script!"
             content))
      (rm written-file))))

(deftest-async run-a-javascript-script-reloads
  (testing "Runs a workspace script written in JavaScript, reloading changes"
    (p/let [script "reloaded-script.js"
            script-path (path/join ws-root-path ".joyride" "scripts" script)
            _ (-> (access script-path fs/constants.F_OK)
                  (p/then #(rm script-path))
                  (p/catch #()))]
      (p/let [_ (write-file script-path "exports.fortytwo = 42;\n")
              result (vscode/commands.executeCommand "joyride.runWorkspaceScript" script)]
        (is (= 42
               (.-fortytwo result)))
        (is (nil? (.-forty2 result)))
        (p/let [_ (append-file script-path "\nexports.forty2 = 42;\n")
                result (vscode/commands.executeCommand "joyride.runWorkspaceScript" script)]
          (is (= 42
                 (.-fortytwo result)))
          (is (= 42
                 (.-forty2 result))))
        (rm script-path)))))

(deftest-async run-a-ws-javascript-script-dashes
  (testing "Runs a workspace script written in JavaScript, named with dash separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a-ws-script.js")]
      (is (= 42
             (.-fortytwo result))))))

(deftest-async run-a-ws-javascript-script-underscores
  (testing "Runs a workspace script written in JavaScript, named with undercore separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.js")]
      (is (= 42
             (.-fortytwo result))))))

(deftest-async run-a-ws-cljc-script
  (testing "Runs a workspace cljc script"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_cljc_script.cljc")]
      (is (= :a-cljc-script
             result)))))