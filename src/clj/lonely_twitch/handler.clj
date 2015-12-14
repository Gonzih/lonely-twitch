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

(def cache (atom []))

(def per-page 100)

(defn number-of-live-streams []
  (:_total (cheshire.core/parse-string (slurp "https://api.twitch.tv/kraken/streams?limit=1&stream_type=live") true)))

(defn get-page [total page]
  (let [offset (- total (* per-page page))]
    (cheshire.core/parse-string (slurp (format "https://api.twitch.tv/kraken/streams?limit=%d&stream_type=live&offset=%d" per-page offset)) true)))

(defn lonely-stream? [{:keys [viewers]}]
  (< viewers 5))

(def pages-limit 10)

(defn get-streams! []
  (loop [total (number-of-live-streams)
         streams []
         page 1]
    (printf "Processing page: %d\n" page)
    (let [data (get-page total page)
          should-stop? (some (complement lonely-stream?) (:streams data))]
      (if (or should-stop? (> page 5))
        streams
        (recur total
               (concat streams (:streams data))
               (inc page))))))

(defn refresh-cache! []
  (let [data (get-streams!)]
    (println "START: Refreshing cache")
    (reset! cache data)
    (println "END: Refreshing cache")))

(defn rand-stream []
  (rand-nth @cache))

(def mount-target
  [:div#app
   [:h3 "ClojureScript has not been compiled!"]
   [:p "please run "
    [:b "lein figwheel"]
    " in order to start the compiler"]])

(def loading-page
  (html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1"}]
      (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
     [:body
      mount-target
      (include-js "js/app.js")]]))


(defroutes routes
  (GET "/" [] loading-page)
  (GET "/rand" [] (cheshire.core/generate-string (rand-stream)))

  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults #'routes site-defaults)]
    (if (env :dev)
      (when-not (seq @cache) (refresh-cache!))
      (future
        (refresh-cache!)
        (Thread/sleep (* 5 60 1000))))
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
