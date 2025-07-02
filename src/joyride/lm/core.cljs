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
  [extension-context file-path]
  (p/create
   (fn [resolve reject]
     (try
       (let [extension-path (when extension-context
                              (str (.-extensionPath ^js extension-context) "/" file-path))]
         (if extension-path
           (-> js/vscode .-workspace
               (.openTextDocument extension-path)
               (.then #(resolve (-> ^js % .-getText)))
               (.catch #(reject %)))
           (reject (js/Error. "Extension context not available"))))
       (catch js/Error e
         (reject e))))))
