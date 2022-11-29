(ns exring.example.hello-world
  (:require ["express" :as express]
            ["axios" :as axios]
            [exring.core :as exring]
            [exring.mw.content-negotiation :as cn]
            [exring.mw.exception-handling :as ex]
            [exring.http-response :as resp]))


(def handler (-> (fn [_req respond _raise]
                   (respond {:status 404
                             :body   {:foo 42}}))
                 (ex/exception-handling-middleware)
                 (cn/content-negotiation-middleware)))


(def app (-> (express)
             (.use (exring/ring-handler->express-handler #'handler))
             (.listen)))

(comment

  (some-> app (.address) .-port)

  (.close app)


  (let [port (some-> app (.address) .-port)
        url  (str "http://localhost:" port "/foo")]
    (println "GET" url)
    (-> (axios (clj->js {:method         "get"
                         :url            url
                         :headers        {"accept" "application/json"}
                         :validateStatus (constantly true)}))
        (.then (fn [^js resp]
                 (let [{:keys [status headers data]} (js->clj resp {:keywordize-keys true})]
                   (println "Ok" status)
                   (println "Content-Type" (.get headers "content-type"))
                   (println "Data" (pr-str data)))))
        (.catch (fn [e]
                  (println "Oh shit:")
                  (js/console.log e)))))

  (type axios/AxiosHeaders)

  (extend-protocol exeq/ExtendedEquality
    axios/AxiosHeaders
    (accept? [expected-value expected-form actual path]
      (println "AxiosHeaders")))

  ; 
  )

