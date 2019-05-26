(ns user
  (:require [civ6hook.core :as cc]))

(defn start []
  (cc/start))

(defn stop []
  (cc/stop))

(defn restart []
  (stop)
  (start))
