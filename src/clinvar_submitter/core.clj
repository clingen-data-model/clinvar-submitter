(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]])
  (:gen-class))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (let [cx-allele (ld1-> t i "cg:A122" "cg:relatedContextualAllele" (prop= t true "cg:preferred" ))]
    ["" ; local id
     "" ; linking id
     "" ; gene symbol
     (ld-> t cx-allele "cg:referenceCoordinate" "cg:A052" "cg:A064")]))

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path]
  (let [t (ld/read-ld interp-path context-path)
        m (vals t)
        interps ((prop= t "cg:VariantInterpretation" "@type") m)
        rows (map #(construct-variant t %) interps)]
    rows))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
