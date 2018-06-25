
Clinvar-submitter Developer's Guide
===
This log demonstrates the fundamental building blocks used to convert a standardized ClinGen Interpretation json file into a ClinVar Submission spreadsheet.
The sample and context files are located in the */doc/resources/data/clinvar-submitter* folder.

JSON-LD context files 
---
*context.jsonld* is the ClinGen json-ld semantic context file which aligns with the published documentation. It contains the iri mappings to the semantic terms defined explicitly by the ClinGen for all variant pathogenicity interpretations.  You can download the context file at [http://datamodel.clinicalgenome.org/interpretation/json/context](http://datamodel.clinicalgenome.org/interpretation/json/context).

*sepio_context.jsonld* is the baseline context file that the clinvar-submitter code depends on to be able to reliably transform the ClinGen standard json file into a consistent form that can be used by the clinvar-submitter generation routines to parse out elements required to construct a clinvar submission worksheet. This is an ontologically purer form of the clingen-dmwg-v2.jsonld file which has some naming overlaps that do not comply with the design of the clinvar-submitter generation routines below. You can download the sepio_context file at [http://datamodel.clinicalgenome.org/interpretation/json/sepio_context](http://datamodel.clinicalgenome.org/interpretation/json/sepio_context).



```Clojure
; add dependencies and import
%classpath add mvn com.github.jsonld-java:jsonld-java:0.10.0
%classpath add mvn metosin:scjsv:0.4.0
%classpath add mvn cheshire:cheshire:5.7.1
%classpath add mvn org.clojure:data.json:0.2.1
%classpath add mvn clojure-csv:clojure-csv:2.0.1
%classpath add mvn org.clojure:tools.logging:0.2.4
%classpath add mvn org.clojure:tools.cli:0.3.1

(require '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.tools.logging :as log]
         '[clojure.string :as str]
         '[clojure.tools.cli :refer [parse-opts]]
         '[scjsv.core :as v]
         '[clojure-csv.core :as csv])
(import '[com.github.jsonldjava.core JsonLdProcessor JsonLdOptions]
        '[com.github.jsonldjava.utils JsonUtils]
        '[java.util Map List])
```



    Added jars: [jackson-core-2.8.6.jar, jackson-databind-2.8.6.jar, slf4j-api-1.7.23.jar, jackson-annotations-2.8.0.jar, jcl-over-slf4j-1.7.23.jar, httpclient-osgi-4.5.3.jar, httpmime-4.5.3.jar, commons-io-2.5.jar, httpclient-cache-4.5.3.jar, jsonld-java-0.10.0.jar, commons-codec-1.9.jar, httpcore-nio-4.4.6.jar, httpclient-4.5.3.jar, fluent-hc-4.5.3.jar, httpcore-osgi-4.4.6.jar, httpcore-4.4.6.jar]




    Added jars: [jsr305-3.0.0.jar, json-schema-core-1.2.5.jar, json-schema-validator-2.2.6.jar, jackson-annotations-2.2.3.jar, activation-1.1.jar, scjsv-0.4.0.jar, msg-simple-1.1.jar, jopt-simple-4.6.jar, uri-template-0.9.jar, jackson-databind-2.2.3.jar, guava-16.0.1.jar, tigris-0.1.1.jar, joda-time-2.3.jar, jackson-dataformat-smile-2.7.5.jar, btf-1.2.jar, libphonenumber-6.2.jar, jackson-core-2.7.5.jar, jackson-coreutils-1.8.jar, mailapi-1.4.3.jar, jackson-dataformat-cbor-2.7.5.jar, rhino-1.7R4.jar, clojure-1.5.1.jar, cheshire-5.6.3.jar]




    Added jars: [jackson-dataformat-smile-2.8.6.jar, jackson-dataformat-cbor-2.8.6.jar, cheshire-5.7.1.jar]




    Added jars: [clojure-1.3.0.jar, data.json-0.2.1.jar]




    Added jar: [clojure-csv-2.0.1.jar]




    Added jar: [tools.logging-0.2.4.jar]




    Added jars: [clojure-1.4.0.jar, tools.cli-0.3.1.jar]





    interface java.util.List



Sample Input JSON file (test-min.json)
---
The sample file used below is an abreviated representation to show the basic aspects that are required by the ClinVar submission process.
It can be modified and re-run through this notebook to play with different outcomes and exceptions.  

*test-min.json* is the sample JSON file used in the demonstration below. It is a subset of a full interpretation for brevity of output.



```Clojure
;display the simple.json contents.
(use 'clojure.pprint)
(defn display-json-file
   "Read and display the contents of a file"
   [filename]
   (with-open [r (io/reader filename)]
       (let [o (json/parse-stream r)]
       (pprint o))))
(display-json-file "../resources/data/clinvar-submitter/test-min.json")
```

    {"@context"
     "file:///home/beakerx/doc/resources/data/clinvar-submitter/context.jsonld",
     "VariantPathogenicityInterpretation"
     [{"condition"
       [{"disease"
         [{"id" "MONDO:0017623", "label" "PTEN hamartoma tumor syndrome"}],
         "inheritancePattern"
         {"id" "HP:0000006", "label" "Autosomal Dominant"},
         "type" "GeneticCondition"}],
       "statementOutcome" {"id" "LOINC:LA6668-3", "label" "Pathogenic"},
       "contribution"
       [{"agent"
         {"id"
          "https://vci.clinicalgenome.org/users/7620a154-1bed-4184-89c1-298ade1cd536/",
          "label" "Brandon Cushman"},
         "contributionDate" "2017-01-24T16:16:59.073653+00:00",
         "contributionRole" {"label" "interpreter"}}],
       "evidenceLine"
       [{"evidenceItem"
         [{"contribution"
           [{"agent"
             {"id"
              "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
              "label" "Jessica Mester"},
             "contributionDate" "2017-10-16T23:52:16.189562+00:00",
             "contributionRole"
             {"id" "SEPIO:0000154", "label" "assessor role"}}],
           "criterion" {"id" "SEPIO-CG:99038", "label" "BA1"},
           "evidenceLine"
           [{"evidenceItem"
             [{"allele"
               {"canonicalAlleleType" "nucleotide",
                "complexity" "simple",
                "id" "CAR:CA000197",
                "relatedContextualAllele"
                [{"relatedCanonicalAllele" "CAR:CA000197",
                  "state" "A",
                  "alleleName"
                  [{"name" "NC_000010.11:g.87864544G>A", "nameType" "hgvs"}
                   {"name" "CM000672.2:g.87864544G>A", "nameType" "hgvs"}],
                  "contextualAlleleType" "genomic",
                  "preferred" true,
                  "referenceCoordinate"
                  {"end" {"index" 87864544},
                   "refState" "G",
                   "referenceSequence"
                   {"label" "NC_000010.11",
                    "reference"
                    "http://reg.genome.network/refseq/RS000058"},
                   "start" {"index" 87864543}}}
                 {"relatedCanonicalAllele" "CAR:CA000197",
                  "state" "A",
                  "alleleName"
                  [{"name" "NC_000010.9:g.89614281G>A",
                    "nameType" "hgvs"}],
                  "contextualAlleleType" "genomic",
                  "referenceCoordinate"
                  {"end" {"index" 89614281},
                   "refState" "G",
                   "referenceSequence"
                   {"label" "NC_000010.9",
                    "reference"
                    "http://reg.genome.network/refseq/RS000010"},
                   "start" {"index" 89614280}}}],
                "relatedIdentifier"
                [{"id" "CLINVAR:184505",
                  "label" "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"}
                 {"id" "DBSNP:786201506"}]},
               "alleleCount" 0,
               "alleleNumber" 0,
               "ascertainment"
               {"id" "SEPIO:0000333", "label" "ESP ascertainment method"},
               "contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-10-16T23:52:15.940129+00:00",
                 "contributionRole"
                 {"id" "SEPIO:0000156", "label" "curator role"}}],
               "population" {"id" "ESP:AA", "label" "African-American"}}]}
            {"evidenceItem"
             [{"allele" "CAR:CA000197",
               "alleleCount" 0,
               "alleleNumber" 0,
               "ascertainment"
               {"id" "SEPIO:0000262",
                "label" "1000 Genomes ascertainment method"},
               "contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-10-16T23:52:15.940129+00:00",
                 "contributionRole"
                 {"id" "SEPIO:0000156", "label" "curator role"}}],
               "homozygousAlleleIndividualCount" 0,
               "population" {"id" "IGSR:eur", "label" "European"}}]}],
           "id" "/evaluations/869d084e-4c19-4ce4-9650-c40ba3c56a5d/",
           "statementOutcome" {"id" "SEPIO:0000224", "label" "Not Met"},
           "variant" "CAR:CA000197"}],
         "evidenceStrength" "SEPIO:0000325"}
        {"evidenceItem"
         [{"contribution"
           [{"agent"
             "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
             "contributionDate" "2017-06-27T23:15:52.199600+00:00",
             "contributionRole"
             {"id" "SEPIO:0000154", "label" "assessor role"},
             "type" "Contribution"}],
           "criterion"
           {"defaultStrength"
            {"id" "SEPIO:0000330", "label" "Pathogenic Strong"},
            "id" "SEPIO-CG:99025",
            "label" "PS3",
            "type" "VariantPathogenicityInterpretationCriterion"},
           "description" "",
           "evidenceLine"
           [{"evidenceItem"
             [{"contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-06-27T23:15:16.217088+00:00",
                 "contributionRole" "SEPIO:0000156",
                 "type" "Contribution"}],
               "description"
               "Variant caused 50% drop in phosphatase activity and couldn't rescue cell growth.",
               "source" ["https://www.ncbi.nlm.nih.gov/pubmed/21828076"],
               "type" "Statement"}],
             "type" "EvidenceLine"}
            {"evidenceItem"
             [{"contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-06-27T23:15:43.770666+00:00",
                 "contributionRole" "SEPIO:0000156",
                 "type" "Contribution"}],
               "description"
               "Variant caused >60% drop in lipid phosphatase activity compared to WT",
               "source" ["PMID:25527629"],
               "type" "Statement"}],
             "type" "EvidenceLine"}],
           "id" "/evaluations/885868b3-70cb-42f8-aa58-8010878d807a/",
           "statementOutcome" {"id" "SEPIO:0000223", "label" "Met"},
           "type" "CriterionAssessment",
           "variant" "CAR:CA377482283"}],
         "evidenceStrength" "SEPIO:0000330",
         "type" "EvidenceLine"}],
       "id"
       "https://vci.clinicalgenome.org/interpretations/b6a698e4-fac2-43b6-bec3-66a13706de17",
       "type" "VariantPathogenicityInterpretation",
       "variant" "CAR:CA000197"}
      {"condition"
       [{"disease"
         [{"id" "MONDO:0017623", "label" "PTEN hamartoma tumor syndrome"}],
         "inheritancePattern"
         {"id" "HP:0000006", "label" "Autosomal Dominant"},
         "type" "GeneticCondition"}],
       "contribution"
       [{"agent"
         {"id"
          "https://vci.clinicalgenome.org/users/7620a154-1bed-4184-89c1-298ade1cd536/",
          "label" "Brandon Cushman"},
         "contributionDate" "2017-01-24T16:16:59.073653+00:00",
         "contributionRole" {"label" "interpreter"}}],
       "evidenceLine"
       [{"evidenceItem"
         [{"contribution"
           [{"agent"
             {"id"
              "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
              "label" "Jessica Mester"},
             "contributionDate" "2017-10-16T23:52:16.189562+00:00",
             "contributionRole"
             {"id" "SEPIO:0000154", "label" "assessor role"}}],
           "criterion" {"id" "SEPIO-CG:99038", "label" "BA1"},
           "evidenceLine"
           [{"evidenceItem"
             [{"allele"
               {"canonicalAlleleType" "nucleotide",
                "complexity" "simple",
                "id" "CAR:CA000197",
                "relatedContextualAllele"
                [{"relatedCanonicalAllele" "CAR:CA000197",
                  "state" "A",
                  "alleleName"
                  [{"name" "NC_000010.11:g.87864544G>A", "nameType" "hgvs"}
                   {"name" "CM000672.2:g.87864544G>A", "nameType" "hgvs"}],
                  "contextualAlleleType" "genomic",
                  "preferred" true,
                  "referenceCoordinate"
                  {"end" {"index" 87864544},
                   "refState" "G",
                   "referenceSequence"
                   {"label" "NC_000010.11",
                    "reference"
                    "http://reg.genome.network/refseq/RS000058"},
                   "start" {"index" 87864543}}}
                 {"relatedCanonicalAllele" "CAR:CA000197",
                  "state" "A",
                  "alleleName"
                  [{"name" "NC_000010.9:g.89614281G>A",
                    "nameType" "hgvs"}],
                  "contextualAlleleType" "genomic",
                  "referenceCoordinate"
                  {"end" {"index" 89614281},
                   "refState" "G",
                   "referenceSequence"
                   {"label" "NC_000010.9",
                    "reference"
                    "http://reg.genome.network/refseq/RS000010"},
                   "start" {"index" 89614280}}}],
                "relatedIdentifier"
                [{"id" "CLINVAR:184505",
                  "label" "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"}
                 {"id" "DBSNP:786201506"}]},
               "alleleCount" 0,
               "alleleNumber" 0,
               "ascertainment"
               {"id" "SEPIO:0000333", "label" "ESP ascertainment method"},
               "contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-10-16T23:52:15.940129+00:00",
                 "contributionRole"
                 {"id" "SEPIO:0000156", "label" "curator role"}}],
               "population" {"id" "ESP:AA", "label" "African-American"}}]}
            {"evidenceItem"
             [{"allele" "CAR:CA000197",
               "alleleCount" 0,
               "alleleNumber" 0,
               "ascertainment"
               {"id" "SEPIO:0000262",
                "label" "1000 Genomes ascertainment method"},
               "contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-10-16T23:52:15.940129+00:00",
                 "contributionRole"
                 {"id" "SEPIO:0000156", "label" "curator role"}}],
               "homozygousAlleleIndividualCount" 0,
               "population" {"id" "IGSR:eur", "label" "European"}}]}],
           "id" "/evaluations/869d084e-4c19-4ce4-9650-c40ba3c56a5d/",
           "statementOutcome" {"id" "SEPIO:0000224", "label" "Not Met"},
           "variant" "CAR:CA000197"}],
         "evidenceStrength" "SEPIO:0000325"}
        {"evidenceItem"
         [{"contribution"
           [{"agent"
             "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
             "contributionDate" "2017-06-27T23:15:52.199600+00:00",
             "contributionRole"
             {"id" "SEPIO:0000154", "label" "assessor role"},
             "type" "Contribution"}],
           "criterion"
           {"defaultStrength"
            {"id" "SEPIO:0000330", "label" "Pathogenic Strong"},
            "id" "SEPIO-CG:99025",
            "label" "PS3",
            "type" "VariantPathogenicityInterpretationCriterion"},
           "description" "",
           "evidenceLine"
           [{"evidenceItem"
             [{"contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-06-27T23:15:16.217088+00:00",
                 "contributionRole" "SEPIO:0000156",
                 "type" "Contribution"}],
               "description"
               "Variant caused 50% drop in phosphatase activity and couldn't rescue cell growth.",
               "source" ["https://www.ncbi.nlm.nih.gov/pubmed/21828076"],
               "type" "Statement"}],
             "type" "EvidenceLine"}
            {"evidenceItem"
             [{"contribution"
               [{"agent"
                 "https://vci.clinicalgenome.org/users/0f23d9fa-ee3d-475b-8e25-9d941a193cac/",
                 "contributionDate" "2017-06-27T23:15:43.770666+00:00",
                 "contributionRole" "SEPIO:0000156",
                 "type" "Contribution"}],
               "description"
               "Variant caused >60% drop in lipid phosphatase activity compared to WT",
               "source" ["PMID:25527629"],
               "type" "Statement"}],
             "type" "EvidenceLine"}],
           "id" "/evaluations/885868b3-70cb-42f8-aa58-8010878d807a/",
           "statementOutcome" {"id" "SEPIO:0000223", "label" "Met"},
           "type" "CriterionAssessment",
           "variant" "CAR:CA377482283"}],
         "evidenceStrength" "SEPIO:0000330",
         "type" "EvidenceLine"}],
       "id"
       "https://vci.clinicalgenome.org/interpretations/2341-2341-p903",
       "type" "VariantPathogenicityInterpretation",
       "variant" "CAR:CA000197"}]}





    null



