(ns joyride.mcp.registry-test
  (:require
   ["fs" :as fs]
   ["os" :as os]
   ["path" :as path]
   [cljs.test :refer [async deftest is testing]]
   [joyride.mcp.registry :as registry]
   [promesa.core :as p]
   [vscode-mcp.registry-writer :as writer]))

(deftest enabled?-test
  (testing "on by default"
    (is (registry/enabled? true))
    (is (registry/enabled? nil)))
  (testing "off only when false"
    (is (not (registry/enabled? false)))))

(deftest compact-data-test
  (testing "does not set envelope keys the writer owns"
    (let [data (registry/compact-data)]
      (is (empty? (select-keys data [:name :workspaceRoot :workspaceFolder
                                     :serverName :windowId :mcp]))))))

(defn- tmp-dir []
  (fs/mkdtempSync (path/join (os/tmpdir) "joyride-reg-")))

(defn- cleanup! [dir]
  (writer/clear-writers!)
  (when (and dir (fs/existsSync dir))
    (fs/rmSync dir #js {:recursive true :force true})))

(deftest start-writes-joyride-entry-test
  (async done
         (let [dir (tmp-dir)
               config {:registry/enabled? true
                       :registry/dir dir
                       :registry/debounce-ms 15
                       :registry/heartbeat-ms 10000
                       :registry/custom-data+ (fn [_]
                                                (p/resolved (registry/compact-data)))
                       :cursor/server-name "joyride"
                       :cursor/script-relative-path "dist/joyride-mcp-server.js"
                       :lifecycle/wrapper-install-dir dir
                       :server/host "127.0.0.1"}
               info {:server/instance-slug "ws-abc"
                     :server/assigned-port 50541
                     :server/host "127.0.0.1"
                     :server/app-id "cursor"
                     :server/workspace-root "/proj/joyride"
                     :server/workspace-folder "/proj/joyride"
                     :server/port-file-uri #js {:fsPath "/tmp/joyride-mcp-server/ws-abc/port"}}]
           (-> (writer/on-started!+ config info)
               (p/then (fn [_]
                         (let [entry-file (path/join dir "joyride-ws-abc.json")]
                           (is (fs/existsSync entry-file))
                           (when (fs/existsSync entry-file)
                             (let [doc (js->clj (js/JSON.parse (fs/readFileSync entry-file "utf8"))
                                                :keywordize-keys true)]
                               (is (= "joyride-ws-abc" (:name doc)))
                               (is (= "joyride" (:serverName doc)))
                               (is (= "ws-abc" (:windowId doc)))
                               (is (= "/proj/joyride" (:workspaceRoot doc)))
                               (is (= "127.0.0.1" (get-in doc [:mcp :host])))
                               (is (= 50541 (get-in doc [:mcp :port])))
                               (is (= "/tmp/joyride-mcp-server/ws-abc/port"
                                      (get-in doc [:mcp :portFilePath]))))))))
               (p/finally (fn []
                            (cleanup! dir)
                            (done)))))))
