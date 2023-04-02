(ns sidecar-hello
  (:require ["child_process" :as child-process]
            ["path" :as path]
            ["util" :as node-util]
            ["vscode" :as vscode]
            [joyride.core :as joyride]
            [promesa.core :as p]))

(def exec!+ (node-util/promisify child-process/exec))
(def sidecar-dir (path/resolve (path/dirname joyride/*file*) "../sidecar"))

(defonce !db (atom {:disposables []}))

(defn- clear-disposables! []
  (run! (fn [disposable]
          (.dispose disposable))
        (:disposables @!db))
  (swap! !db assoc :disposables []))

(defn- push-disposables! [& disposables]
  (swap! !db update :disposables concat disposables)
  (-> (joyride/extension-context)
      .-subscriptions
      (.push disposables)))

(defn install-sidecar!+ []
  (-> (p/do!
       (exec!+ "npx vsce package"
               #js {:cwd sidecar-dir
                    :shell true})
       (exec!+ "code --install-extension joyride-sidecar-0.0.1.vsix"
               #js {:cwd sidecar-dir
                    :shell true}))
      (p/catch (fn [e]
                 (js/console.error "Error installing Sidecar:" e)))))

(defn uninstall-sidecar!+ []
  (vscode/commands.executeCommand "setContext"
                                  "joyride.sidecar:helloWorldViewEnabled"
                                  false)
  (when (vscode/extensions.getExtension "betterthantomorrow.joyride-sidecar")
    (-> (exec!+ "code --uninstall-extension betterthantomorrow.joyride-sidecar"
                #js {:shell true})
        (p/catch (fn [e]
                   (js/console.error "Error uninstalling Sidecar:" (.-message e)))))))

(defn wait-for-sidecar-extension+ []
  (p/create
   (fn [resolve _reject]
     (letfn [(poll [tries]
               (js/console.log "Waiting for Sidecar to be available: " tries)
               (let [sidecar (vscode/extensions.getExtension
                              "betterthantomorrow.joyride-sidecar")]
                 (if sidecar
                   (resolve sidecar)
                   (js/setTimeout #(poll (inc tries)) 10))))]
       (poll 1)))))

(def hello-provider
  #js {:getTreeItem (fn [element] element)
       :getChildren (fn [] #js [#js {:label "Hello World Item"
                                     :command
                                     #js {:command "hello-world.command"
                                          :arguments #js ["Hello World From Joyride Sidecar"
                                                          "OK"]}}])})

(defn activate!+ []
  (-> (p/do!
       (clear-disposables!)
       (uninstall-sidecar!+)
       (install-sidecar!+)
       (js/console.log "Sidecar installed")
       (p/-> (wait-for-sidecar-extension+)
             (.activate))
       (js/console.log "Sidecar activated"))
      (p/catch (fn [e]
                 (js/console.error "Error activating Joyride Sidecar:" e)))
      (p/then (fn []
                (push-disposables!
                 (vscode/commands.registerCommand
                  "hello-world.command"
                  (fn [& args]
                    (apply vscode/window.showInformationMessage args)))
                 (vscode/window.registerTreeDataProvider "hello-world.view" hello-provider))
                (vscode/commands.executeCommand "setContext"
                                                "joyride.sidecar:helloWorldViewEnabled"
                                                true)))))

(defn deactivate!+ []
  (clear-disposables!)
  (uninstall-sidecar!+))

(when (= (joyride/invoked-script) joyride/*file*)
  (activate!+))

(comment
  @!db
  (activate!+)
  (deactivate!+)
  :rcf)

