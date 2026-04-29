(require '["vscode" :as vscode])

(defn ^:async show-hello-message []
  (let [button (await (vscode/window.showInformationMessage "Hello World!" "Cancel" "OK"))]
    (if (= "OK" button)
      (vscode/window.showInformationMessage "You clicked OK! Try clicking Cancel too?.")
      (let [name (await (vscode/window.showInputBox #js {:title "CIA wants to know",
                                                         :prompt "What is your name?"}))]
        (vscode/window.showInformationMessage (str "Hello " name))))))

(show-hello-message)