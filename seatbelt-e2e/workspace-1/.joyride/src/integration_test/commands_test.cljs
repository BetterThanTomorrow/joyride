(ns integration-test.commands-test
  (:require
   [cljs.test :refer [is testing]]
   [integration-test.macros :refer [deftest-async]]
   [promesa.core :as p]
   ["vscode" :as vscode]))

(deftest-async evaluate-selection-without-crash
  (testing "Evaluate selection command completes without stack overflow"
    (p/let [doc (vscode/workspace.openTextDocument #js {:content "(+ 1 2)"
                                                        :language "clojure"})
            editor (vscode/window.showTextDocument doc)
            _ (set! (.-selection editor)
                    (vscode/Selection. 0 0 0 7))
            _ (vscode/env.clipboard.writeText "original-clipboard-content")
            _ (vscode/commands.executeCommand "joyride.evaluateSelection")
            _ (p/delay 500)
            clipboard-after (.then (vscode/env.clipboard.readText) identity)]
      (is (= "original-clipboard-content" clipboard-after)
          "Clipboard content is restored after evaluation"))))

(deftest-async evaluate-selection-with-empty-selection
  (testing "Evaluate selection handles empty selection gracefully"
    (p/let [doc (vscode/workspace.openTextDocument #js {:content "(+ 1 2)"
                                                        :language "clojure"})
            editor (vscode/window.showTextDocument doc)
            _ (set! (.-selection editor)
                    (vscode/Selection. 0 0 0 0))
            _ (vscode/commands.executeCommand "joyride.evaluateSelection")
            _ (p/delay 300)]
      (is true "Command handles empty selection without error"))))

(deftest-async evaluate-selection-preserves-different-clipboard
  (testing "Evaluate selection preserves clipboard when selection differs from clipboard"
    (p/let [doc (vscode/workspace.openTextDocument #js {:content "(println \"test\")"
                                                        :language "clojure"})
            editor (vscode/window.showTextDocument doc)
            _ (set! (.-selection editor)
                    (vscode/Selection. 0 0 0 17))
            _ (vscode/env.clipboard.writeText "different-content")
            _ (vscode/commands.executeCommand "joyride.evaluateSelection")
            _ (p/delay 500)
            clipboard-after (.then (vscode/env.clipboard.readText) identity)]
      (is (= "different-content" clipboard-after)
          "Clipboard is restored even when different from selection"))))
