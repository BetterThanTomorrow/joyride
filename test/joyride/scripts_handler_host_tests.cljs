(ns joyride.scripts-handler-host-tests
  "Tests for `joyride.scripts-handler` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.scripts-handler-host-tests)`."
  (:require ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            ["vscode" :as vscode]
            [cljs.test :refer [async deftest is use-fixtures]]
            [joyride.integrations.calva :as calva]
            [joyride.output :as output]
            [joyride.scripts-handler :as scripts-handler]))

(def ^:private !calva-state (atom nil))

(use-fixtures :once
  {:before (fn []
             (reset! !calva-state calva/state)
             (set! calva/state (constantly :absent)))
   :after (fn []
            (set! calva/state @!calva-state))})

(defn- write-scripts!
  "Writes each `script-name` → `code` in `scripts` to a new temporary directory's `scripts` folder. Returns the directory."
  [scripts]
  (let [base-path (fs/mkdtempSync (path/join (os/tmpdir) "joyride-host-tests-"))]
    (fs/mkdirSync (path/join base-path "scripts"))
    (doseq [[script-name code] scripts]
      (fs/writeFileSync (path/join base-path "scripts" script-name) code))
    base-path))

(defn- run-script-reports+
  "Runs `script-name`, answering error popups with `choice`. Resolves to the error lines in Joyride Output,
   error popups and reveals of Joyride Output it produced."
  [base-path script-name choice]
  (let [!reports (atom [])
        eval-err output/append-line-eval-err!
        other-err output/append-line-other-err!
        show-terminal output/show-terminal!
        show-error-message (.-showErrorMessage vscode/window)
        restore! (fn []
                   (set! output/append-line-eval-err! eval-err)
                   (set! output/append-line-other-err! other-err)
                   (set! output/show-terminal! show-terminal)
                   (set! (.-showErrorMessage vscode/window) show-error-message))]
    (set! output/append-line-eval-err! (fn ([m] (swap! !reports conj m)) ([m _] (swap! !reports conj m))))
    (set! output/append-line-other-err! (fn ([m] (swap! !reports conj m)) ([m _] (swap! !reports conj m))))
    (set! output/show-terminal! (fn reveal ([] (reveal false)) ([_] (swap! !reports conj :output-revealed))))
    (set! (.-showErrorMessage vscode/window) (fn [message & _]
                                                (swap! !reports conj message)
                                                (js/Promise.resolve choice)))
    (-> (scripts-handler/run-script+ "Run Test Script" base-path "scripts" script-name)
        (.then (constantly nil) (constantly nil))
        (.then (fn [_] @!reports))
        (.finally restore!))))

(deftest script-run-reports-rejection-with-nil
  (async done
    (let [base-path (write-scripts! {"reject_nil.cljs" "(js/Promise.reject nil)"})]
      (-> (run-script-reports+ base-path "reject_nil.cljs" js/undefined)
          (.then (fn [reports]
                   (is (= ["nil"
                           "Run Test Script Failed: reject_nil.cljs"
                           "Run Test Script error"]
                          reports)
                       "The run is reported as failed, with nil as the error, and Joyride Output stays hidden")))
          (.finally (fn []
                      (fs/rmSync base-path #js {:recursive true})
                      (done)))))))

(deftest script-failure-reveals-output-on-click
  (async done
    (let [base-path (write-scripts! {"boom.cljs" "(throw (js/Error. \"boom\"))"})]
      (-> (run-script-reports+ base-path "boom.cljs" "Reveal output terminal")
          (.then (fn [reports]
                   (is (= ["boom"
                           "Run Test Script Failed: boom.cljs"
                           "Run Test Script error"
                           :output-revealed]
                          reports)
                       "Clicking the popup's button reveals Joyride Output")))
          (.finally (fn []
                      (fs/rmSync base-path #js {:recursive true})
                      (done)))))))

(def ^:private failing-scripts
  [["throw_string.cljs" "(throw \"a string\")" "a string"]
   ["reject.cljs" "(js/Promise.reject (js/Error. \"later\"))" "later"]
   ["reject_message_42.cljs" "(js/Promise.reject #js {:message 42})" "#js {:message 42}"]])

(deftest script-failure-printed-once
  (async done
    (let [base-path (write-scripts! (into {} (map (juxt first second)) failing-scripts))]
      (-> (reduce (fn [acc [script-name _ message]]
                    (.then acc (fn [_]
                                 (.then (run-script-reports+ base-path script-name js/undefined)
                                        (fn [reports]
                                          (is (= [message
                                                  (str "Run Test Script Failed: " script-name)
                                                  "Run Test Script error"]
                                                 reports)
                                              (str script-name " prints its error once, then the script that failed")))))))
                  (js/Promise.resolve nil)
                  failing-scripts)
          (.finally (fn []
                      (fs/rmSync base-path #js {:recursive true})
                      (done)))))))

(deftest rejecting-thenable-failure-has-its-message
  (async done
    (let [base-path (write-scripts! {"thenable.cljs" "#js {:then (fn [_ reject] (reject (js/Error. \"thenable\")))}"})]
      (-> (run-script-reports+ base-path "thenable.cljs" js/undefined)
          (.then (fn [reports]
                   (is (= ["Run Test Script Failed: thenable.cljs thenable"
                           "Run Test Script error"]
                          reports)
                       "A rejecting thenable that isn't a native promise isn't echoed, so the Failed line carries its error")))
          (.finally (fn []
                      (fs/rmSync base-path #js {:recursive true})
                      (done)))))))

(deftest unreadable-script-failure-has-its-message
  (async done
    (let [base-path (write-scripts! {})]
      (-> (run-script-reports+ base-path "missing.cljs" js/undefined)
          (.then (fn [reports]
                   (is (= [(str "Run Test Script Failed: missing.cljs Error: ENOENT: no such file or directory, stat '"
                                (path/join base-path "scripts" "missing.cljs") "'")
                           "Run Test Script error"]
                          reports)
                       "The Failed line carries the error, since nothing else prints it")))
          (.finally (fn []
                      (fs/rmSync base-path #js {:recursive true})
                      (done)))))))
