(ns blog-backend.date
  (:import
   java.time.Instant
   java.time.ZoneId
   java.time.format.DateTimeFormatter))


(def ^DateTimeFormatter
  formatter-iso
  (-> "yyyyMMdd'T'HHmmss'Z'"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(def ^DateTimeFormatter
  formatter-dash
  (-> "yyyy-MM-dd-HH-mm-ss"
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defn inst-now []
  (Instant/now))


(defn inst->iso [^Instant inst]
  (.format formatter-iso inst))


(defn inst->dash [^Instant inst]
  (.format formatter-dash inst))


(defn ms-now []
  (System/currentTimeMillis))
