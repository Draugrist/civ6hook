(ns civ6hook.stats
  (:require [java-time :as jt]
            [hiccup.core :as h]
            [mount.core :as m]))

(m/defstate db
  :start (atom {}))

(defn- set-game-state! [game state]
  (swap! db assoc game state))

(defn set-current-player-and-turn! [game player turn]
  (set-game-state! game {:player player
                         :turn turn
                         :timestamp (jt/zoned-date-time (jt/system-clock "UTC"))}))

(defn html-render [data]
  (h/html
    (for [[game-name game-data] @db]
      [:div.game
       [:h1 game-name]
       [:p (format "Current player in turn: %s" (:player game-data))]
       [:p (format "It's turn %s, last turn committed at %s"
                   (:turn game-data)
                   (jt/format
                     "dd.MM.yyyy HH:mm"
                     (jt/offset-date-time
                       (:timestamp game-data)
                       (jt/zone-id "Europe/Helsinki"))))]])))

(defn get-game-states []
  (with-meta @db {:render {"text/html" html-render}}))
