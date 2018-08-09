(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clinvar-submitter.form :as form]
            [clojure.string :as str]
            [scjsv.core :as v]
            [clinvar-submitter.report :as report]
            [clojure.java.io :as io]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [clinvar-submitter.web-service :as web]
            [clojure.pprint :refer [pprint]])
  (:import [java.lang.Exception])
  (:gen-class))

(def cli-options
  [;; output file defaults to clinvar-variant.csv and will not overwrite 
   ;; unless -f force-overwrite option is specified
   ["-o" "--output FILENAME" "CSV output filename" :default "clinvar-variant.csv"]
   ["-f" "--force" :default false]
   ["-x" "--jsonld-context URI" "JSON-LD context file URI" 
    :default "http://dataexchange.clinicalgenome.org/interpretation/json/sepio_context"]
   ["-b" "--build BUILD" "Genome build alignment, GRCh37 or GRCh38"
    :default "GRCh37"]
   ["-r" "--report FILENAME" "Run-report filename" :default "clinvar-submitter-run-report.csv"]
   ["-m" "--method METHODNAME" "Assertion-method-name" :default "ACMG Guidelines, 2015"]
   ["-c" "--methodc METHODCITATION" "Method Citation" :default "PMID:25741868"]
   ["-w" "--web-service" :default false]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["The clinvar-submitter program converts one or more ClinGen variant "
        " interpretation json files into a CSV formatted list which can be "
        " pasted into the ClinVar Submission spreadsheet (variant sheet). "
        " Basic validation checking provides warnings and errors at a record "
        " and field level."
        ""
        "Usage: clinvar-submitter [options]"
        ""
        "Options:"
        options-summary
        ""
        "Input:"
        "  <filename>    The filename of a json file to be converted"
        ""
        "Please refer to http://dataexchange.clinicalgenome.org/interpretation "
        " for additional details on the variant pathogenicity interpretation json model."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(def required-opts #{})

(defn missing-required?
  "Returns true if opts is missing any of the required-opts"
  [opts]
  (not-every? opts required-opts))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]  

  ;; TODO Improve error message reporting in case of missing arguments
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (or (:help options) (missing-required? options)) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      (and (:web-service options) (= 0 (count arguments)))
      {:options options}
      
      ;; custom validation on arguments
      (and (= 1 (count arguments)) (nil? (:web-service options)))
      {:input (first arguments) :options options}

      :else ; failed custom validation => exit with usage summary      
      {:exit-message (usage summary)})))

(defn exit [status msg]
 (println msg))

(defn construct-variant
  "Construct and return one row of variant table, with variant pathogenicity interpretation as root"
  [t i n assertion-method method-citation]
  (log/debug "Function: construct-variant - constructing one row of variant table, with variant pathogenicity interpretation as root")
  (try 
  (let [variant (form/get-variant t i n)
        condition (form/get-condition t i n)
        interp (form/get-interpretation t i n)
        evidence (form/get-met-evidence t i n)]       
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
     "" ; comment on variant - req'd if var type is complex
     "" ; Trace or probe data - non sequence var only
     "" ; empty
     (get variant :variantIdentifier) ; Variation identifiers (http://reg.genome.network.org/allele = ABC ABC:CA123123123)
     "" ; Location - N/A
     (get variant :altDesignations)    ; Alternate designations   
     "" ; Official allele name  - N/A
     "" ; URL - bypassing for now, no set home for a public URL at this time
     "" ; empty 
     (get condition :idtype) ; Condition ID type (PURL)- assumes variantInterpretation.condition.disease.coding[0].code match of everything before the underscore.
     (get condition :idvalue)  ; Condition ID value - assumes variantInterpretation.condition.disease.coding[0].code match of everything after the underscore.
     (get condition :name) ; Preferred condition name
     "" ; Condition category  
     "" ; Condition uncertainty 
     "" ; Condition comment 
     "" ; empty 
     (get interp :significance) ; Clinical significance 
     (get interp :eval-date) ; Date last evaluated
     (str assertion-method) ; assertion method
     (str method-citation) ; assertion method citations
     (get condition :moi) ; Mode of Inheritance
     (get evidence :pmid-citations) ; significance citations
     "" ; significance citations without db ids
     (get evidence :summary) ; comment on clinical significance
     "" ; explanation if clinsig is other or drug
    ])
   (catch Exception e (do (pprint e) (log/error (str "Exception construct-variant: " e))))))


(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path assertion-method method-citation]
  (log/debug "Function: construct-variant-table- context and input Filename (construct-variant-table): " interp-path context-path)
  (try
    (let [t (ld/generate-symbol-table interp-path context-path)
          m (vals t)
          interps ((prop= t "variant pathogenicity interpretation" "type") m)
          rows (map #(construct-variant t %  (+ 1 (.indexOf interps %)) assertion-method method-citation) interps)]
      rows)
    (catch Exception e 
      (log/error (str "Exception in construct-variant-table: " e)))))

(def schema-uri "http://dataexchange.clinicalgenome.org/interpretation/json/schema.json")

(defn process-input-file
  "From the command line arguments, process and return appropriate output for input file"
  [input options]
  (let [records (construct-variant-table input (get options :jsonld-context) (get options :method) (get options :methodc))
        output-file (get options :output)
        report-file (get options :report)
        existing-files (remove nil? (map #(if (.exists (io/as-file %)) (str "'" % "'") nil ) [output-file report-file]))
        schema (slurp schema-uri)
        validate (v/validator schema)]
                                        ;(log/debug "Input,output and context filename in main method: " input (get options :jsonld-context) (get options :output))
    (if (nil? (validate (slurp input))) (log/debug "Json input is valid"))
    (try
      ;; if output or report file exists then check if there is a force option. 
      ;; If there is no force option the display an exception
      ;; Otherwise create output and report file     
      (if (and (> (count existing-files) 0) (not (get options :force)))
        (println (str "**Error**"
                      "\nThe file"
                      (if (> (count existing-files) 1) "s " " ")
                      (str/join " & " existing-files)
                      " already exist in the output directory."
                      "\nUse option‚ -f Force overwrite to overwrite existing file(s)."))
        (report/write-files input options records))
      (catch Exception e (log/error (str "Exception in main: " e))))))

(defn -main
  "take input assertion, transformation context, and output filename as input and write variant table in csv format"
  [& args]
  (let [{:keys [input options exit-message ok?]} (validate-args args)] 
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (if (:web-service options)
        (web/run-service)
        (process-input-file input options)))))
