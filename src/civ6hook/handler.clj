(ns civ6hook.handler
  (:require [civ6hook.settings :as settings]
            [civ6hook.stats :as stats]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [postal.core :as postal]
            [taoensso.timbre :as log]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.accept :as rma]
            [ring.util.response :refer [response file-response not-found content-type]]))

(defn create-message [email game player turn]
  (let [{:keys [from subject body]} (settings/message-settings)
        new-subject (s/replace subject "{{game}}" game)
        new-body    (-> body
                        (s/replace "{{game}}" game)
                        (s/replace "{{turn}}" turn)
                        (s/replace "{{player}}" player))]
    {:to      email
     :from    from
     :subject new-subject
     :body    new-body}))

(defn send-email [email game player turn]
  (postal/send-message
    (settings/email-settings)
    (create-message email game player turn))
  "OK")

(defn unknown-player
  "Log but still return OK to webhook caller"
  [player]
  (log/warn (str "Unknown player " player))
  "OK")

(defn handle-turn [{:keys [value1 value2 value3]}]
  (stats/set-current-player-and-turn! value1 value2 value3)
  (if-let [email (settings/email-for-user value2)]
    (send-email email value1 value2 value3)
    (unknown-player value2)))

(defn manual-turn [{:keys [game current next]}]
  (let [game-data (get (stats/get-game-states) (keyword game))]
    (if (= current (:player game-data))
      (handle-turn {:value1 game :value2 next :value3 (:turn game-data)}))))

(defn check-token [headers]
  (if-let [auth-token (get headers "authorization")]
    (= auth-token (settings/auth-token))))

(defn update-settings [_]
  (settings/read-settings!))

(defn check-auth-token [handler]
  (fn [request]
    (if (check-token (:headers request))
      (handler request)
      {:status 401})))

(defn index-page []
  (if (settings/dev?)
    (slurp (io/resource "public/index.html"))
    ; For some reason not-found returns application/octet-stream as content type
    (content-type (not-found "Not Found") "text/html; charset=UTF-8")))

(defroutes public-routes
  (POST "/turn" request (handle-turn (:body request)))
  (POST "/manualturn" request (manual-turn (:body request)))
  (GET "/stats" [] (response (stats/get-game-states)))
  (GET "/" [] (index-page)))

(defn wrap-render [handler]
  (fn [request]
    (let [mime (-> request :accept :mime)
          resp (handler request)]
      (if-let [render-fn (get (-> resp :body meta :render) mime)]
        (-> resp
            (update :body render-fn)
            (assoc-in [:headers "Content-Type"] mime))
        resp))))

(defroutes admin-routes
  (POST "/update-settings" [] update-settings))

(def app
  (-> (routes
        public-routes
        (wrap-routes admin-routes check-auth-token)
        (route/not-found "Not Found"))
      wrap-render
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (rma/wrap-accept {:mime ["text/html" "application/json"]})
      (wrap-defaults {:params    {:urlencoded true
                                  :keywordize true}
                      :static    {:resources "public/img"}
                      :responses {:not-modified-responses true
                                  :absolute-redirects     true
                                  :content-types          true
                                  :default-charset        "utf-8"}})))
