(ns blog-backend.codec
  (:require
   [ring.util.codec :as codec])
  (:import
   java.io.File
   java.io.InputStream
   org.apache.commons.codec.binary.Base64InputStream))


(def base64-encode codec/base64-encode)

(def base64-decode codec/base64-decode)

(def form-decode codec/form-decode)

(defn file? [x]
  (instance? File x))

(defn in-stream? [x]
  (instance? InputStream x))


(defn base64-encode-stream [in]
  (new Base64InputStream in true 0 (byte-array 0)))
