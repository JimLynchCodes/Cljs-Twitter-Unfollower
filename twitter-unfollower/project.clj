(defproject twitter-unfollower "0.1.0-SNAPSHOT"
  :description "Unfollower app"
  :url "http://please.FIXME"
  :codox {:language :clojurescript}
  :dependencies [[org.clojure/clojure       "1.9.0"]
                 [org.clojure/clojurescript "1.10.126"]
                 [org.clojure/core.async    "0.4.474"]
                 [io.nervous/cljs-lambda    "0.3.5"]
                 [cljs-http "0.1.44"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-npm       "0.6.0"]
            [lein-doo       "0.1.7"]
            [io.nervous/lein-cljs-lambda "0.6.6"]
            [lein-ancient "0.6.15"]
            [lein-codox "0.10.3"]]
  :npm {:dependencies [[source-map-support "0.4.0"]
                       [twit "2.2.9"]
                       ]}
  :source-paths ["src"]
  :cljs-lambda
  {:defaults      {:role "arn:aws:iam::954459734159:role/cljs-lambda-default"}
   :resource-dirs ["static"]
   :functions
   [{:name   "cljs-twitter-unfollower"
     :invoke twitter-unfollower.core/run-lambda}]}
  :cljsbuild
  {:builds [{:id "twitter-unfollower"
             :source-paths ["src"]
             :compiler {:output-to     "target/twitter-unfollower/twitter_unfollower.js"
                        :output-dir    "target/twitter-unfollower/"
                        :source-map    "target/twitter-unfollower/map.js.map"
                        :target        :nodejs
                        :language-in   :ecmascript5
                        :optimizations :simple}}
            {:id "twitter-unfollower-test"
             :source-paths ["src" "test"]
             :compiler {:output-to     "target/twitter-unfollower-test/twitter_unfollower.js"
                        :output-dir    "target/twitter-unfollower-test"
                        :target        :nodejs
                        :language-in   :ecmascript5
                        :optimizations :none
                        :main          twitter-unfollower.test-runner}}]})
