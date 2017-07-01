(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> prop=]])
  (:gen-class))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (ld-> t i "cg:A122"))

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path]
  (let [t (ld/read-ld interp-path context-path)
        interps (vals (filter #(= "cg:VariantInterpretation" (-> % val (get "@type"))) t))
        rows (map #(construct-variant t %) interps)]
    rows))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
