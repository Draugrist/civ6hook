(ns civ6hook.settings
  (:require [clojure.edn :as edn]))

(def settings (atom {}))

(defn read-settings! []
  (reset! settings (edn/read-string (slurp "settings.edn"))))

(defn email-settings []
  (:email @settings))

(defn email-for-user [user]
  (get-in @settings [:users user]))

(defn auth-token []
  (:auth-token @settings))

(defn message-settings []
  (:message @settings))