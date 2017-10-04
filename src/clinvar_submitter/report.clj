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
       "\nJSON-LD Context (-x): " cx 
       "\nOutput File (-o): " out
       "\nForce overwrite (-f): " frc
       "\nRun Report File (-r): " r 	"<run-report-filename (def:clinvar-submitter-run-report.txt>"
       "\n\nNOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry.\n\n"
       "\n*******************************************************************\n\n"
       "\n\n"(format "%10s%46s%45s""|----------|""----------------- Input --------------- |""----------------- Output ---------------------------|")
       "\n"(format "%s%10s%s%15s%s%20s%s%8s%s%5s%s%8s%s%8s%s%18s%s"
                           "|""Result#"
                           "|""File Name"
                           "|""Variant (alt desig)"
                           "|""Record"
                           "|""Cell"
                           "|""Status"
                           "|""Code"
                           "|""Description""|")
       "\n"(format "%10s%46s%44s""|----------|""--------------------------------------- |""----------------------------------------------------|\n")] 
       (str/join)))

(defn reportdata
	 [v]
   (cond 
     (nil? v) "" 
	   (number? v) (str v) 
     (seq? v) (apply str v) :else v))

(defn isError [errorcode]
  (let [ecode (first (str/split errorcode  #":"))]
  (let [errorvector ["*E-202" "*E-203" "*E-204" "*E-205" "*E-206" "*E-207" "*E-208" "*W-251" "*E-301" "*E-302" "*E-401" "*E-402" "*E-403" "E-404" "E-501" "W-551"]]
  (some #(= ecode %) errorvector))))


(defn error-description [errorcode]
  (cond
  (= "*E-401" errorcode)
  {
   :desc "Interpretation id not provided."
  }
  (= "*E-202" errorcode)
  {
   :desc "Variant identifier not provided."
  }
  (= "*E-203" errorcode)
  {
   :desc "No preferred variant information provided" 
  }
  (= "*E-204" errorcode)
  {
   :desc "Preferred variant reference sequence not provided"
  }
  (= "*E-205" errorcode)
  {
   :desc "Preferred variant start coordinate not provided." 
  }
  (= "*E-206" errorcode)
  {
   :desc "Preferred variant end coordinate not provided."
  }
  (= "*E-207" errorcode)
  {
   :desc "Preferred variant reference allele not provided."
  }
  (= "*E-208" errorcode)
  {
   :desc "Preferred variant alternate allele not provided."
  }
  (= "*W-251" errorcode)
  {
   :desc "Preferred variant alternate designation not provided."
  }
  (= "*E-301" errorcode)
  {
   :desc "Condition disease code or name not provided."
  }
  (= "*E-302" errorcode)
  {
   :desc "Mode of Inheritance display value not provided."
  }
  (= "*E-402" errorcode)
  {
   :desc "Interpretation significance not provided."
  }
  (= "*E-403" errorcode)
  {
   :desc "Interpretation evaluation date not provided."
  }
  (= "*E-404" errorcode)
  {
   :desc "Interpretation evaluation date format not valid (<eval-date-value>)."
  }
  (= "*E-501" errorcode)
  {
   :desc "<x> met criteria rules and/or strength codes are invalid or missing." 
  }
  (= "*W-551" errorcode)
  {
   :desc "No PMID citations found."
  }))

(defn get-error [errorcode]
  (let [desc (error-description errorcode)]
  {
   :status "Error"
   :code errorcode
   :desc (get desc :desc)
}))

(defn get-variant [records]
  (let [data (for [items records] (get items 3))]
    data))

(defn get-errorcode [items]
  (let [errorlist 
       (for [i (range (count items))]
       (if-not(nil? (get items i))
         (if(isError (get items i))      
          [(get items i) i])))]
       (let [newList (filter some? errorlist)]      
       newList)))

(defn append-to-report [reportfile in out records]  
  (for [n (range (count records))]
    ;get each row from record set
    (let [row (nth records n)]
      ;get errorcode from each row     
      (let [errorcode (get-errorcode row)]
        ;if there is error in any row add error information in the report 
        (if-not(empty? errorcode)
          (for [i (range (count errorcode))]
            (let [ecode (first (nth errorcode i))]
            (let [column (last (nth errorcode i))]
            (let [uid (last (str/split ecode  #":"))]
            (let [error (get-error (first (str/split ecode #":")))]
            (let [outputdata-e (format "%s%10s%s%15s%s%20s%s%8s%s%5s%s%8s%s%8s%s%18s%s"
                                    "|" uid
                                    "|" in
                                    "|" (nth (get-variant records) n)
                                    "|" (+ n 1)
                                    "|" (str n "/" column)
                                    "|" (get error :status)
                                    "|" (get error :code)
                                    "|" (get error :desc) "|\n")]       
             (spit reportfile outputdata-e  :append true)))))))     
           (let [outputdata-s (format "%s%10s%s%15s%s%20s%s%8s%s%5s%s%8s%s%8s%s%18s%s"    
                                   "|" (+ n 101)
                                   "|" in
                                   "|" (nth (get-variant records) n)
                                   "|" (+ n 1)
                                   "|" "--"
                                   "|" "SUCCESS"
                                   "|" "--"
                                   "|" "--" "|\n")]       
             (spit reportfile outputdata-s :append true)))
))))
    
(defn write-report [in cx out frc reportfile]
  (with-open [report (clojure.java.io/writer reportfile :append false)]    (.write report (report-header in cx out frc reportfile)))
  )
    

