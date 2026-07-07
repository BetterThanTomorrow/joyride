(ns joyride.mcp.requests
  (:require
   ["vscode" :as vscode]
   [joyride.lm.evaluation :as evaluation]
   [joyride.lm.human-intelligence :as human-intelligence]
   [promesa.core :as p]
   [vscode-mcp.manifest :as manifest]
   [vscode-mcp.requests :as mcp-requests]
   [vscode-mcp.responses :as responses]))

(defn- lm-result->mcp-result [^js result]
  (if-not result
    {:content [{:type "text" :text "nil"}]}
    (let [content-parts (.-content result)
          mcp-content (mapv (fn [^js part]
                              {:type "text"
                               :text (.-value part)})
                            content-parts)]
      {:content mcp-content})))

(defn- call-tool-impl [tool-name args]
  (let [options #js {:input (clj->js args)}
        token #js {:isCancellationRequested false}]
    (case tool-name
      "joyride_evaluate_code"
      (evaluation/invoke-tool options token)

      "joyride_request_human_input"
      ((human-intelligence/invoke-tool!) options token)

      (throw (js/Error. (str "Unknown tool: " tool-name))))))

(defn- get-settings []
  (let [config (vscode/workspace.getConfiguration "joyride.lm")]
    {"config.joyride.lm.enableReplTool" (.get config "enableReplTool")}))

(defn- request-opts []
  {:settings (get-settings)
   :initialize-opts {:base-text "Joyride MCP server provides access to VS Code Extension API via the Small Clojure Interpreter (SCI)."
                      :settings (get-settings)}})

(defn handle-request
  [{:keys [extension-context]} request]
  (let [{:keys [method params id]} request
        opts (request-opts)]
    (case method
      "tools/call"
      (let [tool-name (:name params)
            args (:arguments params)
            allowed (manifest/tool-call-allowed? extension-context tool-name {:settings (:settings opts)})]
        (cond
          (= :disabled allowed) (responses/error-response id -32601 "Unknown tool")
          :else (-> (p/let [lm-result (call-tool-impl tool-name args)]
                      (responses/success-response id (lm-result->mcp-result lm-result)))
                    (p/catch (fn [e]
                               (responses/success-response id {:content [{:type "text" :text (.-message e)}]
                                                               :isError true}))))))
      (mcp-requests/handle-manifest-request extension-context request opts))))
