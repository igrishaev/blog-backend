(ns blog-backend.env
  (:require
   [blog-backend.ex :as ex]))


(defn get! [^String env]
  (or (System/getenv env)
      (ex/errorf! "Env var not set: %s" env)))
