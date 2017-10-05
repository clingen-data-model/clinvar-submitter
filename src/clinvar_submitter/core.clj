(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clinvar-submitter.form :as form]
            [clojure.string :as str]
            [clinvar-submitter.report :as report]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.lang.Exception])
  (:gen-class))

(def cli-options
  [;; output file defaults to clinvar-variant.csv and will not overwrite 
   ;; unless -f force-overwrite option is specified
   ["-o" "--output FILENAME" "CSV output filename"]
    ;:default "clinvar-variant.csv" :mandatory true]
   ["-f" "--force" :default false]
   ["-x" "--jsonld-context URI" "JSON-LD context file URI"]
    ;:default "http://http://datamodel.clinicalgenome.org/interpretation/context/jsonld" :mandatory true]
   ["-i" "--input FILENAME" "JSON filename"
    :default "dmwg.json"]
   ["-b" "--build BUILD" "Genome build alignment, GRCh37 or GRCh38"
    :default "GRCh37"]
   ["-r" "--report FILENAME" "Run-report filename" :default "clinvar-submitter-run-report.txt"]
   ["-m" "--method METHODNAME" "Assertion-method-name" :default "ACMG Guidelines, 2015"]
   ["-c" "--methodc METHODCITATION" "Method Citation" :default "PMID:25741868"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["The clinvar-submitter program converts one or more ClinGen variant "
        " interpretation json files into a CSV formatted list which can be "
        " pasted into the ClinVar Submission spreadsheet (variant sheet). "
        " Basic validation checking provides warnings and errors at a record "
        " and field level."
        ""
        "Usage: clinvar-submitter [options] input"
        ""
        "Options:"
        options-summary
        ""
        "Input:"
        "  <filename>    The filename of a json file to be converted"
        "  <directory>   A directory containing one or more json files to be converted"
        ""
        "Please refer to http://datamodel.clinicalgenome.org/interpretation "
        " for additional details on the variant interpretation json model."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))


(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]  
  (println args)
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (= 1 (count arguments))
      {:input (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary      
      {:exit-message (usage summary)})))

(defn exit [status msg]
 (println msg))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (log/debug "Function: construct-variant - constructing one row of variant table, with VariantInterpretation as root")
  (try 
  (let [variant (form/get-variant t i)
        condition (form/get-condition t i)
        interp (form/get-interpretation t i)
        evidence (form/get-met-evidence t i)]       
    [(get interp :id) ; local id
     "" ; linking id - only needed if providing case data or func evidence tabs 
     "" ; gene symbol - not provided since we are avoiding contextual allele representation. 
     (get variant :refseq) ;rdefseq
     "" ; hgvs - not providing since we are avoiding contextual allele representation
     "" ; chromosome - not providing since we are using the refseq field to denote the accession.
     (get variant :start) ; start + 1  (from 0-based to 1-based)
     (get variant :stop)  ; stop + 1  (from 0-based to 1-based)
     (get variant :ref)   ; ref
     (get variant :alt)   ; alt
     "" ; variant type - non sequence var only
     "" ; outer start - non sequence var only
     "" ; inner start - non sequence var only
     "" ; inner stop - non sequence var only
     "" ; outer stop - non sequence var only
     "" ; variant length - non sequence var only
     "" ; copy number - non sequence var only
     "" ; ref copy number - non sequence var only
     "" ; breakpoint 1 - non sequence var only
     "" ; breakpoint 2 - non sequence var only
     "" ; Trace or probe data	 - non sequence var only
		 "" ; empty
		 (get variant :variantIdentifier) ; Variation identifiers	(http://reg.genome.network.org/allele = ABC ABC:CA123123123)
		 "" ; Location	- N/A
		 (get variant :altDesignations)    ; Alternate designations 	
		 "" ; Official allele name	- N/A
		 "" ; URL	- bypassing for now, no set home for a public URL at this time
		 "" ; empty 
     (get condition :idtype) ; Condition ID type (PURL)- assumes variantInterpretation.condition.disease.coding[0].code match of everything before the underscore.
		 (get condition :idvalue)  ; Condition ID value	- assumes variantInterpretation.condition.disease.coding[0].code match of everything after the underscore.
		 (get condition :name) ; Preferred condition name
     "" ; Condition category	
     "" ; Condition uncertainty	
     "" ; Condition comment	
     "" ; empty	
     (get interp :significance) ; Clinical significance	
     (get interp :eval-date) ; Date last evaluated
     "" ; assertion method
     "" ; assertion method citations
     (get condition :moi) ; Mode of Inheritance
     (get evidence :pmid-citations) ; significance citations
     "" ; significance citations without db ids
     (get evidence :summary) ; comment on clinical significance
     "" ; explanation if clinsig is other or drug
    ])
   (catch Exception e (log/error (str "Exception construct-variant: " e)))))

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path]
  (log/debug "Function: construct-variant-table- context and input Filename (construct-variant-table): " interp-path context-path)
  (try
  (let [t (ld/generate-symbol-table interp-path context-path)
        m (vals t)
        interps ((prop= t "VariantInterpretation" "type") m)
        rows (map #(construct-variant t %) interps)]
    rows)
  (catch Exception e (println (str "Exception in construct-variant-table: " e))
  (log/error (str "Exception in construct-variant-table: " e)))))

(defn -main 
  "take input assertion, transformation context, and output filename as input and write variant table in csv format"
  [& args]
  (let [{:keys [input options exit-message ok?]} (validate-args args)]
  ;(if (nil? (get options :jsonld-context)) (println "(use‚ -x to enter context file name)."))
  ;(if (nil? (get options :output)) (println "(use‚ -o to enter output file name)."))
  (if exit-message
      (exit (if ok? 0 1) exit-message)
  
  (if-not(or (nil? (get options :output)) (nil? (get options :jsonld-context)))
    (let [records (construct-variant-table input (get options :jsonld-context))]
    (log/debug "Input,output and context filename in main method: " input (get options :jsonld-context) (get options :output))
    ;(report/write-report input (get options :jsonld-context) (get options :output) (get options :force) (get options :report))  
    (try    ;if output or report file exists then check if there is a force option. If there is no force option the throw an error with message     
      ;"ERROR 101 ‚output or report file exists! (use‚ -f Force overwrite to overwrite these files)." Otherwise create output and report file     
      (if(and (.exists (io/as-file (get options :output))) (.exists (io/as-file (get options :report))))     
        (if (get options :force)           
          [(spit (get options :output) (csv/write-csv records))
          (report/append-to-report (get options :report) input (get options :output) (get options :jsonld-context) (get options :force) records)]
          (println "ERROR 101 ‚output or report file exists! (use‚ -f Force overwrite to overwrite these files)."))                      [(spit (get options :output) (csv/write-csv records))
          (report/append-to-report (get options :report) input (get options :output) (get options :jsonld-context) (get options :force) records)])     
    (catch Exception e (log/error (str "Exception in main: " e)))))
    (usage cli-options)   
  ))))                                     
 
  
