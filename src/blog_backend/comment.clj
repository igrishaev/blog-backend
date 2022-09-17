(ns blog-backend.comment
  (:require
   [blog-backend.env :as env]
   [blog-backend.ex :as ex]
   [blog-backend.github :as gh]
   [blog-backend.date :as date]
   [clojure.string :as str]))


(def MESSAGE_OK
  "Your comment has been queued for review and will appear soon.")


(def ne-string?
  (every-pred string? (complement str/blank?)))


(defn render-comment [id path date author comment]
  (with-out-str
    (println "---")
    (println "id:" id)
    (println "is_spam:" false)
    (println "is_deleted:" false)
    (println "post:" path)
    (println "date:" date)
    (println "author_fullname:" (format "'%s'" author))
    (println "---")
    (println)
    (println comment)))


(defn validate-body!
  [body]
  (when-not (map? body)
    (ex/ex-json! 400 {:message "The JSON input is not an object"}))
  (let [{:keys [author comment path]}
        body]
    (when-not (ne-string? author)
      (ex/ex-json! 400 {:message "Author is empty"}))
    (when-not (ne-string? comment)
      (ex/ex-json! 400 {:message "Comment is empty"}))
    (when-not (ne-string? path)
      (ex/ex-json! 400 {:message "Path is empty"}))))


(defn handle-new-comment
  [{:keys [body]}]

  (validate-body! body)

  (let [{:keys [author
                comment
                path]}
        body

        gh
        {:token (env/get! "GITHUB_TOKEN")}

        resp-get-repo
        (gh/get-repo gh "igrishaev" "blog" "master")

        repo-id
        (-> resp-get-repo :data :repository :id)

        commit
        (-> resp-get-repo :data :repository :ref :target :oid)

        ms
        (date/ms-now)

        branch-name
        (format "comment-%s" ms)

        resp-create-branch
        (gh/create-branch gh branch-name repo-id commit)

        branch-id
        (-> resp-create-branch :data :createRef :ref :id)

        inst-now
        (date/inst-now)

        comment-path
        (format "_comments/%s.md" (date/inst-format inst-now "yyyy-MM-dd-HH-mm-ss"))

        comment-id
        ms

        date
        (date/inst-format inst-now "yyyy-MM-dd HH:mm:ss Z")

        comment-content
        (render-comment comment-id path date author comment)

        additions
        [{:path comment-path
          :contents comment-content}]

        _resp-create-commit
        (gh/create-commit gh
                          branch-id
                          "New comment"
                          commit
                          {:additions additions})

        _resp-create-pr
        (gh/create-pull-request gh
                                repo-id
                                "master"
                                branch-name
                                "New comment")]

    {:status 200
     :body {:message MESSAGE_OK}}))
