(ns git-fuzzy
  (:require
   ["vscode" :as vscode]
   [joyride.core :as joyride]
   [promesa.core :as p]))

;; “Install” by by placing this script in ~/.config/joyride/src
;; and add something like this to your keybindings.json
;; {
;;   "key": "ctrl+alt+j ctrl+alt+g",
;;   "command": "joyride.runCode",
;;   "args": "(require '[git-fuzzy :as gz] :reload) (gz/show-git-history!+)"
;; },

;; vscode.git API
;; https://github.com/Microsoft/vscode/blob/main/extensions/git/src/api/git.d.ts
;; (Ask Copilot to fetch it to get great help with hacking on the script.)

(def max-entries 5000)
(def batch-size 250)

(defn get-git-api!+ []
  (some-> (vscode/extensions.getExtension  "vscode.git")
          .-exports
          (.getAPI 1)))

(defn get-repositories!+ []
  (some-> (get-git-api!+)
          .-repositories))

(defn get-current-repository!+ []
  (first (get-repositories!+)))

(defn get-commit-history!+
  ([repo] (get-commit-history!+ repo {}))
  ([repo options]
   (let [default-options {:maxEntries max-entries}
         merged-options (merge default-options options)]
     (when repo
       (.log repo (clj->js merged-options))))))

(defn format-file-for-quickpick [commit file-change]
  (let [hash (.-hash commit)
        short-hash (subs hash 0 7)
        message (.-message commit)
        author-name (.-authorName commit)
        commit-date (.-commitDate commit)
        formatted-date (when commit-date
                         (.toUTCString commit-date))
        file-uri (.-uri file-change)
        file-path (vscode/workspace.asRelativePath file-uri)]
    #js {:label message
         :iconPath (vscode/ThemeIcon. "git-commit")
         :description (str "$(file) " file-path)
         :detail (str short-hash " - " author-name " - " formatted-date)
         :commit commit
         :fileChange file-change
         :fileUri file-uri
         :hash hash
         :buttons #js [#js {:name "copy"
                            :iconPath (vscode/ThemeIcon. "copy")
                            :tooltip "Copy commit id"}
                       #js {:name "open"
                            :iconPath (vscode/ThemeIcon. "go-to-file")
                            :tooltip "Open file"}]}))

(defn show-file-diff!+ [commit file-change preview?]
  (p/let [git-api (get-git-api!+)]
    (when (and git-api commit file-change)
      (let [hash (.-hash commit)
            parents (.-parents commit)
            uri (.-uri file-change)
            file-path (vscode/workspace.asRelativePath uri)
            parent-hash (first parents)
            status (.-status file-change)
            uri1 (.toGitUri git-api uri (if (#{1 11} status)
                                          hash
                                          parent-hash))
            uri2 (.toGitUri git-api uri (if (= 6 status)
                                          parent-hash
                                          hash))
            title (str "Diff: " file-path " (" (subs parent-hash 0 7) " → " (subs hash 0 7) ")")]
        (vscode/commands.executeCommand "vscode.diff"
                                        uri1
                                        uri2
                                        title
                                        #js {:preview preview?
                                             :preserveFocus preview?})))))

(defn get-commit-changes!+ [repo commit]
  (when (and repo commit)
    (let [hash (.-hash commit)
          parents (.-parents commit)]
      (if (empty? parents)
        (.diffWith repo hash)
        (let [parent-hash (first parents)]
          (.diffBetween repo parent-hash hash))))))

(defn show-git-history-search!+ []
  (p/let [repo (get-current-repository!+)
          _ (when-not repo
              (throw (js/Error. "No Git repository found in the current workspace")))

          quick-pick (vscode/window.createQuickPick)]

    (set! (.-busy quick-pick) true)
    (.show quick-pick)
    (set! (.-title quick-pick) "Git History Search")
    (set! (.-placeholder quick-pick) "Loading commit history... Please wait")
    ;; Preserve original item order during filtering
    (set! (.-sortByLabel quick-pick) false)

    (p/let [commits (get-commit-history!+ repo)
            total-commits (count commits)
            batches (partition-all batch-size commits)
            total-batches (count batches)
            _ (do (set! (.-placeholder quick-pick) (str "Processing " total-commits " commits in " total-batches " batches..."))
                  (set! (.-totalSteps quick-pick) total-batches))]

      (reduce (fn [acc-promise [batch-idx batch]]
                (p/let [acc acc-promise
                        _ (set! (.-step quick-pick) (inc batch-idx))
                        processed-commits (* batch-idx batch-size)
                        _ (set! (.-placeholder quick-pick)
                                (str "Processing batch " (inc batch-idx) "/" total-batches
                                     " (commits " processed-commits "-"
                                     (min (+ processed-commits batch-size) total-commits) ")"))
                        changes-promises (map #(get-commit-changes!+ repo %) batch)
                        all-changes (p/all changes-promises)
                        batch-results (->> (map vector batch all-changes)
                                           (mapcat (fn [[commit changes]]
                                                     (map #(format-file-for-quickpick commit %) changes))))
                        new-acc (concat acc batch-results)]

                  ;; Update items in QuickPick as we go (progressive loading!)
                  (set! (.-items quick-pick) (into-array new-acc))
                  new-acc))
              (p/resolved [])
              (map-indexed vector batches))

      (set! (.-busy quick-pick) false)
      (set! (.-step quick-pick) nil)
      (set! (.-totalSteps quick-pick) nil)
      (set! (.-placeholder quick-pick) "Fuzzy search commit messages")
      (set! (.-matchOnDescription quick-pick) true)
      (set! (.-matchOnDetail quick-pick) true)
      ;; Keep the result order as provided
      (set! (.-sortByLabel quick-pick) false)

      (.onDidChangeActive quick-pick (fn [active-items]
                                       (let [first-item (first active-items)]
                                         (when (and first-item (.-fileChange first-item))
                                           (show-file-diff!+ (.-commit first-item) (.-fileChange first-item) true)))))
      (.onDidAccept quick-pick
                    (fn [_e]
                      (p/let [selected-item (first (.-selectedItems quick-pick))
                              commit (.-commit selected-item)
                              file-change (.-fileChange selected-item)]
                        (when (and commit file-change)
                          (show-file-diff!+ commit file-change false))
                        (.dispose quick-pick))))
      (.onDidHide quick-pick
                  (fn [_e]
                    (.dispose quick-pick)))
      (.onDidTriggerItemButton quick-pick
                               (fn [e]
                                 (case (some-> e .-button .-name)
                                   "copy"
                                   (vscode/env.clipboard.writeText (.-hash (.-item e)))
                                   "open"
                                   (p/-> (vscode/workspace.openTextDocument (.-fileUri (.-item e)))
                                         (vscode/window.showTextDocument (.-fileUri (.-item e)) #{:preview true})))
                                 (.dispose quick-pick))))))

(defn ^:export show-git-history!+ []
  (p/catch
   (show-git-history-search!+)
   (fn [err]
     (vscode/window.showErrorMessage (str "Error: " err)))))

(when (= (joyride/invoked-script) joyride/*file*)
  (show-git-history!+))