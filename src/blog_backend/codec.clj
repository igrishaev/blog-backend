(ns blog-backend.codec
  (:import
   java.io.File
   java.io.InputStream
   java.util.Base64
   org.apache.commons.codec.binary.Base64InputStream))


(defn base64-encode
  [^bytes unencoded]
  (String. (.encode (Base64/getEncoder) unencoded)))


(defn base64-decode
  [^String encoded]
  (.decode (Base64/getDecoder) encoded))


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
