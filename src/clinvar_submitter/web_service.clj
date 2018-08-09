(ns clinvar-submitter.web-service
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [clojure.pprint :refer [pprint]]
            [cheshire.core :as json]
            [ring.middleware.params :refer :all]
            [ring.middleware.multipart-params :refer :all]
            ))

(def default-port 3000)

(defn port-num
  []
  (if-let [p (System/getenv "CV_SUBMITTER_PORT")]
    p
    default-port))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body  (-> (slurp (:body request)) json/parse-string json/generate-string)})


(def app handler)

;; (def app
;;   (wrap-multipart-params handler))

(defn run-service
  []
  (println "Running VCI Submitter as web service on " (port-num))
  (run-jetty handler {:port (port-num)}))
