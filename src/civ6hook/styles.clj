(ns civ6hook.styles
  (:refer-clojure :exclude [rem])
  (:require [garden.core :as g]
            [garden.compiler :as gc]
            [garden.color :refer [rgb rgba]]
            [garden.units :refer [px rem]]
            [clojure.string :as st]))

(defn- ->css [& args]
  (st/join " " (map gc/render-css args)))

(def bg-color (rgb 2 48 84))

(def theme
  [:body {:font-family "Trebuchet MS"
          :background-image "url('background.jpg')"
          :background-repeat :no-repeat
          :background-color bg-color
          :background-position :top
          :background-blend-mode :luminosity}])

(def game
  [:.game {:width (px 500)
           :margin-left :auto
           :margin-right :auto
           :background-color bg-color
           :border-radius (px 20)
           :padding (rem 2)
           :padding-top 0
           :color (rgb 255 255 255)
           :box-shadow (->css (px 0) (px 7) (px 16) :black)}
   [:h1 {:border-bottom (->css (px 2) :solid :white)
         :margin-top (px 0)
         :margin-left (rem -2)
         :margin-right (rem -2)
         :padding-left (rem 2)
         :padding-bottom (rem 0.5)
         :padding-top (rem 1)
         :background-color (rgba 0 0 0 0.2)}]])

(def all
  [theme
   game])

(defn styles []
  (g/css
    {:pretty-print? false}
    all))
