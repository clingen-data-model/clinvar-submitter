(ns clinvar-submitter.core
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import [com.github.jsonldjava.core JsonLdProcessor JsonLdOptions]
           [com.github.jsonldjava.utils JsonUtils])
  (:gen-class))

(def test-allele "data/CA090898.jsonld")
(def allele-cx "data/allelecx.jsonld")
(def test-interpretation "data/test_interp_1.dmwg.json")
(def interpretation-cx "data/short-context.jsonld")
(def frame "data/frame.jsonld")
(defn flatten-allele
  []
  (let [allele (json/parse-stream (io/reader test-allele))
        context (json/parse-stream (io/reader allele-cx))
        opts (JsonLdOptions.)]
    (-> (JsonLdProcessor/flatten allele context opts) JsonUtils/toPrettyString println)))

(defn flatten-interpretation
  []
  (with-open [ir (io/reader test-interpretation)
              cxr (io/reader interpretation-cx)]
    (let [i (json/parse-stream ir)
          cx (json/parse-stream cxr)
          opts (JsonLdOptions.)]
      (-> (JsonLdProcessor/flatten i cx opts) JsonUtils/toPrettyString println))))

(defn frame-interpretation
  []
  (with-open [ir (io/input-stream test-interpretation)
              fr (io/input-stream frame)]
    (let [i (JsonUtils/fromInputStream ir)
          f (JsonUtils/fromInputStream fr)
          opts (JsonLdOptions.)]
      ;;(.setEmbed opts true)
      (.setExplicit opts true)
      (-> (JsonLdProcessor/frame i f opts) JsonUtils/toPrettyString println))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
