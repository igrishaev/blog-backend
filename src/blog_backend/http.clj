;; https://github.com/ring-clojure/ring/blob/master/SPEC
;; https://cloud.yandex.ru/docs/functions/concepts/function-invoke

(ns blog-backend.http
  (:require
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-params
                                 wrap-json-response]]
   [blog-backend.html :as html]
   [blog-backend.codec :as codec]
   [blog-backend.const :as const]
   [blog-backend.ex :as ex]
   [blog-backend.log :as log]
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [clojure.string :as str]))


(defn parse-request
  [{:strs [requestContext
           path
           queryStringParameters
           httpMethod
           body
           isBase64Encoded
           headers]}]
  {:remote-addr (get-in requestContext ["identity" "sourceIp"])
   :uri (if (= path "") "/" path)
   :query-params queryStringParameters
   :request-method (-> httpMethod name str/lower-case keyword)
   :headers (update-keys headers str/lower-case)
   :body (if isBase64Encoded
           (-> body
               (codec/str->bytes "UTF-8")
               (codec/b64-decode)
               (io/input-stream))
           (-> body
               (codec/str->bytes "UTF-8")
               (io/input-stream)))})


(defn ->request []
  (-> *in*
      (json/parse-stream)
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
            (html/html-page const/MSG_FAILED "/")))))))


(defn wrap-default [handler]
  (-> handler
      wrap-keyword-params
      wrap-params
      wrap-json-params
      wrap-json-response
      wrap-exception))


(defn encode-body [body]
  (cond

    (string? body)
    {:body body
     :isBase64Encoded false}

    :else
    (throw (ex-info "Wrong response body"
                    {:body body}))))


(defn response->
  [{:keys [status headers body]}]
  (json/with-writer [*out* nil]
    (json/write
     (cond-> nil
       status
       (assoc :statusCode status)
       headers
       (assoc :headers headers)
       body
       (merge (encode-body body))))))
