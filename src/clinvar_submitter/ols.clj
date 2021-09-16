(ns clinvar-submitter.ols
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-http.client :as client]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log]
            [clinvar-submitter.env :as env]))

;OMIM, MeSH, MedGen, UMLS, Orphanet, HPO
(def auth-priority [[:Orphanet #"Orphanet:(.+)" "ORPHA"]
                    [:Orphanet #"http://purl\.obolibrary\.org/obo/Orphanet_(.+)" "ORPHA"]
                    [:OMIM #"http://identifiers\.org/omim/(.+)" ""]
                    [:UMLS #"http://linkedlifedata\.com/resource/umls/id/(.+)" ""]
                    [:MeSH #"http://identifiers\.org/mesh/(.+)" ""]])

;; TODO test for 404 responses for not found or bad url
(defn get-mondo-concept [mondo-id]
  (let [http-out (client/get (str "http://www.ebi.ac.uk/ols/api/ontologies/mondo/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252F" mondo-id))]
    (json/parse-string (:body http-out) true)))

(defn unscrew-mondo [mondo-node]
  (let [props (get-in mondo-node [:annotation :property_value])
        prop-pairs (map #(str/split % #" ") props)
        keyworded-pairs (map #(vector (keyword (nth % 0)) (nth % 1)) prop-pairs)
        props-map (reduce (fn [acc [k v]] (assoc acc k (conj (acc k) v)))
                          {} keyworded-pairs)]
    (assoc mondo-node :annotation (merge (:annotation mondo-node) props-map))))

(defn find-term [term-lookup-tuple term-list]
  (some
    #(when-let [[_ term-id] (re-find (second term-lookup-tuple) %)]
       {:id-type (name (first term-lookup-tuple)), :id-value (str (nth term-lookup-tuple 2) term-id)})
    term-list))

(defn find-prioritized-term [mondo-id]
  (let [mondo-node (unscrew-mondo (get-mondo-concept mondo-id))
        exact-match-list (get-in mondo-node [:annotation :exactMatch])]
    (if-let [result (some #(find-term % exact-match-list) auth-priority)]
      result
      {:preferred-name (:label mondo-node)})))

(def find-prioritized-term-memo (memoize find-prioritized-term))

(defn find-scv [local-key]
  (let [body (str "{\"local_key\": \"" local-key "\"}")]
    (try
      (client/post env/scv-service-url
                   {:body body
                    :content-type :json
                    :accept :json})
      (catch Exception e
        (log/error (str "Exception in find-scv: " e))))))

