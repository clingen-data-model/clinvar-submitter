(ns clinvar-submitter.env)

(def default-port 3000)

(defn parse-int [s]
  (Integer. (re-find #"\d+" s)))

(def port-num (if-let [p (System/getenv "CLINVAR_SUBMITTER_PORT")]
                (parse-int p)
                default-port))

;; Google Cloud Function URL
;; "https://us-central1-clingen-dx.cloudfunctions.net/ClinVarSCV"
(def scv-service-url (System/getenv "SCV_SERVICE_URL"))
                       
(def environment {:port-num port-num
                  :scv-service-url scv-service-url})


