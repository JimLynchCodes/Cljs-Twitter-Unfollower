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

  (def followingMeChan(chan))
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
          (def mapOfIds
            (apply array-map
                   (interleave (map keyword (map str (.-ids data)))
                               (map (fn [x] 0) (.-ids data)))))
          (println mapOfIds)

          (put! followingMeChan mapOfIds)))

  (.get Twitter "friends/ids" queryParams
        (fn [err data]
          (put! followedByMeDetailsChan (.-ids data)))))

;  (.get Twitter "friends/list" queryParams
;    (fn [err data]
;
;      (println "got friends list" data)
;      (println "got friends list 2" (.-users (clj->js data)))
;      (println "length" (.-length (.-users (clj->js data)))))
;;      (println "got friends list" (first (:users data)))
;;      (println "got friends list" (clj->js data))
;;      (put! followedByMeChan (.-ids data)
;
;            ))

(deflambda run-lambda [args ctx]

  (def NUMBER_OF_USERS_TO_UNFOLLOW 3)

  (getData)

  (go
    (let [followingMe                    (<! followingMeChan)
          followedByMe                  (<! followedByMeChan)
          followedByMeDetails           (<! followedByMeDetailsChan)
          ;          followedByMeButNotFollowingMe (filterFn followingMe followedByMe)
          ]
      ;
      (def unfollowChan(chan))

      ;      (println followedByMeDetails)
      ;      (println "down here")
      (def unfollowParams
        {:name "" :screen_name "" :userId ""})


      (<! unfollowChan)

      (println "type " (type (first followingMe)))

      (for [x                          range
            NUMBER_OF_USERS_TO_UNFOLLOW]
        (unfollow))

      (.post Twitter "friendships/destroy" queryParams
             (fn [err data]
               ;               (println "destroyed? " data)
               (put! unfollowChan data)))


      (ctx/succeed! ctx {:followingMe followingMe :followedByMe followedByMe})
      )

    )

  )

