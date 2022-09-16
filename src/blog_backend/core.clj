(ns blog-backend.core
  (:gen-class)
  (:require
   [ring.middleware.json :refer [wrap-json-body
                                 wrap-json-response]]
   [blog-backend.http :as http]
   [blog-backend.comment :as comment]))


(defn router [{:as request
               :keys [request-method uri]}]

  (case [request-method uri]

    [:post "/comment"]
    (comment/handle-new-comment request)

    ;; else
    {:status 404
     :headers {:content-type "text/plain"}
     :body "Not found"}))


(def app
  (-> router
      (wrap-json-body {:keywords? true})
      wrap-json-response
      http/wrap-exception))


(defn -main
  [& _]
  (-> (http/->request)
      (app)
      (http/response->)))


#_
(binding [*in* (-> "yc-request.json" clojure.java.io/resource clojure.java.io/reader)]
  (-main))
