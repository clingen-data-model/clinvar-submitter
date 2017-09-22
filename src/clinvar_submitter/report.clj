(ns clinvar-submitter.report
  (:require [clojure.string :as str]
            [clojure.pprint :as pprint]
            [clojure.java.io :as io]
            [clinvar-submitter.form :as form]
  (:gen-class))
  (:use clojure-csv.core))

(defn report-header [in cx out frc r]
  (->> ["\nClinVar-Submitter Run Report"
       "\nDate/Time: "(new java.util.Date)
       "\nFile(s): " in
       "\nMethod Name (-m): ACMG Guidelines, 2017"
       "\nMethod Citation (-mc): 	PMID:25741868"
       "\nJSON-LD Context (-c): " cx 
       "\nOutput File (-o): " out
       "\nForce overwrite (-f): " frc
       "\nRun Report File (-r): " r 	"<run-report-filename (def:clinvar-submitter-run-report.txt>"
       "\n\nNOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry.\n\n"
       "\n*******************************************************************\n\n"
       "\n\n"(format "%10s%45s%45s""|----------|""----------------- Input --------------- |""----------------- Output ---------------|")
       "\n"(format "%s%10s%s%18s%s%4s%s%15s%s%5s%s%5s%s%10s%s%17s"
                           "|""Result#"
                           "|""File Name"
                           "|""Record"
                           "|""Variant (alt desig)"
                           "|""Cell"
                           "|""Status"
                           "|""Code"
                           "|""Description|\n")] 
       (str/join)))

(defn reportdata
	 [v]
   (cond 
     (nil? v) "" 
	   (number? v) (str v) 
     (seq? v) (apply str v) :else v))

(defn isError [celldata]
  (println celldata)
  (let [errorvector ["E-202" "E-203" "E-204" "E-205" "E-206" "E-207" "E-208" "W-251" "E-301" "E-302" "E-401" "E-402" "E-403" "E-404" "E-501" "W-551"]]
  (println (compare errorvector celldata))
  )) 


(defn error-description [errorcode]
(println errorcode)
{
    :E-202 "Variant identifier not provided."
		:E-203 "No preferred variant information provided" 
		:E-204 "Preferred variant reference sequence not provided"
		:E-205 "Preferred variant start coordinate not provided." 
		:E-206 "Preferred variant end coordinate not provided."
		:E-207 "Preferred variant reference allele not provided."
		:E-208 "Preferred variant alternate allele not provided."
		:W-251 "Preferred variant alternate designation not provided."
		:E-301 "Condition disease code or name not provided."
		:E-302 "Mode of Inheritance display value not provided."
		:E-401 "Interpretation id not provided."
		:E-402 "Interpretation significance not provided."
		:E-403 "Interpretation evaluation date not provided."
		:E-404 "Interpretation evaluation date format not valid (<eval-date-value>)."
		:E-501 "<x> met criteria rules and/or strength codes are invalid or missing." 
		:W-551 "No PMID citations found."
  })

(defn get-error [errorcode]
  {
   :cell "A8"
   :status "Error",
   :code errorcode
   :desc ((error-description errorcode) :E-401)
   })

(defn get-variant [records]
  (println "record size: " (count records))
  (doseq [items records] 
    (get items 3)
    (println "data: " (get items 3) "\n" ))
 )
  
(defn append-to-report [reportfile in out records]
  (let [error (get-error "E-401")] 
  ;(doseq [n (range (count records))]
    (let [n 0]
    (doseq [item records] 
      (println item)
      (let [reportdata (format "%s%10s%s%18s%s%4s%s%15s%s%5s%s%5s%s%10s%s%16s%s"
                               "|" (+ n 101)
                               "|" in
                               "|" (+ n 1)
                               "|" (get item 3)
                               "|" (get error :cell)
                               "|" (get error :status)
                               "|" (get error :code)
                               "|" (get error :desc)"|\n")]
      (spit reportfile reportdata :append true))
      (inc n))
      ;(recur item (inc n))
      )))
    
(defn write-report [in cx out frc reportfile]
  (with-open [report (clojure.java.io/writer reportfile :append false)]    (.write report (report-header in cx out frc reportfile)))
  )
    

