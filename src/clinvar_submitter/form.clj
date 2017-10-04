(ns clinvar-submitter.form
 (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
           [clojure.string :as str]
           [clojure-csv.core :as csv]
           [clojure.tools.logging.impl :as impl]
           [clojure.tools.logging :as log])
 (:import [java.util Map List]))
  
(defn csv-colval
	"Outputs a well-formed value for output of the CSV file being generated. 
	   nil should be empty strings, non-strings should be stringified, etc..."
	[v]
  (cond 
    (nil? v) "" 
	  (number? v) (str v) 
    (seq? v) (apply str v) :else v))
 
 ;TODO Can Nafisa and/or Tristan figure out how we can 
 ; sort the evidence by acmg rule precedence 
 ; in the method evidence-rule-strength.
 ; This method will sort the rules in a precedence defined by the static list (reversed)
 ; putting any non-matching values at the end in natural order. element match is case-insensitive.
 (defn by-acmg-rule-id
    "Sort ACMG rules using an indexed list to define the order."
    [a b]
    (let [ar (str/upper-case (get a "id"))
          br (str/upper-case (get b "id"))
          rule-list (vector "BP7" "BP6" "BP5" "BP4" "BP3" "BP2" "BP1" "BS4" "BS3" "BS2" "BS1" "BA1" "PP5" "PP4" "PP3" "PP2" "PP1" "PM6" "PM5" "PM4" "PM3" "PM2" "PM1" "PS4" "PS3" "PS2" "PS1" "PVS1")]
      (let [ai (.indexOf rule-list ar)
            bi (.indexOf rule-list br)]
        (let [c (compare bi ai)]
          (if (= c 0) (compare a b) c)))))
 
 ; *** Interpretation related transformations
 (defn interp-id
   "Return the id portion of the @id url by taking the last part 
    of the url for VariantInterpretation"
   [i]
   (let [full-id (get i "id")]
     (let [id (get (re-find #"\/([a-z0-9\-]+)\/$" full-id) 1)]
     (if (nil? id) (str "*E-401" ":" (rand-int 200))
     (csv-colval id)))))
 
  (defn interp-significance
  "Return the interpretation clinical significance."
  [t i]
  (let [significance (ld-> t i "clinicalSignificance")]
    (if (nil? significance) (str "*E-402" ":" (rand-int 200))
    (csv-colval (get significance "display")))))
  
  (defn interp-eval-date
  "Return the interpretation evaluation date."
  [t i]
  (let [contribution (ld-> t i "contribution")]
    (if (nil? (get contribution "onDate")) (str "*E-403" ":" (rand-int 200))
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
  (if (nil? (get v "CanonicalAllele")) (str "*E-202" ":" (rand-int 200))
  (csv-colval (get v "CanonicalAllele"))))
 
  (defn variant-alt-designations
  "Return variant hgvs representations"
  [v]
  (if (nil? (get v "alleleName name")) (str "*E-203" ":" (rand-int 200))
  (csv-colval (get v "alleleName name"))))
 
 (defn variant-refseq
  "Return the variant reference sequence."
  [t v]
  (let [refseq (ld-> t v "referenceCoordinate" "referenceSequence")]
    (if (nil? (get refseq "display")) (str "*E-204" ":" (rand-int 200))
    (csv-colval (get refseq "display")))))
 
 (defn variant-start
  "Return the variant reference sequence start pos (0-to-1-based transform)."
  [t v]
  ; if ref is not null add 1 to start otherwise as-is
  (let [ref (ld-> t v "referenceCoordinate" "refAllele")
        alt (get v "allele")
        start (ld-> t v "referenceCoordinate" "start")]
    (if (nil? (get start "index")) (str "*E-205" ":" (rand-int 200))
    (csv-colval (if (str/blank? ref) (get start "index") (+ 1 (get start "index")))))))
     
 (defn variant-stop
  "Return the variant reference sequence stop pos (0-to-1-based transform)."
  [t v]
  ; if ref is blank and alt is not add 1 to stop otherwise as-is
  (let [ref (ld-> t v "referenceCoordinate" "refAllele")
        alt (get v "allele")
        stop (ld-> t v "referenceCoordinate" "end")]
    (if (nil? (get stop "index")) (str "*E-206" ":" (rand-int 200))
    (csv-colval (if (and (str/blank? ref) (not (str/blank? alt))) (+ 1 (get stop "index")) (get stop "index"))))))
 
 (defn variant-ref
  "Return the variant ref allele sequence."
  [t v]
  (let [refcoord (ld-> t v "referenceCoordinate")]
    (if (nil? (get refcoord "refAllele")) (str "*E-207" "." (rand-int 200))
    (csv-colval (get refcoord "refAllele")))))
 
 (defn variant-alt
   "Return the variant alt allele sequence."
   [v]
   (if (nil? (get v "allele")) (str "*E-205" ":" (rand-int 200))
   (csv-colval (get v "allele"))))
 
 (defn get-variant
  "Returns a map of all variant related fields needed for the clinvar 
   'variant' submission form.
     :variantId - canonical allele with ClinGen AR PURL,
     :altDesignations - alternate designations (semi-colon separated)
     :refseq - preferred accession GRCh38 ie. NC_000014.9
     :start - genomic start pos (transform to 1-based)
     :stop - genomic end pos (transform to 1-based)
     :ref - ref seq
     :alt - alternate seq"
  [t i]
  (let [v (ld1-> t i "variant" "relatedContextualAllele" (prop= t true "preferred"))]
    {:variantIdentifier (variant-identifier v),
     :altDesignations (variant-alt-designations v),
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
  (csv-colval (ld-> t c "modeOfInheritance" "display")))

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

  (defn evidence-rule-strength
    "Returns the translated strength based on the clingen/acmg recommended 
     values; PS1, PS1_Moderate, BM2, BM2_Strong, etc..
     NOTE: if the rule's default strength is not the selected strength then 
     create the strength by joining the rule and selected strength with an underscore."
    [t e]
    (try
    (let [crit (ld-> t e "information" "criterion")
         act-strength-coding (ld-> t e "evidenceStrength" "coding")
         def-strength-coding (ld-> t e "information" "criterion" "defaultStrength" "coding")
         def-strength-display (ld-> t e "information" "criterion" "defaultStrength" "coding" "display")]
        (let [def-strength-displaylist
          (cond 
          (instance? List def-strength-display)
           def-strength-display
          :else 
          (list def-strength-display))]
          (let [rule-label (get crit "id")
                def-strength (first def-strength-displaylist)
                act-strength (get act-strength-coding  "display")]
          (let [def-direction (get (str/split def-strength #" ") 0)
                def-weight (get (str/split def-strength #" ") 1)
                act-direction (get (str/split act-strength #" ") 0)
                act-weight (get (str/split act-strength #" ") 1)]
          (if (= def-strength act-strength) rule-label (if (= def-direction act-direction) (str rule-label "_" act-weight) #"error"))))))
    (catch Exception e (log/error (str "Exception in evidence-rule-strength: " e)))))

  (defn criteria-assessments
    "Returns the criterion assessments map translated to the standard
     acmg terminology for all evidence lines passed in."
    [t e]
    (map #(evidence-rule-strength t %) e))
  
  (defn evidence-rules
    "Returns the list of criterion rule names for the evidence provided"
    [t e]
    (let [crits (ld-> t e "information" "criterion")]
      (map #(get % "id") crits))) 
  
  (defn evidence-summary
    "Returns a standard formatted summarization of the rules that were met."
    [t e]
    (str "The following criteria were met: " (csv-colval (clojure.string/join ", " (criteria-assessments t e)))))
  
  (defn re-extract
    "Returns a map of matching regex group captures for any vector, list, map which can be flattened."
    [items re group]
    (map #(get (re-find re %) group) (remove nil? (flatten items))))
  
  (defn evidence-pmid-citations
    "Returns the list of critieron pmid citations for the evidence provided"
    [t e]
    (let [info-sources (ld-> t e "information" "evidence" "information" "source")
          pmids (re-extract info-sources #"https\:\/\/www\.ncbi\.nlm\.nih\.gov\/pubmed\/(\p{Digit}*)" 1)]
      (if (nil? pmids) "*W-551"
      (csv-colval (clojure.string/join ", " (map #(str "PMID:" %) pmids))))))
  
  (defn get-met-evidence
   "Returns a collated map of all 'met' evidence records needed for the 
    clinvar 'variant' submission sheet."
   [t i]
   (let [e (ld-> t i "evidence"  (prop= t "met" "information" "outcome" "code"))]     
     (if (nil? e) "*W-551"
     {:summary (evidence-summary t e),
      :rules (evidence-rules t e),
      :assessments (criteria-assessments t e),
      :pmid-citations (evidence-pmid-citations t e)})))
  
  