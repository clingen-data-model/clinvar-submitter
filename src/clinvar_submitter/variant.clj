(ns clinvar-submitter.variant
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clinvar-submitter.form :as form]
            [clojure.string :as str]
            [scjsv.core :as v]
            [clinvar-submitter.report :as report]
            [clojure.java.io :as io]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]])
  (:import [java.lang.Exception])
  (:gen-class))

(defn construct-variant
  "Construct and return one row of variant table, with variant pathogenicity interpretation as root"
  [sym-tbl interp-input interp-num opts scv-map]
  (let [variant (form/get-variant sym-tbl interp-input interp-num scv-map)
        interp (form/get-interpretation sym-tbl interp-input interp-num)
        condition (form/get-condition sym-tbl interp-input interp-num (:significance interp))
        evidence (form/get-met-evidence sym-tbl interp-input)
        method (form/get-assertion-method sym-tbl interp-input interp-num)
        approver (form/get-contribution sym-tbl interp-input "approver")
        variant-coord (:coord variant)
        hgvs (:hgvs variant)]
    [(:id interp "") ; local id
     (:id interp "") ; linking id - only needed if providing case data or func evidence tabs
     (:gene variant "") ; gene symbol - not provided since we are avoiding contextual allele representation.
     (:refseq variant) ;refseq
     (if (some? hgvs) hgvs "") ; hgvs - not providing since we are avoiding contextual allele representation
     (:chromosome variant "")  ; chromosome - not providing since we are using the refseq field to denote the accession.
     (if (nil? hgvs) (:start variant-coord) "") ; start + 1  (from 0-based to 1-based)
     (if (nil? hgvs) (:stop variant-coord) "")  ; stop + 1  (from 0-based to 1-based)
     (if (nil? hgvs) (:ref variant-coord) "")   ; ref
     (if (nil? hgvs) (:alt variant-coord) "")   ; alt
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
     ;(get variant :variantIdentifier) ; Variation identifiers (http://reg.genome.network.org/allele = ABC ABC:CA123123123)
     "" ; ClinVar does not accept the CAR identifiers? (replacing above commented code)
     "" ; Location - N/A
     (:alt-designations variant)    ; Alternate designations
     "" ; Official allele name  - N/A
     "" ; URL - bypassing for now, no set home for a public URL at this time
     "" ; empty
     ;;
     (:id-type condition "") ; Condition ID type (PURL)- assumes variantInterpretation.condition.disease.coding[0].code match of everything before the underscore.
     (:id-value condition "")   ; Condition ID value - assumes variantInterpretation.condition.disease.coding[0].code match of everything after the underscore.
     (if (and (:id-type condition) (:id-value condition)) "" (:preferred-name condition ""))  ; Preferred condition name
     "" ; Condition category
     "" ; Condition uncertainty
     "" ; Condition comment
     "" ; empty
     (:significance interp) ; Clinical significance
     (:eval-date interp) ; Date last evaluated
     ;; the 2 following assertion fields may need more robust handling for non-vceps that don't have clinvar files submitted.
     "" ; assertion method
     (:file-name method ""); assertion method citations
     (:moi condition "") ; Mode of Inheritance
     (form/get-pmid-list evidence) ; significance citations
     (str "https://erepo.clinicalgenome.org/evrepo/ui/interpretation/" (:id interp)) ; Citations or URLs for clinical significance
     (if (some? (:description interp))
       (:description interp)
       (form/summary-string evidence interp variant condition method approver)) ; comment on clinical significance
     "" ; explanation if clinsig is other or drug
     ""
     ""
     ""
     ""
     (str (:collection-method opts))
     (str (:allele-origin opts))
     (str (:affected-status opts))  ;; this is col#52 AZ
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     ""
     (:id variant "")  ; clinvar or clingen ar variant id
     (first (str/split (:scv variant "") #"\.+"))  ; scv if it was able to find a match, without version info
     (if (str/blank? (:scv variant)) "" "Update")]))  ; Novel or Update .. always update if prior column is not empty.

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path options]
  ;;(get options :jsonld-context) (get options :method) (get options :method-citation) (get)
  ;;context-path assertion-method method-citation]
  (log/debug "Function: construct-variant-table- context and input Filename (construct-variant-table): " interp-path (:jsonld-context options))
  (try
    (let [clinvar-scv-file (:clinvar-scv-file options)
          scv-map (if clinvar-scv-file (json/parse-string (slurp clinvar-scv-file) true))
          sym-tbl (ld/generate-symbol-table interp-path (:jsonld-context options))
          m (vals sym-tbl)
          interps ((prop= sym-tbl "variant pathogenicity interpretation" "type") m)
          rows (map #(construct-variant sym-tbl %  (+ 1 (.indexOf interps %)) options scv-map) interps)]
      rows)
    (catch Exception e
      (log/error (str "Exception in construct-variant-table: " e)))))

(def schema-uri "http://dataexchange.clinicalgenome.org/interpretation/json/schema.json")

(defn process-input
  "From the command line arguments, process and return appropriate output for input file"
  [input-rows options]
  (let [records (construct-variant-table input-rows options)
        output-file (:output options)
        report-file (:report options)
        existing-files () ;; TODO TON - FIX - (remove nil? (map #(if (.exists (io/as-file %)) % nil) [output-file report-file]))
        schema (slurp schema-uri)
        validate (v/validator schema)]
    (if (nil? (validate input-rows))
      (do
        ;; Json vaidated
        (log/debug "Json input is valid")
        
        ;; if web service running, return the list of processed records
        (if (:web-service options)
          records ;; return the list of records 

          ;; Otherwise:
          ;; if output or report file exists then check if there is a force option.
          ;; If there is no force option the display an exception
          ;; Otherwise create output and report file
          (if (and (not (.isEmpty existing-files)) (not (get options :force)))
            (println (str "**Error**"
                               "\nThe file"
                               (if (> (count existing-files) 1) "s " " ")
                               (str/join " & " existing-files)
                               " already exist in the output directory."
                               "\nUse option‚ -f Force overwrite to overwrite existing file(s)."))
            (report/write-files input-rows options records))))
      (log/error "JSON input failed schema validation."))))
