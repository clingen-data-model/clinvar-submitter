(ns clinvar-submitter.report
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io] 
  (:gen-class)))

(defn report-header [in cx out frc r]
  (->> ["\nClinVar-Submitter Run Report"
       "Date/Time: "(new java.util.Date)
       "File(s): " in
       "JSON-LD Context (-c): " cx 
       "Output File (-o): " out
       "Force overwrite (-f): " frc
       "Run Report File (-r): " r 	"<run-report-filename (def:clinvar-submitter-run-report.txt>"
       "NOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry.\n\n"]      
       (str/join \newline)))

(def table-header {:Result# "101" :Input "File Name | Record | Variant (alt desig)" :Output "Cell | Status | Code | Description"})

(defn write-report [in cx out frc reportfile]
  (spit reportfile (report-header in cx out frc reportfile) :append false)
  ;writing table header. TODO - reformat  
  (with-open [report (clojure.java.io/writer  reportfile :append true)]
  (pprint/pprint table-header report)))
  
(defn append-to-report [reportfile errormsg]
  (spit reportfile errormsg :append true))
