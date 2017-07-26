(ns clinvar-submitter.form
 (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]))
  
 (defn csv-colval
	 "Outputs a well-formed value for output of the CSV file being generated. 
	   nil should be empty strings, non-strings should be stringified, etc..."
	 [v]
	 (if (nil? v) "" (if (number? v) (str v) v)))
 
 ; *** Interpretation related transformations
 (defn interp-id
   "Return the id portion of the @id url by taking the last part 
    of the url for VariantInterpretation"
   [i]
   (let [full-id (get i "id")]
     (csv-colval (get (re-find #"\/([a-z0-9\-]+)\/$" full-id) 1))))
 
  (defn interp-significance
  "Return the interpretation clinical significance."
  [t i]
  (let [significance (ld-> t i "clinicalSignificance")]
    (csv-colval (get significance "display"))))
  
  (defn interp-eval-date
  "Return the interpretation evaluation date."
  [t i]
  (let [contribution (ld-> t i "contribution")]
    (if (nil? (get contribution "onDate")) "" 
      (.format 
        (java.text.SimpleDateFormat. "yyyy-MM-dd") 
        (.parse
          (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
          (get contribution "onDate"))))))
  
 (defn get-interpretation
   "Returns a map of all interpretation related fields needed for the 
    clinvar 'variant' submission sheet."
   [t i]
   {:id (interp-id i),
    :significance (interp-significance t i),
    :eval-date (interp-eval-date t i)})
 
 ; *** Variant related transformations
 (defn variant-identifier
  "Return variant identifier, typically the ClinGen AlleleReg id."
  [v]
  (csv-colval (get v "CanonicalAllele")))
 
 (defn variant-refseq
  "Return the variant reference sequence."
  [t v]
  (let [refseq (ld-> t v "referenceCoordinate" "referenceSequence")]
    (csv-colval (get refseq "display"))))
 
 (defn variant-start
  "Return the variant reference sequence start pos (1-based transform)."
  [t v]
  ; TODO figure out how to go from 0-based to 1-based
  (let [start (ld-> t v "referenceCoordinate" "start")]
    (csv-colval (get start "index"))))
 
 (defn variant-stop
  "Return the variant reference sequence stop pos (1-based transform)."
  [t v]
  ; TODO figure out how to go from 0-based to 1-based
  (let [stop (ld-> t v "referenceCoordinate" "end")]
    (csv-colval (get stop "index"))))
 
 (defn variant-ref
  "Return the variant ref allele sequence."
  [t v]
  (let [refseq (ld-> t v "referenceCoordinate")]
    (csv-colval (get refseq "refAllele"))))
 
 (defn variant-alt
   "Return the variant alt allele sequence."
   [v]
   (csv-colval (get v "allele")))
 
 (defn get-variant
  "Returns a map of all variant related fields needed for the clinvar 
   'variant' submission form.
     :variantId - canonical allele with ClinGen AR PURL,
     :refseq - preferred accession build38 ie. NC_000014.9
     :start - genomic start pos (transform to 1-based)
     :stop - genomic end pos (transform to 1-based)
     :ref - ref seq
     :alt - alternate seq"
  [t i]
  (let [v (ld1-> t i "variant" "relatedContextualAllele" (prop= t true "preferred"))]
    {:variantIdentifier (variant-identifier v),
     :refseq (variant-refseq t v),
     :start (variant-start t v),
     :stop  (variant-stop t v),
     :ref (variant-ref t v),
     :alt (variant-alt v)}))
 
    ; *** Condition related transformations
 
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
  [t c]
  (let [name (get c "name")]
    (csv-colval (if (nil? name) "" name))))

(defn condition-idtype
  ;TODO modify to deal with phenotypes and multi-values for satisfying clinvar specs.
  [t c]
  (let [disease-coding (ld-> t c "disease" "coding")]
    (let [disease-code (get disease-coding "code")]
      (csv-colval (get (re-find #"(.*)\_(.*)" disease-code) 1)))))

(defn condition-idvals
   ;TODO modify to deal with phenotypes and multi-values for satisfying clinvar specs.
  [t c]
  (let [disease-coding (ld-> t c "disease" "coding")]
    (let [disease-code (get disease-coding "code")]
      (csv-colval (get (re-find #"(.*)\_(.*)" disease-code) 2)))))

(defn condition-moi
  [t c]
  (csv-colval (get c "modeOfInheritance")))

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
     {:name (condition-name t c),
      :idtype (condition-idtype t c),
      :idvalue (condition-idvals t c),
      :moi (condition-moi t c)}))

  (defn get-met-evidence
   "Returns a collated map of all 'met' evidence records needed for the 
    clinvar 'variant' submission sheet."
   [t i]
   (let [e (ld-> t i "evidence"  (prop= t "met" "information" "outcome" "code"))]
     (let [crits (ld-> t e "information" "criterion")]
       (clojure.string/join ", " (map #(get % "id") crits)))))
 