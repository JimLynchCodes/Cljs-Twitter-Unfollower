(ns twitter-unfollower.core
  "An AWS lambda function that unfollows users not following you back."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-lambda.util :as lambda]
            [cljs-lambda.context :as ctx]
            [cljs.nodejs :as nodejs]
            [cljs.reader :refer [read-string]]
            [cljs-lambda.macros :refer-macros [deflambda]]
            [twit :as twit]
            [goog.object]
            [cljs.core.async :refer [put! chan <!]]))

(defn filterFollowees
  "Takes a vector of ids of users I'm following and map with structure
  {:id String} containing users following me and returns a vector of
  only users I'm following who were also found in the map."
  [followingMe followedByMe]
  (let [cljFollowingMe  (js->clj followingMe)
        cljFollowedByMe (js->clj followedByMe)]
    (filter
      (fn [x] (if-not (some? ((keyword (str x)) cljFollowingMe)) x)) cljFollowedByMe)))

(defn getData
  "Fills the various channels with data using the twitter api."
  [Twitter]
  (def followingMeChan (chan))
  (def followedByMeChan(chan))
  (def followedByMeDetailsChan(chan))
  (def queryParams {})

  (.get Twitter "followers/ids" queryParams
        (fn [err data]
          (println "followers/id returned" err " " data)
          (def mapOfIds
            (apply array-map
                   (interleave (map keyword (map str (.-ids data)))
                               (map (fn [x] 0) (.-ids data)))))
          (put! followingMeChan mapOfIds)))

  (.get Twitter "friends/ids" queryParams
        (fn [err data]
          (println "friends/id returned" err " " data)
          (put! followedByMeChan (.-ids data)))))

(deflambda run-lambda
  "Entry point for this AWS Lambda service!"
  [args ctx]


  (let [Twitter (new twit (clj->js (:creds args)))]

    (getData Twitter)
    (go
      (let [followingMe                   (<! followingMeChan)
            followedByMe                  (<! followedByMeChan)
            followedByMeButNotFollowingMe (filterFollowees followingMe followedByMe)]

        (println "following me" followingMe)
        (println "followed by me" followedByMe)
        (println "followedByMeButNotFollowingMe " followedByMeButNotFollowingMe)

        (def unfollowChan(chan))

        (let [idsToUnfollow [(take 3 followedByMeButNotFollowingMe)]]
          (println "id to unfollower " (idsToUnfollow))

          (println (str "Twitter is: " Twitter))

          (println "unfollowing: " {:user_id (last idsToUnfollow)})

          (.post Twitter "friendships/destroy" (clj->js {:user_id (last idsToUnfollow)})
                 (fn [err data]
                   (put! unfollowChan [data err])))

          (let [unfollowed (<! unfollowChan)]
            (println "User has been unfollowed! " (first unfollowed))
            (println "User has been unfollowed! " (last unfollowed)))


          (ctx/succeed! ctx
                        {:followingMe                   followingMe
                         :followedByMe                  followedByMe
                         :followedByMeButNotFollowingMe followedByMeButNotFollowingMe}))))))
