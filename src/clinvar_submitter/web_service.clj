(ns clinvar-submitter.web-service
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [clojure.pprint :refer [pprint]]
            [cheshire.core :as json]
            [ring.middleware.params :refer :all]
            [ring.middleware.multipart-params :refer :all]))


(def default-port 3000)

(defn parse-int [s]
  (Integer. (re-find #"\d+" s)))

(defn port-num
  []
  (if-let [p (System/getenv "CLINVAR_SUBMITTER_PORT")]
    (parse-int p)
    default-port))

(defn handler [request]
  ;; get request
  (let [req (-> (slurp (:body request)) json/parse-string)]
    ;; validate request

    ;; process request

    ;; return response
    {:status 200
      :headers {"Content-Type" "application/json"}
      :body  (json/generate-string req)}))

(def app handler)

;; (def app
;;   (wrap-multipart-params handler))

(defn run-service
  []
  (let [p ()]
    (println "Running VCI Submitter as web service on " (port-num))
    (run-jetty handler {:port (port-num)})))
