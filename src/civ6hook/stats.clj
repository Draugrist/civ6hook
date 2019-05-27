(ns civ6hook.stats
  (:require [java-time :as jt]
            [mount.core :as m]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [civ6hook.stats-page :as stats-page])
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
  :start (atom (read-state!)))

(m/defstate db-persister
  :start (add-watch db "persister" (fn [_ _ _ state] (persist-state! state))))

(defn set-current-player-and-turn! [game player turn]
  (swap!
    db
    update
    (keyword game)
    (fn [game-data]
      (let [current-player    (:player game-data)
            current-timestamp (:timestamp game-data)]
        (-> game-data
            (assoc :player player
                   :turn turn
                   :timestamp (jt/zoned-date-time (jt/system-clock "UTC")))
            (update-in
              [:turn-times (keyword current-player)]
              conj
              (if current-timestamp
                (jt/time-between
                  current-timestamp
                  (jt/zoned-date-time (jt/system-clock "UTC")) :minutes)
                0)))))))

(defn get-game-states []
  (with-meta @db {:render {"text/html" stats-page/html-render}}))
