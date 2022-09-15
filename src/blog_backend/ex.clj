(ns blog-backend.ex)


(gen-class
 :name "ex.BaseError"
 :extends "clojure.lang.ExceptionInfo")


(gen-class
 :name "ex.ValidationError"
 :extends "ex.BaseError")


#_
(compile 'blog-backend.ex)
