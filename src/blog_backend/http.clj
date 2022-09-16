;; https://github.com/ring-clojure/ring/blob/master/SPEC
;; https://cloud.yandex.ru/docs/functions/concepts/function-invoke

(ns blog-backend.http
  (:require
   [blog-backend.codec :as codec]
   [blog-backend.ex :as ex]
   [blog-backend.log :as log]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))


(defn parse-request
  [{:keys [requestContext
           path
           queryStringParameters
           httpMethod
           body
           isBase64Encoded
           headers]}]
  {:remote-addr (-> requestContext :identity :sourceIp)
   :uri (if (= path "") "/" path)
   :query-string (update-keys queryStringParameters name)
   :request-method (-> httpMethod name str/lower-case keyword)
   :headers (update-keys headers #(-> % name str/lower-case))
   :body (if isBase64Encoded
           (-> body codec/base64-decode io/input-stream)
           (-> body codec/str->bytes io/input-stream))})


(defn ->request []
  (-> *in*
      (json/parse-stream keyword)
      (parse-request)))


(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e

        (cond
          (ex/ex-http? e)
          (ex-data e)

          :else
          (do
            (log/error "Unhandled exception: %s, %s, %s"
                       (ex-message e)
                       (ex-data e)
                       e)
            {:status 500
             :headers {"content-type" "text/plain"}
             :body "Internal Server Error"}))))))


(defn encode-body [body]
  (cond

    (string? body)
    {:body body
     :isBase64Encoded false}

    (codec/file? body)
    {:body (-> body io/input-stream codec/base64-encode-stream slurp)
     :isBase64Encoded true}

    (codec/in-stream? body)
    {:body (-> body codec/base64-encode-stream slurp)
     :isBase64Encoded true}

    :else
    (throw (ex-info "Wrong body" {:body body}))))


(defn response->
  [{:keys [status headers body]}]
  (json/with-writer [*out* nil]
    (json/write
     (cond-> {:statusCode status}
       headers
       (assoc :headers headers)
       body
       (merge (encode-body body))))))
