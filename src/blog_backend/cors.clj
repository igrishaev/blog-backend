(ns blog-backend.cors
  "https://grishaev.me/cors/")

(def ORIGIN "https://grishaev.me")

(defn handle-cors [_request]
  {:status 200
   :headers
   {"Access-Control-Allow-Origin" ORIGIN
    "Access-Control-Allow-Methods" "POST"
    "Content-Type" "application/json; charset=utf-8"}})
