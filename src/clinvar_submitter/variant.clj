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
  "Construct and return one row of variant table, with variant pathogenicity interpretation as root."
  [sym-tbl interp-input interp-num opts]
  (let [variant (form/get-variant sym-tbl interp-input interp-num)
        interp (form/get-interpretation sym-tbl interp-input interp-num)
        condition (form/get-condition sym-tbl interp-input interp-num (:significance interp))
        evidence (form/get-met-evidence sym-tbl interp-input)
        method (form/get-assertion-method sym-tbl interp-input interp-num)
        approver (form/get-contribution sym-tbl interp-input "approver")
        variant-coord (:coord variant)
        hgvs (:hgvs variant)]
    [(:id interp "") ; (A) local id
     (:id interp "") ; (B) linking id - only needed if providing case data or func evidence tabs
     (:gene variant "")        ; (C) gene symbol - not provided since we are avoiding contextual allele representation.
     (:refseq variant)         ; (D) refseq
     (if (some? hgvs) hgvs "") ; (E) hgvs - not providing since we are avoiding contextual allele representation
     (if (nil? (:refseq variant)) (:chromosome variant) "")  ; (F) chromosome - only provide if the refseq field is nil - clinvar does not accept both fields.
     (if (nil? hgvs) (:start variant-coord) "") ; (G) start + 1  (from 0-based to 1-based)
     (if (nil? hgvs) (:stop variant-coord) "")  ; (H) stop + 1  (from 0-based to 1-based)
     (if (nil? hgvs) (:ref variant-coord) "")   ; (I) ref
     (if (nil? hgvs) (:alt variant-coord) "")   ; (J) alt
     "" ; (K) variant type - non sequence var only
     "" ; (L) outer start - non sequence var only
     "" ; (M) inner start - non sequence var only
     "" ; (N) inner stop - non sequence var only
     "" ; (O) outer stop - non sequence var only
     "" ; (P) variant length - non sequence var only
     "" ; (Q) copy number - non sequence var only
     "" ; (R) ref copy number - non sequence var only
     "" ; (S) breakpoint 1 - non sequence var only
     "" ; (T) breakpoint 2 - non sequence var only
     "" ; (U) comment on variant - req'd if var type is complex
     "" ; (V) empty
    ;; (get variant :variantIdentifier) ; Variation identifiers (http://reg.genome.network.org/allele = ABC ABC:CA123123123)
     "" ; (W) ClinVar does not accept the CAR identifiers? (replacing above commented code)
     "" ; (X) Location - N/A
     (:alt-designations variant)    ; (Y) Alternate designations
     "" ; (Z) URL - bypassing for now, no set home for a public URL at this time
     "" ; (AA) empty
     ;;
     (:id-type condition "")    ; (AB) Condition ID type (PURL)- assumes variantInterpretation.condition.disease.coding[0].code match of everything before the underscore.
     (:id-value condition "")   ; (AC) Condition ID value - assumes variantInterpretation.condition.disease.coding[0].code match of everything after the underscore.
     (if (and (:id-type condition) (:id-value condition)) "" (:preferred-name condition ""))  ; (AD) Preferred condition name
     "" ; (AE) Condition uncertainty
     "" ; (AF) Condition comment
     "" ; (AG) empty
     (:significance interp) ; (AH) Clinical significance
     "" ; Assertion score
     (:eval-date interp)    ; (AJ) Date last evaluated
     (:moi condition "")    ; (AK) Mode of Inheritance
     (form/get-pmid-list evidence) ; (AL) significance citations
     (str "https://erepo.clinicalgenome.org/evrepo/ui/interpretation/" (:id interp)) ; (AM) Citations or URLs for clinical significance
     (if (some? (:description interp))
       (:description interp)
       (form/summary-string evidence interp variant condition method approver)) ; (AN) comment on clinical significance
     "" ; explanation if clinsig is other or drug
     "" ; drug response condition
     "" ; empty
     "curation" ;; (AR) - hardcoded per LB
     "germline" ;; (AS) - hardcoded per LB
     "unknown"  ;; (AT) - hardcoded per LB
     "" ; structural variant method/analysis type
     "" ; clinical features
     "" ; comment on clinical features
     "" ; (AX) date phenotype was evaluated
     "" ; tissue
     "" ; sex
     "" ; age range
     "" ; population group/ethnicity
     "" ; geographic origin 
     "" ; family history
     "" ; indication
     "" ; total number of individuals tested
     "" ; (BG) number of families tested
     "" ; empty
     "" ; number of individuals with variant
     "" ; number of families with variant
     "" ; number of families with segregation observed
     "" ; (BL) secondary finding
     "" ; mosaicism
     "" ; number of homozygotes
     "" ; number of single heterozygotes
     "" ; number of compound heterozygotes
     "" ; number of hemizygotes
     "" ; evidence citations
     "" ; comment on evidence
     "" ; empty
     "" ; (BU) test name or type
     "" ; platform type
     "" ; platform name
     "" ; method
     "" ; method purpose
     "" ; method citations
     "" ; software name and version
     "" ; software purpose
     "" ; (CC) testing laboratory
     "" ; date variant was reported by submitter
     "" ; testing laboratory interpretation
     "" ; empty
     "" ; comment
     (:id variant "")                              ; (CH) private comment - post clinvar or clingen ar variant id here for users
     (first (str/split (:scv variant "") #"\.+"))  ; (CI) scv if it was able to find a match, without version info
     (if (str/blank? (:scv variant)) "" "Update")  ; (CJ) Novel or Update .. always update if prior column is not empty.
     ""])) ; replaces ClinVarAccessions

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path options]
  ;;(get options :jsonld-context) (get options :method) (get options :method-citation) (get)
  ;;context-path assertion-method method-citation]
  (log/debug "Function: construct-variant-table- context and input Filename (construct-variant-table): " interp-path)
  (try
    (let [sym-tbl (ld/generate-symbol-table interp-path)
          m (vals sym-tbl)
          interps ((prop= sym-tbl "variant pathogenicity interpretation" "type") m)
          rows (map #(construct-variant sym-tbl %  (+ 1 (.indexOf interps %)) options) interps)]
      rows)
    (catch Exception e
      (log/error (str "Exception in construct-variant-table: " e)))))

(def schema-uri "http://dataexchange.clinicalgenome.org/interpretation/json/schema.json")

(defn process-input
  "From the command line arguments, process and return appropriate output for input file"
  [input-rows options]
  (let [records (construct-variant-table input-rows options)
        schema (slurp schema-uri)
        validate (v/validator schema)]
    (if (nil? (validate input-rows))
      (do
        ;; Json vaidated
        (log/debug "Json input is valid")
        records))))
      
