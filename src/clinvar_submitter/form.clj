(ns clinvar-submitter.form
 (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
           [clojure.string :as str]
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
        (seq? v) (apply str v) 
        :else v))
 
 (defn by-acmg-rule-id
    "Sort ACMG rules using an indexed list to define the order."
    [a b]
    (let [ar (str/upper-case (get a "id"))
          br (str/upper-case (get b "id"))
          rule-list (vector "BP7" "BP6" "BP5" "BP4" "BP3" "BP2" "BP1" "BS4" "BS3" "BS2" "BS1" "BA1" 
          "PP5" "PP4" "PP3" "PP2" "PP1" "PM6" "PM5" "PM4" "PM3" "PM2" "PM1" "PS4" "PS3" "PS2" "PS1" "PVS1")]
      (let [ai (.indexOf rule-list ar)
            bi (.indexOf rule-list br)]
        (let [c (compare bi ai)]
          (if (= c 0) (compare a b) c)))))
 
 ; *** Interpretation related transformations
 (defn interp-id
   "Return the id portion of the @id url by taking the last part 
    of the url for VariantInterpretation"
   [i n]
   (let [full-id (get i "id")]
     (let [id (get (re-find #"[\/]{0,1}([a-z0-9\-]+)[\/]{0,1}$" full-id) 1)]
     (if (nil? id) (str "*E-401" ":" n)
     (csv-colval id)))))
 
(defn interp-significance
  "Return the interpretation clinical significance."
  [t i n]
  (let [significance (ld-> t i "asserted_conclusion")]
    (if (nil? significance) (str "*E-402" ":" n)
    (csv-colval (get significance "label")))))
  
(defn interp-eval-date
  "Return the interpretation evaluation date."
  [t i n]
  (let [contribution (ld-> t i "qualified_contribution")]
    (if (nil? (get contribution "activity_date")) (str "*E-403" ":" n)
      (.format 
        (java.text.SimpleDateFormat. "yyyy-MM-dd") 
        (.parse
          (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
          (get contribution "activity_date"))))))
  
 (defn get-interpretation
   "Returns a map of all interpretation related fields needed for the 
    clinvar 'variant' submission sheet."
   [t i n]
   {:id (interp-id i n),
    :significance (interp-significance t i n),
    :eval-date (interp-eval-date t i n)})
 
 ; *** Variant related transformations
 (defn variant-identifier
  "Return variant identifier, typically the ClinGen AlleleReg id."
  [can n]
  (if (nil? (get can "id")) (str "*E-202" ":" n)
  (csv-colval (get can "id"))))
 
(defn variant-alt-designations
  "Return variant hgvs representations. 
  Prioritize the .relatedIdentifier.label that starts with a transcript (NM_*).
  If not found then defer to the preferred allele name with 'hgvs' as the 'name type'."
  [t can ctx n]
  (try
    (let [related-identifier-labels (ld-> t can "related identifier" "label")
          transcript-hgvs (some #(re-find #"NM_\d+\.\d+.*" %) 
            (if (string? related-identifier-labels) (sequence [related-identifier-labels]) related-identifier-labels))
          preferred-hgvs (ld1-> t ctx "allele name" (prop= t "hgvs" "name type") "name")]
    (if-not (str/blank? transcript-hgvs)
      (csv-colval transcript-hgvs)
      (if-not (str/blank? preferred-hgvs)
        (csv-colval preferred-hgvs)
        (str "*E-203" ":" n))))
    (catch Exception e (log/error (str "Exception in variant-alt-designations: " e)))))

(defn variant-hgvs
    "Return the contextual allele HGVS name if available, this is assumed to be in 
    genomic coordinates.  The variant-alt-designation funtion will use this as a 
    backup if it does not find a related identifier label that contains a value
    starting with an NM_ transcript accession."
    [t ctx n]
    (let [preferred-hgvs (ld1-> t ctx "allele name" (prop= t "hgvs" "name type") "name")]
        (csv-colval preferred-hgvs)))

(defn variant-refseq
  "Return the variant reference sequence."
  [t ctx n]
  (let [refseq (ld-> t ctx "reference coordinate" "reference")]
    (if (nil? (get refseq "label")) (str "*E-204" ":" n)
    (csv-colval (get refseq "label")))))
    
(defn variant-start
  "Return the variant reference sequence start pos (0-to-1-based transform)."
  [t ctx n]
  ; if ref is not null add 1 to start otherwise as-is
  (let [ref (ld-> t ctx "reference coordinate" "reference state")
        alt (get ctx "state")
        start (ld-> t ctx "reference coordinate" "start_position" "index")]
    (if (nil? start) (str "*E-205" ":" n)
    (csv-colval (if (str/blank? ref) (Integer. start) (+ 1 (Integer. start)))))))
     
 (defn variant-stop
  "Return the variant reference sequence stop pos (0-to-1-based transform)."
  [t ctx n]
  ; if ref is blank and alt is not add 1 to stop otherwise as-is
  (let [ref (ld-> t ctx "reference coordinate" "reference state")
        alt (get ctx "state")
        stop (ld-> t ctx "reference coordinate" "end_position" "index")]
    (if (nil? stop) (str "*E-206" ":" n)
    (csv-colval (if (and (str/blank? ref) (not (str/blank? alt))) (+ 1 (Integer. stop)) (Integer. stop))))))
 
 (defn variant-ref
  "Return the variant reference state sequence."
  [t ctx n]
  (let [ref-coord (ld-> t ctx "reference coordinate")]
    (if (nil? (get ref-coord "reference state")) (str "*E-207" ":" n)
    (csv-colval (get ref-coord "reference state")))))
 
 (defn variant-alt
   "Return the variant state sequence."
   [ctx n]
   (if (nil? (get ctx "state")) (str "*E-205" ":" n)
   (csv-colval (get ctx "state"))))
 
(defn get-variant
  "Returns a map of all variant related fields needed for the clinvar 
   'variant' submission form.
     :variantId - canonical allele with ClinGen AR PURL,
     :altDesignations - the related identifier for the canonical allele that 
                        contains the transcript hgvs representation.
     :refseq - preferred accession GRCh38 ie. NC_000014.9
     :hgvs - the preferred contexutal allele's hgvs representation
     :start - genomic start pos (transform to 1-based)
     :stop - genomic end pos (transform to 1-based)
     :ref - ref seq
     :alt - alternate seq"
  [t i n]
  (let [can-allele (ld-> t i "is_about_allele")
        pref-ctx-allele (ld1-> t can-allele "related contextual allele" (prop= t true "preferred"))]
    {:variantIdentifier (variant-identifier can-allele n),
     :altDesignations (variant-alt-designations t can-allele pref-ctx-allele n),
     :hgvs (variant-hgvs t pref-ctx-allele n),
     :refseq (variant-refseq t pref-ctx-allele n),
     :start (variant-start t pref-ctx-allele n),
     :stop  (variant-stop t pref-ctx-allele n),
     :ref (variant-ref t pref-ctx-allele n),
     :alt (variant-alt pref-ctx-allele n)}))
 
; *** Condition related transformations 
(def clinvar-valid-condition-types ["OMIM", "MeSH", "MedGen", "UMLS", "Orphanet", "HPO"])

(defn condition-part
  "Returns the single c.'has part' element. if c is nil or has no 'has part's
    then return an E-301. if more than one 'has part' exists return an E-302."
  [t c n]
  (let [cond-part (ld-> t c "has part")]
    (if (instance? java.util.LinkedHashMap cond-part)
      (csv-colval cond-part)
      (if (= (count cond-part) 0) nil (str "*E-302" ":" n)))))

(defn condition-part-id
  "Returns only a valid clinvar id in a map {val, type} from the passed in condition-part, otherwise
  it returns a string indicating the issue."
  [t c n]
  (let [cond-part (ld-> t c "has part")]
    (if (instance? java.util.LinkedHashMap cond-part)
      (let [cond-part-id (get cond-part "id")]
        (if (some? (re-find #"(.*):(.*)" cond-part-id))
          (let [cond-id-type (first (str/split cond-part-id #":"))]
            (if (.contains clinvar-valid-condition-types cond-id-type)
              {:type cond-id-type, :value (last (str/split cond-part-id #":"))}
              (str "*E-303" ":" n)))
          (str "*E-304" ":" n)))
        (csv-colval cond-part))))
   
(defn condition-moi
  [t c n]
  (csv-colval (ld-> t c "has disposition" "label")))

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
  [t i n]
  (let [c (ld-> t i "is_about_condition")
    cond-part (condition-part t c n)
    interp-sig (interp-significance t i n)
    moi (condition-moi t c n)]
    (if (or (nil? c) (nil? cond-part))
      (if (.contains ["Pathogenic", "Likely Pathogenic" ] interp-sig)
        {:name (str "*E-301" ":" n), :idtype "", :idvalue "", :moi moi}
        {:name "Not Specified", :idtype "", :idvalue "", :moi moi})
      (if (instance? java.lang.String cond-part)
          {:name cond-part,
           :idtype "",
           :idvalue "",
           :moi moi}
        (let [cond-part-id (condition-part-id t c n)]
          (if (instance? java.lang.String cond-part-id)
            {:name (get cond-part "label"),
             :idtype "",
             :idvalue "",
             :moi moi}
            {:name "",
             :idtype (get cond-part-id :type),
             :idvalue (get cond-part-id :value),
             :moi moi}))))))

(defn evidence-rule-strength
    "Returns the translated strength based on the clingen/acmg recommended 
     values; PS1, PS1_Moderate, BM2, BM2_Strong, etc..
     NOTE: if the rule's default strength is not the selected strength then 
     create the strength by joining the rule and selected strength with an underscore."
    [t e n]
    (try
    (let [crit (ld-> t e "has_evidence_item" "is_specified_by")
         act-strength-coding (ld-> t e "evidence_has_strength")
         def-strength-coding (ld-> t e "has_evidence_item" "is_specified_by" "default_criterion_strength")
         def-strength-display (ld-> t e "has_evidence_item" "is_specified_by" "default_criterion_strength" "label")]
        (let [def-strength-displaylist
          (cond 
          (instance? List def-strength-display)
           def-strength-display
          :else 
          (list def-strength-display))]
          (let [rule-label (get crit "label")
                def-strength (first def-strength-displaylist)
                act-strength (get act-strength-coding  "label")]
          (let [def-direction (get (str/split def-strength #" ") 0)
                def-weight (get (str/split def-strength #" ") 1)
                act-direction (get (str/split act-strength #" ") 0)
                act-weight (get (str/split act-strength #" ") 1)]
          (if (= def-strength act-strength) rule-label (if (= def-direction act-direction) (str rule-label "_" act-weight) #"error"))))))
    (catch Exception e (log/error (str "Exception in evidence-rule-strength: " e)))))

(defn criteria-assessments
    "Returns the criterion assessments map translated to the standard
     acmg terminology for all evidence lines passed in."
    [t e n]
    (map #(evidence-rule-strength t % n) e))
  
(defn evidence-rules
    "Returns the list of criterion rule names for the evidence provided"
    [t e n]
    (let [crits (ld-> t e "has_evidence_item" "is_specified_by")]
      (map #(get % "id") crits)))
  
(defn evidence-summary
  "Returns a standard formatted summarization of the rules that were met."
  [t e n]
  (let  [criteria-str (if (instance? List e)
                        (csv-colval (str/join ", " (criteria-assessments t e n)))
                        (csv-colval (evidence-rule-strength t e n)))]
    (str "The following criteria were met: " criteria-str)))

(defn re-extract
  "Returns a map of matching regex group captures for any vector, list, map which can be flattened."
  [items re group]
  (map #(get (re-find re %) group) (remove nil? (flatten items))))

(defn evidence-pmid-citations
  "Returns the list of critieron pmid citations for the evidence provided"
  [t e n]
  (let [info-sources (ld-> t e "has_evidence_item" "has_evidence_line" "has_evidence_item" "source")
        ; convert fully qualified pmid iri or compact pmid iri to compact form for clinvar 
        pmids (re-extract info-sources #"(https\:\/\/www\.ncbi\.nlm\.nih\.gov\/pubmed\/|PMID:)(\p{Digit}*)" 2)] 
    (if (empty? pmids) (str "*W-551" ":" n)
        (csv-colval (str/join ", " (map #(str "PMID:" %) pmids))))))

(defn get-met-evidence
  "Returns a collated map of all 'met' evidence records needed for the 
    clinvar 'variant' submission sheet."
  [t i n]                                        
  (let [e (ld-> t i "has_evidence_line" (prop= t "Met" "has_evidence_item" "asserted_conclusion" "label"))]
    {:summary (if (empty? e) (str "*W-552:" n) (evidence-summary t e n)),
     :rules (if (empty? e) (str "*W-552:" n) (evidence-rules t e n)),
     :assessments (if (empty? e) (str "*W-552:" n) (criteria-assessments t e n)),
     :pmid-citations (if (empty? e) (str "*W-552:" n) (evidence-pmid-citations t e n))
     }))