(ns blog-backend.comment
  (:require
   [blog-backend.github :as gh]
   [blog-backend.util :as util]))


(defn validate!
  [{:keys [author comment]}]
  (when-not (string? author)
    1))


(defn handle-new-comment
  [{:keys [formParams]}]

  (let [{:keys [author
                comment]}
        formParams

        gh
        {:token (util/get-env! "GITHUB_TOKEN")}

        resp-get-repo
        (gh/get-repo gh "igrishaev" "blog" "master")

        repo-id
        (-> resp-get-repo :data :repository :id)

        commit
        (-> resp-get-repo :data :repository :ref :target)

        branch-name
        (format "comment-%s" (System/currentTimeMillis))

        resp-create-branch
        (gh/create-branch gh branch-name repo-id commit)

        branch-id
        (-> resp-create-branch :data :createRef :ref :id)

        comment-path
        "_comments/foobar.md" ;; todo

        comment-content
        "aaaaa" ;; todo

        additions
        [{:path comment-path
          :contents comment-content}]

        resp-create-commit
        (gh/create-commit gh
                          branch-id
                          "New comment"
                          commit
                          {:additions additions})

        _
        (gh/create-pull-request gh
                                repo-id
                                "master"
                                branch-name
                                "New comment")



        ]


    )



  )
