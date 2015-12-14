(ns lonely-twitch.prod
  (:require [lonely-twitch.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
