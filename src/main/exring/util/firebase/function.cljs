(ns exring.util.firebase.function
  (:require ["firebase-functions" :as functions]
            [exring.core :as exring]))


(defn firebase-function
  ([ring-handler] (firebase-function ring-handler nil))
  ([ring-handler opts]
   (-> (functions/runWith (clj->js (or opts {})))
       .-https
       (.onRequest
        (exring/ring-handler->express-handler ring-handler)))))