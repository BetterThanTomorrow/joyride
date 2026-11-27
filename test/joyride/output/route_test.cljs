(ns joyride.output.route-test
  (:require [cljs.test :refer [deftest is testing]]
            [joyride.output.route :as route]))

(def ^:private calva-on {:setting/use-calva? true
                         :calva/state :active
                         :joyride/starting? false
                         :terminal/live? false
                         :output/who "joyride"})

(deftest route-table
  (is (= :joyride-output (route/route (assoc calva-on :setting/use-calva? false)))
      "The setting off gives Joyride Output")
  (is (= :joyride-output (route/route (assoc calva-on :calva/state :absent)))
      "No Calva gives Joyride Output")
  (is (= :calva (route/route calva-on))
      "An active Calva gets the output")
  (is (= :calva (route/route (assoc calva-on :joyride/starting? true :terminal/live? true)))
      "An active Calva gets the output also while Joyride starts")
  (testing "Calva installed but not started"
    (is (= :hold (route/route (assoc calva-on :calva/state :inactive :joyride/starting? true)))
        "Joyride's own output is held while Joyride starts")
    (is (= :joyride-output (route/route (assoc calva-on :calva/state :inactive :joyride/starting? true :output/who "joyride-user")))
        "People's output is never held")
    (is (= :joyride-output (route/route (assoc calva-on :calva/state :inactive :joyride/starting? true :terminal/live? true)))
        "Output goes to a live Joyride Output terminal also while Joyride starts")
    (is (= :joyride-output (route/route (assoc calva-on :calva/state :inactive)))
        "Output goes to Joyride Output once Joyride has started")))

(deftest route-for-calva-nrepl-clients
  (is (= :nowhere (route/route (assoc calva-on :output/calva-client? true)))
      "Calva shows its own nREPL evaluations")
  (is (= :joyride-output (route/route (assoc calva-on :output/calva-client? true :setting/use-calva? false)))
      "With the setting off, Calva's nREPL evaluations show in Joyride Output, as before"))

(deftest joyride-output-after-calva
  (is (true? (route/joyride-output-after-calva? nil))
      "A Calva without the display API returns nothing")
  (is (true? (route/joyride-output-after-calva? {:calva/printed-to [] :calva/skipped ["repl-window"]}))
      "The REPL Window as the only destination")
  (is (false? (route/joyride-output-after-calva? {:calva/printed-to ["terminal"] :calva/skipped ["repl-window"]}))
      "Calva printed it somewhere")
  (is (false? (route/joyride-output-after-calva? {:calva/printed-to [] :calva/skipped []}))
      "The Calva settings send the category nowhere"))

(deftest calva-message
  (is (= {:category "evaluationResults" :text "3" :who "joyride-user" :ns "user" :replSessionKey "joyride" :evaluationId 5}
         (route/calva-message {:output/category :output.category/evaluation-results
                               :output/text "3"
                               :output/line? true
                               :output/who "joyride-user"
                               :output/ns "user"
                               :output/evaluation-id 5}))
      "Calva ends a result's line itself")
  (is (= {:category "evaluationResults" :text "#<Promise [1 2]>" :who "x" :replSessionKey "joyride"}
         (route/calva-message {:output/category :output.category/evaluation-results
                               :output/text "[1 2]"
                               :output/promise? true
                               :output/who "x"}))
      "A promise's value reads as the promise")
  (is (= {:category "evaluatedCode" :text "(+ 1 2)" :who "x" :ns "user" :replSessionKey "joyride"}
         (route/calva-message {:output/category :output.category/evaluated-code
                               :output/text "(+ 1 2)"
                               :output/who "x"
                               :output/ns "user"}))
      "Evaluated code as it is")
  (is (= {:category "otherOutput" :text "hello\n" :who "joyride" :replSessionKey "joyride"}
         (route/calva-message {:output/category :output.category/other-output
                               :output/text "hello"
                               :output/line? true
                               :output/who "joyride"}))
      "A stream line gets its line end, and no ns means no ns")
  (is (= "hello" (:text (route/calva-message {:output/category :output.category/evaluation-output
                                              :output/text "hello"
                                              :output/who "x"})))
      "Stream text that isn't a line is kept as it is"))

(deftest who-badges
  (is (false? (route/who-badge? "joyride-user")) "People's evaluations get no badge")
  (is (false? (route/who-badge? "joyride-nrepl")) "Other nREPL clients get no badge")
  (is (false? (route/who-badge? "joyride")) "Joyride's own messages get no badge")
  (is (true? (route/who-badge? "some-agent")) "Agents get their badge"))

(deftest nrepl-who
  (is (= "an-agent" (route/nrepl-who {:who "an-agent"})) "A client's who is kept")
  (is (= "joyride-user" (route/nrepl-who {:calva-client? true})) "Calva's evaluations are the user's")
  (is (= "joyride-nrepl" (route/nrepl-who {})) "Other clients without a who")
  (is (= "joyride-nrepl" (route/nrepl-who {:who "ui"})) "A reserved who is replaced")
  (is (= "joyride-nrepl" (route/nrepl-who {:who "api" :calva-client? true})) "A reserved who is replaced also for Calva"))
