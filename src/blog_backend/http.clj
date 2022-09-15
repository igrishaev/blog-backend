(ns blog-backend.http
  (:require
   [blog-backend.util :as util]
   [ring.util.codec :as codec]
   [clojure.java.io :as io]
   [cheshire.core :as json]))


(defn wrap-base64 [handler]
  (fn [request]
    123))


(defn wrap-form-params [handler]
  (fn [request]
    123))


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
