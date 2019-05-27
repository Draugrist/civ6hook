(ns civ6hook.stats
  (:require [java-time :as jt]
            [hiccup.core :as h]
            [mount.core :as m]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [clojure.string :as st])
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

(defn- time-str-formatter [time-name mins-per-unit]
  (fn [m]
    (let [x (quot m mins-per-unit)]
      (when (> x 0)
        [(format "%s %s" x time-name) (- m (* mins-per-unit x))]))))

(defn- mins->str-seq [mins]
  (:result
    (reduce
      (fn [res f]
        (if-let [[s remainder] (f (:mins res))]
          (-> res
              (assoc :mins remainder)
              (update :result conj s))
          res))
      {:mins mins, :result []}
      [(time-str-formatter "years" 525600)
       (time-str-formatter "days" 1440)
       (time-str-formatter "hours" 60)
       (time-str-formatter "minutes" 1)])))

(defn mins->time-str [mins]
  (let [res (mins->str-seq mins)]
    (if (empty? res)
      "Just now"
      (st/join " and " (filter (complement st/blank?) [(st/join ", " (drop-last res)) (last res)])))))

(defn- sort-turn-times [game-data]
  (->> (for [[player times] (:turn-times game-data)]
         [player (long (/ (reduce + times) (count times)))])
       (sort-by second)
       reverse))

(defn html-render [data]
  (h/html
    (for [[game-name game-data] data]
      [:div.game
       [:h1 game-name]
       [:h3  "Turn: " (:turn game-data)]
       [:p (format "Current player in turn: %s" (:player game-data))]
       [:p "Time since last turn: " (mins->time-str
                                      (jt/time-between
                                        (:timestamp game-data)
                                        (jt/zoned-date-time (jt/system-clock "UTC"))
                                        :minutes))]
       [:h4 "Average turn times"]
       [:ul.turn-times
        (for [[player avg-time] (sort-turn-times game-data)]
          [:li player ": " (mins->time-str avg-time)])]])))

(defn get-game-states []
  (with-meta @db {:render {"text/html" html-render}}))
