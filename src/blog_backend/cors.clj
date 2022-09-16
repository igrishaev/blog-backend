(ns blog-backend.cors)


(defn handle-cors [_request]
  {:status 200
   :headers
   {"Access-Control-Allow-Origin" "https://grishaev.me"
    "Access-Control-Allow-Methods" "POST"
    "Content-Type" "application/json; charset=utf-8"}})
