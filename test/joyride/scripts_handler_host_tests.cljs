(ns joyride.scripts-handler-host-tests
  "Tests for `joyride.scripts-handler` that need the Extension Development Host.
   The name doesn't end in `-test`, so the node test build leaves them out. Run them from the
   Extension Development Host REPL with `(cljs.test/run-tests 'joyride.scripts-handler-host-tests)`."
  (:require ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            ["vscode" :as vscode]
            [cljs.test :refer [async deftest is]]
            [joyride.output :as output]
            [joyride.scripts-handler :as scripts-handler]))

(defn- write-scripts!
  "Writes each `script-name` → `code` in `scripts` to a new temporary directory's `scripts` folder. Returns the directory."
  [scripts]
  (let [base-path (fs/mkdtempSync (path/join (os/tmpdir) "joyride-host-tests-"))]
    (fs/mkdirSync (path/join base-path "scripts"))
    (doseq [[script-name code] scripts]
      (fs/writeFileSync (path/join base-path "scripts" script-name) code))
    base-path))

(defn- run-script-reports+
  "Runs `script-name`, answering error popups with `choice`. Resolves to the failure headlines,
   error popups and reveals of Joyride Output it produced."
  [base-path script-name choice]
  (let [!reports (atom [])
        other-err output/append-line-other-err!
        show-terminal output/show-terminal!
        show-error-message (.-showErrorMessage vscode/window)
        restore! (fn []
                   (set! output/append-line-other-err! other-err)
                   (set! output/show-terminal! show-terminal)
                   (set! (.-showErrorMessage vscode/window) show-error-message))]
    (set! output/append-line-other-err! #(swap! !reports conj %))
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
                   (is (= ["Run Test Script Failed: reject_nil.cljs nil"
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
                   (is (= ["Run Test Script Failed: boom.cljs boom"
                           "Run Test Script error"
                           :output-revealed]
                          reports)
                       "Clicking the popup's button reveals Joyride Output")))
          (.finally (fn []
                      (fs/rmSync base-path #js {:recursive true})
                      (done)))))))
