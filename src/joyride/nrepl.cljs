(ns joyride.nrepl
  "Original implementation taken from https://github.com/viesti/nrepl-cljs-sci."
  (:require
   ["net" :as node-net]
   ["path" :as path]
   ["vscode" :as vscode]
   [joyride.bencode :refer [encode decode-all]]
   [joyride.when-contexts :as when-contexts]
   [joyride.repl-utils :as repl-utils :refer [the-sci-ns]]
   [joyride.sci :as jsci]
   [joyride.utils :refer [info warn error cljify]]
   [promesa.core :as p]
   [sci.core :as sci]
   [sci.ctx-store :as store]
   [clojure.pprint :as pp]
   [clojure.string :as string]))

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

(defn eval-ctx-mw [handler {:keys [sci-ctx-atom]}]
  (fn [request send-fn]
    (handler (assoc request
                    :sci-ctx-atom sci-ctx-atom)
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

(defn format-value [nrepl-pprint pprint-options value]
  (if nrepl-pprint
    (if-let [pprint-fn (pretty-print-fns-map nrepl-pprint)] 
      (let [{:keys [right-margin length level]} pprint-options]
        (binding [*print-length* length
                  *print-level* level
                  pp/*print-right-margin* right-margin]
          (with-out-str (pprint-fn value))))
      (do
        (debug "Pretty-Printing is only supported for cider.nrepl.pprint/pprint")
        (pr-str value)))
    (pr-str value)))

(defn do-handle-eval [{:keys [ns code _sci-ctx-atom _load-file? file] :as request} send-fn]
  (sci/with-bindings
    {sci/ns ns
     sci/print-length @sci/print-length
     sci/print-newline true
     sci/file (or file @sci/file)}
    ;; we alter-var-root this because the print-fn may go out of scope in case
    ;; of returned delays
    (sci/alter-var-root sci/print-fn (constantly
                                      (fn [s]
                                        (send-fn request {"out" s}))))
    (try (let [v (jsci/eval-string code)]
           (sci/alter-var-root sci/*3 (constantly @sci/*2))
           (sci/alter-var-root sci/*2 (constantly @sci/*1))
           (sci/alter-var-root sci/*1 (constantly v))
           (send-fn request {"value" (format-value (:nrepl.middleware.print/print request)
                                                   (:nrepl.middleware.print/options request)
                                                   v)
                             "ns" (str @sci/ns)})
           (send-fn request {"status" ["done"]}))
         (catch :default e
           (sci/alter-var-root sci/*e (constantly e))
           (let [data (ex-data e)]
             (when-let [message (or (:message data) (.-message e))]
               (send-fn request {"err" (str message "\n")}))
             (send-fn request {"ex" (str e)
                               "ns" (str @sci/ns)
                               "status" ["done"]}))))))

(defn handle-eval [{:keys [ns sci-ctx-atom] :as request} send-fn]
  (do-handle-eval (assoc request :ns (or (when ns
                                           (the-sci-ns @sci-ctx-atom (symbol ns)))
                                         @jsci/!last-ns
                                         @sci/ns))
                  send-fn))

(defn handle-clone [request send-fn]
  (send-fn request {"new-session" (str (random-uuid))
                    "status" ["done"]}))

(defn handle-close [request send-fn]
  (send-fn request {"status" ["done"]}))

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
                           :ns @sci/ns
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
         (if had-error?
           (debug "Connection lost")
           (debug "Connection closed")))))

(defn port-file-uri [root-uri]
  (vscode/Uri.file
   (path/join root-uri ".joyride" ".nrepl-port")))

(defn remove-port-file [^js path]
  (let [uri (port-file-uri path)]
    (-> uri
        vscode/workspace.fs.stat
        (p/then (fn [stat]
                  (when stat
                    (vscode/workspace.fs.delete uri)))))))

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
        ctx-atom (store/get-ctx)
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
                         (partial on-connect {:sci-ctx-atom ctx-atom}))]
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
                          (info "nREPL server started on port" port "on host"
                                (str host "- nrepl://" host ":" port))
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
           (error "Invalid host address, check setting `joyride.nreplHostAddress`")))))))

(defn start-server+ [opts]
  (if-not (server-running?)
    (start-server'+ opts)
    (do
      (info "The nREPL server is already running")
      (p/rejected (js/Error. "The nREPL server is already running" {})))))

(defn stop-server+ []
  (sci/alter-var-root sci/print-fn (constantly *print-fn*))
  (p/create (fn [resolve _reject]
              (if (server-running?)
                (let [server (::server @!db)]
                  (debug "nREPL stop-server")
                  (.close server
                          (fn []
                            (swap! !db dissoc ::server)
                            (p/do
                              (when-contexts/set-context! ::when-contexts/joyride.isNReplServerRunning false)
                              (-> (remove-port-file (::root-path @!db))
                                  (p/then (fn []
                                            (swap! !db dissoc ::root-path)
                                            (info "nREPL server stopped")
                                            (resolve server))))))))
                (do (info "There is no nREPL Server running")
                    (resolve))))))

(defn enable-message-logging! []
  (swap! !db ::log-messages? true))

(defn disable-message-logging! []
  (swap! !db ::log-messages? false))

(comment
  (-> (start-server+ {:root-path "/Users/pez/Projects/joyride/playground" #_"/hello-joyride"})
      (p/catch #(js/console.error %)))
  (stop-server+)
  (server-running?)
  )