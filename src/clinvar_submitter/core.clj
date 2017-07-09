(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clojure-csv.core :as csv])
  (:gen-class))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (let [cx-allele (ld1-> t i "variant" "relatedContextualAllele" (prop= t true "preferred" ))]
    ["" ; local id
     "" ; linking id
     "" ; gene symbol
     (ld-> t cx-allele "referenceCoordinate" "referenceSequence" "display")]))

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path]
  (let [t (ld/read-ld interp-path context-path)
        m (vals t)
        interps ((prop= t "VariantInterpretation" "type") m)
        rows (map #(construct-variant t %) interps)]
    rows))

(defn -main
  "take input assertion, transformation context, and output filename as input
  and write variant table in csv format"
  [in cx out & args]
  (spit out (csv/write-csv (construct-variant-table in cx))))
