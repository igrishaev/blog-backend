(ns blog-backend.cors
  "https://grishaev.me/cors/"
  (:require
   [blog-backend.const :as const]))


(defn handle-cors [_request]
  {:status 200
   :headers
   {"Access-Control-Allow-Origin" const/CORS_ORIGIN
    "Access-Control-Allow-Methods" "POST"
    "Access-Control-Allow-Headers" "Content-Type"
    "Content-Type" "application/json; charset=utf-8"}})
