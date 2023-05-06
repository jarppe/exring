(ns exring.core-test
  (:require [clojure.test :as test :refer [deftest is use-fixtures async]]
            [match.core :refer [matches?]]
            ["express" :as express]
            ["axios" :as axios]
            [cognitect.transit :as t]
            [exring.core :as exring]
            [exring.mw.content-negotiation :as cn]
            [exring.mw.exception-handling :as ex]
            [exring.http-response :as resp]))


(def reader (t/reader :json))


(def app (atom nil))
(def server (atom nil))


(use-fixtures :each
  {:before (fn []
             (async done
                    (let [^js app'    (express)
                          ^js server' (.listen app' 0 "localhost")]
                      (.on server' "listening" (fn []
                                                 (reset! app app')
                                                 (reset! server server')
                                                 (done))))))
   :after  (fn []
             (async done
                    (let [^js server' @server]
                      (.close server' (fn []
                                        (reset! app nil)
                                        (reset! server nil)
                                        (done))))))})


(deftest sanity-test
  (is (some? @app))
  (is (some? @server))
  (is (pos? (some-> @server (.address) .-port))))


(defn GET
  ([url] (GET url {"accept" "application/json"}))
  ([url headers]
   (let [port    (-> @server (.address) .-port)
         request {:method  "get"
                  :url     (str "http://localhost:" port url)
                  :headers headers}]
     (-> (js/Promise.resolve (clj->js request))
         (.then axios)
         (.then (fn [^js resp]
                  (js->clj resp {:keywordize-keys true})))))))


(defn use-handler [handler]
  (let [^js app' @app]
    (.use app' (exring/ring-handler->express-handler handler))))


(deftest get-test
  (use-handler (fn [_req respond _reject]
                 (respond {:status  200
                           :headers {"content-type" "application/json; charset=utf-8"}
                           :body    "{\"foo\": 42}"})))
  (async done (-> (GET "/foo")
                  (.then (fn [^js resp]
                           (is (matches? {:status  200
                                          :headers (fn [^js axios-headers]
                                                     (is (= (.get axios-headers "content-type")
                                                            "application/json; charset=utf-8")))
                                          :data    {:foo 42}}
                                         resp))))
                  (.finally done))))


(deftest get-with-content-negotiation
  (use-handler (-> (fn [_req respond _reject]
                     (respond {:status 200
                               :body   {:foo 42}}))
                   (cn/content-negotiation-middleware)))
  (async done (-> (GET "/foo")
                  (.then (fn [^js resp]
                           (is (matches? {:status  200
                                          :headers (fn [^js axios-headers]
                                                     (is (= (.get axios-headers "content-type")
                                                            "application/json; charset=utf-8")))
                                          :data    {:foo 42}}
                                         resp))))
                  (.finally done))))


(defn transit-json->clj [data]
  (->> data
       (clj->js)
       (js/JSON.stringify)
       (t/read reader)))


(deftest get-with-content-negotiation-2
  (use-handler (-> (fn [req respond _reject]
                     (is (= "application/transit+json"
                            (-> req :headers (get "accept"))))
                     (respond {:status 200
                               :body   {:foo 42}}))
                   (cn/content-negotiation-middleware)))
  (async done (-> (GET "/foo" {"accept" "application/transit+json"})
                  (.then (fn [^js resp]
                           (is (matches? {:status  200
                                          :headers (fn [^js axios-headers]
                                                     (is (= (.get axios-headers "content-type")
                                                            "application/transit+json; charset=utf-8")))
                                          :data    (fn [data]
                                                     (is (= {:foo 42}
                                                            (transit-json->clj data))))}
                                         resp))))
                  (.finally done))))
