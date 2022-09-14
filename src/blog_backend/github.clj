(ns blog-backend.github
  "
  Github Graphql API.
  "
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as http])
  (:import
   java.io.InputStream
   java.io.File
   java.util.Base64
   org.apache.commons.codec.binary.Base64InputStream))


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



(defn base64-encode-stream [in]
  (new Base64InputStream in true 0 (byte-array 0)))


(defn file? [x]
  (instance? File x))


(defn stream? [x]
  (instance? InputStream x))


(defn process-addition
  [{:as addition :keys [contents]}]

  (cond

    (string? contents)
    (update addition :contents
            #(-> ^String %
                 .getBytes
                 io/input-stream
                 base64-encode-stream
                 slurp))

    (stream? contents)
    (update addition :contents
            #(-> %
                 base64-encode-stream
                 slurp))

    (file? contents)
    (update addition :contents
            #(-> %
                 io/input-stream
                 base64-encode-stream
                 slurp))

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


#_
(comment

  (def -repo-id "MDEwOlJlcG9zaXRvcnk0ODk1MDMzNw==")

  (def -c {:token ""})

  ;;--

  (get-repo -c "igrishaev" "blog" "master")

  {:data
   {:repository
    {:id "MDEwOlJlcG9zaXRvcnk0ODk1MDMzNw==",
     :ref
     {:id "MDM6UmVmNDg5NTAzMzc6cmVmcy9oZWFkcy9tYXN0ZXI=",
      :target {:oid "e47f3dfc26c3dd1b2f83c5ec5f9f3137474af48c"}}}}}


  ;;--

  (create-branch -c "foobar2" -repo-id "e47f3dfc26c3dd1b2f83c5ec5f9f3137474af48c")

  {:data
   {:createRef
    {:clientMutationId nil,
     :ref {:id "MDM6UmVmNDg5NTAzMzc6cmVmcy9oZWFkcy9mb29iYXIy"}}}}

  ;;--

  (create-commit -c
                 "MDM6UmVmNDg5NTAzMzc6cmVmcy9oZWFkcy9mb29iYXIy"
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

  )
