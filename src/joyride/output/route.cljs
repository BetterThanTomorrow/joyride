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

(def ^:private first-display-version
  "The first Calva version whose `log()` writes to the Output Destinations."
  [2 0 613])

(defn display-api?
  "True when Calva `version` has the display option of `log()`."
  [version]
  (let [numbers (mapv js/parseInt (take 3 (re-seq #"\d+" (str version))))]
    (and (= 3 (count numbers))
         (not (neg? (compare numbers first-display-version))))))

(defn route
  "Returns where a message goes: `:calva`, `:hold`, `:joyride-output`, or `:nowhere` for an nREPL
   evaluation Calva sent, which Calva shows itself. Only Joyride's own messages are held, and only
   for a Calva that can show them."
  [{:setting/keys [use-calva?]
    :calva/keys [state display?]
    :joyride/keys [starting?]
    :terminal/keys [live?]
    :output/keys [who calva-client?]}]
  (cond
    (not (and use-calva? display?)) :joyride-output
    (and (= :active state) calva-client?) :nowhere
    (= :active state) :calva
    (and (= :inactive state)
         starting?
         (not live?)
         (= "joyride" who)) :hold
    :else :joyride-output))

(defn joyride-output-after-calva?
  "True when a message written to Calva goes to Joyride Output too: when Calva's `log()` returned
   nothing, or printed it nowhere but skipped a destination."
  [{:calva/keys [printed-to skipped] :as result}]
  (or (nil? result)
      (boolean (and (empty? printed-to)
                    (seq skipped)))))

(defn- calva-text
  "Returns the text Calva shows for a message: a promise's value as the promise, and a stream line
   with its line end. Calva ends evaluated code and results itself."
  [{:output/keys [category text promise? line?]}]
  (cond
    promise? (str "#<Promise " text ">")
    (and line? (not (categories-calva-ends category))) (str text "\n")
    :else text))

(defn calva-message
  "Returns `message` as Calva's `log()` takes it, in the `joyride` session."
  [{:output/keys [category who ns evaluation-id] :as message}]
  (cond-> {:category (calva-categories category)
           :text (calva-text message)
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
