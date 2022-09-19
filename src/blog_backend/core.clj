(ns blog-backend.core
  (:gen-class)
  (:require
   [blog-backend.http :as http]
   [blog-backend.comment :as comment]
   [blog-backend.cors :as cors]))


(defn router [{:as request
               :keys [request-method]}]

  (case request-method

    :post
    (comment/handle-new-comment request)

    :options
    (cors/handle-cors request)

    ;; else
    {:status 404
     :headers {"content-type" "text/plain"}
     :body "Not found"}))


(def app
  (http/wrap-default router))


(defn -main
  [& _]
  (-> (http/->request)
      (app)
      (http/response->)))


#_
(binding [*in* (-> "yc-request.json"
                   clojure.java.io/resource
                   clojure.java.io/reader)]
  (-main))
