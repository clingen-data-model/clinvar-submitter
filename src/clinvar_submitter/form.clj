(ns clinvar-submitter.form
 (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
           [clojure.string :as str]
           [clinvar-submitter.report :as report]
           [clojure.tools.logging.impl :as impl]
           [clojure.tools.logging :as log]))

(defn csv-colval
	"Outputs a well-formed value for output of the CSV file being generated. 
	   nil should be empty strings, non-strings should be stringified, etc..."
	[v errorcode]
  (cond 
    (nil? v) errorcode
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

;*** Interpretation related transformations ***
(defn interp-id
   "Return the id portion of the @id url by taking the last part 
    of the url for VariantInterpretation. If id is null return error message"
   [i r]
   (let [full-id (get i "id")]
   (let [id (get (re-find #"\/([a-z0-9\-]+)\/$" full-id) 1)]
     (if (nil? id) (report/append-to-report r "\n\n*E-401: Interpretation id not provided"))
     (csv-colval id "*E-401"))))
 
  (defn interp-significance
  "Return the interpretation clinical significance."
  [t i r]
    (let [significance (ld-> t i "clinicalSignificance")]
    (if (nil? significance) (report/append-to-report r "\n\n*E-402: Interpretation significance not provided"))
    (csv-colval (get significance "display") "*E-402")))
  
  (defn interp-eval-date
  "Return the interpretation evaluation date.  
   The date should be wrapped in double quotes and preceded by an equal sign (=)
   in order to prevent Excel from converting it to a date internally.
   ClinVar requires that the yyyy-MM-dd format is maintained in their submission."
  [t i r]
  (if(nil? (ld-> t i "contribution")) (report/append-to-report r "\n\n*E-403: Interpretation evaluation date not provided"))
  (let [contribution (ld-> t i "contribution")]
    (if (nil? (get contribution "onDate")) "*E-403" 
      (try
      (.format 
        (java.text.SimpleDateFormat. "'=\"'yyyy-MM-dd'\"'") 
        (.parse
          (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
          (get contribution "onDate")))
       (catch Exception e
         (report/append-to-report r "\n\n*E-404: Interpretation evaluation date format not valid")  (get contribution "onDate") "*E-404")))))
  
  (defn get-interpretation
   "Returns a map of all interpretation related fields needed for the 
    clinvar 'variant' submission sheet."
   [t i r]
   {:id (interp-id i r),
    :significance (interp-significance t i r),
    :eval-date (interp-eval-date t i r)})

;*** Variant related transformations ***

 (defn variant-identifier
   "Return variant identifier, typically the ClinGen AlleleReg id."
   [v r]
   (if (nil? v) (report/append-to-report r "\n\n*E-202: Variant identifier not provided."))
   (csv-colval (get v "CanonicalAllele") "*E-202"))


  (defn variant-alt-designations
  "Return variant hgvs representations"
  [v r]
  (if (nil? v) (report/append-to-report r "\n\n*W-251: Preferred variant alternate designation not provided"))
  (csv-colval (get v "alleleName name") "*W-251"))
 
 (defn variant-refseq
  "Return the variant reference sequence."
  [t v r] 
  (let [refseq (ld-> t v "referenceCoordinate" "referenceSequence")]
  (if (nil? refseq) (report/append-to-report r "\n\n*E-204: Preferred variant reference sequence not provided."))
    (csv-colval (get refseq "display") "*E-204")))

(defn variant-start
  "Return the variant reference sequence start pos (0-to-1-based transform)."
  [t v r]
   ;if ref is not null add 1 to start otherwise as-is
  (let [ref (ld-> t v "referenceCoordinate" "refAllele")
        alt (get v "allele")
        start (ld-> t v "referenceCoordinate" "start")]
    (if (nil? start) (report/append-to-report r "\n\n*E-205: Preferred variant start coordinate not provided."))
    (csv-colval (if (str/blank? ref) (get start "index") (+ 1 (get start "index"))) "*E-205")))

(defn variant-stop
  "Return the variant reference sequence stop pos (0-to-1-based transform)."
  [t v r]
   ;if ref is blank and alt is not add 1 to stop otherwise as-is
  (let [ref (ld-> t v "referenceCoordinate" "refAllele")
        alt (get v "allele")
        stop (ld-> t v "referenceCoordinate" "end")]
    (if (nil? stop) (report/append-to-report r "\n\n*E-206: Preferred variant stop coordinate not provided."))
    (csv-colval (if (and (str/blank? ref) (not (str/blank? alt))) (+ 1 (get stop "index")) (get stop "index")) "*E-206")))
 
 (defn variant-ref
  "Return the variant ref allele sequence."
  [t v r]
  (let [refcoord (ld-> t v "referenceCoordinate")] 
    (if (nil? v) (report/append-to-report r "\n\n*E-207: Preferred variant reference allele not provided."))
    (csv-colval (get refcoord "refAllele") "*E-207")))
 
 (defn variant-alt
   "Return the variant alt allele sequence."
   [v r]
   (if (nil? v) (report/append-to-report r "\n\n*E-208: Preferred variant alternate allele not provided."))
   (csv-colval (get v "allele") "*E-208"))
 
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
  [t i r]
  (let [v (ld1-> t i "variant" "relatedContextualAllele" (prop= t true "preferred"))]
  (if (nil? v ) (log/error (str "Exception in get-variant: relatedContextualAllele not found")))
    {:variantIdentifier (variant-identifier v r),
     :altDesignations (variant-alt-designations v r),
     :refseq (variant-refseq t v r),
     :start (variant-start t v r),
     :stop  (variant-stop t v r),
     :ref (variant-ref t v r),
     :alt (variant-alt v r)}))

;*** Condition related transformations 

 (defn condition-name
  "Returns clinvar condition name based on available content. 
  May log warnings if available content does not conform to
  clinvar specifcations."
  [t c r]
  (if (not (nil? c)) 
  (let [name (get c "name")]
    (csv-colval (if (nil? name) "" name) "*E-301"))
  (report/append-to-report r "\n\n*E-301: Condition disease code or name not provided.")))

(defn condition-idtype
  ;TODO modify to deal with phenotypes and multi-values for satisfying clinvar specs.
  [t c r]
  (if (not (nil? c)) 
  (let [disease-coding (ld-> t c "disease" "coding")]
    (let [disease-code (get disease-coding "code")]
      (csv-colval (if (nil? disease-code) "" (get (re-find #"(.*)\_(.*)" disease-code) 1)) "*E-301")))
  (report/append-to-report r "\n\n*E-301: Condition disease code or name not provided.")))


(defn condition-idvals
;TODO modify to deal with phenotypes and multi-values for satisfying clinvar specs.
[t c r]
(if (not (nil? c)) 
(let [disease-coding (ld-> t c "disease" "coding")]
(let [disease-code (get disease-coding "code")]
(csv-colval (if (nil? disease-code) "" (get (re-find #"(.*)\_(.*)" disease-code) 2)) "*E-301")))
(report/append-to-report r "\n\n*E-301: Condition disease code or name not provided.")))


(defn condition-moi
  [t c r]
  (if (not (nil? c)) (csv-colval (if (nil? (ld-> t c "modeOfInheritance" "display")) "" (ld-> t c "modeOfInheritance" "display")) "*E-302")
  (report/append-to-report r "\n\n*E-302: Mode of Inheritance display value not provided.")))

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
  [t i r]
  (let [c (ld-> t i "condition")]
     (if (nil? c) (log/debug (str "Exception in function get-condition: condition not found")))
     {:name (if (nil? c) "" (condition-name t c r)),
      :idtype (if (nil? c) "" (condition-idtype t c r)),
      :idvalue (if (nil? c) "" (condition-idvals t c r)),
      :moi (if (nil? c) "" (condition-moi t c r))}))

  (defn evidence-rule-strength
    "Returns the translated strength based on the clingen/acmg recommended 
     values; PS1, PS1_Moderate, BM2, BM2_Strong, etc..
     NOTE: if the rule's default strength is not the selected strength then 
     create the strength by joining the rule and selected strength with an underscore."
    [t e]
    (let [crit (ld-> t e "information" "criterion")
          act-strength-coding (ld-> t e "evidenceStrength" "coding")
          def-strength-coding (ld-> t e "information" "criterion" "defaultStrength" "coding")]
        (let [rule-label (get crit "id")
              def-strength (get def-strength-coding "display")
              act-strength (get act-strength-coding "display")]
          (let [def-direction (get (str/split def-strength #" ") 0)
                def-weight (get (str/split def-strength #" ") 1)
                act-direction (get (str/split act-strength #" ") 0)
                act-weight (get (str/split act-strength #" ") 1)]
            (if (= def-strength act-strength) rule-label (if (= def-direction act-direction) (str rule-label "_" act-weight) #"error"))))))

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
  
  (defn evidence-summary    "Returns a standard formatted summarization of the rules that were met."    [t e]    (str "The following criteria were met: " (csv-colval (clojure.string/join ", " (criteria-assessments t e)) "ERROR-evidence-summary")))
  
  (defn re-extract
    "Returns a map of matching regex group captures for any vector, list, map which can be flattened."
    [items re group]
    (map #(get (re-find re %) group) (remove nil? (flatten items))))
  
  (defn evidence-pmid-citations
    "Returns the list of critieron pmid citations for the evidence provided"
    [t e]
    (let [info-sources (ld-> t e "information" "evidence" "information" "source")
          pmids (re-extract info-sources #"https\:\/\/www\.ncbi\.nlm\.nih\.gov\/pubmed\/(\p{Digit}*)" 1)]
      (csv-colval (clojure.string/join ", " (map #(apply str "PMID:" %) pmids)) "*W-551")))
  
  (defn get-met-evidence
   "Returns a collated map of all 'met' evidence records needed for the 
    clinvar 'variant' submission sheet."
   [t i r]
   (let [e (ld-> t i "evidence"  (prop= t "met" "information" "outcome" "code"))]
     {:summary (evidence-summary t e),
      :rules (evidence-rules t e),
      :assessments (criteria-assessments t e),
      :pmid-citations (evidence-pmid-citations t e)}))
 
  