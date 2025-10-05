(ns flares-examples
  "Demonstrates Joyride Flares for creating WebView panels and sidebar views"
  (:require
   ["vscode" :as vscode]
   [joyride.core]
   [joyride.flare :as flare]
   [promesa.core :as p]))

(comment
  ;; Simple greeting panel, with themed icon
  (flare/flare!+ {:html [:h1 "Hello, Joyride Flares!"]
                  :icon {:dark "assets/circle-dark-theme.svg"
                         :light "assets/circle-light-theme.svg"}
                  :title "Greeting"
                  :key "greeting"})

  ;; Icon from url
  (flare/flare!+ {:html [:img {:src "https://raw.githubusercontent.com/sindresorhus/awesome/refs/heads/main/media/logo.png"}]
                  :title "Awesome"
                  :icon "https://raw.githubusercontent.com/sindresorhus/awesome/refs/heads/main/media/logo.png"
                  :key :awesome})

  ;; Simple greeting panel using HTML string instead of Hiccup, default icon
  (flare/flare!+ {:html "<h1>Hello, Joyride Flares!</h1>"
                  :title "Greeting HTML"
                  :key :greeting})

  ;; Default flare-key is `:anonymous`
  (flare/flare!+ {:html [:h1 "An anonymous Joyride Flare"]
                  :title "I get a default key"})

  (flare/flare!+ {:html [:h1 "An updated anonymous Joyride Flare"]
                  :title "Because the same default key"})

  ; Inspect the result in the repl
  (p/let [editor-flare (flare/flare!+ {:html [:h1 "A Flare"]
                                       :title "a flare"
                                       :key 42})]
    (def flare editor-flare)
    ; flare => {42 <view-object>}
    )

  ;; Sidebar example
  (flare/flare!+ {:html [:div {:style {:padding "10px"}}
                         [:h3 "Joyride Sidebar"]
                         [:p "This flare appears in the sidebar instead of a separate panel."]
                         [:ul
                          [:li "Useful for quick info"]
                          [:li "Persistent views"]
                          [:li "Space-efficient"]]
                         [:hr]
                         [:small "Use " [:code ":key :sidebar-1"] " through " [:code ":key :sidebar-5"] " for sidebar slots"]]
                  :title "Sidebar Demo"
                  :reveal? true
                  :key :sidebar-1})

  ; List all currently open flares
  (flare/ls)

  (flare/get-flare :sidebar-1)

  (flare/close! :sidebar-1)

  (flare/close-all!)

  (flare/ls)

  (flare/flare!+ {:html [:svg {:height 200 :width 200 :style {:border "1px solid #ccc"}}
                         [:circle {:r 30 :cx 50 :cy 50 :fill "red" :opacity 0.7}]
                         [:rect {:x 70 :y 70 :width 60 :height 40 :fill :blue :opacity 0.7}]
                         [:line {:x1 10 :y1 180 :x2 190 :y2 20 :stroke :green :stroke-width 3}]
                         [:text {:x 100 :y 190 :text-anchor :middle :fill :purple} "Joyride!"]]
                  :title "SVG Shapes"
                  :key :some-svg})


  ;; Bidirectional message example
  (flare/flare!+
   {:html [:div
           [:h1 "Message Handler Test"]
           [:img {:src "assets/joyride-logo.png"}]
           [:p "Testing message handling:"]
           [:button {:onclick "sendMessage('button-click', 'Hello from button!')"}
            "Send Message"]
           [:div
            [:input {:type "text" :id "textInput" :placeholder "Type something..."}]
            [:button {:onclick "sendTextMessage()"}
             "Send Text"]]
           [:div
            "log:"
            [:pre {:id "messageLog"}]]
           [:script {:type "text/javascript"}
            "
               // Acquire VS Code API once when the page loads
               const vscode = acquireVsCodeApi();

               function sendMessage(type, data) {
                 vscode.postMessage({type: type, data: data, timestamp: Date.now()});
                 console.log('Sent message:', type, data);
               }

               function sendTextMessage() {
                 const input = document.getElementById('textInput');
                 sendMessage('text-input', input.value);
                 input.value = '';
               }

               function handleMessage(message) {
                 console.log('Received message:', message);
                 const messageLogElement = document.getElementById('messageLog');
                 const logEntry = document.createElement('div');
                 logEntry.textContent = `[${new Date().toLocaleTimeString()}] Type: ${message.type}, Data: ${message.data} ${message.data.foo}`;
                 messageLogElement.appendChild(logEntry);
               }

               window.addEventListener('message', event => {
                 const message = event.data; // Message from extension
                 handleMessage(message);
               });


               "]]
    :title "Message Test"
    :key :sidebar-2
    :reveal? false
    :preserve-focus? true
    :webview-options {:enableScripts true
                      :retainContextWhenHidden true}
    :message-handler (fn [message]
                       (let [msg-type (.-type message)
                             msg-data (.-data message)]
                         (println "🔥 Received message from flare, type:" msg-type "data:" msg-data)
                         (case msg-type
                           "button-click" (println "✅ Button was clicked!")
                           "text-input" (println "📝 Text input received:" msg-data)
                           (println "❓ Unknown message type:" msg-type))))})

  (flare/post-message!+ :sidebar-2 {:type "command" :data {:foo "foo"}})

  (flare/get-flare :sidebar-2)

  (flare/ls)

  (flare/close! :sidebar-1)

  (flare/close-all!)

  (flare/flare!+ {:file "assets/example-flare.html"
                  :title "My HTML File"
                  :key :my-html-file-test})

  (flare/flare!+ {:file "assets/test-flare.edn"
                  :title "My Hiccup File"
                  :key :sidebar-4
                      ;:reveal? true
                  :preserve-focus? true
                  :webview-options {:enableScripts true
                                    :retainContextWhenHidden true}
                  :message-handler (fn [message]
                                     (let [msg-type (.-type message)
                                           msg-data (.-data message)]
                                       (println "🔥 Received message from flare, type:" msg-type "data:" msg-data)
                                       (case msg-type
                                         "button-click" (println "✅ Button was clicked!")
                                         "text-input" (println "📝 Text input received:" msg-data)
                                         (println "❓ Unknown message type:" msg-type))))})

  (p/let [result (flare/post-message!+ :sidebar-4 {:type "command" :data #js {:foo "foo"}})]
    (def result result))

  (flare/flare!+
   {:file "assets/example-flare.html"
    :title "HTML File with Bi-directional Messaging"
    :key "html-file-messaging"
    :message-handler
    (fn [message]
      (let [msg-type (.-type message)
            msg-data (.-data message)]
        (println "🔥 Message from HTML file, type:" msg-type "data:" msg-data)
        (case msg-type
          "alert-clicked"
          (do
            (println "🚨 Alert button was clicked!" (pr-str msg-data))
            (apply vscode/window.showInformationMessage msg-data)
            (flare/post-message!+ "html-file-messaging"
                                  {:type "response"
                                   :data "Alert acknowledged from Clojure! 🎉"}))
          "color-changed"
          (do
            (println "🎨 Color changed to:" msg-data)
            (flare/post-message!+ "html-file-messaging"
                                  {:type "color-feedback"
                                   :data (str "Beautiful " msg-data " choice! 🌈")}))
          "input-processed"
          (do
            (println "📝 Input processed:" msg-data)
            (flare/post-message!+ "html-file-messaging"
                                  {:type "input-response"
                                   :data (str "Clojure processed: '" msg-data "' ✨")}))
          "progress-animated"
          (do
            (println "📊 Progress animated to:" msg-data)
            (flare/post-message!+ "html-file-messaging"
                                  {:type "progress-feedback"
                                   :data (str "Progress at " msg-data "% - "
                                              (cond
                                                (< msg-data 25) "Just getting started! 🌱"
                                                (< msg-data 50) "Making good progress! 🚀"
                                                (< msg-data 75) "More than halfway there! 💪"
                                                (< msg-data 90) "Almost finished! 🔥"
                                                :else "Excellent work! 🎯"))}))
          "timer-completed"
          (do
            (println "⏰ Timer completed:" msg-data)
            (flare/post-message!+ "html-file-messaging"
                                  {:type "response"
                                   :data "Timer event received in Clojure! ⏰"}))

          (println "❓ Unknown message type:" msg-type))))})

  (flare/post-message!+ "html-file-messaging" {:type "animate-process" :data {}})

  (flare/flare!+ {:url "https://calva.io/"
                  :title "My URL Flare"
                  :key :my-file-test})
  :rcf)



(def j-icon-svg
  [:svg {:width "256"
         :height "256"
         :viewBox "0 0 2132 2132"
         :fill :none
         :xmlns "http://www.w3.org/2000/svg"}
   [:path
    {:d "M1193.77 193.156L1156.35 203.053L1158.62 241.698C1162.17 301.978 1163.95 365.96 1163.95 433.661C1163.95 686.826 1140.78 925.228 1094.6 1148.98C1071.71 1259.89 1041.91 1340.7 1007.18 1394.59C973.11 1447.47 936.705 1471.12 899.172 1476.8C883.031 1478.8 879.876 1474.75 879.128 1473.8C879.098 1473.75 879.068 1473.71 879.039 1473.68C875.724 1469.72 869.129 1456.57 869.129 1423.62C869.129 1369.47 882.817 1298.28 912.681 1208.67L947.209 1105.1L848.01 1150.7L512.613 1304.87L492.302 1314.21L486.611 1335.83C467.475 1408.55 458 1484.74 458 1564.26C458 1699.5 488.547 1817.16 552.256 1914.72L552.565 1915.19L552.886 1915.65C623.717 2018.68 738.183 2066 884.005 2066C1064.46 2066 1210.28 1962.97 1322.18 1775.28C1434.13 1590.5 1516.76 1353.04 1571.52 1064.73C1627.95 777.03 1661.54 465.266 1672.43 129.554L1674.49 66L1613.01 82.2598L1193.77 193.156Z"
     :fill "#FEE719"
     :stroke :black
     :stroke-width "97.7548"}]
   [:path {:class "sunrise"
           :d "M1500.08 1184.37C1497.61 1193.45 1453.33 1380.68 1455.97 1343.23C1408.91 1500.86 1350.79 1636.75 1281.61 1750.89C1175.22 1929.41 1042.69 2018.66 884.005 2018.66C748.77 2018.66 651.395 1975.39 591.887 1888.84C534.184 1800.48 505.336 1692.29 505.336 1564.26C505.336 1538 506.423 1512.17 508.591 1486.77C609.168 1447.94 714.611 1410.94 824.544 1375.9C822.714 1392.72 821.794 1408.63 821.794 1423.62C821.794 1497.54 849.745 1530.91 905.647 1523.69C991.414 1511.03 1058 1434.24 1105.39 1293.31C1328.14 1232.99 1249.65 1240.05 1500.08 1184.37Z"
           :fill "#F00C18"}]])

(comment
  (flare/flare!+ {:html
                  [:div {:style {:text-align :center
                                 :padding-top "100px"
                                 :overflow :hidden
                                 :height "500px"}}
                   [:style "@keyframes pulse-glow {
                              0%, 100% { fill-opacity: 0.1;}
                              50% { fill-opacity: 0.4;}
                            }
                            @keyframes rotate-slow {
                              from { transform: rotate(0deg); }
                              to { transform: rotate(360deg); }
                            }
                            @keyframes bounce {
                              0%, 100% { transform: translateY(18px); }
                              50% { transform: translateY(-18px); }
                            }
                            .sunrise { animation: pulse-glow 2.5s ease-in-out infinite; }
                            .icon-bounce { animation: bounce 4s ease-in-out infinite; }
                            .bg-rotate { animation: rotate-slow 20s linear infinite; }
                          "]
                   [:div {:style {:position :relative :display :inline-block}}
                    [:div {:class "bg-rotate"
                           :style {:position :absolute
                                   :top "-40px" :left "-40px"
                                   :width "336px" :height "336px"
                                   :border "7px solid #FEE719"
                                   :background "#F00C18"
                                   :border-radius "20%"
                                   :z-index 1}}]
                    [:div {:class "icon-bounce" :style {:position :relative :z-index 2}}
                     j-icon-svg]]]
                  :title "J-icon"
                  :key :sidebar-3})
  :rcf)

;; Data table example
(defn data-table [data]
  [:table {:style {:border-collapse :collapse :width "100%"}}
   [:thead
    [:tr
     (for [header (keys (first data))]
       [:th {:style {:border "1px solid #ddd; padding: 8px"
                     :color :darkgrey
                     :background "#f2f2f2"}}
        (name header)])]]
   [:tbody
    (for [row data]
      [:tr
       (for [value (vals row)]
         [:td {:style {:border "1px solid #ddd; padding: 8px"}}
          (str value)])])]])

(def sample-data
  [{:name "Alice" :age 30 :city "New York"}
   {:name "Bob" :age 25 :city "San Francisco"}
   {:name "Carol" :age 35 :city "Chicago"}])

(comment
  (flare/flare!+ {:html [:div
                         [:h2 "Sample Data"]
                         (data-table sample-data)]
                  :title "Data Table"
                  :key "data-table"})
  :rcf)

;; Multiple Sidebar Slots Demo
(comment
  ;; Show multiple sidebar flares at once using different slots
  (flare/flare!+ {:html [:div {:style {:padding "10px"}}
                         [:h3 "🔥 Sidebar Slot 1"]
                         [:p "This appears in the first sidebar slot"]
                         [:p "Always visible when content is present"]]
                  :title "Slot 1"
                  :key :sidebar-1})

  (flare/flare!+ {:html [:div {:style {:padding "10px"}}
                         [:h3 "⚡ Sidebar Slot 2"]
                         [:p "This appears in the second sidebar slot"]
                         [:p "Only visible when content is added"]]
                  :title "Slot 2"
                  :key :sidebar-2})

  (flare/flare!+ {:html [:div {:style {:padding "10px"}}
                         [:h3 "🎯 Sidebar Slot 3"]
                         [:p "This appears in the third sidebar slot"]
                         [:p "Independent of other slots"]]
                  :title "Slot 3"
                  :key :sidebar-3})

  ;; Close individual slots
  (flare/close! :sidebar-2)
  (flare/close! :sidebar-3)

  ;; Or close all
  (flare/close-all!)

  :rcf)

;; Fancy animated flare (vibe coded)
(def fancy-html
  [:div
   ;; Add CSS animations in a style tag
   [:style "
     @keyframes pulse {
       0% { transform: scale(1); }
       50% { transform: scale(1.05); }
       100% { transform: scale(1); }
     }
     @keyframes float {
       0%, 100% { transform: translateY(0px); }
       50% { transform: translateY(-10px); }
     }
     .pulse { animation: pulse 2s infinite; }
     .float { animation: float 3s ease-in-out infinite; }
     .float-delay { animation: float 3s ease-in-out infinite 0.5s; }
     .float-delay-2 { animation: float 3s ease-in-out infinite 1s; }
   "]

   ;; Main container with style map
   [:div {:style {:font-family "'SF Pro Display', system-ui, sans-serif"
                  :background "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
                  :color :white
                  :padding "30px"
                  :border-radius "16px"
                  :min-height "500px"}}

    ;; Header
    [:div {:style {:text-align :center :margin-bottom "40px"}}
     [:h1 {:class "pulse"
           :style {:font-size "3em"
                   :margin "0"
                   :text-shadow "2px 2px 8px rgba(0,0,0,0.5)"}}
      "✨ Fancy Joyride Flare ✨"]
     [:p {:style {:font-size "1.3em" :opacity "0.9" :margin-top "10px"}}
      "A beautiful, animated HTML experience"]]

    ;; Feature cards
    [:div {:style {:display :flex
                   :justify-content :space-around
                   :flex-wrap :wrap
                   :gap "20px"
                   :margin-bottom "40px"}}

     [:div {:class "float"
            :style {:background "rgba(255,255,255,0.15)"
                    :backdrop-filter "blur(10px)"
                    :border-radius "20px"
                    :padding "25px"
                    :min-width "180px"
                    :text-align :center
                    :border "1px solid rgba(255,255,255,0.2)"
                    :box-shadow "0 8px 32px rgba(0,0,0,0.1)"}}
      [:div {:style {:font-size "3em" :margin-bottom "15px"}} "🚀"]
      [:h3 {:style {:margin "0 0 10px 0" :font-size "1.4em"}} "Fast"]
      [:p {:style {:margin "0" :opacity "0.8" :font-size "0.95em"}} "Lightning quick rendering"]]

     [:div {:class "float-delay"
            :style {:background "rgba(255,255,255,0.15)"
                    :backdrop-filter "blur(10px)"
                    :border-radius "20px"
                    :padding "25px"
                    :min-width "180px"
                    :text-align :center
                    :border "1px solid rgba(255,255,255,0.2)"
                    :box-shadow "0 8px 32px rgba(0,0,0,0.1)"}}
      [:div {:style {:font-size "3em" :margin-bottom "15px"}} "⚡"]
      [:h3 {:style {:margin "0 0 10px 0" :font-size "1.4em"}} "Interactive"]
      [:p {:style {:margin "0" :opacity "0.8" :font-size "0.95em"}} "Dynamic and responsive"]]

     [:div {:class "float-delay-2"
            :style {:background "rgba(255,255,255,0.15)"
                    :backdrop-filter "blur(10px)"
                    :border-radius "20px"
                    :padding "25px"
                    :min-width "180px"
                    :text-align :center
                    :border "1px solid rgba(255,255,255,0.2)"
                    :box-shadow "0 8px 32px rgba(0,0,0,0.1)"}}
      [:div {:style {:font-size "3em" :margin-bottom "15px"}} "🎨"]
      [:h3 {:style {:margin "0 0 10px 0" :font-size "1.4em"}} "Beautiful"]
      [:p {:style {:margin "0" :opacity "0.8" :font-size "0.95em"}} "Modern design language"]]]

    ;; Progress section
    [:div {:style {:margin "40px 0"}}
     [:h3 {:style {:margin-bottom "20px" :font-size "1.3em"}} "🔋 Joyride Power Level"]
     [:div {:style {:background "rgba(255,255,255,0.2)"
                    :border-radius "30px"
                    :height "24px"
                    :overflow :hidden
                    :position :relative}}
      [:div {:style {:background "linear-gradient(90deg, #00ff88, #00ccff, #8c52ff)"
                     :height "100%"
                     :width "87%"
                     :border-radius "30px"
                     :box-shadow "0 0 20px rgba(0,255,136,0.4)"}}]]
     [:div {:style {:text-align :right :margin-top "8px" :font-size "1em" :opacity "0.9"}} "87% Awesome"]]

    ;; Footer
    [:div {:style {:text-align :center
                   :margin-top "40px"
                   :padding-top "25px"
                   :border-top "1px solid rgba(255,255,255,0.2)"}}
     [:div {:style {:display :flex
                    :justify-content :center
                    :align-items :center
                    :gap "10px"
                    :margin-bottom "15px"}}
      [:span {:style {:font-size "1.5em"}} "❤️"]
      [:span {:style {:font-size "1.1em"}} "Created with Joyride Flares"]
      [:span {:style {:font-size "1.5em"}} "🎉"]]
     [:p {:style {:margin "0" :font-size "0.9em" :opacity "0.7"}}
      (str "Generated at " (.toLocaleTimeString (js/Date.)))]
     [:p {:style {:margin "10px 0 0 0" :font-size "0.85em" :opacity "0.6"}}
      "Interactive ClojureScript in VS Code"]]]])

(comment
  ;; Fancy animated flare with modern design and CSS animations
  (flare/flare!+ {:html fancy-html
                  :title "Fancy Joyride Flare"
                  :key "fancy-flare"})

  :rcf)

;; Fancy SVG flare with animations and beautiful graphics
;; (Yes, vibe coded)
(def fancy-svg
  [:div {:style {:background "linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)"
                 :padding "30px"
                 :border-radius "20px"
                 :text-align :center}}

   [:style "
     @keyframes rotate {
       from { transform: rotate(0deg); }
       to { transform: rotate(360deg); }
     }
     @keyframes pulse-svg {
       0%, 100% { transform: scale(1); opacity: 0.8; }
       50% { transform: scale(1.1); opacity: 1; }
     }
     @keyframes wave {
       0%, 100% { d: path('M0,50 Q25,30 50,50 T100,50'); }
       50% { d: path('M0,50 Q25,70 50,50 T100,50'); }
     }
     .rotating { animation: rotate 8s linear infinite; }
     .pulsing { animation: pulse-svg 3s ease-in-out infinite; }
     .floating { animation: pulse-svg 4s ease-in-out infinite 1s; }
   "]

   [:h2 {:style {:color :white :margin-bottom "30px" :font-size "2.2em"}} "🎨 Fancy SVG Showcase"]

   [:div {:style {:display :flex :justify-content :space-around :flex-wrap :wrap :gap "30px"}}

    ;; Animated geometric pattern
    [:div {:style {:background "rgba(255,255,255,0.1)" :border-radius "15px" :padding "20px"}}
     [:h3 {:style {:color :white :margin-bottom "15px"}} "Rotating Geometry"]
     [:svg {:width 200 :height 200 :style {:background "rgba(0,0,0,0.2)" :border-radius "10px"}}
      [:defs
       [:linearGradient {:id "grad1" :x1 "0%" :y1 "0%" :x2 "100%" :y2 "100%"}
        [:stop {:offset "0%" :style {:stop-color "#ff6b6b" :stop-opacity 1}}]
        [:stop {:offset "100%" :style {:stop-color "#4ecdc4" :stop-opacity 1}}]]
       [:radialGradient {:id "grad2" :cx "50%" :cy "50%" :r "50%"}
        [:stop {:offset "0%" :style {:stop-color "#ffe66d" :stop-opacity 1}}]
        [:stop {:offset "100%" :style {:stop-color "#ff6b6b" :stop-opacity 0.8}}]]]

      [:g {:class "rotating" :style {:transform-origin "100px 100px"}}
       [:polygon {:points "100,20 180,80 180,120 100,180 20,120 20,80"
                  :fill "url(#grad1)"
                  :opacity 0.8}]
       [:circle {:cx 100 :cy 100 :r 25 :fill "url(#grad2)"}]]

      [:circle {:cx 100 :cy 100 :r 85 :fill :none :stroke "#fff" :stroke-width 2 :opacity 0.3}]
      [:circle {:cx 100 :cy 100 :r 65 :fill :none :stroke "#fff" :stroke-width 1 :opacity 0.2}]]]

    ;; Pulsing data visualization
    [:div {:style {:background "rgba(255,255,255,0.1)" :border-radius "15px" :padding "20px"}}
     [:h3 {:style {:color :white :margin-bottom "15px"}} "Data Pulse"]
     [:svg {:width 200 :height 200 :style {:background "rgba(0,0,0,0.2)" :border-radius "10px"}}
      [:defs
       [:linearGradient {:id "barGrad" :x1 "0%" :y1 "100%" :x2 "0%" :y2 "0%"}
        [:stop {:offset "0%" :style {:stop-color "#667eea" :stop-opacity 1}}]
        [:stop {:offset "100%" :style {:stop-color "#764ba2" :stop-opacity 1}}]]]

      ;; Animated bars representing data
      (for [i (range 6)]
        [:rect {:key i
                :x (+ 20 (* i 25))
                :y (- 180 (* (inc i) 20))
                :width 20
                :height (* (inc i) 20)
                :fill "url(#barGrad)"
                :class "pulsing"
                :style {:animation-delay (str (* i 0.3) "s")}}])

      ;; Grid lines
      (for [i (range 5)]
        [:line {:key i
                :x1 10 :y1 (+ 40 (* i 30))
                :x2 190 :y2 (+ 40 (* i 30))
                :stroke "#fff"
                :stroke-width 0.5
                :opacity 0.3}])]]

    ;; Organic flowing shapes
    [:div {:style {:background "rgba(255,255,255,0.1)" :border-radius "15px" :padding "20px"}}
     [:h3 {:style {:color :white :margin-bottom "15px"}} "Organic Flow"]
     [:svg {:width 200 :height 200 :style {:background "rgba(0,0,0,0.2)" :border-radius "10px"}}
      [:defs
       [:linearGradient {:id "flowGrad" :x1 "0%" :y1 "0%" :x2 "100%" :y2 "100%"}
        [:stop {:offset "0%" :style {:stop-color "#00f5ff" :stop-opacity 0.8}}]
        [:stop {:offset "50%" :style {:stop-color "#00bcd4" :stop-opacity 0.6}}]
        [:stop {:offset "100%" :style {:stop-color "#4fc3f7" :stop-opacity 0.8}}]]]

      [:path {:d "M20,100 Q60,60 100,100 T180,100 Q160,140 120,140 Q80,140 60,120 Q40,120 20,100 Z"
              :fill "url(#flowGrad)"
              :class "floating"}]

      [:circle {:cx 60 :cy 80 :r 8 :fill "#fff" :opacity 0.9 :class "pulsing"}]
      [:circle {:cx 140 :cy 120 :r 6 :fill "#fff" :opacity 0.7 :class "floating"}]
      [:circle {:cx 100 :cy 100 :r 4 :fill "#fff" :opacity 0.8 :class "pulsing"}]]]]

   ;; Stats section
   [:div {:style {:margin-top "30px" :color :white}}
    [:h3 {:style {:margin-bottom "15px"}} "📊 SVG Performance"]
    [:div {:style {:display :flex :justify-content :space-around :text-align :center}}
     [:div
      [:div {:style {:font-size "2em" :color "#4ecdc4"}} "60"]
      [:div {:style {:font-size "0.9em" :opacity "0.8"}} "FPS"]]
     [:div
      [:div {:style {:font-size "2em" :color "#ffe66d"}} "12"]
      [:div {:style {:font-size "0.9em" :opacity "0.8"}} "Elements"]]
     [:div
      [:div {:style {:font-size "2em" :color "#ff6b6b"}} "3"]
      [:div {:style {:font-size "0.9em" :opacity "0.8"}} "Animations"]]]]

   [:p {:style {:color :white :opacity "0.8" :margin-top "20px" :font-size "0.9em"}}
    "Smooth SVG animations powered by Joyride Flares ✨"]])

(comment
  ;; Fancy SVG flare with animations
  (flare/flare!+ {:html fancy-svg
                  :title "Fancy SVG Showcase"
                  :key "fancy-svg"})
  :rcf)

(comment
  (flare/ls)

  (flare/close! "greeting")

  (flare/ls)

  (flare/close! :message-test)
  (flare/close! "fancy-svg")

  (flare/close-all!)

  :rcf)

(comment
  ; TODO revisit!
  (flare/flare!+ {:html [:img {:src "assets/joyride-logo.png"}]
                  :key :my-flare})

  ;; Using a local resource file as a fallback
  (p/let [;; Step 1: Create flare to get webview
          {view :my-flare} (flare/flare!+ {:html [:div "Loading..."]
                                           :key :my-flare
                                           :preserve-focus? true})

          _ (def view view)
          ;; Step 2: Get webview and convert paths
          webview (.-webview view)
          workspace-uri (.-uri (first vscode/workspace.workspaceFolders))
          local-uri (vscode/Uri.joinPath workspace-uri "assets" "joyride-logo.png")
          webview-uri (.asWebviewUri webview local-uri)]
    ;; Step 3: Update flare with converted URI
    (flare/flare!+ {:html [:img {:src (str webview-uri)}] ; Reuse existing flare
                    :key :my-flare}))
  ;; Future Joyride versions will probably make this easier!
  :rcf)

