(ns integration-test.commands-test
  (:require
   [cljs.test :refer [is testing]]
   [integration-test.macros :refer [deftest-async]]
   [promesa.core :as p]
   ["vscode" :as vscode]))

(deftest-async evaluate-selection-without-crash
  (testing "Evaluate selection command completes without stack overflow"
    (p/let [;; Create a new document with test code
            doc (vscode/workspace.openTextDocument #js {:content "(+ 1 2)"
                                                        :language "clojure"})
            editor (vscode/window.showTextDocument doc)
            ;; Select all the text
            _ (set! (.-selection editor)
                    (vscode/Selection. 0 0 0 7))
            ;; Set some clipboard content to verify restoration works
            _ (vscode/env.clipboard.writeText "original-clipboard-content")
            ;; Execute the evaluate selection command
            ;; This should not throw "Maximum call stack size exceeded"
            _ (vscode/commands.executeCommand "joyride.evaluateSelection")
            ;; Wait a bit for the command to complete
            _ (p/delay 500)
            ;; Verify clipboard was restored (use native promise to avoid stack overflow)
            clipboard-after (.then (vscode/env.clipboard.readText) identity)]
      ;; Clipboard should be restored to original content
      (is (= "original-clipboard-content" clipboard-after)
          "Clipboard content is restored after evaluation"))))

(deftest-async evaluate-selection-with-empty-selection
  (testing "Evaluate selection handles empty selection gracefully"
    (p/let [doc (vscode/workspace.openTextDocument #js {:content "(+ 1 2)"
                                                        :language "clojure"})
            editor (vscode/window.showTextDocument doc)
            ;; Create an empty selection (cursor at position 0)
            _ (set! (.-selection editor)
                    (vscode/Selection. 0 0 0 0))
            ;; Execute command with empty selection - should complete without error
            _ (vscode/commands.executeCommand "joyride.evaluateSelection")
            _ (p/delay 300)]
      ;; Just verify it completes - the assertion is implicit (no throw)
      (is true "Command handles empty selection without error"))))

(deftest-async evaluate-selection-preserves-different-clipboard
  (testing "Evaluate selection preserves clipboard when selection differs from clipboard"
    (p/let [doc (vscode/workspace.openTextDocument #js {:content "(println \"test\")"
                                                        :language "clojure"})
            editor (vscode/window.showTextDocument doc)
            ;; Select the code
            _ (set! (.-selection editor)
                    (vscode/Selection. 0 0 0 17))
            ;; Set different clipboard content
            _ (vscode/env.clipboard.writeText "different-content")
            ;; Execute evaluate selection
            _ (vscode/commands.executeCommand "joyride.evaluateSelection")
            ;; Wait for completion
            _ (p/delay 500)
            ;; Check clipboard was restored (use native promise to avoid stack overflow)
            clipboard-after (.then (vscode/env.clipboard.readText) identity)]
      (is (= "different-content" clipboard-after)
          "Clipboard is restored even when different from selection"))))
