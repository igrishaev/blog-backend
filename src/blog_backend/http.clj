;; https://github.com/ring-clojure/ring/blob/master/SPEC
;; https://cloud.yandex.ru/docs/functions/concepts/function-invoke

(ns blog-backend.http
  (:require
   [blog-backend.codec :as codec]
   [blog-backend.ex :as ex]
   [blog-backend.log :as log]
   [cheshire.core :as json]))


(defn content-type-matches?
  [request re-content-type]
  (some-> request
          :headers
          :Content-Type
          (some->> (re-find re-content-type))))


(defn wrap-base64 [handler]
  (fn [{:as request :keys [isBase64Encoded]}]
    (if isBase64Encoded
      (handler (update request :body
                       (fn [body]
                         (-> body
                             (codec/str->bytes "UTF-8")
                             (codec/b64-decode)
                             (codec/bytes->str "UTF-8")))))
      (handler request))))


(defn wrap-json-request [handler]
  (fn [{:as request :keys [body]}]
    (if (content-type-matches? request #"(?i)application/json")
      (handler (assoc request :jsonParams
                      (json/parse-string body keyword)))
      (handler request))))


(defn wrap-json-response [handler]
  (fn [request]
    (let [{:as response :keys [body]}
          (handler request)]
      (if (coll? body)
        (-> response
            (update :body json/generate-string)
            (assoc-in [:headers :Content-Type] "application/json; charset=utf-8"))
        response))))


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


(defn wrap-default [handler]
  (-> handler
      wrap-json-request
      wrap-json-response
      wrap-base64
      wrap-exception))


(defn encode-body [body]
  (cond

    (string? body)
    {:body body
     :isBase64Encoded false}

    :else
    (throw (ex-info "Wrong response body"
                    {:body body}))))


(defn ->request []
  (json/parse-stream *in* keyword))


(defn response->
  [{:keys [status headers body]}]
  (json/with-writer [*out* nil]
    (json/write
     (cond-> {:statusCode status}
       headers
       (assoc :headers headers)
       body
       (merge (encode-body body))))))
