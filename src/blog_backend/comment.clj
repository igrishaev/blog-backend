(ns blog-backend.comment
  (:require
   [blog-backend.const :as const]
   [blog-backend.html :as html]
   [blog-backend.env :as env]
   [blog-backend.ex :as ex]
   [blog-backend.github :as gh]
   [blog-backend.date :as date]
   [clojure.string :as str]))


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
  [params]
  (when-not (map? params)
    (ex/ex-response!
     (html/html-response 400 const/MSG_NOT_MAP "/")))

  (let [{:keys [author comment path]}
        params]

    (when-not (ne-string? path)
      (ex/ex-response!
       (html/html-response 400 const/MSG_PATH_NOT_SET "/")))

    (when-not (ne-string? author)
      (ex/ex-response!
       (html/html-response 400 const/MSG_AUTHOR_NOT_SET path)))

    (when-not (ne-string? comment)
      (ex/ex-response!
       (html/html-response 400 const/MSG_COMMENT_NOT_SET path)))))


(defn handle-new-comment
  [{:keys [params]}]

  (validate-body! params)

  (let [{:keys [author
                comment
                path]}
        params

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

    (html/html-response 200 const/MSG_OK path)))
