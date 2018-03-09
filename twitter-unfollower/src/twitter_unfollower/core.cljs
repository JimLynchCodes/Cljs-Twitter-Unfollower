(ns twitter-unfollower.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-lambda.util :as lambda]
            [cljs-lambda.context :as ctx]
            [cljs.nodejs :as nodejs]
            [cljs.reader :refer [read-string]]
            [cljs-lambda.macros :refer-macros [deflambda]]
            [twit :as twit]
            [cljs.core.async :refer [put! chan <!]]))


(defn filterfn [followingMe followedByMe]
  (println [followingMe followedByMe]))

(defn getData []

  (def followingMeChan (chan))
  (def followedByMeChan(chan))
  (def followedByMeDetailsChan(chan))

  (def config
    (-> (nodejs/require "fs")
        (.readFileSync "static/config.edn" "UTF-8")
        read-string))

  (println "sending request...")

  (let [twitter twit]
    (def Twitter
      (new twit (clj->js (:creds config)))))

  (def queryParams {})

  (.get Twitter "followers/ids" queryParams
    (fn [err data]
      (def mapOfIds (apply array-map
        (interleave (map keyword (.-ids data))
          (map (fn [x] 0) (.-length (.-ids data))))))
      (println mapOfIds)
      (put! followingMeChan (.-ids data))))

  (.get Twitter "friends/ids" queryParams
    (fn [err data]
      (put! followedByMeDetailsChan (.-ids data)))))

(deflambda run-lambda [args ctx]
  (getData)
  (go
    (let [followingMe                   (<! followingMeChan)
          followedByMe                  (<! followedByMeChan)
          followedByMeButNotFollowingMe (filterFn followingMe followedByMe)]

      (def unfollowChan(chan))
      (def unfollowParams
        {:name        ""
         :screen_name ""
         :userId      ""})

      (.post Twitter "friendships/destroy" queryParams
        (fn [err data]
          (put! unfollowChan data)))

      (<! unfollowChan)
      (println "type " (type (first followingMe)))
      (ctx/succeed! ctx {:followingMe followingMe :followedByMe followedByMe}))))
