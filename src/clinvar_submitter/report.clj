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

(def table-header {:Result# "101" :Input "File Name | Record | Variant (alt desig)" :Output "Cell | Status | Code | Description"})

(defn write-report [in cx out frc reportfile]
  (spit reportfile (report-header in cx out frc reportfile) :append false))
  ;writing table header. TODO - reformat  
  ;(with-open [report (clojure.java.io/writer  reportfile :append true)]
  ;(pprint/pprint table-header report)))
  
(defn append-to-report [reportfile errormsg]
  (spit reportfile errormsg :append true))
