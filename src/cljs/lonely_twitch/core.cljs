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
        :handler #(reset! current-stream %)
        :keywords? true}))

;; -------------------------
;; Views

(defn home-page []
  [:div.container
   [:h2 [:img {:src "/img/twitch.png"
               :height "40px"
               :style {:top 6 :position :relative}}]
    " should not be lonely!"]
   [:button.rand-button {:on-click fetch-rand-stream!}
    "Show random stream"]
   (when @current-stream
     (let [{{:keys [url display_name status game broadcaster_language] :as channel} :channel} @current-stream]
       [:div {:style {:width "100%"}}
        (when broadcaster_language
          [:h3 status " [" broadcaster_language "]"])
        [:h4
         [:a {:href url :target :_blank} display_name]
         " playing "
         game]
        [:div
         [:iframe {:src (str url "/embed")
                   :frameBorder 0
                   :autoPlay "autoplay"
                   :width "1024px"
                   :height "576px"}]
         [:iframe {:src (str url "/chat?popup=true")
                   :frameBorder 0
                   :width "300px"
                   :height "576px"}]]]))])

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
