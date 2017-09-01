(ns todo-datival.datascript
  (:require [datival.core :as dv]
            [datascript.core :as d]))

(defn make-datascript-event
  ([q f] (make-datascript-event q [:db/role :anchor] f []))
  ([q x1 x2]
   (if (not (fn? x2))
     (make-datascript-event q [:db/role :anchor] x1 x2)
     (make-datascript-event q x1 x2 [])))
  ([query id f sources]
   {:sources (conj sources :datascript)
    :body (fn [state args]
            (f state (update args :datascript
                             #(d/pull % query (if (fn? id) (id args) id)))))}))

(def schema {:many-ref [:root/todos]})

(def conn (dv/set-up-db schema))
