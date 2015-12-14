(ns lonely-twitch.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [ajax.core :refer [GET]]))

(defonce current-stream (atom nil))

(defn fetch-rand-stream! []
  (reset! current-stream nil)
  (GET "/rand"
       {:response-format :json
        :handler #(reset! current-stream %)}))

;; -------------------------
;; Views

(defn home-page []
  [:div
   [:h2 "Twitch should not be lonely!"]
   [:button {:on-click fetch-rand-stream!} "Show random stream"]
   (when @current-stream
     (let [channel (get @current-stream "channel")
           url (get channel "url")
           nickname (get channel "display_name")
           status (get channel "status") ]
     [:div
      [:div
       status
       " - "
       [:a {:href url} nickname]]
      [:iframe {:src (str url "/embed")
                :frameborder 0
                :autoplay "autoplay"
                :width "800px"
                :height "600px"}]]))])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (fetch-rand-stream!)
  (mount-root))
