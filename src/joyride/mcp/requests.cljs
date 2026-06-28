(ns joyride.mcp.requests
  (:require
   ["vscode" :as vscode]
   [joyride.lm.evaluation :as evaluation]
   [joyride.lm.human-intelligence :as human-intelligence]
   [promesa.core :as p]
   [vscode-mcp.manifest :as manifest]
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

(defn handle-request
  "Dispatches MCP JSON-RPC requests to Joyride's tools and resources."
  [{:keys [extension-context]} request]
  (let [{:keys [method params id]} request
        get-settings (fn []
                       (let [config (vscode/workspace.getConfiguration "joyride.lm")]
                         {"config.joyride.lm.enableReplTool" (.get config "enableReplTool")}))]
    (case method
      "initialize"
      (responses/success-response id
       {:protocolVersion "2024-11-05"
        :capabilities {:tools {}
                       :resources {}}
        :serverInfo {:name "joyride-mcp-server"
                     :version "0.0.1"}})

      "tools/list"
      (let [tools (manifest/get-tools extension-context {:settings (get-settings)})]
        (responses/success-response id {:tools tools}))

      "tools/call"
      (let [tool-name (:name params)
            args (:arguments params)]
        (-> (p/resolved (call-tool-impl tool-name args))
            (p/then (fn [lm-result]
                      (responses/success-response id (lm-result->mcp-result lm-result))))
            (p/catch (fn [e]
                       (responses/success-response id {:content [{:type "text" :text (.-message e)}]
                                                       :isError true})))))

      "resources/list"
      (let [resources (manifest/get-resources extension-context {:settings (get-settings)})]
        (responses/success-response id {:resources resources}))

      "resources/read"
      (let [resource (manifest/read-resource extension-context (:uri params) {:settings (get-settings)})]
        (if resource
          (responses/success-response id {:contents [(dissoc resource :skill-path)]})
          (responses/error-response id -32602 "Resource not found")))

      ;; Default for unknown methods
      (responses/error-response id -32601 (str "Method not found: " method)))))
