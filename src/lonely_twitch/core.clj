(ns lonely-twitch.core
  (:require [cheshire.core]))

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
    (prn page)
    (let [data (get-page total page)
          should-stop? (some (complement lonely-stream?) (:streams data))]
      (if (or should-stop? (> page 5))
        streams
        (recur total
               (concat streams (:streams data))
               (inc page))))))

(defn refresh-cache! []
  (let [data (get-streams!)]
    (reset! cache data)))

(defn rand-stream []
  (rand-nth @cache))