ld.clj - Understanding the LD (linked data) library
==
The following sections highlight the key functions of the linked data library built for processing the json files that comply with the ClinGen var interpretation compliant JSON-LD context (see [context](http://datamodel.clinicalgenome.org/interpretation/json/context)).

ld.flatten-interpretation ( interp-path, context-path )
---
This function takes the incoming json file in the 1st argument `interp-path` with it's inlined @context jsonld reference and uses it to first expand the json file to its fully specified representation.  This presumes that the json and it's @context is based on the ClinGen var interp model's context file.
The 2nd argument, `context-path` is a clinvar-submitter project specific context file based on the pure SEPIO iri labels, which provides a unique *iri-label* for each iri referenced throught the message structure.  This context file (see [sepio_context](http://datamodel.clinicalgenome.org/interpretation/json/sepio_context) is used to re-compact the expanded version to create a flattened data representation based on the `sepio_context` labels.  This flattened version is the form that must be input into the clinvar-submitter tool so that it can reliably locate the data elements that are needed to for mapping to the clinvar submission output.


```Clojure
;define the baseline flatten-interpretation function which processes the input file .
(defn flatten-interpretation
  "Use JSONLD-API to read a JSON-LD interpretation using context-path to translate symbols into local properties"
  [interp-path context-path]
  (with-open [ir (io/reader interp-path)
              cxr (io/reader context-path)]
    (let [i (json/parse-stream ir)
          cx (json/parse-stream cxr)
          opts (JsonLdOptions.)]
      (JsonLdProcessor/flatten i cx opts))))
(def interp-path "../resources/data/clinvar-submitter/test-min.json")
(def context-path "http://datamodel.clinicalgenome.org/interpretation/json/sepio_context")
(flatten-interpretation interp-path context-path)
```



ld.construct-symbol-table ( coll ) & ld.generate-symbol-table ( interp-path, context-path)
----
These 2 functions are used to transform the flattened JSON-LD intpretation into a map with the ids of the nodes in the json file as keys and the values as a reference to it's immediate child nodes.  

This `symbol-table` is the representatio of the json interpretation that is used to navigate through the data using the node labels. In the end, it provides the ability for the other ld functions below to provide an XPath expression like find/lookup capability for the interpretation record.


```Clojure
(defn construct-symbol-table
  "Construct a map that takes a flattened JSON-LD interpretation as input and associates
  the IDs in the flattened interpretation with the nodes in the flattened interpretation"
  [coll]
  (let [graph (into [] (get coll "@graph"))]
    (reduce (fn [acc v] (assoc acc (.get v "id") v)) {} graph)))

(defn generate-symbol-table
  "Flatten JSON-LD document and return a symbol table returning IDs of nodes mapped to the nodes themselves"
  [interp-path context-path]
  (try (construct-symbol-table (flatten-interpretation interp-path context-path))
  (catch Exception e (log/debug (str "Exception in generate-symbol-table: " (.getMessage e))))))

(def t (generate-symbol-table interp-path context-path))
(generate-symbol-table interp-path context-path)
```



ld.resolve-id ( t, s )
----
The resolve-id function is a core function that returns the child nodes in map form for a given IRI (or id) in the symbol table.
`t` is the symbol table generated from the ld.generate-symbol-table function. The map of the internally normalized json nodes.
`s` is the iri in either map form `{"id" "<iri>"}` or string form `"<iri>"` to resolve by returning a map of that iri's child nodes.


```Clojure
(defn resolve-id
  "Return the referent of an identifier if found in symbol table
  otherwise return a bare string"
  [t s]
  (if (instance? Map s) (if-let [v (get s "id")]
                          (resolve-id t v) s)
      (if-let [ret (get t s)] ret s)))
(resolve-id t "CAR:CA000197")
```



ld.ld-get ( t, loc, prop )
----
ld.ld-> ( t, loc, & preds )
----
ld.ld1-> ( t, loc, & preds ) 
----
ld.prop= ( t, v, & ks )   --operates on the values from `t` symbol table
----
These four functions provide useful approaches to querrying the data in the symbol table. The spirit of the design behind these functions is to provide similar lookup capability as there would be in an XPath-like expression.

The `prop=` function is unique from the other 3 functions in that it creates a predicate (or function) that can participate in the other 3 functions to traverse the data from the symbol table. In addition, the `prop=` function operates on the returned map from any of the nodes (values) contained in the symbol table.  For example if the symbol table was flipped to create a map from the keys, the `prop=` could operate on it using the labels to traverse this transformed mapping. This is essentiallly how the initial lookup is done to separate out the individual interpretation records from the interp-path file when there are more than one.




```Clojure
(defn ld-get
  "Get property from a map, if the property is a string matching a key
  in the symbol table, return the value of that key instead. If the property
  is a map with a single 'id' property, return either the value of the property
  or the value of the referent of that property, if it exists in the symbol table"
  [t loc prop]
  (cond 
    (fn? prop) (prop loc)
     (instance? List loc) (map #(ld-get t % prop) loc)
     :else (let [v (get loc prop)]
      (cond 
        (string? v) (resolve-id t v)
        (instance? Map v) (if-let [vn (get v "id")] (resolve-id t vn) v)
        (instance? List v) (map #(resolve-id t %) v)
        :else v))))

(defn ld->
  "Take a symbol table, a location, and a set of predicates, 
  return the set of nodes mapping to a given property"
  [t loc & preds]
  (let [res (reduce #(ld-get t %1 %2) loc preds)]
    ;(println preds res)
    res))

(defn ld1->
  "Take the first result of a ld-> path expression"
  [t loc & preds]
  (first (apply ld-> t loc preds)))

(defn prop=
  "Filter ressults where a specific key (or recursive chain of keys}
  is equal to value"
  [t v & ks]
  (fn [m] (if (instance? List m)
            (filter (fn [m1] (let [r (apply ld-> t m1 ks)]
                               (if (instance? List r) (some #(= % v) r) (= r v))))  m)
            (when (= v (apply ld-> t m ks)) m))))

;; getting the first Interpretation record from the symbol table
(def m (vals t))
(def interps ((prop= t "variant pathogenicity interpretation" "type") m))
(def i (first interps))
(first interps)
```



form.clj - The ClinVar submission form library
==
This libary (package) contains the functions needed to extract the data elements from the standard clingen interpretation json file used by the ClinVar Submission excel spreadsheet.  

In this first version of the tool, we are primarily interested in the basic elements needed to get a minimal submission constructed for each interpretation json record. The "variants" worksheet in the ClinVar Submission spreadsheet is the target output that will be created. See core.clj section below for full details on what is generated.

csv-colval (v)
---
This function is a simple wrapper that makes sure that regardless of the form the data element value being extracted from the json, it will be formatted such that it will be importable into the final ClinVar Submission excel spreadsheet without issue. Almost all functions that extract the final data elements to be used will call this function.



```Clojure
(defn csv-colval
    "Outputs a well-formed value for output of the CSV file being generated. 
    nil should be empty strings, non-strings should be stringified, etc..."
    [v]
    (cond 
        (nil? v) "" 
            (number? v) (str v) 
            (seq? v) (apply str v) :else v))
(str "[" (csv-colval nil) "\t" (csv-colval 500) "\t" (csv-colval "item1, item2, item 3.") "]")
```




    [	500	item1, item2, item 3.]



Parsing the Variant
---
Only genomic sequence variant representations are supported in version 1 of the clinvar-submitter generation tool. We intend to expand support for CNVs and structural variants in the future. 
The variant component of the interpretation contains an iri that points to a ClinGen canonical allele. 

*NOTE: the ClinGen VCI does support variants that do not have CAR ids - these are not currently handled by this version.*

Getting the *preferred* contextual allele representation.
The allele submodel has a Canonical Allele at its root and contains a list of related contextual alleles, one of which should have a boolean flag called *preferred* marked `TRUE`. This contextual allele will then contain the genomic start and end positions, reference state and alternate state (0-based interval coordinates to be converted to 1-based HGVS for the clinvar submissions). The genome build is defaulted to GRCH38 for the clingen




```Clojure
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
  (let [related-identifier-labels (ld-> t can "related identifier" "label")
        transcript-hgvs (some #(re-find #"NM_\d+\.\d+.*" %) related-identifier-labels)
        preferred-hgvs (ld1-> t ctx "allele name" (prop= t "hgvs" "name type") "name")]
    (if-not (nil? transcript-hgvs)
      (csv-colval transcript-hgvs)
      (if-not (nil? preferred-hgvs)
        (csv-colval preferred-hgvs)
        (str "*E-203" ":" n)))))

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
     :altDesignations - alternate designations (semi-colon separated)
     :refseq - preferred accession GRCh38 ie. NC_000014.9
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

(get-variant t i 2)
```



Getting a handle on Genetic Conditions
---
The following code focuses on getting the required elements needed for interpretations that have an association to a GeneticCondition.  The GeneticCodition has the potential to have some assortment of the following attributes:

* diseases (0 or more)
* phenotypes (0 or more)
* inheritancePattern (0 or 1)

Additionally, Clinvar submissions constrain the way that *conditions* are represented. Ideally they would like a condition ID value and type from an disease/phenotype authority that they can map to.  The list of authorized disease/phenotype (aka condition) system types are:

* OMIM, MeSH, MedGen, UMLS, Orphanet, and HPO

If more than one disease/phenotype is needed they "MUST" be from a single authorizing disease/phenotype authority in the list above. If all of these requirements are met then ClinVar wants the *id-type* in one cell and the one or more *id-type*s separated by semi-colons in another cell. When this is possible, not disease/condition name should be supplied and ClinVar will rely on these two fields to map the entry to the authorities representation of that condition.

If any of the *id* requirements cannot be met, then the conditon *name* is placed on the submission record and will be used by clinvar to find the best match within the MedGen system that ClinVar ultimately relies on.



```Clojure
(defn interp-significance
  "Return the interpretation clinical significance."
  [t i n]
  (let [significance (ld-> t i "asserted_conclusion")]
    (if (nil? significance) (str "*E-402" ":" n)
    (csv-colval (get significance "label")))))

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

(get-condition t i 3)
```



Getting the Interpretation components
---
The Interpretation is the root concept that defines the json message.  The subject is the variant (or allele) and the predicate is the genetic condition. The clinical signficance (path, like path, etc) evaluation date, interpreter, etc.. are covered in this section. The evidence line that follows the section below demonstrates how the ACMG criterion assessment and supporting PMIDs are collected and used to provide the clinvar submission values for a complete variant interpretation submission.

To get the *interpretation* from the message at its root, we use the strategy of applying the `prop=` function against the values of the generated symbol table to get the set of nodes that contain the elements `type=variant pathogenicity interpretation` as follows...


```Clojure
;; getting the first Interpretation record from the symbol table
(def m (vals t))
(def interps ((prop= t "variant pathogenicity interpretation" "type") m))
((prop= t "variant pathogenicity interpretation" "type") m)
(def i (first interps))
(first interps)

;; a method for extracting the evaluation date
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

(defn interp-id
   "Return the id portion of the @id url by taking the last part 
    of the url for VariantInterpretation"
   [i n]
   (let [full-id (get i "id")]
     (let [id (get (re-find #"[\/]{0,1}([a-z0-9\-]+)[\/]{0,1}$" full-id) 1)]
     (if (nil? id) (str "*E-401" ":" n)
     (csv-colval id)))))
     
(defn get-interpretation
   "Returns a map of all interpretation related fields needed for the 
    clinvar 'variant' submission sheet."
   [t i n]
   {:id (interp-id i n),
    :significance (interp-significance t i n),
    :eval-date (interp-eval-date t i n)})

(get-interpretation t i 4)
```



Creating the Interpretation Text 'Blurb' and collecting the PMIDs
---
Each interpretation submitted to clinvar should have a text explanation to describe why the variant-disease is assessed to have the provided clinical significance. If a `description` is provided on the *variant pathogenicity interpretation* it should be used in addition to collecting the *Met* ACMG criterion evidence that may also be provided.  By adding the ACMG met criterion it provides additional information to human users of the scope of evidence that was used to conclude the interpretation.  The PMIDs for the Met ACMG rules will also be collected and submitted to ClinVar as supporting pub med evidence ids.


```Clojure
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

(defn evidence-rules
    "Returns the list of criterion rule names for the evidence provided"
    [t e n]
    (let [crits (ld-> t e "has_evidence_item" "is_specified_by")]
      (map #(get % "id") crits))) 

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
    (if (empty? pmids) (str "**W-551" ":" n)
        (csv-colval (str/join ", " (map #(str "PMID:" %) pmids))))))

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

(defn evidence-summary
  "Returns a standard formatted summarization of the rules that were met."
  [t e n]
  (let  [criteria-str (if (instance? List e)
                        (csv-colval (str/join ", " (criteria-assessments t e n)))
                        (csv-colval (evidence-rule-strength t e n)))]
    (str "The following criteria were met: " criteria-str)))
    
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

(get-met-evidence t i 5)
```



report.clj  - The report library 
===
The Report library contains the methods for writing the output and run-report files. Most of the functionality is related to the exception

The Run Report
---
The Run Report is a separate file that is produced to verify how the process of transforming the interpretation JSON file into a ClinVar submission. It will indicate exceptions that occur at a system level as well as record and field level. Warnings will also be generated for situations that may be possible but are not necessarily recommended when submitting data to ClinVar. Not only do the exceptions get identified, but also all passing interpretations are indicated as well. This allows the user to get verification that all of the submissions expected are generated. And it provides an audit/troubleshooting tool if the output does not come out as expected.

The Run Report is given the default name of `clinvar-submitter-run-report.csv` if it is not specified on the command line to differ.

About Logging
---
The logging file is a lower level output produced by the application to help a developer troubleshoot issues that potentially occur.  The logging file will appear in a subdirectory of the running application called `log` and will be called `clinvar-submitter-tech-log.log`. The system uses log4j and the properties file that controls the configuration of it can be found in `src/log4j.properties`.


```Clojure
(defn report-header 
  [input options]
  [["ClinVar-Submitter Run Report"]
   ["" "Date/Time: " (str (new java.util.Date))]
   ["" "File(s): " input]
   ["" "Method Name (-m): " (get options :method)]
   ["" "Method Citation (-c): " (get options :methodc)]
   ["" "JSON-LD Context (-x): " (get options :jsonld-context)]
   ["" "Output File (-o): " (get options :output)]
   ["" "Force overwrite (-f): " (str (get options :force))]
   ["" "Run Report File (-r): " (get options :report)]
   []
   ["" "NOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry."]
   []
   ["Record #" "File Name" "Interp Id" "Variant (alt desig)" "Output Cell" "Status" "Code" "Description"]])

(def exception-code-map 
    {"*E-202" "Variant identifier not provided.",
     "*E-203" "No preferred variant information provided.",
     "*E-204" "Preferred variant reference sequence not provided",
     "*E-205" "Preferred variant start coordinate not provided.",
     "*E-206" "Preferred variant end coordinate not provided.",
     "*E-207" "Preferred variant reference allele not provided.",
     "*E-208" "Preferred variant alternate allele not provided.",
     "*W-251" "Preferred variant alternate designation not provided.",
     "*E-301" "Interpretations with Path or Lik Path outcomes require a condition disease code or name and none was provided.",
     "*E-302" "Only one disease/phenotype is supported, multiple found.",
     "*E-303" "Condition id type not supported by ClinVar. Using disease name option in submission instead.",
     "*E-304" "Condition identifier must have a colon delimiter to separate the type and id values. Using disease name option in submission instead.",
     "*E-305" "Mode of Inheritance display value not provided.",
     "*E-401" "Interpretation id not provided.", 
     "*E-402" "Interpretation significance not provided.",
     "*E-403" "Interpretation evaluation date not provided.",
     "*E-404" "Interpretation evaluation date format not valid (<eval-date-value>).",
     "*E-501" "<x> met criteria rules and/or strength codes are invalid or missing.",
     "*W-551" "No PMID citations found.",
     "*W-552" "No ACMG criterion evidence was met."})
  
(defn get-exception 
  [ecode]
    (let [code (first (str/split ecode #":"))
          desc (get exception-code-map code)
          type-str (subs (first (str/split code #"-")) 1)]
  {:type (cond (= "E" type-str) "Error" (= "W" type-str) "Warning ":else "Unknown")
   :code code
   :desc desc}))

(defn is-exception [ecode]
  (let [exception-code (first (str/split ecode  #":"))]
      (not (nil? (get exception-code-map exception-code)))))

(defn get-record-exceptions [items]
  (let [exception-list 
       (for [i (range (count items))]
       (if-not(nil? (get items i))
         (if(is-exception (get items i))      
          [(get items i) i])))]
       (let [newList (filter some? exception-list)]      
       newList)))
  
(defn write-run-report
  [input options records]  
  (let [reportfile (get options :report)]
    (do 
      ;; header
      (spit reportfile (csv/write-csv (report-header input options) :force-quote true) :append false)

      (dotimes [n (count records)] 

        ;; for each record
        (let [row (nth records n)
              row-exception-list (get-record-exceptions row)]

          ;if there is error in any row add error information in the report 
          (if-not (empty? row-exception-list)
            (dotimes [i (count row-exception-list)]
              (let [ecode (first (nth row-exception-list i))
                    column (+ 1 (last (nth row-exception-list i)))
                    record-number (last (str/split ecode  #":"))
                    exception (get-exception ecode)
                    outputdata-e [(str record-number)
                                  input
                                  (get (nth records n) 0)
                                  (get (nth records n) 25)
                                  (str "[" record-number ", " column "]")
                                  (get exception :type)
                                  (get exception :code)
                                  (get exception :desc)]]
                (spit reportfile (csv/write-csv [outputdata-e] :force-quote true) :append true)))
              (let [outputdata-s [(str (+ 1 n))
                                  input
                                  (get (nth records n) 0)
                                  (get (nth records n) 25)
                                  "--"
                                  "Success"
                                  "--"
                                  "--"]]       
                (spit reportfile (csv/write-csv [outputdata-s] :force-quote true) :append true)))
          ))
      )))

(defn write-files
    [input options records]
    (do 
      ;; write clinvar submitter output file
      (spit (get options :output) (csv/write-csv records :force-quote true)) 
      ;; write run report output file
      (write-run-report input options records)))
```




    #'beaker_clojure_shell_528ddda1-67b5-4069-b248-a60b9255f555/write-files



core.clj - Producing the ClinVar submission output: The main library
===
The `construct-variant` and `construct-variant-table` functions in the core.clj library are the functions that parse through each interpretation record in the incoming json message and produce the individual submission records into a nested map that is then written to a comma-separated-value (CSV) file.


```Clojure
(defn construct-variant
  "Construct and return one row of variant table, with variant pathogenicity interpretation as root"
  [t i n assertion-method method-citation]
  (log/debug "Function: construct-variant - constructing one row of variant table, with variant pathogenicity interpretation as root")
  (try 
  (let [variant (get-variant t i n)
        condition (get-condition t i n)
        interp (get-interpretation t i n)
        evidence (get-met-evidence t i n)]       
    [(get interp :id) ; local id
     "" ; linking id - only needed if providing case data or func evidence tabs 
     "" ; gene symbol - not provided since we are avoiding contextual allele representation. 
     (get variant :refseq) ;rdefseq
     "" ; hgvs - not providing since we are avoiding contextual allele representation
     "" ; chromosome - not providing since we are using the refseq field to denote the accession.
     (get variant :start) ; start + 1  (from 0-based to 1-based)
     (get variant :stop)  ; stop + 1  (from 0-based to 1-based)
     (get variant :ref)   ; ref
     (get variant :alt)   ; alt
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
     (get variant :variantIdentifier) ; Variation identifiers	(http://reg.genome.network.org/allele = ABC ABC:CA123123123)
     "" ; Location - N/A
     (get variant :altDesignations)    ; Alternate designations 	
     "" ; Official allele name	- N/A
     "" ; URL - bypassing for now, no set home for a public URL at this time
     "" ; empty 
     (get condition :idtype) ; Condition ID type (PURL)- assumes variantInterpretation.condition.disease.coding[0].code match of everything before the underscore.
     (get condition :idvalue)  ; Condition ID value	- assumes variantInterpretation.condition.disease.coding[0].code match of everything after the underscore.
     (get condition :name) ; Preferred condition name
     "" ; Condition category	
     "" ; Condition uncertainty	
     "" ; Condition comment	
     "" ; empty	
     (get interp :significance) ; Clinical significance	
     (get interp :eval-date) ; Date last evaluated
     (str assertion-method) ; assertion method
     (str method-citation) ; assertion method citations
     (get condition :moi) ; Mode of Inheritance
     (get evidence :pmid-citations) ; significance citations
     "" ; significance citations without db ids
     (get evidence :summary) ; comment on clinical significance
     "" ; explanation if clinsig is other or drug
    ])
   (catch Exception e (log/error (str "Exception construct-variant: " e)))))


(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path assertion-method method-citation]
  (log/debug "Function: construct-variant-table- context and input Filename (construct-variant-table): " interp-path context-path)
  (try
  (let [t (generate-symbol-table interp-path context-path)
        m (vals t)
        interps ((prop= t "variant pathogenicity interpretation" "type") m)
        rows (map #(construct-variant t %  (+ 1 (.indexOf interps %)) assertion-method method-citation) interps)]
    rows)
  (catch Exception e (println (str "Exception in construct-variant-table: " e))
  (log/error (str "Exception in construct-variant-table: " e)))))
  
(pprint (construct-variant-table interp-path context-path "ACMG guidelines" "PMID:25741868"))
```

    (["b6a698e4-fac2-43b6-bec3-66a13706de17"
      ""
      ""
      "NC_000010.11"
      ""
      ""
      "87864544"
      "87864544"
      "G"
      "A"
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
      "CAR:CA000197"
      ""
      "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"
      ""
      ""
      ""
      ""
      ""
      "PTEN hamartoma tumor syndrome"
      ""
      ""
      ""
      ""
      "Pathogenic"
      "2017-01-24"
      "ACMG guidelines"
      "PMID:25741868"
      "Autosomal Dominant"
      "PMID:21828076, PMID:25527629, PMID:21828076, PMID:25527629"
      ""
      "The following criteria were met: PS3"
      ""]
     ["2341-2341-p903"
      ""
      ""
      "NC_000010.11"
      ""
      ""
      "87864544"
      "87864544"
      "G"
      "A"
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
      "CAR:CA000197"
      ""
      "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"
      ""
      ""
      ""
      ""
      ""
      "PTEN hamartoma tumor syndrome"
      ""
      ""
      ""
      ""
      "*E-402:2"
      "2017-01-24"
      "ACMG guidelines"
      "PMID:25741868"
      "Autosomal Dominant"
      "PMID:21828076, PMID:25527629, PMID:21828076, PMID:25527629"
      ""
      "The following criteria were met: PS3"
      ""])





    null



The '-main' method
---
In the core library is the -main method which will accept the options and input file and orchestrate the processing.
Below are the additional methods along with the `-main` method and an example of how it is used to process our sample input file `test-min.json`.


```Clojure
(def cli-options
  [;; output file defaults to clinvar-variant.csv and will not overwrite 
   ;; unless -f force-overwrite option is specified
   ["-o" "--output FILENAME" "CSV output filename" :default "clinvar-variant.csv"]
   ["-f" "--force" :default false]
   ["-x" "--jsonld-context URI" "JSON-LD context file URI" 
    :default "http://datamodel.clinicalgenome.org/interpretation/json/sepio_context"]
   ["-b" "--build BUILD" "Genome build alignment, GRCh37 or GRCh38"
    :default "GRCh37"]
   ["-r" "--report FILENAME" "Run-report filename" :default "clinvar-submitter-run-report.csv"]
   ["-m" "--method METHODNAME" "Assertion-method-name" :default "ACMG Guidelines, 2015"]
   ["-c" "--methodc METHODCITATION" "Method Citation" :default "PMID:25741868"]
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
      
      ;; custom validation on arguments
      (= 1 (count arguments))
          {:input (first arguments) :options options}
      
      :else ; failed custom validation => exit with usage summary      
          {:exit-message (usage summary)})))

(defn exit [status msg]
 (println msg))

(def schema
  (slurp "http://datamodel.clinicalgenome.org/interpretation/json/schema.json"))
 
(def validate (v/validator schema))

(defn -main
  "take input assertion, transformation context, and output filename as input and write variant table in csv format"
  [& args]
  (let [{:keys [input options exit-message ok?]} (validate-args args)] 
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [records (construct-variant-table input (get options :jsonld-context) (get options :method) (get options :methodc))
            output-file (get options :output)
            report-file (get options :report)
            existing-files (remove nil? (map #(if (.exists (io/as-file %)) (str "'" % "'") nil ) [output-file report-file]))]
        ;(log/debug "Input,output and context filename in main method: " input (get options :jsonld-context) (get options :output))
        (if (nil? (validate (slurp input))) (log/debug "Json input is valid"))
        (try
            ;; if output or report file exists then check if there is a force option. 
            ;; If there is no force option the display an exception
            ;; Otherwise create output and report file     
            (if (and (> (count existing-files) 0) (not (get options :force)))    
              (str "**Error**"
                   "\nThe file"
                   (if (> (count existing-files) 1) "s " " ")
                   (str/join " & " existing-files)
                   " already exist in the output directory."
                   "\nUse option‚ -f Force overwrite to overwrite existing file(s).")
              (write-files input options records))
        (catch Exception e (log/error (str "Exception in main: " e))))
    ))))

(-main "-f" 
       "-o" "../resources/data/clinvar-submitter/myoutput.csv" 
       "-r" "../resources/data/clinvar-submitter/myreport.csv" 
       "../resources/data/clinvar-submitter/test-min.json")
```




    null



myoutput.csv (output file)
---
The output file is mapped to the columns in either the [Lite](ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/submission_templates/SubmissionTemplateLite.xlsx) or [Full](ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/submission_templates/SubmissionTemplate.xlsx) ClinVar Submission Template spreadsheets. 

To import the output file into one of these templates, 
1. open a copy of either template 
2. go to the `variant` sheet
3. select the first column in the first row just beneath the row the states *Please start your submission in the next row.* (row 6)
4. choose the Excel File:Import menu item and follow the dialogs to load your CSV file which has double-quoted values and comma-separated cells.

Below is a display of the content from our myoutput.csv file...


```Clojure
(defn display-csv-file
   "Read and display the contents of a file"
   [filename]
   (with-open [r (io/reader filename)]
       (let [o (csv/parse-csv r)]
       (pprint o))))
(display-csv-file "../resources/data/clinvar-submitter/myoutput.csv")
```

    (["b6a698e4-fac2-43b6-bec3-66a13706de17"
      ""
      ""
      "NC_000010.11"
      ""
      ""
      "87864544"
      "87864544"
      "G"
      "A"
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
      "CAR:CA000197"
      ""
      "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"
      ""
      ""
      ""
      ""
      ""
      "PTEN hamartoma tumor syndrome"
      ""
      ""
      ""
      ""
      "Pathogenic"
      "2017-01-24"
      "ACMG Guidelines, 2015"
      "PMID:25741868"
      "Autosomal Dominant"
      "PMID:21828076, PMID:25527629, PMID:21828076, PMID:25527629"
      ""
      "The following criteria were met: PS3"
      ""]
     ["2341-2341-p903"
      ""
      ""
      "NC_000010.11"
      ""
      ""
      "87864544"
      "87864544"
      "G"
      "A"
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
      "CAR:CA000197"
      ""
      "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"
      ""
      ""
      ""
      ""
      ""
      "PTEN hamartoma tumor syndrome"
      ""
      ""
      ""
      ""
      "*E-402:2"
      "2017-01-24"
      "ACMG Guidelines, 2015"
      "PMID:25741868"
      "Autosomal Dominant"
      "PMID:21828076, PMID:25527629, PMID:21828076, PMID:25527629"
      ""
      "The following criteria were met: PS3"
      ""])





    null



myreport.csv (run report results)
---
The run report from our example file shows the header section in the first several rows followed by a list of the interpretation record inputs processing results.
The header section contains the date/time and input file of the run followed by the options used to process the input file.
The interpretation record processing results has a header row for each column, which are:
1. Record # - A sequential number assigned during processing from 1 to n based on the order of interpretation records in the input file.
2. File Name - The input json file containing one or more interpretation records.
3. Interp Id - The variant pathogenicity interpretation id (iri).
4. Variant (alt desig) - The human readable variant alt designation - typically the HGVS representation of the variant being interpreted.
5. Output Cell - the row/col position of the cell in the output file in numeric form (i.e 1/4 => first interpretation record and column 'D'- 4th column)
6. Status - Error/Warning/Success to indicate the state of the processed record. NOTE: a single record may have multiple Error or Warning run results but should never have more than one run result if it is Successful.
7. Code - The error or warning code if it is an Error or Warning status, respectfully. No value for Success.
Description - The description of the error or warning. No value for Success.


```Clojure
(display-csv-file "../resources/data/clinvar-submitter/myreport.csv")
```

    (["ClinVar-Submitter Run Report"]
     ["" "Date/Time: " "Sun Jun 24 23:10:54 UTC 2018"]
     ["" "File(s): " "../resources/data/clinvar-submitter/test-min.json"]
     ["" "Method Name (-m): " "ACMG Guidelines, 2015"]
     ["" "Method Citation (-c): " "PMID:25741868"]
     [""
      "JSON-LD Context (-x): "
      "http://datamodel.clinicalgenome.org/interpretation/json/sepio_context"]
     [""
      "Output File (-o): "
      "../resources/data/clinvar-submitter/myoutput.csv"]
     ["" "Force overwrite (-f): " "true"]
     [""
      "Run Report File (-r): "
      "../resources/data/clinvar-submitter/myreport.csv"]
     [""]
     [""
      "NOTE: The variant coordinates are based on the preferred genomic representation from the ClinGen Allele Registry."]
     [""]
     ["Record #"
      "File Name"
      "Interp Id"
      "Variant (alt desig)"
      "Output Cell"
      "Status"
      "Code"
      "Description"]
     ["1"
      "../resources/data/clinvar-submitter/test-min.json"
      "b6a698e4-fac2-43b6-bec3-66a13706de17"
      "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"
      "--"
      "Success"
      "--"
      "--"]
     ["2"
      "../resources/data/clinvar-submitter/test-min.json"
      "2341-2341-p903"
      "NM_000314.6(PTEN):c.75G>A (p.Leu25=)"
      "[2, 37]"
      "Error"
      "*E-402"
      "Interpretation significance not provided."])





    null


