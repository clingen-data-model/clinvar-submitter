(ns clinvar-submitter.web-service
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [cheshire.core :as json]
            [ring.middleware.params :refer :all]
            [ring.middleware.multipart-params :refer :all]
            [clinvar-submitter.report :as report :refer :all]
            [clinvar-submitter.variant :as variant :refer [process-input]]
            [clinvar-submitter.env :as env]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log]))

;; Return REST response to POST request.
;; Response is a JSON array of objects,
;; each entry containing a "variant" JSON object containing an array of values,
;; and an "errors" object, containing an array of error values pertinent to the
;; variant.
;; [
;;    {
;;       "variant" : [...],
;;       "errors"  : [...]
;;    },
;;    {
;;       "variant" : [...],
;;       "errors"  : [...]
;;    },
;;    ...
;; ]
;;
(defn submitter-v1-handler [request]
  (try
    (let [req (slurp (:body request))
          options (:options request)
          records (variant/process-input req options)
          variants (into [] (map (fn [record] {:submission record
                                               :errors (report/get-webservice-exception-data-for-row record options)}) records))
          success_count (count (filter empty? (map #(:errors %) variants)))
          body {:status {:totalRecords (count records)
                         :successCount success_count
                         :errorCount (- (count records) success_count)}
                :variants variants}
          resp (json/generate-string body)]
      (if (some? req)
        (log/debug "\nRequest: " req " \nResponse: " resp)
        (log/debug "Request contained no 'body' element content."))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body resp})
    (catch Exception ex
      (log/error "\nCaught Exception: " (.getMessage ex) "\nStackTrace: "(.printStackTrace ex)))))    

;; Routing handler
(defn route-handler [request]
  ;; this is admittedly rudimentary routing
  (let [uri (:uri request)]
    (case uri
      "/api/v1/submission" (submitter-v1-handler request)
      ;; default not-found
      {:status 404
       :headers {"Content-Type" "text/html"}
       :body "Not found. Perhaps you meant to use a URI of '/api/v1/submission'."})))

;; Middleware that injects the options map into the
;; http request map with the :options keyword before calling handler.
(defn wrap-options [handler options]
  (fn [request]
    (handler (assoc request :options options))))

(defn run-service
  ([] (run-service {}))
  ([options]
   (println "Running VCI Submitter as web service " env/environment)
   (run-jetty (wrap-options route-handler options) {:port env/port-num})))
