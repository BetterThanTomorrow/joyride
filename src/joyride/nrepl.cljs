(ns joyride.nrepl
  "Original implementation taken from https://github.com/viesti/nrepl-cljs-sci."
  (:require
   ["net" :as node-net]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.bencode :refer [encode decode-all]]
   [joyride.when-contexts :as when-contexts]
   [joyride.printing :as printing]
   [joyride.repl-utils :as repl-utils :refer [the-sci-ns]]
   [joyride.sci :as jsci]
   [joyride.utils :refer [cljify]]
   [joyride.vscode-utils :refer [info warn error]]
   [joyride.who-tracking :as who-tracking]
   [promesa.core :as p]
   [sci.core :as sci]
   [sci.ctx-store :as store]
   [clojure.pprint :as pp]
   [clojure.string :as string]
   [joyride.output :as output]
   [joyride.output.route :as route]))

(defonce !db (atom {::log-messages? false
                    ::server nil
                    ::root-path nil}))

(defn debug [& objects]
  (when true
    (apply (.-debug js/console) objects)))

(defn response-for-mw [handler]
  (fn [{:keys [id session] :as request} response]
    (let [response (cond-> (assoc response
                                  "id" id)
                     session (assoc "session" session))]
      (handler request response))))

(defn coerce-request-mw [handler]
  (fn [request send-fn]
    (handler (update request :op keyword) send-fn)))

(defn log-request-mw [handler]
  (fn [request send-fn]
    (when (::log-messages? @!db)
      (debug "request" request))
    (handler request send-fn)))

(defn log-response-mw [handler]
  (fn [request response]
    (when (::log-messages? @!db)
      (debug "response" response))
    (handler request response)))

(defn eval-ctx-mw [handler {:keys [sci-ctx]}]
  (fn [request send-fn]
    (handler (assoc request
                    :sci-ctx sci-ctx)
             send-fn)))

(declare ops)

(defn version-string->data [v]
  (assoc (zipmap ["major" "minor" "incremental"]
                 (js->clj (.split v ".")))
         "version-string" v))

(defn handle-describe [request send-fn]
  (send-fn request
           {"versions" {"nbb-nrepl" (version-string->data "TODO")
                        "node" (version-string->data js/process.version)}
            "aux" {}
            "ops" (zipmap (map name (keys ops)) (repeat {}))
            "status" ["done"]}))

(def pretty-print-fns-map
  {"cider.nrepl.pprint/pprint" pp/write})

