(ns exring.mw.content-negotiation
  (:require [clojure.string :as str]
            [cognitect.transit :as t]))


(def writer (t/writer :json))
(def reader (t/reader :json))


(def content-type-application-json "application/json")
(def content-type-application-transit "application/transit+json")
(def content-type-application-json-with-charset "application/json; charset=utf-8")
(def content-type-application-transit-with-charset "application/transit+json; charset=utf-8")


;;
;; Parsing request body:
;;


(defn json-parser [^js body]
  (when body
    (js->clj body {:keywordize-keys true})))


(defn transit-parser [^js body]
  (when body
    (t/read reader body)))


(defn parse-request-body [req]
  (let [content-type (-> req :headers (get "content-type" ""))
        parser       (condp str/starts-with? content-type
                       content-type-application-json json-parser
                       content-type-application-transit transit-parser
                       identity)]
    (assoc req :body-params (-> req :body (parser)))))

;;
;; Formatting response body:
;;


(defn json-response-formater [body]
  (-> body (clj->js) (js/JSON.stringify)))


(defn transit-response-formater [body]
  (t/write writer body))


(defn format-response-body [req resp]
  (cond
    ; Response has set content-type, don't change anything:
    (-> resp :headers (get "content-type") (some?))
    resp

    ; Caller accepts transit:
    (-> req :headers (get "accept" "") (str/includes? content-type-application-transit))
    (-> resp
        (update :body transit-response-formater)
        (update :headers assoc "content-type" content-type-application-transit-with-charset))

    ; Default is JSON:
    :else
    (-> resp
        (update :body json-response-formater)
        (update :headers assoc "content-type" content-type-application-json-with-charset))))


;;
;; Content negotiation middleware:
;;


(defn content-negotiation-middleware [handler]
  (fn [req respond raise]
    (handler (parse-request-body req)
             (fn [resp]
               (respond (format-response-body req resp)))
             raise)))
