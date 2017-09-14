(ns clinvar-submitter.report
  (:require [clojure.string :as str]         
  (:gen-class)))

(defn report-header [in cx out frc r]
  (->> ["ClinVar-Submitter Run Report"
       "\n\nDate/Time: "(new java.util.Date)
       "\nFile(s): " in
       "\nJSON-LD Context (-c): " cx 
       "\nOutput File (-o): " out
       "\nForce overwrite (-f): " frc
       "\nRun Report File (-r): " r 	"<run-report-filename (def:clinvar-submitter-run-report.txt>"
       "\n\nNOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry."
       "\n========================================"
       "\n\nFilename: " in
       "\nRecord#: 	1"]
       (str/join)))

(defn write-report [in cx out frc reportfile]
  (spit reportfile (report-header in cx out frc reportfile) :append false))

(defn append-to-report [reportfile errormsg]
  (println reportfile errormsg)
  (spit reportfile errormsg :append true))