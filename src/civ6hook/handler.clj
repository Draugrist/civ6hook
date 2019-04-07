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
        new-body (-> body
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

(defn handle-turn [{:keys [value1 value2 value3]}]
  (if-let [email (settings/email-for-user value2)]
    (send-email email value1 value2 value3)
    (println (str "Unknown user " value2))))

(defn check-token [headers]
  (if-let [auth-token (get headers "authorization")]
    (= auth-token (settings/auth-token))))

(defn update-settings [request]
  (when (check-token (:headers request))
    (settings/read-settings!)))

(defroutes app-routes
  (POST "/turn" request (handle-turn (:body request)))
  (POST "/update-settings" request (update-settings request))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true})
      (wrap-defaults {:params    {:urlencoded true
                                  :keywordize true}
                      :responses {:not-modified-responses true
                                  :absolute-redirects     true
                                  :content-types          true
                                  :default-charset        "utf-8"}
                      :static    {:resources "public"}})))
