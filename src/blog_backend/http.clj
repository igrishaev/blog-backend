(ns blog-backend.http
  (:require
   [blog-backend.util :as util]
   [ring.util.codec :as codec]
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
        (util/logf "Unhandled exception: %s, %s, %s"
                   (ex-message e)
                   (ex-data e)
                   e)
        {:status 500
         :headers {:content-type "text/plain"}
         :body "Internal Server Error"}))))


(defn in->request []
  (json/parse-stream *in* keyword))


(defn encode-body [body]
  (cond

    (string? body)
    {:body body
     :isBase64Encoded false}

    (util/file? body)
    {:body (-> body io/input-stream util/base64-encode-stream slurp)
     :isBase64Encoded true}

    (util/in-stream? body)
    {:body (-> body util/base64-encode-stream slurp)
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
