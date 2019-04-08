(defproject civ6hook "0.1.0"
  :description "Civ 6 webhook email notifier"
  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.draines/postal "2.0.3"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0"]]

  :plugins [[lein-ring "0.12.5"]]

  :ring {:handler civ6hook.handler/app
         :init civ6hook.settings/read-settings!}

  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
