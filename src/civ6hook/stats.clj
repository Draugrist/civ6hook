(ns civ6hook.stats)

(def db (atom {}))

(defn- set-game-state! [game state]
  (swap! db assoc game state))

(defn get-game-states []
  @db)

(defn set-current-player-and-turn! [game player turn]
  (set-game-state! game {:player player :turn turn}))