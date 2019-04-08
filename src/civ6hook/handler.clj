(ns civ6hook.handler
  (:require [civ6hook.settings :as settings]
            [clojure.string :as s]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [postal.core :as postal]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]))

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

(defn unknown-player [player]
  "Log but still return OK to webhook caller"
  (println (str "Unknown player " player))
  "OK")

(defn handle-turn [{:keys [value1 value2 value3]}]
  (if-let [email (settings/email-for-user value2)]
    (send-email email value1 value2 value3)
    (unknown-player value2)))

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

(defroutes public-routes
  (POST "/turn" request (handle-turn (:body request))))

(defroutes admin-routes
  (POST "/update-settings" [] update-settings))

(def app
  (-> (routes
        public-routes
        (wrap-routes admin-routes check-auth-token)
        (route/not-found "Not Found"))
      (wrap-json-body {:keywords? true})
      (wrap-defaults (cond->
                       {:params    {:urlencoded true
                                    :keywordize true}
                        :responses {:not-modified-responses true
                                    :absolute-redirects     true
                                    :content-types          true
                                    :default-charset        "utf-8"}}
                       (settings/dev?) (assoc :static {:resources "public"})))))
