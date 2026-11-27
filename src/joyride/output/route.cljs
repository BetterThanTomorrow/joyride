(ns joyride.output.route
  "Decides where Joyride's output goes, and how a message reads in Calva's Output Destinations.")

(def ^:private calva-categories
  {:output.category/evaluated-code "evaluatedCode"
   :output.category/evaluation-results "evaluationResults"
   :output.category/evaluation-output "evaluationOutput"
   :output.category/evaluation-error-output "evaluationErrorOutput"
   :output.category/other-output "otherOutput"
   :output.category/other-error-output "otherErrorOutput"})

(def ^:private categories-calva-ends
  "The categories Calva writes a line end after."
  #{:output.category/evaluated-code
    :output.category/evaluation-results})

(def ^:private joyride-whos #{"joyride-user" "joyride-nrepl" "joyride"})

(def ^:private reserved-whos #{"ui" "api"})

(defn route
  "Returns where a message goes: `:calva`, `:hold`, `:joyride-output`, or `:nowhere` for an nREPL
   evaluation Calva sent, which Calva shows itself."
  [{:setting/keys [use-calva?]
    :calva/keys [state]
    :joyride/keys [starting?]
    :terminal/keys [live?]
    :output/keys [calva-client?]}]
  (cond
    (not use-calva?) :joyride-output
    (and (= :active state) calva-client?) :nowhere
    (= :active state) :calva
    (and (= :inactive state) starting? (not live?)) :hold
    :else :joyride-output))

(defn joyride-output-after-calva?
  "True when a message written to Calva goes to Joyride Output too: when Calva's `log()` returned
   nothing, or printed it nowhere but skipped a destination."
  [{:calva/keys [printed-to skipped] :as result}]
  (or (nil? result)
      (boolean (and (empty? printed-to)
                    (seq skipped)))))

(defn calva-message
  "Returns `message` as Calva's `log()` takes it, in the `joyride` session."
  [{:output/keys [category text who ns evaluation-id]}]
  (cond-> {:category (calva-categories category)
           :text (if (categories-calva-ends category)
                   (subs text 0 (- (count text) (if (.endsWith text "\n") 1 0)))
                   text)
           :who who
           :replSessionKey "joyride"}
    ns (assoc :ns ns)
    evaluation-id (assoc :evaluationId evaluation-id)))

(defn who-badge?
  "True when Joyride Output shows a badge for `who`. The who values Joyride sets itself get none."
  [who]
  (not (contains? joyride-whos who)))

(defn nrepl-who
  "Returns the who of an nREPL evaluation: the client's own `who`, or `joyride-user` for Calva and
   `joyride-nrepl` for other clients. A reserved who gets `joyride-nrepl`."
  [{:keys [who calva-client?]}]
  (cond
    (contains? reserved-whos who) "joyride-nrepl"
    (some? who) who
    calva-client? "joyride-user"
    :else "joyride-nrepl"))
