(ns clinvar-submitter.report
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io] 
  (:gen-class)))

(defn report-header [in cx out frc r]
  (->> ["\nClinVar-Submitter Run Report"
       "\nDate/Time: "(new java.util.Date)
       "\nFile(s): " in
       "\nJSON-LD Context (-c): " cx 
       "\nOutput File (-o): " out
       "\nForce overwrite (-f): " frc
       "\nRun Report File (-r): " r 	"<run-report-filename (def:clinvar-submitter-run-report.txt>"
       "\n\nNOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry.\n\n"
       "\n*******************************************************************\n"
       "\n\nFile Name: " in "\n"]      
       (str/join)))

(def table-header [{:#  "Result# " :Input "File Name | Record | Variant (alt desig)" :Output "Cell | Status | Code | Description"}])

(defn write-report [in cx out frc reportfile]
  (pprint/print-table table-header)
  ;(println (concat [report-header in cx out frc reportfile] table-header))
  (spit reportfile (report-header in cx out frc reportfile) :append false))
  ;(spit reportfile table-header :append true))

(defn append-to-report [reportfile errormsg]
  (spit reportfile errormsg :append true))


