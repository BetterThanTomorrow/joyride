(ns joyride.nrepl-host-tests
  "Tests for `joyride.nrepl` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.nrepl-host-tests)`."
  (:require ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [cljs.test :refer [async deftest is]]
            [joyride.nrepl :as nrepl]))

(defn- temp-root []
  (fs/mkdtempSync (path/join (os/tmpdir) "joyride-nrepl-host-tests-")))

(deftest remove-port-file-that-is-there
  (async done
    (let [root (temp-root)
          port-file (path/join root ".joyride" ".nrepl-port")]
      (fs/mkdirSync (path/dirname port-file) #js {:recursive true})
      (fs/writeFileSync port-file "12345")
      (-> (nrepl/remove-port-file root)
          (.then (fn [_]
                   (is (not (fs/existsSync port-file))
                       "The port file is deleted")))
          (.catch (fn [e] (is (nil? e) "Removing the port file resolves")))
          (.finally (fn []
                      (fs/rmSync root #js {:recursive true :force true})
                      (done)))))))

(deftest remove-port-file-that-is-gone
  (async done
    (let [root (temp-root)]
      (-> (nrepl/remove-port-file root)
          (.then (fn [_]
                   (is true "Removing a port file that isn't there resolves")))
          (.catch (fn [e] (is (nil? e) "Removing a port file that isn't there resolves")))
          (.finally (fn []
                      (fs/rmSync root #js {:recursive true :force true})
                      (done)))))))
