(ns clinvar-submitter.core
  (:require [clinvar-submitter.web-service :as web]
            [clinvar-submitter.env :as env])
  (:gen-class))

(defn -main
  "Start web-service to process input assertion,
   transformation context, process input and return
  variant table in csv format for submission to ClinVar"
  [& args]
  (web/run-service))
  
