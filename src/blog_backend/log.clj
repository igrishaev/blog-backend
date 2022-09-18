(ns blog-backend.log)


(defmacro log [level template & args]
  `(do
     (.print System/err ~level)
     (.print System/err " ")
     (.print System/err (ns-name *ns*))
     (.print System/err " ")
     (.println System/err (format ~template ~@args))))


(defmacro debug [template & args]
  `(log "DEBUG" ~template ~@args))


(defmacro info [template & args]
  `(log "INFO" ~template ~@args))


(defmacro error [template & args]
  `(log "ERROR" ~template ~@args))
