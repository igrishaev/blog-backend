(ns blog-backend.codec
  (:require
   [ring.util.codec :as codec])
  (:import
   java.io.File
   java.io.InputStream
   org.apache.commons.codec.binary.Base64InputStream))


(def base64-encode codec/base64-encode)

(def base64-decode codec/base64-decode)

(defn file? [x]
  (instance? File x))

(defn in-stream? [x]
  (instance? InputStream x))

(defn base64-encode-stream [in]
  (new Base64InputStream in true 0 (byte-array 0)))


(defn str->bytes
  (^bytes [^String string]
   (.getBytes string))
  (^bytes [^String string ^String encoding]
   (.getBytes string encoding)))


(defn bytes->str
  (^String [^bytes bytes]
   (new String bytes))
  (^String [^bytes bytes ^String encoding]
   (new String bytes encoding)))
