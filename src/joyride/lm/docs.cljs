(ns joyride.lm.docs
  "Documentation tools for language models"
  (:require
   ["vscode" :as vscode]
   ["path" :as path]
   [joyride.lm.core :as core]
   [promesa.core :as p]
   [clojure.string :as string]))

(def agent-guide-path
  "assets/llm-contexts/agent-joyride-eval.md")

(def ^:private github-base-url
  "https://raw.githubusercontent.com/BetterThanTomorrow/joyride/refs/heads/master")

(defn- fetch-agent-guide [extension-context file-path]
  (let [timeout-ms 10000
        controller (js/AbortController.)
        signal (.-signal controller)
        timeout-id (js/setTimeout #(.abort controller) timeout-ms)]
    (-> (p/let [response (js/fetch (str github-base-url "/" file-path) #js {:signal signal})
                _ (js/clearTimeout timeout-id)
                _ (when-not (.-ok response)
                    (throw (js/Error. (str "GitHub fetch failed: " (.-status response)))))
                text (.text response)]
          {:type "success"
           :content text
           :source "github"})
        (p/catch
         (fn [error]
           (js/console.warn "Failed to fetch agent guide from GitHub" (.-message error))
           (-> (p/let [text (core/read-extension-file extension-context (apply path/join (string/split file-path "/")))]
                 {:type "success"
                  :content text
                  :source "local"})
               (p/catch
                (fn [error]
                  (js/console.error "Failed to read local agent guide" (.-message error))
                  {:type "error"
                   :message "Failed to fetch agent guide from both GitHub and local extension"})))))
        (p/finally (fn [] (js/clearTimeout timeout-id))))))

(defn- invoke-tool! [extension-context file-path]
  (fn [^js _options _token]
    (p/let [result (fetch-agent-guide extension-context file-path)]
      (if (= (:type result) "success")
        (core/create-success-result (:content result))
        (core/create-error-result (:message result))))))

(defn register-tool!
  "Register `tool-name` with VS Code's Language Model API, serving `file-path`.
   Returns the disposable for proper lifecycle management."
  [extension-context tool-name file-path]
  (try
    (let [tool-impl #js {:invoke (invoke-tool! extension-context file-path)
                         :extensionContext extension-context
                         :filePath file-path}
          disposable (vscode/lm.registerTool tool-name tool-impl)]
      (js/console.log tool-name "LM Tool registered successfully")
      disposable)
    (catch js/Error e
      (js/console.error "Failed to register" tool-name "LM Tool:" (.-message e))
      nil)))
