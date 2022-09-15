(ns blog-backend.util
  (:import
   java.io.File
   java.io.InputStream
   java.io.PrintWriter
   org.apache.commons.codec.binary.Base64InputStream))


(defn file? [x]
  (instance? File x))


(defn in-stream? [x]
  (instance? InputStream x))


(defn base64-encode-stream [in]
  (new Base64InputStream in true 0 (byte-array 0)))


(defn logf [template & args]
  (.println ^PrintWriter *err* (apply format template args)))


(defn error! [template & args]
  (throw (new Exception ^String (apply format template args))))


(defn get-env! [^String env]
  (or (System/getenv env)
      (error! "Env var not set: %s" env)))
