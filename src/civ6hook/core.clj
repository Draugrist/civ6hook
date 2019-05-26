(ns civ6hook.core
  (:require [civ6hook.handler :as api]
            [ring.adapter.jetty :as jetty]
            [mount.core :as m]
            [taoensso.timbre :as log]
            [cheshire.generate :as cg]
            [java-time :as jt]
            [civ6hook.settings]))

(cg/add-encoder java.time.ZonedDateTime
                (fn [c jsonGenerator]
                  (.writeString jsonGenerator (jt/format "yyyy-MM-dd'T'HH:mm:ssZ" c))))

(m/defstate server
  :start (let [server (jetty/run-jetty api/app {:join? false :port 3000})]
           (log/info "Server running")
           server)
  :stop  (do
           (.stop server)
           (log/info "Server stopped")))

(defn start-for-lein-ring []
  (m/start-without #'server))

(defn start []
  (m/start))

(defn stop []
  (m/stop))

(defn -main [& args]
  (start))
