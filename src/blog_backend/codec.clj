(ns blog-backend.codec
  (:import
   java.io.File
   java.io.InputStream
   java.util.Base64
   org.apache.commons.codec.binary.Base64InputStream))

(defn file? [x]
  (instance? File x))

(defn in-stream? [x]
  (instance? InputStream x))

(defn b64-encode
  [^bytes unencoded]
  (.encode (Base64/getEncoder) unencoded))

(defn b64-decode
  [^bytes encoded]
  (.decode (Base64/getDecoder) encoded))

(defn str->bytes
  ^bytes [^String string ^String encoding]
  (.getBytes string encoding))

(defn bytes->str
  ^String [^bytes bytes ^String encoding]
  (new String bytes encoding))

(defn base64-encode-stream [in]
  (new Base64InputStream in true 0 (byte-array 0)))

(defn base64-decode-stream [in]
  (new Base64InputStream in false 0 (byte-array 0)))
