(ns civ6hook.stats
  (:require [java-time :as jt]
            [hiccup.core :as h]
            [mount.core :as m]
            [taoensso.timbre :as log]
            [cheshire.core :as json])
  (:import (java.io FileNotFoundException)))

(defonce STATE_FILE "civ6hook-state.json")

(defn persist-state! [state]
  (spit STATE_FILE (json/generate-string state)))

(defn read-state! []
  (try
    (clojure.walk/postwalk
      (fn [x]
        (if (and (vector? x) (= :timestamp (first x)))
          [:timestamp (jt/zoned-date-time "yyyy-MM-dd'T'HH:mm:ssZ" (second x) (jt/zone-offset 0))]
          x))
      (-> (slurp STATE_FILE)
          (json/parse-string true)))
    (catch FileNotFoundException _
      (log/info "No state file found, initializing empty state")
      {})))

(m/defstate db
  :start (atom (read-state!))
  :stop (reset! db {}))

(m/defstate db-persister
  :start (add-watch db "persister" (fn [_ _ _ state] (persist-state! state))))

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
