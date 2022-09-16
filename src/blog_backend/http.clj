(ns blog-backend.http
  (:require
   [blog-backend.ex :as ex]
   [blog-backend.log :as log]
   [blog-backend.codec :as codec]
   [clojure.walk :as walk]
   [clojure.string :as str]
   [clojure.java.io :as io]
   [cheshire.core :as json]))


(defn content-type-matches? [request content-type]
  (some-> request
          :headers
          :Content-Type
          (str/includes? content-type)))


(defn wrap-base64 [handler]
  (fn [{:as request :keys [isBase64Encoded]}]
    (if isBase64Encoded
      (handler (update request :body
                       (fn [body]
                         (-> body
                             ^bytes (codec/base64-decode)
                             (String. "UTF-8")))))
      (handler request))))


(defn wrap-json-request [handler]
  (fn [{:as request :keys [body]}]
    (if (content-type-matches? request "application/json")
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
            (assoc-in [:headers :content-type] "application/json"))
        response))))


(defn wrap-json [handler]
  (-> handler
      wrap-json-request
      wrap-json-response))


(defn wrap-form-params [handler]
  (fn [{:as request :keys [body]}]
    (if (content-type-matches? request "x-www-form-urlencoded")
      (handler (assoc request :formParams
                      (-> body
                          (codec/form-decode)
                          (walk/keywordize-keys))))
      (handler request))))


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
             :headers {:content-type "text/plain"}
             :body "Internal Server Error"}))))))


(defn in->request []
  (json/parse-stream *in* keyword))


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


(defn response->out
  [{:keys [status headers body]}]
  (json/with-writer [*out* nil]
    (json/write
     (cond-> {:statusCode status}
       headers
       (assoc :headers headers)
       body
       (merge (encode-body body))))))
