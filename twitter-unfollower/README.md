# Cljs Twitter Unfollower


## What It Does

Currently, this function uses the [twit](https://github.com/ttezel/twit) javascript library (in a clojurescript project!) to pull an array of all the user accounts following a specific user and all the accounts being followed by that user.

## Non Serverless
This project uses cljs-lambda rather than the serverless framework with the standard serverless.yaml file and `sls` commands.

## Usage 

### 1) Set Up Twitter Credentials

First, create a folder in the twitter-unfollower directory alongside the _src_ and _test_ folders anmed `static`. Inside of it create a file name _config.edn_ in which you can place this credentials map:


```
{:creds {:consumer_key        "XXXXXXXXXXXXXXXXXXXXXXXXXXXX"
         :consumer_secret     "XXXXXXXXXXXXXXXXXXXXXXXXXXXX"
         :access_token        "XXXX-XXXXXXXXXXXXXXXXXXXXXXX"
         :access_token_secret "XXXXXXXXXXXXXXXXXXXXXXXXXXXX"}}
```
You can find these credentials for your account by making an app in the [twitter developer console](https://apps.twitter.com/). 

         
### 2) Set Up AWS Credentials

Option 1 - use `aws configure`

Optino 2 - Run `lein cljs-lambda default-iam-role` if you don't have yet have suitable
execution role to place in your project file.  This command will create an IAM
role under your default (or specified) AWS CLI profile, and modify your project
file to specify it as the execution default.

Otherwise, add an IAM role ARN under the function's `:role` key in the
`:functions` vector of your profile file, or in `:cljs-lambda` -> `:defaults` ->
`:role`.


### 3) Deploy To AWS

```sh
$ lein cljs-lambda deploy
$ lein cljs-lambda invoke work-magic ...
```

## Testing

```sh
lein doo node twitter-unfollower-test
```

Doo is provided to avoid including code to set the process exit code after a
 test run.
