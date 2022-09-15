(ns blog-backend.github
  "
  Github Graphql API.
  "
  (:require
   [blog-backend.util :as util]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as http]))


(defn make-request [config operation query variables]

  (let [{:keys [token]}
        config

        payload
        (cond-> nil

          operation
          (assoc :operationName operation)

          query
          (assoc :query query)

          variables
          (assoc :variables variables))

        request-headers
        {"content-type" "application/json"
         "authorization" (format "bearer %s" token)}

        request
        {:url "https://api.github.com/graphql"
         :method :post
         :as :stream
         :headers request-headers
         :body (json/generate-string payload)}

        {:keys [error status body headers]}
        @(http/request request)]

    (when error
      (throw (ex-info "Github API error"
                      {:operation operation
                       :query query
                       :message (ex-message error)
                       :variables variables}
                      error)))

    (let [{:keys [content-type]}
          headers

          json?
          (some-> content-type
                  (str/starts-with? "application/json"))

          _
          (when-not json?
            (throw (ex-info "Github response was not JSON"
                            {:http-status status
                             :http-headers headers
                             :content-type content-type
                             :operation operation
                             :query query
                             :variables variables})))

          json-parsed
          (-> body io/reader (json/decode-stream keyword))

          {:keys [errors]}
          json-parsed]

      (if errors
        (throw (ex-info "Github error response"
                        {:errors errors
                         :http-status status
                         :http-headers headers
                         :operation operation
                         :query query
                         :variables variables}))
        json-parsed))))


(defn branch->ref [branch]
  (format "refs/heads/%s" branch))


(def QUERY_REPO "
query Query($name: String!, $owner: String!, $ref: String!) {
  repository(name: $name, owner: $owner) {
    id
    ref(qualifiedName: $ref) {
      id target {
        oid
      }
    }
  }
}
")


(defn get-repo [config user repo branch]
  (make-request config
                :Query
                QUERY_REPO
                {:name repo
                 :owner user
                 :ref (branch->ref branch)}))


(def QUERY_CREATE_REF "
mutation Mutation ($input: CreateRefInput!) {
  createRef(input: $input) {
    clientMutationId
    ref {
      id
    }
  }
}
")


(defn create-branch [config branch repo-id commit]
  (make-request config
                :Mutation
                QUERY_CREATE_REF
                {:input
                 {:name (branch->ref branch)
                  :repositoryId repo-id
                  :oid commit}}))


(def QUERY_DO_COMMIT "
mutation Mutation($input: CreateCommitOnBranchInput!) {
  createCommitOnBranch(input: $input) {
   clientMutationId
    commit {
      id
    }
  }
}
")


(defn process-addition
  [{:as addition :keys [contents]}]

  (cond

    (string? contents)
    (update addition :contents
            (fn [^String string]
              (-> string
                  .getBytes
                  io/input-stream
                  util/base64-encode-stream
                  slurp)))

    (util/in-stream? contents)
    (update addition :contents
            (fn [in-stream]
              (-> in-stream
                  util/base64-encode-stream
                  slurp)))

    (util/file? contents)
    (update addition :contents
            (fn [file]
              (-> file
                  io/input-stream
                  util/base64-encode-stream
                  slurp)))

    :else
    (throw (ex-info "Wrong contents type"
                    :addition addition ))))


(defn process-additions [additions]
  (mapv process-addition additions))


(defn create-commit [config
                     branch-id
                     message-headline
                     head-commit
                     {:keys [message-body
                             additions
                             deletions]}]

  (make-request config
                :Mutation
                QUERY_DO_COMMIT
                {:input
                 {:branch {:id branch-id}
                  :message
                  (cond-> {:headline message-headline}
                    message-body
                    (assoc :body message-body))
                  :expectedHeadOid head-commit
                  :fileChanges
                  (cond-> nil
                    additions
                    (assoc :additions (process-additions additions))
                    deletions
                    (assoc :deletions deletions))}}))


(def QUERY_CREATE_PR "
mutation Mutation ($input: CreatePullRequestInput!) {
  createPullRequest(input: $input) {
    clientMutationId
    pullRequest {
      id
    }
  }
}
")


;; (defn foo [a b & {:keys [c d e] :or {c false}}]
;;   [a b c d e])
;; (defn foo [a b (c false) (d nil) (e true)]
;;   [a b c d e])
;; (require '[clojure.spec.alpha :as s])
;; (s/def ::args
;;   (s/cat :req (s/* symbol?)
;;          :opt (s/* (s/tuple symbol? any?))))
;; (s/conform ::args '[a b c])


(defn create-pull-request [config
                           repo-id
                           branch-base
                           branch-head
                           title
                           & {:keys [body
                                     mutation-id
                                     draft?
                                     maintainer-can-modify?]
                              :or {draft? false
                                   maintainer-can-modify? true}}]
  (make-request config
                :Mutation
                QUERY_CREATE_PR
                {:input
                 (cond-> {:baseRefName (branch->ref branch-base)
                          :headRefName (branch->ref branch-head)
                          :repositoryId repo-id
                          :title title}

                   body
                   (assoc :body body)

                   mutation-id
                   (assoc :clientMutationId mutation-id)

                   (boolean? draft?)
                   (assoc :draft draft?)

                   (boolean? maintainer-can-modify?)
                   (assoc :maintainerCanModify maintainer-can-modify?))}))


#_
(comment

  (def -c {:token "..."})

  (def -repo-id "MDEwOlJlcG9zaXRvcnk0ODk1MDMzNw==")

  ;;--

  (get-repo -c "igrishaev" "blog" "master")

  {:data
   {:repository
    {:id "MDEwOlJlcG9zaXRvcnk0ODk1MDMzNw==",
     :ref
     {:id "MDM6UmVmNDg5NTAzMzc6cmVmcy9oZWFkcy9tYXN0ZXI=",
      :target {:oid "e47f3dfc26c3dd1b2f83c5ec5f9f3137474af48c"}}}}}


  ;;--

  (create-branch -c "foobar3" -repo-id "e47f3dfc26c3dd1b2f83c5ec5f9f3137474af48c")

  {:data
   {:createRef
    {:clientMutationId nil,
     :ref {:id "MDM6UmVmNDg5NTAzMzc6cmVmcy9oZWFkcy9mb29iYXIy"}}}}

  ;;--

  (create-commit -c
                 "MDM6UmVmNDg5NTAzMzc6cmVmcy9oZWFkcy9mb29iYXIz"
                 "hello"
                 "e47f3dfc26c3dd1b2f83c5ec5f9f3137474af48c"
                 {:additions [{:path "foo/tttt.txt"
                               :contents "AAA BBB CCC AAA BBB CCC AAA BBB CCC AAA BBB CCC AAA BBB CCC AAA BBB CCC "
                               }]})

  {:data
   {:createCommitOnBranch
    {:clientMutationId nil,
     :commit
     {:id
      "C_kwDOAursQdoAKDE3NjI2NDhkNmFiN2U4Yjc1MGVlMjRlNjM3MDhiOTJlODRjMTRiNDg"}}}}

  ;; --

  (create-pull-request -c
                       "MDEwOlJlcG9zaXRvcnk0ODk1MDMzNw=="
                       "master"
                       "foobar3"
                       "test")

  {:data
   {:createPullRequest
    {:clientMutationId nil,
     :pullRequest {:id "PR_kwDOAursQc4_A4y4"}}}}


  )
