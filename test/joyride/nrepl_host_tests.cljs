(ns joyride.nrepl-host-tests
  "Tests for `joyride.nrepl` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.nrepl-host-tests)`."
  (:require ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [cljs.test :refer [async deftest is use-fixtures]]
            [joyride.integrations.calva :as calva]
            [joyride.nrepl :as nrepl]
            [joyride.sci :as jsci]
            [sci.ctx-store :as store]))

(def ^:private !calva-state (atom nil))

(use-fixtures :once
  {:before (fn []
             (reset! !calva-state calva/state)
             (set! calva/state (constantly :absent)))
   :after (fn []
            (set! calva/state @!calva-state))})

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

(defn- clone-session
  "Clones a session with `client-name` and returns the new session's id."
  [client-name]
  (let [!response (atom nil)]
    (nrepl/handle-clone (cond-> {:op :clone}
                          client-name (assoc :client-name client-name))
                        (fn [_ response] (reset! !response response)))
    (get @!response "new-session")))

(deftest sessions-remember-their-client
  (let [calva-session (clone-session "Calva")
        other-session (clone-session "CIDER")
        nameless-session (clone-session nil)]
    (is (nrepl/calva-session? calva-session)
        "A session Calva cloned is Calva's")
    (is (not (nrepl/calva-session? other-session))
        "A session another client cloned isn't Calva's")
    (is (not (nrepl/calva-session? nameless-session))
        "A session cloned without a client name isn't Calva's")
    (is (not (nrepl/calva-session? nil))
        "A request without a session isn't Calva's")
    (doseq [session [calva-session other-session nameless-session]]
      (nrepl/handle-close {:op :close :session session} (fn [_ _])))
    (is (not (nrepl/calva-session? calva-session))
        "A closed session is forgotten")))

(deftest eval-runs-in-the-requested-ns
  (let [!responses (atom [])
        eval! (fn [request]
                (reset! !responses [])
                (nrepl/handle-eval (merge {:op :eval :sci-ctx (store/get-ctx)} request)
                                   (fn [_ response] (swap! !responses conj response)))
                (some #(get % "value") @!responses))]
    (jsci/eval-string "(ns joyride-host-tests.nrepl-ns) (def y 43)")
    (jsci/eval-string "(in-ns 'user)")
    (is (= "[43 \"joyride-host-tests.nrepl-ns\"]"
           (eval! {:code "[y (str *ns*)]" :ns "joyride-host-tests.nrepl-ns"}))
        "An evaluation runs in the namespace the request names, not the last one used")
    (is (= "\"joyride-host-tests.nrepl-ns\""
           (eval! {:code "(str *ns*)"}))
        "Without a namespace, an evaluation runs in the last one used")))
