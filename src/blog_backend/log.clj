(ns blog-backend.log
  (:import
   java.io.PrintWriter))


(defmacro log [level template & args]
  `(let [out# ^PrintWriter *err*]
     (.print out# ~level)
     (.print out# " ")
     (.print out# (ns-name *ns*))
     (.print out# " ")
     (.println out# (format ~template ~@args))))


(defmacro debug [template & args]
  `(log "DEBUG" ~template ~@args))


(defmacro info [template & args]
  `(log "INFO" ~template ~@args))


(defmacro error [template & args]
  `(log "ERROR" ~template ~@args))
