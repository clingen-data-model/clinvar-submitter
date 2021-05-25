(ns clinvar-submitter.ld
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log])
  (:import [com.github.jsonldjava.core JsonLdProcessor JsonLdOptions]
           [com.github.jsonldjava.utils JsonUtils]
           [java.util Map List]))

(def jsonld-context "http://dataexchange.clinicalgenome.org/interpretation/json/sepio_context")

(defn resolve-id
  "Return the referent of an identifier if found in symbol table
  otherwise return a bare string"
  [t s]
  (if (instance? Map s)
    (if-let [v (get s "id")] (resolve-id t v) s)
    (if-let [ret (get t s)] ret s)))

(defn ld-get
  "Get property from a map, if the property is a string matching a key
  in the symbol table, return the value of that key instead. If the property
  is a map with a single 'id' property, return either the value of the property
  or the value of the referent of that property, if it exists in the symbol table"
  [t loc prop]
  (cond
    (fn? prop) (prop loc)
     (instance? List loc) (map #(ld-get t % prop) loc)
     :else (let [v (get loc prop)]
            (cond
              (string? v) (resolve-id t v)
              (instance? Map v) (if-let [vn (get v "id")] (resolve-id t vn) v)
              (instance? List v) (map #(resolve-id t %) v)
              :else v))))

(defn ld->
  "Take a symbol table, a location, and a set of predicates,
  return the set of nodes mapping to a given property"
  [t loc & preds]
  (let [res (reduce #(ld-get t %1 %2) loc preds)]
    (if (or (not (seqable? res)) (string? res) (instance? Map res))
      [res]
      res)))

(defn ld1->
  "Take the first result of a ld-> path expression"
  [t loc & preds]
  (first (apply ld-> t loc preds)))

(defn prop=
  "Filter ressults where a specific key (or recursive chain of keys}
  is equal to value"
  [t v & ks]
  (fn [m] (if (instance? List m)
            (filter (fn [m1] (let [r (apply ld-> t m1 ks)]
                               (some #(= % v) r)))  m)
            (when (some #(= % v) (apply ld-> t m ks)) m))))

(defn construct-symbol-table
  "Construct a map that takes a flattened JSON-LD interpretation as input and associates
  the IDs in the flattened interpretation with the nodes in the flattened interpretation"
  [coll]
  (let [graph (into [] (get coll "@graph"))]
    (reduce (fn [acc v] (assoc acc (.get v "id") v)) {} graph)))

(defn flatten-interpretation
  "Use JSONLD-API to read a JSON-LD interpretation using jsonld-context to translate
  symbols into local properties"
  [interp-rows]
  (try
    (with-open [cxr (io/reader jsonld-context)]
      (let [interps (json/parse-string interp-rows)
            cx (json/parse-stream cxr)
            opts (JsonLdOptions.)]
        (JsonLdProcessor/flatten interps cx opts)))
    (catch Exception e
      (log/error (str "Exception in flatten-interpretation: " (.getMessage e))))))

(defn generate-symbol-table
  "Flatten JSON-LD document and return a symbol table returning IDs of nodes mapped
  to the nodes themselves"
  [interp-rows]
  (try
    (construct-symbol-table (flatten-interpretation interp-rows))
   (catch Exception e
     (log/error (str "Exception in generate-symbol-table: " (.getMessage e))))))
