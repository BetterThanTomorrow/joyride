(ns test-project
  "Starts and stops what the Joyride test protocol needs in this window: Calva
   connected to this window's Joyride nREPL server, and the tic-tac-toe Flare with
   Calva connected to its Scittle REPL. Every step looks at what is already
   running first, so `start!` can run again at any time without opening anything
   twice."
  (:require ["fs" :as fs]
            ["net" :as net]
            ["path" :as path]
            ["vscode" :as vscode]
            [joyride.flare :as flare]))

(def joyride-sequence
  {:name "Joyride"
   :projectType "joyride"
   :projectRootPath ["."]
   :nReplPortFile [".joyride" ".nrepl-port"]
   :selectedPortBehaviour "connect"
   :replSessionNames {:primary "joyride"}
   :replSessionFilePatterns {:primary {:always-claim [".joyride/**/*.cljs"]}}})

(def tic-tac-toe-port 13400)

(def tic-tac-toe-sequence
  {:name "Tic-tac-toe"
   :projectType "scittle"
   :projectRootPath ["."]
   :webSocketPort tic-tac-toe-port
   :replSessionNames {:primary "tic-tac-toe"}
   :replSessionFilePatterns {:primary {:always-claim ["tic-tac-toe/**/*.cljs"]}}})

(def tic-tac-toe-key :tic-tac-toe)

(def tic-tac-toe-dir "tic-tac-toe/replicant_tictactoe/")

(def scittle-url "https://cdn.jsdelivr.net/npm/scittle@0.8.31/dist/")

