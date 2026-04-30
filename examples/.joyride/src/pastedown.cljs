;; npm install turndown turndown-plugin-gfm

;; Keybindings to facilitate pasting as markdown at will, even in the chat window:
  ;; {
  ;;   "key": "ctrl+alt+j ctrl+alt+v",
  ;;   "command": "joyride.runCode",
  ;;   "when": "inChatInput",
  ;;   "args": "(require 'pastedown :reload) (pastedown/pastedown-in-chat!)"
  ;; },
  ;; {
  ;;   "key": "ctrl+alt+j ctrl+alt+v",
  ;;   "command": "editor.action.pasteAs",
  ;;   "when": "editorTextFocus",
  ;;   "args": {
  ;;     "kind": "pastedown"
  ;;   }
  ;; },

(ns pastedown
  "Add Markdown formatting options to VS Code's 'Paste As...' command"
  (:require
   ["turndown" :as TurndownService]
   ["turndown-plugin-gfm" :as gfm]
   ["vscode" :as vscode]
   [clojure.string :as s]
   [joyride.core :as joyride]
   [promesa.core :as p]))

(defonce !db (atom {:disposables []}))

(defn- clear-disposables! []
  (run! (fn [disposable]
          (.dispose disposable))
        (:disposables @!db))
  (swap! !db assoc :disposables []))

(defn- push-disposable! [disposable]
  (swap! !db update :disposables conj disposable)
  (-> (joyride/extension-context)
      .-subscriptions
      (.push disposable)))

