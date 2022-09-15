(ns blog-backend.core
  (:gen-class)
  (:require
   [blog-backend.util :as util]
   [blog-backend.http :as http]))


(defn get-config! []
  {:token (util/get-env! "GITHUB_TOKEN")
   :repo (util/get-env! "GITHUB_REPO")
   :user (util/get-env! "GITHUB_USER")})


(defn handle-new-comment [{:keys [body
                                  isBase64Encoded]}]
  )



(defn app [{:as request
            :keys [httpMethod path]}]

  (case [httpMethod path]

    ["POST" "/comment"]
    (handle-new-comment request)

    ;; else
    {:status 404
     :headers {:content-type "text/plain"}
     :body "Not found"}))


(defn -main
  [& _]

  (let [config
        (get-config! )


        request
        (http/in->request)

        response
        (app request)]

    (http/response->out response)))
