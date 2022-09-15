(ns blog-backend.comment
  (:require
   [blog-backend.github :as gh]
   [blog-backend.date :as date]
   [blog-backend.util :as util]
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
  [{:keys [author comment path]}]
  (when-not (ne-string? author)
    1))


(defn handle-new-comment
  [{:keys [formParams]}]

  (let [{:keys [author
                comment
                path]}
        formParams

        ;; TODO
        _
        (validate! formParams)

        gh
        {:token (util/get-env! "GITHUB_TOKEN")}

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

        resp-create-commit
        (gh/create-commit gh
                          branch-id
                          "New comment"
                          commit
                          {:additions additions})

        resp-create-pr
        (gh/create-pull-request gh
                                repo-id
                                "master"
                                branch-name
                                "New comment")]

    {:status 200
     :headers {:content-type "text/plain"}
     :body "OK"}))
