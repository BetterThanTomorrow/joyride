(ns joyride.mcp.registry)

(defn enabled?
  "Registry is on unless the setting is explicitly false."
  [setting]
  (not (false? setting)))

(defn compact-data
  "Extra registry fields. vscode-mcp owns name, workspace, host, and port file."
  []
  {})