(def session-keys
  "The Calva sessions this window's test protocol uses."
  #{"joyride" "tic-tac-toe"})

;; Waiting

(defn- sleep+ [ms]
  (js/Promise. (fn [resolve] (js/setTimeout resolve ms))))

(defn ^:async wait-for!+
  "Polls `pred+` (a function returning a value or a promise) until it is truthy
   or `timeout-ms` has passed. Resolves to the last value."
  [pred+ timeout-ms]
  (loop [waited 0]
    (let [v (await (pred+))]
      (if (or v (>= waited timeout-ms))
        v
        (do (await (sleep+ 500))
            (recur (+ waited 500)))))))

(defn- with-timeout+
  "Resolves to what `promise` resolves to, or to `nil` after `ms`."
  [promise ms]
  (js/Promise.race #js [promise (.then (sleep+ ms) (constantly nil))]))

;; Calva

(defn ^:async calva-repl+ []
  (let [ext (vscode/extensions.getExtension "betterthantomorrow.calva")
        exports (await (.activate ext))]
    (.. exports -v1 -repl)))

(defn ^:async sessions+ []
  (js->clj (.listSessions (await (calva-repl+))) :keywordize-keys true))

(defn ^:async connected?+ [session-key]
  (boolean (some #(= session-key (:replSessionKey %)) (await (sessions+)))))

(defn ^:async answers?+
  "True when `session-key` evaluates `(+ 1 2)` to 3 within two seconds. A Scittle
   session waits for its browser, so it answers only when the Flare is connected."
  [session-key]
  (try
    (let [evaluation (await (with-timeout+
                              (.evaluate (await (calva-repl+))
                                         "(+ 1 2)"
                                         #js {:sessionKey session-key
                                              :ns "user"
                                              :who "test-project"
                                              :description "Check that the session answers"})
                              2000))]
      (= "3" (some-> evaluation .-result)))
    (catch :default _
      false)))

(defn- port-open?+ [port]
  (js/Promise.
   (fn [resolve]
     (let [socket (net/connect port "127.0.0.1")]
       (.on socket "connect" (fn []
                               (.end socket)
                               (resolve true)))
       (.on socket "error" (fn [_]
                             (resolve false)))))))

;; The tic-tac-toe Flare

(defn- workspace-root []
  (.. (first vscode/workspace.workspaceFolders) -uri -fsPath))

(defn- nrepl-port
  "The port in Joyride's nREPL port file, or nil."
  []
  (let [file (path/join (workspace-root) ".joyride" ".nrepl-port")]
    (when (fs/existsSync file)
      (let [n (js/parseInt (fs/readFileSync file "utf8") 10)]
        (when-not (js/isNaN n) n)))))

(defn ^:async start-nrepl-server!+
  "Starts this window's Joyride nREPL server unless the port in its port file
   takes connections. Resolves to `:was-running`, `:started` or `:failed`."
  []
  (let [port (nrepl-port)]
    (if (and port (await (port-open?+ port)))
      :was-running
      (await (-> (.startNReplServer (.-exports (vscode/extensions.getExtension "betterthantomorrow.joyride"))
                                    (workspace-root))
                 (.then (constantly :started))
                 (.catch (fn [e]
                           (println "test-project: the nREPL server didn't start:" (.-message e))
                           :failed)))))))

(defn ^:async connect-joyride!+
  "Connects Calva to Joyride's nREPL server through its port file, unless the
   `joyride` session is there. Resolves to `:was-connected`, `:connected` or
   `:timed-out`."
  []
  (if (await (connected?+ "joyride"))
    :was-connected
    (do
      ;; Not awaited: the command does not resolve when the session is up.
      (vscode/commands.executeCommand "calva.connect"
                                      (clj->js {:connectSequence joyride-sequence
                                                :disableAutoSelect true}))
      (if (await (wait-for!+ #(answers?+ "joyride") 60000))
        :connected
        :timed-out))))

(defn ^:async start-websocket-server!+
  "Connects Calva with the tic-tac-toe sequence, which starts Calva's WebSocket
   server, unless the server listens already. Calva makes the `tic-tac-toe`
   session when the Flare's page connects, so this waits for the port only.
   Resolves to `:was-listening`, `:started` or `:timed-out`."
  []
  (if (await (port-open?+ tic-tac-toe-port))
    :was-listening
    (do
      ;; Not awaited: the command resolves when a browser has connected.
      (vscode/commands.executeCommand "calva.connect"
                                      (clj->js {:connectSequence tic-tac-toe-sequence
                                                :disableAutoSelect true}))
      (if (await (wait-for!+ #(port-open?+ tic-tac-toe-port) 30000))
        :started
        :timed-out))))

(defn- tic-tac-toe-html []
  [:html
   [:head
    [:script (str "var SCITTLE_NREPL_WEBSOCKET_PORT = " tic-tac-toe-port ";"
                  "var SCITTLE_NREPL_WEBSOCKET_HOST = 'localhost';")]
    [:script {:src (str scittle-url "scittle.js")}]
    [:script {:src (str scittle-url "scittle.nrepl.js")}]
    [:script {:src (str scittle-url "scittle.replicant.js")}]
    [:script {:type "application/x-scittle" :src (str tic-tac-toe-dir "ui.cljs")}]
    [:script {:type "application/x-scittle" :src (str tic-tac-toe-dir "game.cljs")}]
    [:script {:type "application/x-scittle" :src (str tic-tac-toe-dir "core.cljs")}]
    [:link {:rel "stylesheet" :href (str tic-tac-toe-dir "style.css")}]]
   [:body
    [:h1 "Scittle tic-tac-toe built with Replicant"]
    [:div#app]]])

(defn open-tic-tac-toe!+
  "Opens the tic-tac-toe Flare, or reveals it when it is open. The page stays
   loaded while other editors cover the Flare. It connects to Calva's WebSocket
   server for the `tic-tac-toe` session."
  []
  (flare/flare!+ {:html (tic-tac-toe-html)
                  :title "Tic-tac-toe"
                  :key tic-tac-toe-key
                  :reveal? true
                  :preserve-focus? true
                  :webview-options {:retainContextWhenHidden true}}))

(defn- tic-tac-toe-open? []
  (contains? (flare/ls) tic-tac-toe-key))

(defn ^:async reload-tic-tac-toe!+
  "Closes the tic-tac-toe Flare and opens it again, which loads a new page with a
   new game. Resolves to `:reloaded` when the `tic-tac-toe` session answers from
   the new page, or to `:timed-out`."
  []
  (flare/close! tic-tac-toe-key)
  (await (open-tic-tac-toe!+))
  (if (await (wait-for!+ #(answers?+ "tic-tac-toe") 60000))
    :reloaded
    :timed-out))

(defn ^:async ensure-tic-tac-toe!+
  "Makes the `tic-tac-toe` session answer from the Flare. An open Flare gets
   `grace-ms` to connect, and is reloaded when it doesn't. Resolves to
   `:was-connected`, `:opened`, `:reloaded` or `:timed-out`."
  [grace-ms]
  (let [open? (tic-tac-toe-open?)]
    (cond
      (and open?
           (await (wait-for!+ #(answers?+ "tic-tac-toe") grace-ms)))
      :was-connected

      open?
      (await (reload-tic-tac-toe!+))

      :else
      (do
        (await (open-tic-tac-toe!+))
        (if (await (wait-for!+ #(answers?+ "tic-tac-toe") 60000))
          :opened
          :timed-out)))))

;; Start and stop

(defn- ok? [step]
  (contains? #{:was-running :started :was-connected :connected :was-listening} step))

(defn ^:async start!
  "Starts this window's Joyride nREPL server and connects Calva to it, starts
   Calva's WebSocket server for the Scittle REPL, and opens the tic-tac-toe Flare,
   whose page connects to it. Resolves to what each step found or did."
  []
  (let [nrepl-server (await (start-nrepl-server!+))
        joyride (when (ok? nrepl-server)
                  (await (connect-joyride!+)))
        websocket-server (await (start-websocket-server!+))
        tic-tac-toe (when (ok? websocket-server)
                      (await (ensure-tic-tac-toe!+ 5000)))
        result (cond-> {:nrepl-server nrepl-server
                        :websocket-server websocket-server}
                 joyride (assoc :joyride joyride)
                 tic-tac-toe (assoc :tic-tac-toe tic-tac-toe))]
    (println "test-project/start!" (pr-str result))
    result))

(defn ^:async disconnect!+
  "Disconnects the Calva connection that `session-key` is on, when it is there."
  [session-key]
  (when-let [client-key (->> (await (sessions+))
                             (filter #(= session-key (:replSessionKey %)))
                             first
                             :clientKey)]
    (await (vscode/commands.executeCommand "calva.disconnect" #js {:clientKey client-key}))))

(defn ^:async stop!
  "Closes the tic-tac-toe Flare, disconnects both Calva sessions, and stops the
   Joyride nREPL server."
  []
  (flare/close! tic-tac-toe-key)
  (await (disconnect!+ "tic-tac-toe"))
  (await (disconnect!+ "joyride"))
  (await (vscode/commands.executeCommand "joyride.stopNReplServer"))
  (let [stopped? (await (wait-for!+ #(.then (sessions+)
                                            (fn [sessions]
                                              (not-any? (comp session-keys :replSessionKey) sessions)))
                                    30000))
        result (if stopped? :stopped :timed-out)]
    (println "test-project/stop!" result)
    result))

(comment
  (start!)
  (stop!)
  (sessions+)
  (answers?+ "tic-tac-toe")
  (open-tic-tac-toe!+)
  (reload-tic-tac-toe!+)
  :rcf)
