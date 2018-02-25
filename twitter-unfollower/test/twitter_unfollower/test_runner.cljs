(ns twitter-unfollower.test-runner
 (:require [doo.runner :refer-macros [doo-tests]]
           [twitter-unfollower.core-test]
           [cljs.nodejs :as nodejs]))

(try
  (.install (nodejs/require "source-map-support"))
  (catch :default _))

(doo-tests
 'twitter-unfollower.core-test)
