(ns twitter-unfollower.core
  "ya some stuff!"
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
  "takes a vector of ids of users I'm following and map with structure {:id 0}
  containing users following me. Returns a vector of only users I'm folowing who
  were also found in the map."
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

  (println "sending request...")


  (def queryParams {})

  (.get Twitter "followers/ids" queryParams
    (fn [err data]
      (def mapOfIds (apply array-map
        (interleave (map keyword (map str (.-ids data)))
          (map (fn [x] 0) (.-ids data)))))
            (put! followingMeChan mapOfIds)))

  (.get Twitter "friends/ids" queryParams
    (fn [err data]
      (put! followedByMeChan (.-ids data)))))

(deflambda run-lambda
  "Entry point for this AWS Lambda service!"
  [args ctx]

  (println "args are " args)
  (println "args stuff are " (:stuff args))
  (println "args stuff are " (.-stuff args))
  (println "ctx is " ctx)

  (def config
    (-> (nodejs/require "fs")
        (.readFileSync "static/config.edn" "UTF-8")
        read-string))

  (let [Twitter (new twit (clj->js (:creds config)))]

    (getData Twitter)
  (go
    (let [followingMe                   (<! followingMeChan)
          followedByMe                  (<! followedByMeChan)
          followedByMeButNotFollowingMe (filterFollowees followingMe followedByMe)]

      (def unfollowChan(chan))

      (let [idsToUnfollow (take 1 followedByMeButNotFollowingMe)]
        (println (first idsToUnfollow))

        (println (str "Twitter is: " Twitter))

        (println "posting...")
        (.post Twitter "friendships/destroy" (clj->js {:user_id (first idsToUnfollow)})
               (fn [err data]
                 (put! unfollowChan [data err])
                 )
               )

                 (let [unfollowed (<! unfollowChan)]
                   (println "User has been unfollowed! " (first unfollowed))
                   (println "User has been unfollowed! " (last unfollowed))

                   )
/




      (ctx/succeed! ctx { :followingMe                   followingMe
                          :followedByMe                  followedByMe
                          :followedByMeButNotFollowingMe followedByMeButNotFollowingMe}))))))
