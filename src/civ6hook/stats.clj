(ns civ6hook.stats
  (:require [java-time :as jt]))

(def db (atom {}))

(defn- set-game-state! [game state]
  (swap! db assoc game state))

(defn set-current-player-and-turn! [game player turn]
  (set-game-state! game {:player player
                         :turn turn
                         :timestamp (jt/zoned-date-time (jt/system-clock "UTC"))}))

(defn get-game-states []
  @db)
