(ns clinvar-submitter.report
  (:require [clojure.string :as str]
            [clojure-csv.core :as csv]
            [clojure.java.io :as io]))

(defn report-header 
  [input options]
  [["ClinVar-Submitter Run Report"]
   ["" "Date/Time: " (str (new java.util.Date))]
   ["" "File(s): " input]
   ["" "Method Name (-m): " (get options :method)]
   ["" "Method Citation (-c): " (get options :methodc)]
   ["" "JSON-LD Context (-x): " (get options :jsonld-context)]
   ["" "Output File (-o): " (get options :output)]
   ["" "Force overwrite (-f): " (str (get options :force))]
   ["" "Run Report File (-r): " (get options :report)]
   []
   ["" "NOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry."]
   []
   ["Record #" "File Name" "Interp Id" "Variant (alt desig)" "Output Cell" "Status" "Code" "Description"]])

(def exception-code-map 
    {"*E-202" "Variant identifier not provided.",
     "*E-203" "No preferred variant information provided.",
     "*E-204" "Preferred variant reference sequence not provided",
     "*E-205" "Preferred variant start coordinate not provided.",
     "*E-206" "Preferred variant end coordinate not provided.",
     "*E-207" "Preferred variant reference allele not provided.",
     "*E-208" "Preferred variant alternate allele not provided.",
     "*W-251" "Preferred variant alternate designation not provided.",
     "*E-301" "Interpretations with Path or Lik Path outcomes require a condition disease code or name and none was provided.",
     "*E-302" "Only one disease/phenotype is supported, multiple found.",
     "*E-303" "Condition id type not supported by ClinVar. Using disease name option in submission instead.",
     "*E-304" "Condition identifier must have a colon delimiter to separate the type and id values. Using disease name option in submission instead.",
     "*E-305" "Mode of Inheritance display value not provided.",
     "*E-401" "Interpretation id not provided.", 
     "*E-402" "Interpretation significance not provided.",
     "*E-403" "Interpretation evaluation date not provided.",
     "*E-404" "Interpretation evaluation date format not valid (<eval-date-value>).",
     "*E-501" "<x> met criteria rules and/or strength codes are invalid or missing.",
     "*W-551" "No PMID citations found.",
     "*W-552" "No ACMG criterion evidence was met."})
  
(defn get-exception 
  [ecode]
    (let [code (first (str/split ecode #":"))
          desc (get exception-code-map code)
          type-str (subs (first (str/split code #"-")) 1)]
  {:type (cond (= "E" type-str) "Error" (= "W" type-str) "Warning ":else "Unknown")
   :code code
   :desc desc}))

(defn is-exception [ecode]
  (let [exception-code (first (str/split ecode  #":"))]
      (not (nil? (get exception-code-map exception-code)))))

(defn get-record-exceptions [items]
  (let [exception-list 
       (for [i (range (count items))]
       (if-not(nil? (get items i))
         (if(is-exception (get items i))      
          [(get items i) i])))]
       (let [newList (filter some? exception-list)]      
       newList)))
  
(defn write-run-report
  [input options records]  
  (let [reportfile (get options :report)]
    (do 
      ;; header
      (spit reportfile (csv/write-csv (report-header input options) :force-quote true) :append false)

      (dotimes [n (count records)] 

        ;; for each record
        (let [row (nth records n)
              row-exception-list (get-record-exceptions row)]

          ;if there is error in any row add error information in the report 
          (if-not (empty? row-exception-list)
            (dotimes [i (count row-exception-list)]
              (let [ecode (first (nth row-exception-list i))
                    column (+ 1 (last (nth row-exception-list i)))
                    record-number (last (str/split ecode  #":"))
                    exception (get-exception ecode)
                    outputdata-e [(str record-number)
                                  input
                                  (get (nth records n) 0)
                                  (get (nth records n) 25)
                                  (str "[" record-number ", " column "]")
                                  (get exception :type)
                                  (get exception :code)
                                  (get exception :desc)]]
                (spit reportfile (csv/write-csv [outputdata-e] :force-quote true) :append true)))
              (let [outputdata-s [(str (+ 1 n))
                                  input
                                  (get (nth records n) 0)
                                  (get (nth records n) 25)
                                  "--"
                                  "Success"
                                  "--"
                                  "--"]]       
                (spit reportfile (csv/write-csv [outputdata-s] :force-quote true) :append true)))
          ))
      )))

(defn write-files
    [input options records]
    (do 
      ;; write clinvar submitter output file
      (spit (get options :output) (csv/write-csv records :force-quote true)) 
      ;; write run report output file
      (write-run-report input options records)))