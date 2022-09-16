(ns blog-backend.ex)


(defn errorf! [template & args]
  (throw (new Exception ^String (apply format template args))))


(defn ex-json! [status body]
  (throw (ex-info "JSON response exception"
                  ^:ex-http?
                  {:status status
                   :body body})))


(defn ex-http? [e]
  (some-> e ex-data meta :ex-http?))