(defn- pprint-str
  "Returns `value` pretty printed with `pprint-fn` and the client's `pprint-options`,
   or on one line within the same limits when pretty printing throws."
  [pprint-fn {:keys [right-margin length level]} value fallback-opts]
  (try
    (binding [*print-length* length
              *print-level* level
              pp/*print-right-margin* right-margin]
      (with-out-str (pprint-fn value :dispatch printing/pprint-dispatch)))
    (catch :default _
      (printing/value->str value (assoc fallback-opts :print-length length :print-level level)))))

(defn format-value [nrepl-pprint pprint-options value]
  (let [fallback-opts (jsci/print-fallback-opts)]
    (if-let [pprint-fn (some-> nrepl-pprint pretty-print-fns-map)]
      (pprint-str pprint-fn pprint-options value fallback-opts)
      (do
        (when nrepl-pprint
          (debug "Pretty-Printing is only supported for cider.nrepl.pprint/pprint"))
        (printing/value->str value fallback-opts)))))

(defn- error-str
  "Returns `x` as `str` gives it, `nil` as \"nil\", or `x` printed with `printing/value->str` when `str` throws."
  [x]
  (if (nil? x)
    "nil"
    (try
      (str x)
      (catch :default _
        (printing/value->str x {})))))

(defn calva-session?
  "True when Calva cloned the nREPL `session`, as it says in the `client-name` it clones with."
  [session]
  (= "Calva" (get-in @!db [::sessions session :client-name])))

(defn- start-nrepl-evaluation!
  "Records the nREPL evaluation `request` and writes its code, or for `load-file` the file it loads.
   Returns the evaluation, as `output/start-evaluation!` does."
  [{:keys [ns code load-file? file session] :as request}]
  (let [calva-client? (calva-session? session)
        who (route/nrepl-who {:who (:who request) :calva-client? calva-client?})
        context {:output/who who
                 :output/ns (str ns)
                 :output/calva-client? calva-client?}]
    (who-tracking/record-evaluation! who)
    (if load-file?
      (do
        (output/append-line-other-out! (str "Loading file: " file) context)
        {:evaluation/context context
         :evaluation/end! (fn [])})
      (output/start-evaluation! code context))))

(defn do-handle-eval [{:keys [ns code _sci-ctx file] :as request} send-fn]
  (sci/with-bindings
    {sci/ns ns
     sci/print-length @sci/print-length
     sci/print-newline true
     sci/file (or file @sci/file)}
    (sci/alter-var-root sci/print-fn (constantly
                                      (fn [s]
                                        (send-fn request {"out" s}))))
    (sci/alter-var-root sci/print-err-fn (constantly
                                          (fn [s]
                                            (send-fn request {"err" s}))))
    (let [{:evaluation/keys [context end!]} (start-nrepl-evaluation! request)
          !pending-promise? (volatile! false)]
      (try (let [v (jsci/eval-string code ns context)
                 send-result! (fn [val promise?]
                                (sci/alter-var-root sci/*3 (constantly @sci/*2))
                                (sci/alter-var-root sci/*2 (constantly @sci/*1))
                                (sci/alter-var-root sci/*1 (constantly val))
                                (let [formatted (format-value (:nrepl.middleware.print/print request)
                                                              (:nrepl.middleware.print/options request)
                                                              val)]
                                  (send-fn request {"value" (if promise?
                                                              (str "#<Promise " formatted ">")
                                                              formatted)
                                                    "ns" (str @sci/ns)}))
                                (send-fn request {"status" ["done"]}))]
             (if (instance? js/Promise v)
               (do
                 (vreset! !pending-promise? true)
                 (-> v
                     (.then (fn [resolved]
                              (send-result! resolved true)))
                     (.catch (fn [rejected]
                               (sci/alter-var-root sci/*e (constantly rejected))
                               (send-fn request {"err" (str (error-str rejected) "\n")})
                               (send-fn request {"ex" (error-str rejected)
                                                 "ns" (str @sci/ns)
                                                 "status" ["done"]})))
                     (.finally end!)))
               (send-result! v false)))
           (catch :default e
             (sci/alter-var-root sci/*e (constantly e))
             (let [data (ex-data e)]
               (when-let [message (or (:message data) (.-message e))]
                 (send-fn request {"err" (str (error-str message) "\n")}))
               (send-fn request {"ex" (error-str e)
                                 "ns" (str @sci/ns)
                                 "status" ["done"]})))
           (finally
             (when-not @!pending-promise?
               (end!)))))))

(defn handle-eval [{:keys [ns sci-ctx] :as request} send-fn]
  (do-handle-eval (assoc request :ns (or (when ns
                                           (the-sci-ns sci-ctx (symbol ns)))
                                         @jsci/!last-ns
                                         @sci/ns))
                  send-fn))

(defn handle-clone [{:keys [client-name] :as request} send-fn]
  (let [session (str (random-uuid))]
    (swap! !db assoc-in [::sessions session] {:client-name client-name})
    (send-fn request {"new-session" session
                      "status" ["done"]})))

(defn handle-close [{:keys [session] :as request} send-fn]
  (swap! !db update ::sessions dissoc session)
  (send-fn request {"status" ["done" "session-closed"]}))

#_(defn handle-classpath [request send-fn]
    (send-fn
     request
     {"status" ["done"]
      "classpath"
      (cp/split-classpath (cp/get-classpath))}))

(defn handle-load-file [{:keys [file file-path] :as request} send-fn]
  (sci/with-bindings {sci/file file-path}
    (do-handle-eval (assoc request
                           :code file
                           :load-file? true
                           :ns (or @jsci/!last-ns @sci/ns)
                           :file file-path)
                    send-fn)))

;;;; Completions, based on babashka.nrepl

(defn handle-complete [request send-fn]
  (send-fn request (repl-utils/handle-complete* request)))

;;;; End completions

(def ops
  "Operations supported by the nrepl server"
  {:eval handle-eval
   :describe handle-describe
   :clone handle-clone
   :close handle-close
   ;; :classpath handle-classpath
   :load-file handle-load-file
   :complete handle-complete})

(defn handle-request [{:keys [op] :as request} send-fn]
  (if-let [op-fn (get ops op)]
    (op-fn request send-fn)
    (do
      (when (::log-messages? @!db)
        (warn "Unhandled operation" op))
      (send-fn request {"status" ["error" "unknown-op" "done"]}))))

(defn make-request-handler [opts]
  (-> handle-request
      coerce-request-mw
      (eval-ctx-mw opts)
      log-request-mw))

(defn make-send-fn [socket]
  (fn [_request response]
    (.write socket (encode response))))

(defn make-response-handler [socket]
  (-> (make-send-fn socket)
      log-response-mw
      response-for-mw))

(defn on-connect [opts socket]
  (debug "Connection accepted")
  (swap! !db update ::sockets (fnil conj #{}) socket)
  (.setNoDelay ^node-net/Socket socket true)
  (let [handler (make-request-handler opts)
        response-handler (make-response-handler socket)
        pending (atom nil)]
    (.on ^node-net/Socket socket "data"
         (fn [data]
           (let [data (if-let [p @pending]
                        (let [s (str p data)]
                          (reset! pending nil)
                          s)
                        data)
                 [requests unprocessed] (decode-all data :keywordize-keys true)]
             (when (not (string/blank? unprocessed))
               (reset! pending unprocessed))
             (doseq [request requests]
               (handler request response-handler))))))
  (.on ^node-net/Socket socket "close"
       (fn [had-error?]
         (swap! !db update ::sockets disj socket)
         (if had-error?
           (debug "Connection lost")
           (debug "Connection closed")))))

(defn port-file-uri [root-uri]
  (vscode/Uri.file
   (path/join root-uri ".joyride" ".nrepl-port")))

(defn remove-port-file
  "Deletes the nREPL port file under `path`. Resolves when the file is gone, also when it wasn't there."
  [^js path]
  (-> (vscode/workspace.fs.delete (port-file-uri path))
      (p/catch (fn [e]
                 (when-not (= "FileNotFound" (.-code e))
                   (throw e))))))

(defn server-running? []
  (when-contexts/context ::when-contexts/joyride.isNReplServerRunning))

(defn- start-server'+
  "Start nREPL server. Accepts options either as JS object or Clojure map."
  [js-or-clj-opts]
  (let [opts (cljify js-or-clj-opts)
        port (or (:port opts)
                 0)
        root-path ^js (or (:root-path opts)
                          vscode/workspace.rootPath)
        _log_level (or (if (object? opts)
                         (.-log_level ^Object opts)
                         (:log_level opts))
                       "info")
        ctx (store/get-ctx)
        #_#_on-exit (js/require "signal-exit")]

    (swap! !db assoc ::root-path root-path)

    ;; TODO: I don't understand the following comment
    ;; Expose "app" key under js/app in the repl

    #_(on-exit (fn [_code _signal]
                 (debug "Process exit, removing port file")
                 (remove-port-file (port-file-uri))))

    (p/create
     (fn [resolve _reject]
       (let [nrepl-host-address (-> (vscode/workspace.getConfiguration "joyride")
                                    (.get "nreplHostAddress"))]
         (if (< 0 (node-net/isIP nrepl-host-address))
           (let [server (node-net/createServer
                         (partial on-connect {:sci-ctx ctx}))]
             (swap! !db assoc ::server server)
             (p/do
               (when-contexts/set-context! ::when-contexts/joyride.isNReplServerRunning true))
             (.listen server
                      port
                      nrepl-host-address
                      (fn []
                        (let [addr (-> server (.address))
                              port (-> addr .-port)
                              host (-> addr .-address)]
                          (output/append-line-other-out!
                           (str "nREPL server started on port " port "on host "
                                host " - nrepl://" host ":" port))
                          (-> (vscode/workspace.fs.writeFile
                               (port-file-uri root-path)
                               (-> (new js/TextEncoder) (.encode (str port))))
                              (p/handle (fn [_result error]
                                          (resolve port)
                                          (when error
                                            (warn "Could not write port file" error))))))))
             (.on server "error"
                  (fn [e]
                    (when-contexts/set-context! ::when-contexts/joyride.isNReplServerRunning false)
                    (.close server)
                    (error "Problems with the nREPL server connection:" e))))
           (error "Invalid host address" "Check setting `joyride.nreplHostAddress`")))))))

(defn start-server+ [opts]
  (if-not (server-running?)
    (start-server'+ opts)
    (do
      (info "The nREPL server is already running")
      (p/rejected (js/Error. "The nREPL server is already running" {})))))

(defn stop-server+
  "Stops the nREPL server and ends its open connections, which the server waits for before it
   closes. Resolves to the server once its port file is removed."
  []
  (p/let [stopped+ (p/create (fn [resolve reject]
                               (if (server-running?)
                                 (let [server (::server @!db)]
                                   (debug "nREPL stop-server")
                                   (.close server
                                           (fn []
                                             (swap! !db dissoc ::server)
                                             (p/do
                                               (when-contexts/set-context! ::when-contexts/joyride.isNReplServerRunning false)
                                               (-> (remove-port-file (::root-path @!db))
                                                   (p/catch (fn [e]
                                                              (warn "Could not remove the nREPL port file" e)))
                                                   (p/then (fn []
                                                             (swap! !db dissoc ::root-path)
                                                             (output/append-other-out! "nREPL server stopped")
                                                             (resolve server)))))))
                                   (doseq [^node-net/Socket socket (::sockets @!db)]
                                     (.destroy socket)))
                                 (do (output/append-other-err! "There is no nREPL Server running")
                                     (reject (js/Error. "There is no nREPL Server running"))))))]
    (sci/alter-var-root sci/print-fn (constantly *print-fn*))
    (sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*))
    stopped+))

(defn enable-message-logging! []
  (swap! !db ::log-messages? true))

(defn disable-message-logging! []
  (swap! !db ::log-messages? false))

(comment
  (-> (start-server+ {:root-path "/Users/pez/Projects/joyride/playground" #_"/hello-joyride"})
      (p/catch #(js/console.error %)))
  (stop-server+)
  (server-running?))