(defn- clean-list-content
  "Clean content but don't double-indent nested lists that are already indented"
  [content indent-spaces]
  (let [indent (apply str (repeat indent-spaces " "))]
    (-> content
        (.replace #"^\n+" "")              ; remove leading newlines
        (.replace #"\n+$" "\n")            ; keep single trailing newline
        ;; Only indent lines that don't already start with spaces (nested lists)
        (.replace #"\n(?! )" (str "\n" indent)))))  ; indent nested content with single space

(defn- get-list-prefix
  "Returns the prefix for a list item (bullet or number)"
  [node options]
  (let [parent (.-parentNode node)]
    (if (= (.-nodeName parent) "OL")
      (let [start (.getAttribute parent "start")
            children (.-children parent)
            index (.call (.-indexOf js/Array.prototype) children node)
            num (if start (+ (js/parseInt start) index) (inc index))]
        (str num ". "))
      (str (.-bulletListMarker options) " "))))

(defn- get-leading-indent
  "Get fixed indent: 2 spaces for bullet lists, 3 for numbered lists"
  [node]
  (when-let [parent (.-parentNode node)]
    (when (or (= (.-nodeName parent) "OL") (= (.-nodeName parent) "UL"))
      (when-let [grandparent (.-parentNode parent)]
        (when (= (.-nodeName grandparent) "LI")
          (let [gg-parent (.-parentNode grandparent)]
            (if (= (.-nodeName gg-parent) "OL")
              3  ; parent is in numbered list
              2)))))))

(defn- list-item-replacement
  "Replacement function for list items with proper 2/3 space nesting alignment"
  [content node options]
  (let [prefix (get-list-prefix node options)
        leading-indent-count (or (get-leading-indent node) 0)
        leading-indent (apply str (repeat leading-indent-count " "))
        total-indent-spaces (+ leading-indent-count (count prefix))
        cleaned-content (clean-list-content content total-indent-spaces)
        has-next (.-nextSibling node)
        needs-trailing-nl (and has-next (not (.test #"\n$" cleaned-content)))
        trailing-nl (if needs-trailing-nl "\n" "")]
    (str leading-indent prefix cleaned-content trailing-nl)))

(defn- copilot-link-replacement
  "Convert Copilot Chat's data-href links to proper markdown file links with workspace-relative paths"
  [content node _options]
  (if-let [data-href (.getAttribute node "data-href")]
    (let [decoded (js/decodeURIComponent data-href)
          file-match (re-find #"file://(/.*?)(?:#(\d+)(?:,(\d+))?)?$" decoded)]
      (if file-match
        (let [[_ file-path line-num _column-num] file-match
              relative-path (vscode/workspace.asRelativePath file-path)
              ;; Prepend file:// for absolute paths (files outside workspace)
              path-with-uri (if (s/starts-with? relative-path "/")
                              (str "file://" relative-path)
                              relative-path)
              link-target (if line-num
                            (str path-with-uri "#L" line-num)
                            path-with-uri)]
          (str "[" content "](" link-target ")"))
        content))
    content))

(defn convert-to-markdown
  "Intelligently convert clipboard content to markdown using turndown with full GFM support"
  [dataTransfer]
  (let [html-item (.get dataTransfer "text/html")
        plain-item (.get dataTransfer "text/plain")]
    (p/let [html (when html-item (.asString html-item))
            plain-text (when plain-item (.asString plain-item))
            turndown-service (TurndownService. #js {:headingStyle "atx"
                                                    :hr "---"
                                                    :bulletListMarker "-"
                                                    :codeBlockStyle "fenced"
                                                    :fence "```"
                                                    :emDelimiter "*"
                                                    :strongDelimiter "**"})]
      (.use turndown-service (.-gfm gfm))
      (.addRule turndown-service "listItem"
                #js {:filter "li"
                     :replacement list-item-replacement})
      (.addRule turndown-service "copilotLink"
                #js {:filter (fn [node]
                               (and (= (.-nodeName node) "A")
                                    (.hasAttribute node "data-href")))
                     :replacement copilot-link-replacement})
      (cond
        (and html (not (s/blank? html)))
        (.turndown turndown-service html)

        ;; If it looks like a URL, format as link
        (and plain-text (re-find #"^https?://" plain-text))
        (str "[" plain-text "](" plain-text ")")

        :else
        (str plain-text)))))

(def pastedown-kind (.append vscode/DocumentDropOrPasteEditKind.Empty "pastedown"))

(defn create-markdown-paste-edits
  "Create a single markdown paste edit with intelligent formatting"
  [dataTransfer]
  ;; Return a promise since convert-to-markdown is now async
  (p/let [markdown-content (convert-to-markdown dataTransfer)]
    #js [(new vscode/DocumentPasteEdit
              markdown-content
              "Insert Markdown"
              pastedown-kind)]))

(defn create-markdown-paste-provider
  "Create a paste provider that intelligently converts rich text to markdown"
  []
  #js {:provideDocumentPasteEdits
       (fn [_document _ranges dataTransfer _context _token]
         (when-let [raw-text (.get dataTransfer "text/plain")]
           (let [text (if (string? raw-text) raw-text (str raw-text))]
             (when (and text (not (s/blank? text)))
               (create-markdown-paste-edits dataTransfer)))))})

(defn register-markdown-paste-provider!
  "Register the markdown paste provider with VS Code"
  []
  (clear-disposables!)
  (let [provider (create-markdown-paste-provider)
        disposable (vscode/languages.registerDocumentPasteEditProvider
                    "*"
                    provider
                    #js {:providedPasteEditKinds #js [pastedown-kind]
                         :pasteMimeTypes #js ["text/plain" "text/html"]})]
    (push-disposable! disposable)

    (println "📋 Markdown paste provider registered! Use Ctrl+Shift+V (Cmd+Shift+V on Mac) after copying text to see markdown formatting options.")

    disposable))

(defn ^:export deactivate!
  "Remove all markdown paste providers"
  []
  (clear-disposables!)
  (vscode/window.showInformationMessage
   "Markdown paste providers removed."))

(defn status
  "Show current status of markdown paste providers"
  []
  (let [count (count (:disposables @!db))]
    {:active? (pos? count)
     :providers-count count
     :disposables (:disposables @!db)}))

(defn ^:export pastedown-in-chat!
  "In the chat window, our provider isn't available. We do this cheesy thing."
  []
  (p/let [doc (vscode/workspace.openTextDocument #js {:language "markdown" :content ""})
          _ (vscode/window.showTextDocument doc)
          _ (vscode/commands.executeCommand "editor.action.pasteAs" #js {:kind "pastedown"})
          _ (p/delay 0) ;; For some reason, select all won't work without this delay
          _ (vscode/commands.executeCommand "editor.action.selectAll")
          _ (vscode/commands.executeCommand "editor.action.clipboardCopyAction")
          _ (vscode/commands.executeCommand "undo")
          _ (vscode/commands.executeCommand "workbench.action.closeActiveEditor")
          _ (vscode/commands.executeCommand "workbench.panel.chat")
          _ (vscode/commands.executeCommand "editor.action.clipboardPasteAction")]))

(defn ^:export activate!
  "Main function to register the markdown paste provider"
  []
  (register-markdown-paste-provider!))

;; Auto-run when script is invoked, but not when loading the code in the REPL
(when (= (joyride/invoked-script) joyride/*file*)
  (activate!))
