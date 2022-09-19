(ns blog-backend.html
  (:require
   [blog-backend.const :as const]
   [hiccup.core :as hiccup]))


(defn html-page [message backpath]
  (hiccup/html
   [:html
    [:body
     [:h2
      [:center message]]
     [:form {:action (str const/BLOG_URL backpath)}
      [:center
       [:button {:type "submit"} "Вернуться к обсуждению"]]]]]))


(defn html-response [status message backpath]
  {:status status
   :headers {:content-type "text/html;charset=utf-8"}
   :body (html-page message backpath)})
