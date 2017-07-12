(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clinvar-submitter.form :as form] 
            [clojure-csv.core :as csv])
  (:gen-class))

; TODO build out formal condition-XXX methods with clinvar rules and logging.
;((defn condition-name
;  "Returns the condition name based on the clinvar submission rules.
;   The disease (dis) and phenotype (phen) maps are passed in as well in order
;   to determine whether any warnings should be reported. 
;   Additionally, the significance (sig) of the interp is passed in to
;   determine if the [Not Specified] name should be defaulted for B, LB, VUS."
;  [c dis phen sig]
;  (let [cn (if (nil? c) nil (get c "name"))]
;    ; test to see if no name and no diseases or phenotypes were provided. if so, report warning.
;    (if (and (every? nil? '(c cn dis phen)) (some (partial = (lower cn)) ["not specified" "not provided"] (lower cn) )) 
;      (log "WARNING - No condition name, disease or phenotype has been specified for a Path or Likely Path interpretation.")
;      ())))
      

(defn condition-name
  "Returns clinvar condition name based on available content. 
  May log warnings if available content does not conform to
  clinvar specifcations."
  [c]
  (get c "name"))

(defn condition-idtype
  ;TODO modify to deal with phenotypes and multi-values for satisfying clinvar specs.
  [t c]
  (let [disease-coding (ld-> t c "disease" "coding")]
    (let [disease-code (get disease-coding "code")]
      (get (re-find #"(.*)\_(.*)" disease-code) 1))))

(defn condition-idvals
   ;TODO modify to deal with phenotypes and multi-values for satisfying clinvar specs.
  [t c]
  (let [disease-coding (ld-> t c "disease" "coding")]
    (let [disease-code (get disease-coding "code")]
      (get (re-find #"(.*)\_(.*)" disease-code) 2))))

(defn condition-moi
  [t c]
  [""])

(defn get-condition
  "Processes the condition element to derive the ClinVar sanctioned fields: 
   for idtype & idvalue or preferred name. 

   -- conflicting disease, phenotype and/or name values
   If more than one of disease, phenotype or name is not nil than a warning should 
   be reported 'disease and phenotypes should not be combined in clinvar 
   submission form' or 'preferred name and disease/phenotype should not be 
   combined in clinvar submission form'.
   
   -- multiple disease.codings or phenotype.codings with different idtypes
   Also, if more than one coding is provided and they do not all have the same
   idtype a warning should be reported 'clinvar requires a single idtype, only XXX 
   is being included in the submission.'  

   If more than one coding exists with a single idtype, then all idvalues should be 
   separated with semi-colons.
   
   -- blank or nil disease, phenotype or name values
   if the disease, phenotype and name values are all blank (or nil) and the 
   clinical significance value is not Path or Lik Path, then the name will
   be set to 'Not Specified' by default.  If it is Path or Lik Path a
   warning will be reported 'Condition is missing for a clinically significant
   interpretation.'"
  [t i]
  (let [c (ld-> t i "condition")]
     {:name (condition-name c),
      :idtype (condition-idtype t c),
      :idvalue (condition-idvals t c),
      :moi (condition-moi t c)}))

(defn get-cx-allele
  "Returns a map of all variant related fields needed for the clinvar submission
   form.
     :variantId - canonical allele with ClinGen AR PURL,
     :refseq - preferred accession build38 ie. NC_000014.9
     :start - genomic start pos (1-based)
     :end - genomic end pos (1-based)
     :ref - ref seq
     :alt - alternate seq"
  ; TODO finish building map here and using as described above.
  [t i]
  (ld1-> t i "variant" "relatedContextualAllele" (prop= t true "preferred")))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (let [cx-allele (get-cx-allele t i)
        condition (get-condition t i)]
    ["" ; local id  -- TODO need to figure out if there's a stable id we can depend on here with VCI team.
     "" ; linking id  
     "" ; gene symbol
     (form/csv-colval (ld-> t cx-allele "referenceCoordinate" "referenceSequence" "display")) ;rdefseq
     "" ; hgvs
     "" ; chromosome
     (form/csv-colval (ld-> t cx-allele "referenceCoordinate" "start" "index")) ; start
     (form/csv-colval (ld-> t cx-allele "referenceCoordinate" "end" "index")) ; stop
     (form/csv-colval (ld-> t cx-allele "referenceCoordinate" "refAllele")) ; ref
     (form/csv-colval (ld-> t cx-allele "allele")) ; alt
     "" ; variant type
     "" ; outer start
     "" ; inner start
     "" ; inner stop
     "" ; outer stop
     "" ; variant length
     "" ; copy number
     "" ; ref copy number
     "" ; breakpoint 1
     "" ; breakpoint 2
     "" ; Trace or probe data	
		 "" ; empty
		 "" ; Variation identifiers	(http://reg.genome.network.org/allele = ABC ABC:CA123123123)
		 "" ; Location	
		 "" ; Alternate designations 	
		 "" ; Official allele name	
		 "" ; URL	
		 "" ; empty 
     (form/csv-colval (get condition :idtype)) ; Condition ID type (PURL)- assumes variantInterpretation.condition.disease.coding[0].code match of everything before the underscore.
		 (form/csv-colval (get condition :idvalue))  ; Condition ID value	- assumes variantInterpretation.condition.disease.coding[0].code match of everything after the underscore.
		 (form/csv-colval (get condition :name)) ; Preferred condition name
     "" ; Condition category	
     "" ; Condition uncertainty	
     "" ; Condition comment	
     "" ; empty	
     (form/csv-colval (ld-> t i "clinicalSignificance" "display")) ; Clinical significance	
     "" ; Date last evaluated
     "" ; TODO add remaining values from Variant Sheet in CLINVAR submission form...
     ]))

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path]
  (let [t (ld/read-ld interp-path context-path)
        m (vals t)
        interps ((prop= t "VariantInterpretation" "type") m)
        rows (map #(construct-variant t %) interps)]
    rows))

(defn -main
  "take input assertion, transformation context, and output filename as input
  and write variant table in csv format"
  [in cx out & args]
  (spit out (csv/write-csv (construct-variant-table in cx))))
