(ns blog-backend.comment
  (:require
   [blog-backend.html :as html]
   [blog-backend.env :as env]
   [blog-backend.ex :as ex]
   [blog-backend.github :as gh]
   [blog-backend.date :as date]
   [clojure.string :as str]))


(def MESSAGE_OK
  #_
  "Your comment has been queued for review and will appear soon."
  "Ваш комментарий добавлен в очередь и скоро появится на сайте.")


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
  [payload]
  (when-not (map? payload)
    (ex/ex-response! (html/html-page "..." nil)))

  (let [{:keys [author comment path]}
        payload]

    (when-not (ne-string? path)
      (ex/ex-response! (html/html-page "..." nil)))

    (when-not (ne-string? author)
      (ex/ex-response! (html/html-page "..." path)))

    (when-not (ne-string? comment)
      (ex/ex-response! (html/html-page "..." path)))))


(defn handle-new-comment
  [{:keys [formParams]}]

  (validate-body! formParams)

  (let [{:keys [author
                comment
                path]}
        formParams

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

    (html/html-page MESSAGE_OK path)))
