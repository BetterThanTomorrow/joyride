(ns integration-test.activate-test
  (:require [cljs.test :refer [deftest testing is]]
            [promesa.core :as p]
            [joyride.core :as joyride]
            ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]))

(defn path-exists-in-workspace?+ [path]
  (-> (p/let [ws-path (path/join vscode/workspace.rootPath path)]
        (fs/existsSync ws-path))))

(defn path-exists-in-user-dir?+ [path]
  (-> (let [user-path (path/join joyride.core/user-joyride-dir path)]
        (fs/existsSync user-path))))

(comment
  (p/let [e? (path-exists-in-workspace?+ ".joyride")]
    (vscode/window.showInformationMessage e?)
    (def e? e?))
  :rcf)

(deftest user-activate
  (testing "Default joyride content is created"
    (is (= true
           (path-exists-in-user-dir?+ "deps.edn")))
    (is (= true
           (path-exists-in-user-dir?+ "README.md")))
    (is (= true
           (path-exists-in-user-dir?+ ".gitignore")))
    (is (= true
           (path-exists-in-user-dir?+ ".joyride/scripts/workspace_activate.cljs")))
    (is (= true
           (path-exists-in-user-dir?+ ".github/copilot-instructions.md")))))

(deftest ws-activate
  (testing "Workspace activation script defines a symbol"
    (is (= :symbol-1
           #_{:clj-kondo/ignore [:unresolved-namespace]}
           workspace-activate/symbol-1)))

  (testing "Workspace activation script defines a function"
    (is (= :fn-1
           #_{:clj-kondo/ignore [:unresolved-namespace]}
           (workspace-activate/fn-1))))

  (testing "Workspace activation script finds workspace root"
    (is (= "workspace-1"
           #_{:clj-kondo/ignore [:unresolved-namespace]}
           (-> (workspace-activate/ws-root)
               .-uri
               .-fsPath
               path/basename)))))