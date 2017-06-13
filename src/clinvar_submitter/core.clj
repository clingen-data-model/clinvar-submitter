(ns clinvar-submitter.core
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :as walk])
  (:import [com.github.jsonldjava.core JsonLdProcessor JsonLdOptions]
           [com.github.jsonldjava.utils JsonUtils]
           [java.util Map List])
  (:gen-class))

(def test-allele "data/CA090898.jsonld")
(def allele-cx "data/allelecx.jsonld")
(def test-interpretation "data/test_interp_1.dmwg.json")
(def interpretation-cx "data/short-context.jsonld")
(def frame "data/frame.jsonld")


;; TODO, this arrangement seems not to be handling collection types appropriately
;; need to figure out why
(defn resolve-id
  "Return the referent of an identifier if found in symbol table
  otherwise return a bare string"
  [t s]
  (println "resolve-id" s)
  (if (instance? Map s) (if-let [v (get s "id")]
                                        (resolve-id t v) s))
  (if-let [ret (get t s)] ret s))

(defn ld-get
  "Get property from a map, if the property is a string matching a key
  in the symbol table, return the value of that key instead. If the property
  is a map with a single 'id' property, return either the value of the property
  or the value of the referent of that property, if it exists in the symbol table"
  [t loc prop]
  (let [v (get loc prop)]
    (cond 
      (string? v) (resolve-id t v)
      (instance? Map v) (if-let [vn (get v "id")] (resolve-id t vn) v)
      (instance? List v) (map #(resolve-id t %) v)
      :else v)))

(defn ld->
  "Take a symbol table, a location, and a set of predicates, 
  return the set of nodes mapping to a given property"
  [t loc & preds]
  (reduce #(ld-get t %1 %2) loc preds))

(defn convert-jsonld
  "Convert java structures from jsonld library into clojure structures"
  [m]
  (walk/postwalk (fn [n] (cond
                           (instance? Map n) (into {} n)
                           (instance? List n) (into [] n)
                           :else n)) m))

(defn construct-symbol-table
  [coll]
  (let [graph (into [] (get coll "@graph"))]
    (reduce (fn [acc v] (assoc acc (.get v "id") (convert-jsonld v))) {} graph)))

(defn flatten-interpretation
  []
  (with-open [ir (io/reader test-interpretation)
              cxr (io/reader interpretation-cx)]
    (let [i (json/parse-stream ir)
          cx (json/parse-stream cxr)
          opts (JsonLdOptions.)]
      (-> (JsonLdProcessor/flatten i cx opts) convert-jsonld))))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (ld-> t i "cg:A122" "cg:relatedContextualAllele"))

(defn construct-variant-table
  "Construct and return variant table"
  []
  (let [t (construct-symbol-table (flatten-interpretation))
        interps (vals (filter #(= "cg:VariantInterpretation" (-> % val (get "@type"))) t))
        rows (map #(construct-variant t %) interps)]
    rows))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
