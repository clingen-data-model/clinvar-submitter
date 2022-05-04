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

(defn variant-scv
  "Returns the SCV for any ClinVar variant identifier by
   looking it up."
  [vci-id]
  (let [response (->> vci-id
                     (re-find #"https://vci.clinicalgenome.org/interpretations/([0-9a-z-]*)/")
                     fnext
                     (ols/find-scv))
        status (:status response)
        body (:body response)]
    (if (and (= 200 status)
             (str/starts-with? body "SCV"))
      body
      nil)))

(defn variant-coord
  "Returns the 1-based index position start, stop, ref and alt for the contextual allele."
  [sym-tbl ctx interp-num]
  (let [ref (ld1-> sym-tbl ctx "reference coordinate" "reference state")
        alt (get ctx "state")
        start (ld1-> sym-tbl ctx "reference coordinate" "start_position" "index")
        stop (ld1-> sym-tbl ctx "reference coordinate" "end_position" "index")]
    (if (every? some? [start stop])
      {:start (csv-colval (if (str/blank? ref) (Integer. start) (+ 1 (Integer. start))))
       :stop (csv-colval (if (and (str/blank? ref) (not (str/blank? alt))) (+ 1 (Integer. stop)) (Integer. stop)))
       :ref ref
       :alt alt})))

(defn get-variant
  "Returns a map of all variant related fields needed for the clinvar
   'variant' submission form.
     :id - clinvar or clingen ar id depending on the 'source'
     :label - preferred name
     :chromosome - chromosome label only if no refseq is available
     :gene - gene symbol if available
     :refseq - accession associated with the preferred related ctx
     :hgvs -  c., g., n. portion of full hgvs expression for pref allele
     :coord - the ref, alt, start and stop of the hgvs expression, if no hgvs is available
     :alt-designations - the label"
  [sym-tbl interp-input interp-num]
  (let [can-allele (ld1-> sym-tbl interp-input "is_about_allele")
        b38-ctx-allele (ld1-> sym-tbl can-allele "related contextual allele" (prop= sym-tbl "GRCh38" "reference genome build" "label"))
        b37-ctx-allele (ld1-> sym-tbl can-allele "related contextual allele" (prop= sym-tbl "GRCh37" "reference genome build" "label"))
        pref-ctx-allele (ld1-> sym-tbl can-allele "related contextual allele" (prop= sym-tbl true "preferred"))
        b38-hgvs (ld1-> sym-tbl b38-ctx-allele "allele name" (prop= sym-tbl "hgvs" "name type") "name")
        b37-hgvs (ld1-> sym-tbl b37-ctx-allele "allele name" (prop= sym-tbl "hgvs" "name type") "name")
        pref-hgvs (ld1-> sym-tbl pref-ctx-allele "allele name" (prop= sym-tbl "hgvs" "name type") "name")
        [refseq hgvs] (if-not (nil? b38-hgvs)
                        (str/split b38-hgvs #":")
                        (if-not (nil? b37-hgvs)
                          (str/split b37-hgvs #":")
                          (if-not (nil? pref-hgvs)
                            (str/split pref-hgvs #":")
                            (vec (repeat 2 (str "*E-203" ":" interp-num))))))
        chr (if-not (nil? hgvs) 
              (or
                (ld1-> sym-tbl b38-ctx-allele "related chromosome" "label")
                (ld1-> sym-tbl b37-ctx-allele "related chromosome" "label")))
        coord (if-not (nil? hgvs)
                (or
                  (variant-coord sym-tbl b38-ctx-allele interp-num)
                  (variant-coord sym-tbl b37-ctx-allele interp-num)))]
    {:id (variant-identifier can-allele interp-num)
     :label (get can-allele "label")
     :scv (csv-colval (variant-scv (get interp-input "id")))
     :chromosome  (csv-colval chr)
     :gene (csv-colval (ld1-> sym-tbl pref-ctx-allele "related gene" "label"))
     :hgvs (csv-colval hgvs)
     :refseq (csv-colval refseq)
     :coord coord
     :alt-designations (str/replace (get can-allele "label") #"(.*) \((p\..*)\)$" "$1|$2")}))

(defn condition-moi
  [sym-tbl c interp-num]
  (csv-colval (ld-> sym-tbl c "has disposition" "label")))

(defn construct-phenotype-list
  " assume an HP:xxxx set of ids
    { :id-type <HPO>
      :id-value <semi-colon separated HP:xxx ids>}  "
  [phenotype]
  (when (seq phenotype)
    (let [ids (map #(get % "id") phenotype)]
      {:id-type "HPO"
       :id-value ids})))

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
         If the disease is a MONDO disease use the MONDO ID,
         If the disease is a freeform disease then...
            use disease label for ClinVar condition.
         If phenotypes exists create semi-colon separated list of id values for condition ids.
   The resulting structure should be:
      {:id-type <MONDO or HPO>,
       :id-value <semi-colon separated ids for id-type e.g. MONDO:000000, HP:000000>,
       :preferred-name <only if id-type/id-val are blank, disease.label>,
       :moi <mode of inh>} "
  [sym-tbl interp-input interp-num interp-sig]
  (let [cond (ld-> sym-tbl interp-input "is_about_condition")
        disease (ld1-> sym-tbl cond "is_about_disease")
        phenotype (ld1-> sym-tbl cond "is_about_phenotype")
        moi (condition-moi sym-tbl cond interp-num)]
    (if disease
      ;; test for free form or mondo version of disease
      (let [disease-id (get disease "id")]
        (if (str/starts-with? disease-id "MONDO:")
          {:id-type "MONDO"
           :id-value disease-id
           :preferred-name (get disease "label")
           :moi moi}
          (if phenotype
            (let [result (construct-phenotype-list phenotype)]
              (merge result {:preferred-name (get disease "label")
                             :moi moi}))
            {:id-type "???"
             :id-value disease-id
             :preferred-name (get disease "label")
             :moi moi})))
      (if (.contains ["Pathogenic" "Likely Pathogenic" ] interp-sig)
        {:id-type ""
         :id-value ""
         :preferred-name (str "*E-301" ":" interp-num)
         :moi moi}
        {:id-type ""
         :id-value ""
         :preferred-name "Not Specified"
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
