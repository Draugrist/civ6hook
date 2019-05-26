(ns civ6hook.settings
  (:require [clojure.edn :as edn]
            [mount.core :as m]
            [taoensso.timbre :as log]))

(defn- read-settings []
  (edn/read-string (slurp "settings.edn")))

(m/defstate settings
  :start (atom (read-settings)))

(defn read-settings! []
  (log/info "Reading settings")
  (reset! settings (read-settings)))

(defn email-settings []
  (:email @settings))

(defn email-for-user [user]
  (get-in @settings [:users user]))

(defn auth-token []
  (:auth-token @settings))

(defn message-settings []
  (:message @settings))

(defn dev? []
  (:dev @settings))
