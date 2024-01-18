(require '["vscode" :as vscode]
         '[promesa.core :as p])

(defn show-hello-message []
  (p/let [button (vscode/window.showInformationMessage "Hello World!" "Cancel" "OK")]
    (if (= "OK" button)
      (vscode/window.showInformationMessage "You clicked OK! Try clicking Cancel too?.")
      (p/let [name (vscode/window.showInputBox #js {:title "CIA wants to know",
                                                    :prompt "What is your name?"})]
        (vscode/window.showInformationMessage (str "Hello " name))))))

(show-hello-message)