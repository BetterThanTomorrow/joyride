(ns joyride.lm.core
  "Core utilities shared across language model tools"
  (:require
   ["vscode" :as vscode]
   [promesa.core :as p]))

(defn create-success-result
  "Create a successful tool result with standardized format"
  [content]
  (vscode/LanguageModelToolResult.
   #js [(vscode/LanguageModelTextPart. content)]))

(defn create-error-result
  "Create an error tool result with standardized format"
  [error-message]
  (vscode/LanguageModelToolResult.
   #js [(vscode/LanguageModelTextPart.
         (str "**Error:** " error-message))]))

(defn read-extension-file
  "Read a file from the extension directory.
   Returns a promise that resolves to the file content as string."
  [^js extension-context file-path]
  (p/let [extension-path (str (.-extensionPath extension-context) "/" file-path)
          ^js doc (vscode/workspace.openTextDocument extension-path)]
    (.getText doc)))
