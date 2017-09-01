(ns todo-datival.core
  (:require [reagent.core :as reagent]
            [todo-datival.views :as views]
            [datascript.core :as d]
            [todo-datival.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (reagent/render views/app-view
                  (.getElementById js/document "app")))

(defn ^:export init []
  (dev-setup)
  (mount-root))
