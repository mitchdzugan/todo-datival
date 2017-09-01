(ns todo-datival.views
  (:require [todo-datival.events :refer [dispatch]]
            [todo-datival.datascript :refer [conn]]
            [datival.core :as dv]))

(defn filter-todos [todos filter]
  (case filter
    :completed (remove #(not (:todo/completed? %)) todos)
    :active (remove :todo/completed? todos)
    todos))

(def todo-view
  (dv/make-ui conn
              [:todo/text :todo/editing? :todo/completed?]
              {:id (fn [[id]] id)
               :render (fn [[{:keys [todo/text todo/editing? todo/completed?]} id]]
                         [:li {:className (str ""
                                               (if editing? "editing " "")
                                               (if completed? "completed " ""))}
                          (if editing?
                            [:input.edit {:defaultValue text
                                          :on-blur #(dispatch :end-edit [id (.trim (.-value (.-target %)))])
                                          :on-key-press #(if (= 13 (.-charCode %))
                                                           (dispatch :end-edit [id (.trim (.-value (.-target %)))]))}]
                            [:div.view
                             [:input.toggle {:type "checkbox"
                                             :checked completed?
                                             :on-change #(dispatch :toggle-todo [id (not completed?)])}]
                             [:label {:on-double-click #(dispatch :begin-edit id)} text]
                             [:button.destroy {:on-click #(dispatch :delete-todo id)}]])])}))

(def todos-view
  (dv/make-ui conn
              [{:root/todos [:db/id :todo/completed?]} :root/filter]
              {:render (fn [[{:keys [root/todos root/filter]}]]
                         (let [all-count (count todos)
                               active-count (count (filter-todos todos :active))
                               all-completed? (every? :todo/completed? todos)]
                           (if (> all-count 0)
                             [:div
                              [:section.main
                               [:input#toggle-all.toggle-all {:type "checkbox" :checked all-completed?
                                                              :on-change #(dispatch :toggle-todos (not all-completed?))}]
                               [:label {:for "toggle-all"} "Mark as complete"]
                               [:ul.todo-list
                                (for [{:keys [db/id]} (filter-todos todos filter)]
                                  ^{:key id} [todo-view id])]]
                              [:footer.footer
                               [:span.todo-count
                                [:strong (str active-count)]
                                (str " item" (if (= 1 active-count) "" "s") " left")]
                               (let [nav-link (fn [url text]
                                                [:li [:a {:href url
                                                          :on-click #(set! (.-hash js/location) url)}
                                                      text]])]
                                 [:ul.filters
                                  [nav-link "#/" "All"]
                                  [nav-link "#/active" "Active"]
                                  [nav-link "#/completed" "Completed"]])
                               (if (< active-count all-count)
                                 [:button.clear-completed {:on-click #(dispatch :delete-completed)} "Clear completed"]
                                 [:div])]]
                             [:div])))}))

(def app-view
  [:div
   [:section.todoapp
    [:header.header
     [:h1 "todos"]
     [:input.new-todo {:placeholder "What needs to be done?" :autoFocus true
                       :on-key-press #(dispatch :input-key-down [(.-charCode %) (.-target %)])}]]
    [todos-view]]
   [:footer.info
    [:p "Double-click to edit a todo"]
    [:p "Written by Mitch Dzugan"]
    [:p "Based on template at " [:a {:href "https://github.com/tastejs/todomvc-app-template"}
                                 "tastejs/todomvc-app-template"]]]])
