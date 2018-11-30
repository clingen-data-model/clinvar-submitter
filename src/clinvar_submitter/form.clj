(ns clinvar-submitter.form
 (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
           [clinvar-submitter.ols :as ols]
           [clojure.string :as str]
           [clojure.pprint :refer [pprint]]
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
                            "PP5" "PP4" "PP3" "PP2" "PP1" "PM6" "PM5" "PM4" "PM3" "PM2" "PM1" "PS4" "PS3" "PS2" "PS1" "PVS1")
          ai (.indexOf rule-list ar)
          bi (.indexOf rule-list br)
          c (compare bi ai)]
       (if (= c 0) (compare a b) c)))

(defn get-contribution [sym-tbl interp-input role]
  (let [contribution (ld1-> sym-tbl interp-input "qualified_contribution"
                            (prop= sym-tbl role "realizes"
                                   (ld1-> sym-tbl "label")))
        agent (ld1-> sym-tbl contribution "has_agent")]
    (if (some? contribution)
      {:by (get agent "label")
       :on (get contribution "activity_date")
       :role role}
      {})))

 ; *** Interpretation related transformations
(defn interp-id
   "Return the id portion of the @id url by taking the last part
    of the url for VariantInterpretation"
   [interp-input interp-num]
   (let [full-id (get interp-input "id")
         id (get (re-find #"[\/]{0,1}([a-z0-9\-]+)[\/]{0,1}$" full-id) 1)]
     (if (nil? id)
       (str "*E-401" ":" interp-num)
       (csv-colval id))))

(defn interp-significance
  "Return the interpretation clinical significance."
  [sym-tbl interp-input interp-num]
  (let [significance (ld1-> sym-tbl interp-input "asserted_conclusion")]
    (if (nil? significance)
      (str "*E-402" ":" interp-num)
      (csv-colval (get significance "label")))))

(defn parse-date
  "Attempts to handle either UTC or Locale.US format conversions."
  [s]
  ;; if s has a 'Z' at the end then assume it must be converted from US to UTC
  (when (some? s)
    (cond
      (some? (re-find #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z" s)) (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") s)
      (some? (re-find #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3,6}\+\d{2}:\d{2}" s)) (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX") s)
      :else (throw (Exception. (str "Unexpected date format for parsing '" s "'"))))))


(defn interp-eval-date
  "Return the interpretation evaluation date."
  [sym-tbl interp-input interp-num]
  (let [approval (get-contribution sym-tbl interp-input "approver")
        approval-date (:on approval)]
    (if (nil? approval-date)
      (str "*E-403" ":" interp-num)
      (.format
        (java.text.SimpleDateFormat. "yyyy-MM-dd")
        (parse-date approval-date)))))

(defn get-interpretation
  "Returns a map of all interpretation related fields needed for the
    clinvar 'variant' submission sheet."
  [sym-tbl interp-input interp-num]
  (let [desc (ld1-> sym-tbl interp-input "description")
        desc-no-nl (if (nil? desc) "" (str/replace desc #"\n" "  "))]
    {:id (interp-id interp-input interp-num)
     :description desc-no-nl
     :significance (interp-significance sym-tbl interp-input interp-num)
     :eval-date (interp-eval-date sym-tbl interp-input interp-num)}))

 ; *** Variant related transformations
(defn variant-identifier
  "Return variant identifier, typically the ClinGen AlleleReg id."
  [can interp-num]
  (if (nil? (get can "id"))
    (str "*E-202" ":" interp-num)
    (csv-colval (get can "id"))))

(defn -extract-gene-symbol [s]
    (let [m (re-find #"NM_\d+\.\d+\(([A-Z0-9]+)\)\:.*" s)]
     (when m
       (second m))))

(defn variant-scv
  "Returns the SCV for any ClinVar variant identifier by
   looking it up in the scv-map provided."
  [can scv-map]
  (let [id (get can "id")
        id-parts (if id (str/split id #":"))
        id-type (if id-parts (first id-parts))
        id-value (if id-parts (second id-parts))]
    (if (and (= "ClinVar" id-type) (not (empty? scv-map)))
      (if-let [result (filter #(= (:VariationID %) (Integer. id-value)) scv-map)]
        (if (= (count result) 1)
          (:SCV (first result)))))))

(defn variant-label
  "Return variant label as provided by source (i.e. VCI clinvarvarianttitle).
   Use the canonical allele label if available, otherwise the first hgvs name
   for the preferred contextual allele."
  [sym-tbl can preferred-ctx interp-num]
  (let [can-label (ld1-> sym-tbl can "label")
        preferred-hgvs (ld1-> sym-tbl preferred-ctx "allele name" (prop= sym-tbl "hgvs" "name type") "name")]
    (if-not (str/blank? can-label)
      (csv-colval can-label)
      (if-not (str/blank? preferred-hgvs)
        (csv-colval preferred-hgvs)
        (str "*E-203" ":" interp-num)))))

(defn variant-hgvs
    "Return the contextual allele HGVS name if available, this is assumed to be in
    genomic coordinates.  The variant-alt-designation funtion will use this as a
    backup if it does not find a related identifier label that contains a value
    starting with an NM_ transcript accession."
    [sym-tbl ctx interp-num]
    (let [preferred-hgvs (ld1-> sym-tbl ctx "allele name" (prop= sym-tbl "hgvs" "name type") "name")]
        (csv-colval preferred-hgvs)))

(defn variant-refseq
  "Return the variant reference sequence."
  [sym-tbl ctx interp-num]
  (let [refseq (ld1-> sym-tbl ctx "reference coordinate" "reference")]
    (if-let [label (get refseq "label")]
      (csv-colval label)
      (str "*E-204" ":" interp-num))))

(defn variant-start
  "Return the variant reference sequence start pos (0-to-1-based transform)."
  [sym-tbl ctx interp-num]
  ; if ref is not null add 1 to start otherwise as-is
  (let [ref (ld1-> sym-tbl ctx "reference coordinate" "reference state")
        alt (get ctx "state")
        start (ld1-> sym-tbl ctx "reference coordinate" "start_position" "index")]
    (if (nil? start)
      (str "*E-205" ":" interp-num)
      (csv-colval (if (str/blank? ref) (Integer. start) (+ 1 (Integer. start)))))))

(defn variant-stop
  "Return the variant reference sequence stop pos (0-to-1-based transform)."
  [sym-tbl ctx interp-num]
  ; if ref is blank and alt is not add 1 to stop otherwise as-is
  (let [ref (ld1-> sym-tbl ctx "reference coordinate" "reference state")
        alt (get ctx "state")
        stop (ld1-> sym-tbl ctx "reference coordinate" "end_position" "index")]
    (if (nil? stop)
      (str "*E-206" ":" interp-num)
      (csv-colval (if (and (str/blank? ref) (not (str/blank? alt))) (+ 1 (Integer. stop)) (Integer. stop))))))

(defn variant-ref
  "Return the variant reference state sequence."
  [sym-tbl ctx interp-num]
  (let [ref-coord (ld1-> sym-tbl ctx "reference coordinate")]
    (if-let [ref (get ref-coord "reference state")]
      (csv-colval ref)
      (str "*E-207" ":" interp-num))))

(defn variant-alt
   "Return the variant state sequence."
   [ctx interp-num]
   (if-let [alt (get ctx "state")]
    (csv-colval alt)
    (str "*E-205" ":" interp-num)))

(defn get-variant
  "Returns a map of all variant related fields needed for the clinvar
   'variant' submission form.
     :id - clinvar or clingen ar id depending on the 'source'
     :label - preferred title
     :scv - scv from scv-map if only 1 is found (only for ClinVar:xxx ids)
     :chromosome - chromosome label if available
     :gene - gene symbol if available
     :refseq - accession associated with the preferred related ctx
     :hgvs -  c., g., n. portion of full hgvs expression for pref allele
     :start - start pos (transform to 1-based) of preferred allele
     :stop - end pos (transform to 1-based) of preferred allele
     :ref - ref seq of preferred allele
     :alt - alternate seq of preferred allele"
  [sym-tbl interp-input interp-num scv-map]
  (let [can-allele (ld1-> sym-tbl interp-input "is_about_allele")
        pref-ctx-allele (ld1-> sym-tbl can-allele "related contextual allele" (prop= sym-tbl true "preferred"))
        preferred-hgvs (ld1-> sym-tbl pref-ctx-allele "allele name" (prop= sym-tbl "hgvs" "name type") "name")
        [refseq hgvs] (str/split preferred-hgvs #":")]
    {:id (variant-identifier can-allele interp-num)
     :label (variant-label sym-tbl can-allele pref-ctx-allele interp-num)
     :scv (csv-colval (variant-scv can-allele scv-map))
     :chromosome  (csv-colval (ld1-> sym-tbl pref-ctx-allele "related chromsome" "label"))
     :gene (csv-colval (ld1-> sym-tbl pref-ctx-allele "related gene" "label"))
     :hgvs (csv-colval hgvs)
     :refseq (csv-colval refseq)
     :start (variant-start sym-tbl pref-ctx-allele interp-num)
     :stop  (variant-stop sym-tbl pref-ctx-allele interp-num)
     :ref (variant-ref sym-tbl pref-ctx-allele interp-num)
     :alt (variant-alt pref-ctx-allele interp-num)}))

(defn condition-moi
  [sym-tbl c interp-num]
  (csv-colval (ld-> sym-tbl c "has disposition" "label")))

(defn construct-phenotype-list
  " assume an HP:xxxx set of ids"
  [phenotype]
  (when (seq phenotype)
    (let [ids (map #(second (re-find #"HP:(.+)" %)) (map #(get % "id") phenotype))]
      {:id-type "HP"
       :id-value (str/join ";" ids)})))

(defn get-condition
  "Processes the condition element to derive the ClinVar sanctioned fields:
   The inputs from the VCI sourced SEPIO message are:
      condition - the genetic condition for the interp, can be nil or {}
        disease[] - always array of one (either MONDO or free text), can be empty or nil
        phenotype[] - the array of HP pheno iris for free text disease, can be empty or nil
        inheritancePattern - a qualifying moi, can be nil
   ClinVar rules
       NOTE: A condition is considered provided if either disease[0] or phenotype[*]
             are not empty or nil.
       A condition is only required if the interp significance is Path or Lik Path.
       If a condition is not provided and required then return *E-301 exception.
       If a condition is not provided and not required then 'Not Specified' is the default label.
       If a condition is provided then...
         If the disease is a MONDO disease use prioritized id transform if available,
         If the MONDO id cannot be transformed or if a freeform disease then...
            use disease label for ClinVar condition.
         If phenotypes exists create semi-colon separated list of id values for condition ids.
   The resulting structure should be:
      {:id-type <MONDO or HP>,
       :id-value <semi-colon separated ids for id-type>,
       :preferred-name <only if id-type/id-val are blank, disease.label>,
       :moi <mode of inh>} "
  [sym-tbl interp-input interp-num interp-sig]
  (let [cond (ld-> sym-tbl interp-input "is_about_condition")
        disease (ld1-> sym-tbl cond "is_about_disease")
        phenotype (ld-> sym-tbl cond "is_about_phenotype")
        moi (condition-moi sym-tbl cond interp-num)]
    (if disease
      ;; test for free from or mondo version of disease
      (let [disease-id (get disease "id")]
        (if (str/starts-with? disease-id "MONDO:")
          (let [result (ols/find-prioritized-term (str/replace disease-id ":" "_"))]
            (merge result {:preferred-name (get disease "label")
                           :moi moi}))
          (let [result (construct-phenotype-list phenotype)]
            (merge result {:preferred-name (get disease "label")
                           :moi moi}))))
      (if (.contains ["Pathogenic" "Likely Pathogenic" ] interp-sig)
        {:preferred-name (str "*E-301" ":" interp-num)
          :moi moi}
        {:preferred-name "Not Specified"
          :moi moi}))))

(defn evidence-rule-strength
  "Returns the translated strength based on the clingen/acmg recommended
     values; PS1, PS1_Moderate, BM2, BM2_Strong, etc..
     NOTE: if the rule's default strength is not the selected strength then
     create the strength by joining the rule and selected strength with an underscore."
  [sym-tbl e]
  (try
    (let [crit (ld1-> sym-tbl e "has_evidence_item" "is_specified_by")
          act-strength-coding (ld1-> sym-tbl e "evidence_has_strength")
          def-strength-coding (ld1-> sym-tbl e "has_evidence_item" "is_specified_by" "default_criterion_strength")
          def-strength-display (ld1-> sym-tbl e "has_evidence_item" "is_specified_by" "default_criterion_strength" "label")]
      (let [def-strength-displaylist
            (cond
              (instance? List def-strength-display) def-strength-display
              :else (list def-strength-display))]
        (let [rule-label (get crit "label")
              def-strength (first def-strength-displaylist)
              act-strength (get act-strength-coding  "label")]
          (let [def-direction (get (str/split def-strength #" ") 0)
                def-weight (str/capitalize (str/replace (get (str/split def-strength #" " 2) 1) #" " "-"))
                act-direction (get (str/split act-strength #" ") 0)
                act-weight (str/capitalize (str/replace (get (str/split act-strength #" " 2) 1) #" " "-"))]
            (if (= def-strength act-strength) rule-label (if (= def-direction act-direction) (str rule-label "_" act-weight) #"error"))))))
    (catch Exception e (log/error (str "Exception in evidence-rule-strength: " e)))))

(defn -build-pmid-list [pmid-list]
  (let [prefix-pmid-list (map #(str "PMID:" %) pmid-list)]
    (str/join "; " prefix-pmid-list)))

(defn get-pmid-list [m]
  (->> (vals m) (map :pmids) flatten (into #{}) -build-pmid-list))

(defn summary-string-evidence-code [tuple]
  (let [m (val tuple)
        pmid-list (when (seq (:pmids m))
                    (str " (" (-build-pmid-list (:pmids m)) ")"))]
    (str (key tuple) ": " (:description m) pmid-list)))

(defn summary-string
  "Generate the summary string for the record based on the map returned from
  get-met-evidence-map (m), interpretation (i), variant (v) and condition (c)"
  [evidence interp-input variant condition method approver]
  (let [sig (:significance interp-input)
        cond-name (:preferred-name condition)
        moi (:moi condition)
        method-name (:label method)
        expert-panel-name (:by approver)
        rule-list (not-empty (keys evidence))
        evid-phrase (str/join "; " (map summary-string-evidence-code evidence))]

      (str method-name " applied: " (str/replace evid-phrase #"[\r\n]" " ")
          ". In summary this variant meets criteria to be classified as "
          (str/lower-case sig) " for " (str/lower-case cond-name)
          (if (empty? moi) "" (str " in an " (str/lower-case moi) " manner"))
          " based on the ACMG/AMP criteria applied as specified by the "
          expert-panel-name
          (when (some? rule-list) (str ": (" (str/join ", " rule-list) ")"))
          ".")))

(defn -extract-pmid [s]
  (let [m (re-find #"https://www.ncbi.nlm.nih.gov/pubmed/(\d+)" s)]
    (when m
      (second m))))

(defn -evidence-pmids [sym-tbl e]
  (let [base-list (ld-> sym-tbl e "has_evidence_item" "has_evidence_line"
                        "has_evidence_item" "source")
        pmid-list (cond (string? base-list) [(-extract-pmid base-list)]
                        (seq base-list) (map -extract-pmid (remove nil? base-list))
                        :default nil)]
    (remove nil? pmid-list)))

(defn -get-evidence-tuple [sym-tbl e]
  [(evidence-rule-strength sym-tbl e)
   {:id (get (ld1-> sym-tbl e "has_evidence_item" "is_specified_by") "id")
    :description (str/replace (ld1-> sym-tbl e "has_evidence_item" "description") #"\n" "  ")
    :pmids (-evidence-pmids sym-tbl e)}])

(defn get-met-evidence
  [sym-tbl interp-input]
  (let [e (ld-> sym-tbl interp-input "has_evidence_line"(prop= sym-tbl "Met" "has_evidence_item" "asserted_conclusion" "label"))]
    (into {} (map #(-get-evidence-tuple sym-tbl %) e))))

(defn get-assertion-method [sym-tbl interp-input interp-num]
  (let [guideline-url (ld1-> sym-tbl interp-input "is_specified_by" "has_url")
        label (ld1-> sym-tbl interp-input "is_specified_by" "label")]

    (if guideline-url
      {:label (csv-colval label)
       :file-name (csv-colval (last (str/split guideline-url #"/")))}
      {:label (csv-colval label)})))
