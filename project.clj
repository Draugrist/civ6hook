(defproject civ6hook "0.1.0"
  :description "Civ 6 webhook email notifier"
  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.draines/postal "2.0.3"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0"]
                 [clojure.java-time "0.3.2"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [mount "0.1.16"]
                 [com.taoensso/timbre "4.10.0"]]

  :plugins [[lein-ring "0.12.5"]]

  :ring {:handler civ6hook.handler/app
         :init    civ6hook.core/start-for-lein-ring}

  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.3.2"]]
                   :source-paths ["dev"]
                   :main         user}}

  :main civ6hook.core)
