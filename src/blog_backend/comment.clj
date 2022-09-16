(ns blog-backend.comment
  (:require
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


(defn validate!
  [jsonParams]
  (when-not (map? jsonParams)
    (ex/ex-json! 400 {:message "The JSON input is not an object"}))
  (let [{:keys [author comment path]}
        jsonParams]
    (when-not (ne-string? author)
      (ex/ex-json! 400 {:message "Author is empty"}))
    (when-not (ne-string? comment)
      (ex/ex-json! 400 {:message "Comment is empty"}))
    (when-not (ne-string? path)
      (ex/ex-json! 400 {:message "Path is empty"}))))


(defn handle-new-comment
  [{:keys [jsonParams]}]

  (validate! jsonParams)

  (let [{:keys [author
                comment
                path]}
        jsonParams

        gh
        {:token (env/get! "GITHUB_TOKEN")}

        resp-get-repo
        (gh/get-repo gh "igrishaev" "blog" "master")

        repo-id
        (-> resp-get-repo :data :repository :id)

        commit
        (-> resp-get-repo :data :repository :ref :target)

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
        (format "_comments/%s.md" (date/inst->dash inst-now))

        comment-id
        ms

        date
        (date/inst->iso inst-now)

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
     :headers {:content-type "text/plain"}
     :body "OK"}))
