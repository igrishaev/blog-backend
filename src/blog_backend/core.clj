(ns blog-backend.core
  (:gen-class)
  (:require
   [blog-backend.http :as http]))


(defn router [{:as request
               :keys [httpMethod path]}]

  (case [httpMethod path]

    ["POST" "/comment"]
    (handle-new-comment request)

    ;; else
    {:status 404
     :headers {:content-type "text/plain"}
     :body "Not found"}))


(def app
  (-> router
      http/wrap-base64
      http/wrap-form-params
      http/wrap-exception))


(defn -main
  [& _]
  (-> (http/in->request)
      (app)
      (http/response->out)))
