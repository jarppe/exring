(ns exring.core
  (:require [clojure.string :as str]))

;;
;; Mapping between ExpressJS (https://expressjs.com/)
;; and Ring (https://github.com/ring-clojure/ring/wiki/)
;;


;; ExpressJS request to Ring request.
;; See:
;;   https://expressjs.com/en/api.html#req
;;   https://github.com/ring-clojure/ring/wiki/Concepts#requests


(defn- express-req->ring-req [^js req]
  {:server-port    0 ; Can't find any way to get port number from req
   :server-name    (-> req .-hostname (or "0.0.0.0"))
   :remote-addr    (-> req .-ip (or "0.0.0.0"))
   :uri            (-> req .-path)
   :query-params   (-> req .-query (js->clj {:keywordize-keys true}))
   :scheme         (-> req .-protocol (keyword))
   :request-method (-> req .-method (str/lower-case) (keyword))
   :headers        (-> req .-headers (js->clj))
   :body           (-> req .-body)})


;;
;; Write Ring response into ExpressJS response.
;; See:
;;   https://expressjs.com/en/api.html#res
;;   https://github.com/ring-clojure/ring/wiki/Concepts#responses


(defn- write-ring-resp->express-resp [ring-resp ^js express-resp]
  (doseq [[k v] (-> ring-resp :headers)]
    (.set express-resp k v))
  (.status express-resp (-> ring-resp :status (or 200)))
  (if-let [body (-> ring-resp :body)]
    (.send express-resp body)
    (.end express-resp))
  nil)


;;
;; Write unhandled JS error to Exprss response.
;;


(defn- write-error-to-express-resp [e ^js express-response]
  (.set express-response "content-type" "application/json; charset=utf-8")
  (.status express-response 500)
  (.send express-response (clj->js {:type    :error
                                    :message "Unexpected error"
                                    :error   (str (.-name e) ": " (.-message e))}))
  nil)


;;
;; Adapt ring async handler to Express handler
;;

(defn ring-handler->express-handler [ring-handler]
  (fn [^js express-req ^js express-resp]
    (ring-handler (express-req->ring-req express-req)
                  (fn [resp] (write-ring-resp->express-resp resp express-resp))
                  (fn [e] (write-error-to-express-resp e express-resp)))
    nil))
