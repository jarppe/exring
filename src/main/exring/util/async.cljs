(ns exring.util.async
  (:require [goog :as g]
            [goog.object :as go]))


(defn promise? [^js p]
  (and (= (g/typeOf p) "object")
       (= (g/typeOf (go/get p "then")) "function")))


(defn async [handler]
  (fn [req respond raise]
    (try
      (let [^js resp (handler req)]
        (if (promise? resp)
          (-> resp
              (.then respond)
              (.catch raise))
          (respond resp)))
      (catch js/Error e
        (raise e)))))
