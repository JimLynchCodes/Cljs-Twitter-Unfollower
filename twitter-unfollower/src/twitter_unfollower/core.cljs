(ns twitter-unfollower.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-lambda.util :as lambda]
            [cljs-lambda.context :as ctx]
            [cljs.nodejs :as nodejs]
            [cljs.reader :refer [read-string]]
            [cljs-lambda.macros :refer-macros [deflambda]]
            [twit :as twit]
            [goog.object]
            [cljs.core.async :refer [put! chan <!]]))

(defn filterFollowees [followingMe followedByMe]
  "takes a vector of ids of users I'm following and map with structure {:id 0}
  containing users following me. Returns a vector of only users I'm folowing who
  were also found in the map."
  (let [cljFollowingMe  (js->clj followingMe)
        cljFollowedByMe (js->clj followedByMe)]
    (filter
      (fn [x] (if-not (some? ((keyword (str x)) cljFollowingMe)) x)) cljFollowedByMe)))

(defn getData []
  "Fills the various channels with data using the twitter api."
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
        (interleave (map keyword (map str (.-ids data)))
          (map (fn [x] 0) (.-ids data)))))
            (put! followingMeChan mapOfIds)))

  (.get Twitter "friends/ids" queryParams
    (fn [err data]
      (put! followedByMeChan (.-ids data)))))

(deflambda run-lambda [args ctx]
  "Entry point for this AWS Lambda service!"
  (getData)
  (go
    (let [followingMe                   (<! followingMeChan)
          followedByMe                  (<! followedByMeChan)
          followedByMeButNotFollowingMe (filterFollowees followingMe followedByMe)]

      (def unfollowChan(chan))
      (def unfollowParams
        {:name        ""
         :screen_name ""
         :userId      ""})

      (let [idsToUnfollow (take 1 followedByMeButNotFollowingMe)]
        (println (first idsToUnfollow))


        ;; TODO call to twitter and actually unfolow a user!


        )


      (ctx/succeed! ctx { :followingMe                   followingMe
                          :followedByMe                  followedByMe
                          :followedByMeButNotFollowingMe followedByMeButNotFollowingMe}))))
