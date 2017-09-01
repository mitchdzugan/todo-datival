(ns todo-datival.events
  (:require [todo-datival.config :as config]
            [todo-datival.datascript :refer [make-datascript-event conn]]
            [datival.core :as dv]))

(def dispatch
  (->> (dv/make-event-system
        config/debug?
        [{:initial-dispatching [[:set-filter :all]]
          :sinks {:clear-target (fn [target]
                                  (set! (.-value target) ""))}
          :events {:input-key-down (fn [_ {[char-code target] :user}]
                                     (let [todo-text (.trim (.-value target))]
                                       (if (and (= 13 char-code)
                                                (not-empty todo-text))
                                         {:dispatch [:add-todo todo-text]
                                          :clear-target target}
                                         {})))
                   :set-filter (fn [_ {filter :user}]
                                 {:datascript [{:db/path [[:db/role :anchor]]
                                                :root/filter filter}]})
                   :add-todo (fn [_ {todo-text :user}]
                               (let [id (dv/tempid)]
                                 {:datascript [{:db/id id
                                                :todo/text todo-text
                                                :todo/completed? false
                                                :todo/editing? false}
                                               {:db/path [[:db/role :anchor]]
                                                :root/todos id}]}))
                   :toggle-todo (fn [_ {[id completed?] :user}]
                                  {:datascript [{:db/id id
                                                 :todo/completed? completed?}]})
                   :toggle-todos (make-datascript-event
                                  [{:root/todos [:db/id :todo/completed?]}]
                                  (fn [_ {{:keys [root/todos]} :datascript
                                          completed? :user}]
                                    {:datascript (->> todos
                                                      (map #(-> {:db/id (:db/id %)
                                                                 :todo/completed? completed?})))}))
                   :delete-todo (fn [_ {id :user}]
                                  {:datascript [{:db/retract-path [id]}]})
                   :delete-completed (make-datascript-event
                                      [{:root/todos [:db/id :todo/completed?]}]
                                      (fn [_ {{:keys [root/todos]} :datascript}]
                                        {:datascript (->> todos
                                                          (filter :todo/completed?)
                                                          (map #(-> {:db/retract-path [(:db/id %)]})))}))
                   :begin-edit (fn [_ {id :user}]
                                 {:datascript [{:db/id id
                                                :todo/editing? true}]})
                   :end-edit (fn [_ {[id text] :user}]
                               (if (not-empty text)
                                 {:datascript [{:db/id id
                                                :todo/editing? false
                                                :todo/text text}]}
                                 {:datascript [{:db/retract-path [id]}]}))}}
         dv/dispatch-system
         (dv/datascript-system {:sync-local-storage {:selector [{:root/todos [:db/id :todo/completed? :todo/text]}]
                                                     :key "datoms"
                                                     :platform :web}}
                               conn)
         (dv/route-system {"/" {"active" :active
                                "completed" :completed
                                "" :all}}
                          {:make-set-route-event-res (fn [_ {{[page _] :handler} :user}]
                                                       {:dispatch [:set-filter page]})})])
       :dispatch))
