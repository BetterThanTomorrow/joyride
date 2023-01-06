(ns integration-test.scripts-test
  (:require [cljs.test :refer [testing is]]
            [integration-test.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            ["util" :as util]))

(deftest-async run-a-user-script
  (testing "Runs a user script"
    (p/let [result (vscode/commands.executeCommand "joyride.runUserScript" "hello_joyride_user_script.cljs")]
      (is (= "🎸"
             result)))))

(deftest-async run-a-user-javascript-script
  (testing "Runs a user script written in JavaScript"
    (p/let [result (vscode/commands.executeCommand "joyride.runUserScript" "hello_joyride_user_script.js")]
      (is (= "Hello World!"
             (.hello result))))))

(deftest-async run-a-ws-script
  (testing "Runs a workspace script"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.cljs")]
      (is (= :a-ws-script
             result)))))

(deftest-async run-a-ws-javascript-script-dashes
  (testing "Runs a workspace script written in JavaScript, named with dash separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a-ws-script.js")]
      (is (= 42
             (.-fortytwo result))))))

(deftest-async run-a-javascript-script-reloads
  (testing "Runs a workspace script written in JavaScript, reloading changes"
    (p/let [write-file (util/promisify fs.writeFile)
            append-file (util/promisify fs.appendFile)
            access (util/promisify fs.access)
            rm (util/promisify fs.rm)
            script "reloaded-script.js"
            script-path (path/join (-> (first vscode/workspace.workspaceFolders)
                                       .-uri
                                       .-fsPath)
                                   ".joyride"
                                   "scripts"
                                   script)
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

(deftest-async run-a-ws-javascript-script-underscores
  (testing "Runs a workspace script written in JavaScript, named with undercore separators"
    (p/let [result (vscode/commands.executeCommand "joyride.runWorkspaceScript" "a_ws_script.js")]
      (is (= 42
             (.-fortytwo result))))))