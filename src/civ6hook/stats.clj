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

(defn- set-game-state! [game state]
  (swap! db assoc game state))

(defn set-current-player-and-turn! [game player turn]
  (set-game-state! (keyword game) {:player    player
                                   :turn      turn
                                   :timestamp (jt/zoned-date-time (jt/system-clock "UTC"))}))

(defn- mod-times
  ([n divider] (mod-times n divider 0))
  ([n divider c]
   (cond
     (= 1 divider)
     n

     (>= (- n divider) 0)
     (recur (- n divider) divider (inc c))

     :else
     c)))

(defn- time-str-formatter [time-name mins-per-unit]
  (fn [m]
    (let [x (mod-times m mins-per-unit)]
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

(defn html-render [data]
  (h/html
    (for [[game-name game-data] @db]
      [:div.game
       [:h1 game-name]
       [:h3  "Turn: " (:turn game-data)]
       [:p (format "Current player in turn: %s" (:player game-data))]
       [:p "Time since last turn: " (mins->time-str
                                      (jt/time-between
                                        (:timestamp game-data)
                                        (jt/zoned-date-time (jt/system-clock "UTC"))
                                        :minutes))]])))

(defn get-game-states []
  (with-meta @db {:render {"text/html" html-render}}))
