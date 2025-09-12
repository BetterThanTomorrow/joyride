(ns joyride.flare.error-handling
  "Error handling and validation for flare requests")

(defn validate-flare-options
  "Validate flare options structure and return normalized options"
  [options]
  (cond
    ;; Check that options is a map
    (not (map? options))
    (throw (ex-info "Flare options must be a map"
                    {:provided options
                     :expected "map"}))

    ;; Check that either :html or :url is provided
    (and (not (:html options)) (not (:url options)))
    (throw (ex-info "Flare options must specify either :html or :url content"
                    {:provided options
                     :expected "map with :html or :url key"}))

    ;; Check that both :html and :url are not provided
    (and (:html options) (:url options))
    (throw (ex-info "Flare options cannot specify both :html and :url content"
                    {:provided options
                     :expected "map with either :html or :url key, not both"}))

    ;; Validate :html content
    (and (:html options)
         (not (or (string? (:html options))
                  (vector? (:html options)))))
    (throw (ex-info "Flare :html content must be a string or Hiccup vector"
                    {:provided (:html options)
                     :expected "string or vector starting with keyword/string tag"}))

    ;; Validate :url content
    (and (:url options)
         (not (string? (:url options))))
    (throw (ex-info "Flare :url content must be a string"
                    {:provided (:url options)
                     :expected "string"}))

    ;; Validate :title if provided
    (and (:title options)
         (not (string? (:title options))))
    (throw (ex-info "Flare :title must be a string"
                    {:provided (:title options)
                     :expected "string"}))

    ;; Validate :key if provided
    (and (:key options)
         (not (or (string? (:key options))
                  (keyword? (:key options)))))
    (throw (ex-info "Flare :key must be a string or keyword"
                    {:provided (:key options)
                     :expected "string or keyword"}))

    ;; All validations passed
    :else
    options))

(defn validate-hiccup-structure
  "Validate that a data structure is proper Hiccup format"
  [hiccup-data]
  (cond
    ;; Must be a vector
    (not (vector? hiccup-data))
    (throw (ex-info "Hiccup data must be a vector"
                    {:provided hiccup-data
                     :expected "vector starting with keyword or string tag"}))

    ;; Must not be empty
    (empty? hiccup-data)
    (throw (ex-info "Hiccup data cannot be empty"
                    {:provided hiccup-data
                     :expected "vector starting with keyword or string tag"}))

    ;; First element must be a keyword or string (tag name)
    (not (or (keyword? (first hiccup-data))
             (string? (first hiccup-data))))
    (throw (ex-info "Hiccup data must start with a keyword or string tag"
                    {:provided (first hiccup-data)
                     :expected "keyword like :div or string like \"div\""}))

    ;; If second element exists and is a map, validate it as attributes
    (and (> (count hiccup-data) 1)
         (map? (second hiccup-data))
         (not (every? #(or (keyword? %) (string? %)) (keys (second hiccup-data)))))
    (throw (ex-info "Hiccup attributes must have keyword or string keys"
                    {:provided (second hiccup-data)
                     :expected "map with keyword or string keys"}))

    ;; All validations passed
    :else
    hiccup-data))

(defn safe-flare-processing
  "Safely process flare options with comprehensive error handling"
  [options process-fn]
  (try
    ;; Validate options first
    (let [validated-options (validate-flare-options options)]
      ;; If HTML is Hiccup, validate the structure
      (when (and (:html validated-options)
                 (vector? (:html validated-options)))
        (validate-hiccup-structure (:html validated-options)))

      ;; Process the validated options
      (process-fn validated-options))

    (catch js/Error e
      ;; Re-throw with additional context
      (throw (ex-info (str "Flare processing failed: " (.-message e))
                      {:original-options options
                       :error-type (.-name e)
                       :original-error e})))

    (catch :default e
      ;; Handle any other type of error
      (throw (ex-info (str "Unexpected error during flare processing: " (str e))
                      {:original-options options
                       :error e})))))