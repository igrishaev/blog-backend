(ns blog-backend.html
  (:require
   [hiccup.core :as hiccup]))


(def BASE_URL
  "https://grishaev.me")


(defn html-page [message backpath]
  (hiccup/html
   [:html
    [:body
     [:h2
      [:center message]]
     [:form {:action (str BASE_URL backpath)}
      [:center
       [:button {:type "submit"} "Вернуться к обсуждению"]]]]]))


(defn html-response [status message backpath]
  {:status status
   :headers {:content-type "text/html;charset=utf-8"}
   :body (html-page message backpath)})
