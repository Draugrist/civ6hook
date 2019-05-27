(ns civ6hook.stats-page
  (:require [java-time :as jt]
            [hiccup.page :as h]
            [clojure.string :as st]
            [civ6hook.styles :as styles]))

(defn- time-str-formatter [singular-name plural-name mins-per-unit]
  (fn [m]
    (let [x (quot m mins-per-unit)]
      (when (> x 0)
        [(format "%s %s" x (if (= 1 x) singular-name plural-name))
         (mod m mins-per-unit)]))))

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
      [(time-str-formatter "year" "years" 525600)
       (time-str-formatter "day" "days" 1440)
       (time-str-formatter "hour" "hours" 60)
       (time-str-formatter "minute" "minutes" 1)])))

(defn mins->time-str [mins]
  (let [res (mins->str-seq mins)]
    (if (empty? res)
      "Just now"
      (st/join " and " (remove st/blank? [(st/join ", " (drop-last res)) (last res)])))))

(defn- sort-turn-times [game-data]
  (->> (for [[player times] (:turn-times game-data)]
         [player (long (/ (reduce + times) (count times)))])
       (sort-by second >)))

(defn- html-body [data]
  [:body
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
         [:li player ": " (mins->time-str avg-time)])]])])

(defn html-head [data]
  [:head
   [:title "Civilization 6 - Play by Cloud hook handler"]
   [:style (styles/styles)]])

(defn html-render [data]
  (h/html5
    (html-head data)
    (html-body data)))
