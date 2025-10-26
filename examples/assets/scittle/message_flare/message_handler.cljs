(ns message-handler.core
  (:require [clojure.string :as str]))

(def vscode (js/acquireVsCodeApi))

(declare animate-progress!)

(defn log-message [text]
  (let [log-element (js/document.getElementById "messageLog")
        timestamp (.toLocaleTimeString (js/Date.))
        entry-div (js/document.createElement "div")]
    (set! (.-textContent entry-div) (str "[" timestamp "] " text))
    (.appendChild log-element entry-div)
    (set! (.-scrollTop log-element) (.-scrollHeight log-element))))

(defn send-message! [type data]
  (.postMessage vscode (clj->js {:type type :data data :timestamp (.now js/Date)}))
  (log-message (str "📤 Sent: " type " - " data)))

(defn handle-incoming-message [message-event]
  (let [message (js->clj (.-data message-event) :keywordize-keys true)
        msg-type (:type message)
        msg-data (:data message)]
    (log-message (str "📥 Received: " msg-type " - " msg-data))
    (case msg-type
      "animate-progress" (animate-progress!)
      "color-feedback" (log-message (str "🎨 " msg-data))
      "response" (log-message (str "💬 " msg-data))
      (log-message (str "❓ Unknown: " msg-type)))))

(defn show-alert! []
  (log-message "🚨 Alert clicked!")
  (send-message! "alert-clicked" "Alert button clicked"))

(defn change-color! []
  (let [colors [{:gradient "linear-gradient(135deg, #667eea 0%, #764ba2 100%)" :name "Purple Blend"}
                {:gradient "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)" :name "Pink Passion"}
                {:gradient "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)" :name "Ocean Blue"}
                {:gradient "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)" :name "Green Mint"}
                {:gradient "linear-gradient(135deg, #fa709a 0%, #fee140 100%)" :name "Sunset Glow"}]
        random-color (rand-nth colors)]
    (set! (.. js/document -body -style -background) (:gradient random-color))
    (log-message (str "🎨 Changed to " (:name random-color)))
    (send-message! "color-changed" (:name random-color))))

(defn animate-progress! []
  (let [progress-fill (js/document.getElementById "progressFill")
        progress-text (js/document.getElementById "progressText")
        target-progress (rand-int 100)]
    (set! (.-width (.-style progress-fill)) (str target-progress "%"))
    (set! (.-textContent progress-text) (str target-progress "%"))
    (log-message (str "📊 Progress: " target-progress "%"))
    (send-message! "progress-animated" target-progress)))

(defn process-input! []
  (when-let [input (js/document.getElementById "textInput")]
    (let [value (.-value input)]
      (when-not (empty? value)
        (log-message (str "💬 Input: \"" value "\""))
        (send-message! "input-processed" value)
        (set! (.-value input) "")
        (let [lower-value (str/lower-case value)]
          (cond
            (str/includes? lower-value "hello") (log-message "👋 Hello there!")
            (str/includes? lower-value "test") (log-message "✅ Test successful!")
            (str/includes? lower-value "flare") (log-message "🔥 Flares are awesome!")
            :else (log-message (str "🤔 Interesting: \"" value "\""))))))))

(defn init! []
  ;; Set up message handler
  (.addEventListener js/window "message" handle-incoming-message)

  ;; Set up button handlers
  (when-let [btn (js/document.getElementById "alertBtn")]
    (.addEventListener btn "click" (fn [] (show-alert!))))

  (when-let [btn (js/document.getElementById "colorBtn")]
    (.addEventListener btn "click" (fn [] (change-color!))))

  (when-let [btn (js/document.getElementById "progressBtn")]
    (.addEventListener btn "click" (fn [] (animate-progress!))))

  (when-let [btn (js/document.getElementById "processBtn")]
    (.addEventListener btn "click" (fn [] (process-input!))))

  ;; Set up keyboard handler
  (when-let [input (js/document.getElementById "textInput")]
    (.addEventListener input "keydown"
                      (fn [e] (when (= (.-key e) "Enter") (process-input!)))))

  ;; Initial logs
  (log-message "✅ Scittle ClojureScript loaded!")
  (log-message "🔗 Bi-directional messaging enabled")

  ;; Timer demo
  (js/setTimeout
   (fn []
     (log-message "⏰ 2-second timer completed")
     (send-message! "timer-completed" "2 seconds elapsed"))
   2000))

;; Initialize when script loads
(init!)
