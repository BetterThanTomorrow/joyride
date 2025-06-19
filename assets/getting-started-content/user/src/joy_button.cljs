(ns joy-button
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(defn ^:export show-joy-picker! []
  (p/let [options  [{:iconPath (vscode/ThemeIcon. "go-to-file")
                     :label "Open user_activate.cljs script"
                     :description "Edit your activation script"
                     :action :open-script}
                    {:iconPath (vscode/ThemeIcon. "copy")
                     :label "Copy link to Joyride Examples page"
                     :description "Check out examples in your browser of choice"
                     :action :copy-examples-link}
                    {:iconPath (vscode/ThemeIcon. "device-camera-video")
                     :label "Copy Joyride Copilot video url"
                     :description "Share the video around, please!"
                     :action :copy-video-url}]
          selection (vscode/window.showQuickPick (clj->js options)
                                                 #js {:title "Welcome, Hacker"
                                                      :placeHolder "What would you like to do?"})]

    (when selection
      (case (keyword (.-action selection))
        :open-script (p/do (vscode/commands.executeCommand "joyride.openUserScript" "../src/joy_button.cljs")
                           (vscode/commands.executeCommand "joyride.openUserScript" "user_activate.cljs"))
        :copy-examples-link (p/do (vscode/env.clipboard.writeText "https://github.com/BetterThanTomorrow/joyride/tree/master/examples")
                                  (vscode/window.showInformationMessage "Examples link copied to your clipboard" "OK"))
        :copy-video-url (p/do (vscode/env.clipboard.writeText "https://www.youtube.com/watch?v=-yYJV7WEFjI")
                              (vscode/window.showInformationMessage "Video link copied to your clipboard" "🎸 I'm sharing! ♥️"))
        nil))))

(defn install! []
  (let [status-item (vscode/window.createStatusBarItem vscode/StatusBarAlignment.Left -1000)]
    ;; During development, it's convenient to have a namespace reference to the status-item
    (def status-item status-item) ; In the `(comment ...)` form below you can dispose of the button
                                  ; and create it again
    ;; Configure status bar item
    (set! (.-text status-item) "$(smiley) Joy!")
    (set! (.-tooltip status-item) "Welcome to Joyride! Click for options.")
    (set! (.-command status-item) (clj->js {:command "joyride.runCode"
                                            :arguments ["(joy-button/show-joy-picker!)"]}))

    ;; Show the status bar item
    (.show status-item)

    ;; Return the disposable
    status-item))

(comment
  (.dispose status-item)
  (install!)
  :rcf)

