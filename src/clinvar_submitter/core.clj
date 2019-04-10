(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clinvar-submitter.form :as form]
            [clinvar-submitter.variant :as variant]
            [clojure.string :as str]
            [scjsv.core :as v]
            [clinvar-submitter.report :as report]
            [clojure.java.io :as io]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [cheshire.core :as json]
            [clinvar-submitter.web-service :as web]
            [clojure.pprint :refer [pprint]])
  (:import [java.lang.Exception])
  (:gen-class))

(def cli-options
  [;; output file defaults to clinvar-variant.csv and will not overwrite
   ;; unless -f force-overwrite option is specified
   ["-o" "--output FILENAME" "CSV output filename" :default "clinvar-variant.csv"]
   ["-c" "--clinvar-scv-file FILENAME" "JSON clinvar scv filename for this EP"]
   ["-f" "--force" :default false]
   ["-x" "--jsonld-context URI" "JSON-LD context file URI"
    :default "http://dataexchange.clinicalgenome.org/interpretation/json/sepio_context"]
   ["-r" "--report FILENAME" "Run-report filename" :default "clinvar-submitter-run-report.csv"]
   ["-l" "--collection-method COLLECTIONMETHOD" "Collection method (see ClnVar for allowed values)" :default "curation"]
   ["-a" "--allele-origin ALLELEORIGIN" "Allele origin (see ClnVar for allowed values)" :default "germline"]
   ["-s" "--affected-status AFFECTEDSTATUS" "Affected status (see ClnVar for allowed values)" :default "unknown"]
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
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (or (:help options) (missing-required? options)) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; command-line argument file processing mode
      (= 1 (count arguments))
      ;; add :input = input file name k/v to options
      {:input (first arguments) :options (assoc options :input (first arguments))}

      ;; web-service processing mode
      (= (:web-service options) true)
      {:options (dissoc options :output :report)} ;; remove output file name and report file name from options
      
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
 (println msg))


(defn -main
  "take input assertion, transformation context, and output filename as input and write variant table in csv format"
  [& args]
  (let [{:keys [input options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (if (:web-service options)
        (web/run-service options) ;; run as web service - pass in options map
        (variant/process-input (slurp input) options))))) ;; or as command-line processor

