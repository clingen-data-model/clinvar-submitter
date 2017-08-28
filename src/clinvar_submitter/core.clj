(ns clinvar-submitter.core
  (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]
            [clinvar-submitter.form :as form] 
            [clojure-csv.core :as csv]
            [clojure.tools.logging.impl :as impl]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn construct-variant
  "Construct and return one row of variant table, with VariantInterpretation as root"
  [t i]
  (log/debug "Function: construct-variant - constructing one row of variant table, with VariantInterpretation as root")
  (let [variant (form/get-variant t i)
        condition (form/get-condition t i)
        interp (form/get-interpretation t i)
        evidence (form/get-met-evidence t i)]
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
     "" ; Trace or probe data	 - non sequence var only
		 "" ; empty
		 (get variant :variantIdentifier) ; Variation identifiers	(http://reg.genome.network.org/allele = ABC ABC:CA123123123)
		 "" ; Location	- N/A
		 (get variant :altDesignations)    ; Alternate designations 	
		 "" ; Official allele name	- N/A
		 "" ; URL	- bypassing for now, no set home for a public URL at this time
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
     "" ; assertion method
     "" ; assertion method citations
     (get condition :moi) ; Mode of Inheritance
     (get evidence :pmid-citations) ; significance citations
     "" ; significance citations without db ids
     (get evidence :summary) ; comment on clinical significance
     "" ; explanation if clinsig is other or drug
    ]))

(defn construct-variant-table
  "Construct and return variant table"
  [interp-path context-path]
  (log/debug "Function: construct-variant-table- context and input Filename (construct-variant-table): " interp-path context-path)
  (let [t (ld/generate-symbol-table interp-path context-path)
        m (vals t)
        interps ((prop= t "VariantInterpretation" "type") m)
        rows (map #(construct-variant t %) interps)]
    rows))

(defn -main
  "take input assertion, transformation context, and output filename as input
  and write variant table in csv format"
  [in cx out & args]
  (log/debug "Input and output filename in main method: " cx out)
  (spit out (csv/write-csv (construct-variant-table in cx))))
