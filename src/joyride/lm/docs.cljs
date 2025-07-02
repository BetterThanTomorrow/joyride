(ns joyride.lm.docs
  "Documentation tools for language models"
  (:require
   ["vscode" :as vscode]
   [joyride.lm.core :as core]
   [promesa.core :as p]))

;; Constants
(def ^:private GITHUB-AGENT-GUIDE-URL
  "https://raw.githubusercontent.com/BetterThanTomorrow/joyride/refs/heads/master/assets/llm-contexts/agent-joyride-eval.md")

(def ^:private LOCAL-AGENT-GUIDE-PATH
  "assets/llm-contexts/agent-joyride-eval.md")

(defn- fetch-agent-guide [extension-context]
  (let [timeout-ms 10000
        controller (js/AbortController.)
        signal (.-signal controller)
        timeout-id (js/setTimeout #(.abort controller) timeout-ms)]
    (-> (p/let [response (js/fetch GITHUB-AGENT-GUIDE-URL #js {:signal signal})
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
           (p/let [text (core/read-extension-file extension-context LOCAL-AGENT-GUIDE-PATH)]
             {:type "success"
              :content text
              :source "local"})))
        (p/catch
         (fn [error]
           (js/console.error "Failed to read local agent guide" (.-message error))
           {:type "error"
            :message "Failed to fetch agent guide from both GitHub and local extension"}))
        ;; Add timeout cancellation to avoid hanging
        (p/finally (fn [] (js/clearTimeout timeout-id))))))

(defn- handle-basics-for-agents [options _token]
  (let [extension-context (.-extensionContext ^js options)]
    (p/let [result (fetch-agent-guide extension-context)]
      (if (= (:type result) "success")
        (core/create-success-result (:content result))
        (core/create-error-result (:message result))))))

(defn register-tool!
  "Register the Joyride Basics for Agents tool with VS Code's Language Model API.
   Returns the disposable for proper lifecycle management."
  [extension-context]
  (try
    (let [tool-impl #js {:extensionContext extension-context
                          :invoke handle-basics-for-agents}
          disposable (vscode/lm.registerTool "joyride_basics_for_agents" tool-impl)]
      (js/console.log "Joyride Basics for Agents LM Tool registered successfully")
      disposable)
    (catch js/Error e
      (js/console.error "Failed to register Joyride Basics for Agents LM Tool:" (.-message e))
      nil)))
