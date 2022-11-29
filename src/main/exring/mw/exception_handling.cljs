(ns exring.mw.exception-handling
  (:require [exring.http-response :as resp]))


(defn handle-error [respond e]
  (let [{:keys [type response]} (ex-data e)]
    (respond (if (= type :ring.util.http-response/response)
               response
               (resp/internal-server-error {:name    (.-name e)
                                            :message (.-message e)})))))


(defn exception-handling-middleware [handler]
  (fn [req respond raise]
    (try
      (handler req respond raise)
      (catch js/Error e
        (handle-error respond e)))))
