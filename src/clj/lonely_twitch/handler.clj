(ns lonely-twitch.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]
            [cheshire.core]))

(defonce cache (atom []))

(def per-page 100)

(defn number-of-live-streams []
  (:_total (cheshire.core/parse-string (slurp "https://api.twitch.tv/kraken/streams?limit=1&stream_type=live") true)))

(defn get-page [total page]
  (let [offset (- total (* per-page page))]
    (cheshire.core/parse-string (slurp (format "https://api.twitch.tv/kraken/streams?limit=%d&stream_type=live&offset=%d" per-page offset)) true)))

(defn lonely-stream? [{:keys [viewers]}]
  (< viewers 5))

(def pages-limit (if (env :dev) 5 60))

(defn get-streams! []
  (loop [total (number-of-live-streams)
         streams []
         page 1]
    (printf "Processing page: %d\n" page)
    (let [data (get-page total page)
          should-stop? (some (complement lonely-stream?) (:streams data))]
      (if (or should-stop? (>= page pages-limit))
        streams
        (recur total
               (concat streams (:streams data))
               (inc page))))))

(defn refresh-cache! []
  (println "START: Refreshing cache")
  (let [data (get-streams!)]
    (reset! cache data)
    (println "END: Refreshing cache")))

(defn rand-stream []
  (when (seq @cache)
    (rand-nth @cache)))

(def mount-target
  [:div#app])

(def loading-page
  (html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "description"
              :content "Twitch should not be lonely! Find unpopular stream that you might like!"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1"}]
      [:title "Lonely Twitch"]
      (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
     [:body
      [:a {:href "https://github.com/Gonzih/lonely-twitch"
           :target :_blank}
       [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
              :src "https://camo.githubusercontent.com/a6677b08c955af8400f44c6298f40e7d19cc5b2d/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f677261795f3664366436642e706e67"
              :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_gray_6d6d6d.png"
              :alt "Fork me on GitHub"}]]
      mount-target
      (include-js "js/app.js")]]))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/rand" [] (cheshire.core/generate-string (rand-stream)))

  (not-found "Not Found"))

(defn init []
  (if (env :dev)
    (when-not (seq @cache) (refresh-cache!))
    (future
      (loop []
        (try
          (refresh-cache!)
          (catch Exception e
            (println "Exception during refresh call: " (.getMessage e)))
          (finally (Thread/sleep (* 5 60 1000))))
        (recur)))))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
