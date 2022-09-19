(ns blog-backend.ex)


(defn errorf! [template & args]
  (throw (new Exception ^String (apply format template args))))


(defn ex-response! [response]
  (throw (ex-info "HTTP response" ^:ex-http? response)))


(defn ex-json! [status body]
  (ex-response! {:status status :body body}))


(defn ex-http? [e]
  (some-> e ex-data meta :ex-http?))


(defmacro pcall [& body]
  `(try
     (let [result# (do ~@body)]
       [nil result#])
     (catch Throwable e#
       [e# nil])))
