(ns blog-backend.date
  (:import
   java.time.Instant
   java.time.ZoneId
   java.time.format.DateTimeFormatter))


(defn make-formatter
  ^DateTimeFormatter [^String pattern]
  (-> pattern
      (DateTimeFormatter/ofPattern)
      (.withZone (ZoneId/of "UTC"))))


(defn inst-format [^Instant inst pattern]
  (-> pattern
      ^DateTimeFormatter (make-formatter)
      (.format inst)))


(defn inst-now []
  (Instant/now))


(defn ms-now []
  (System/currentTimeMillis))
