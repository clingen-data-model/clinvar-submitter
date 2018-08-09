(ns clinvar-submitter.form-test
  (:require [clojure.test :refer :all]
            [clinvar-submitter.core :refer :all]
            [clinvar-submitter.ld :as ld]
            [clinvar-submitter.form :as form]))

;; (def interp-path "data/dmwg2.json")
;; (def context-path "data/cg-interpretation.jsonld")
;; (def t (ld/generate-symbol-table interp-path context-path))
;; (def m (vals t))
;; (def interps ((ld/prop= t "VariantInterpretation" "type") m))
;; (def i (first interps))
;; (def variant (form/get-variant t i))
;; (def expectedvalue (construct-variant t i))
;; (def id "4fc46e87-fad0-4fe7-8776-95cf901243aa")
;; (def significance "Pathogenic")
;; (def eval-date "2017-01-24")
;; (def c (ld/ld-> t i "condition"))
;; (def name "") 
;; (def idtype "DOID") 
;; (def idvalue "11984") 
;; (def moi "Autosomal Dominant")
;; (def e (ld/ld-> t i "evidence"  (ld/prop= t "met" "information" "outcome" "code"))) 
;; (def summary "The following criteria were met: PM2, PP3, PP2, PP1, PS4") 
;; (def rules ["PM2" "PP3" "PP2" "PP1" "PS4"]) 
;; (def pmid-citations "PMID:15358028, PMID:15858117, PMID:21511876, PMID:23396983, PMID:24510615")
;; (def v (ld/ld1-> t i "variant" "relatedContextualAllele" (ld/prop= t true "preferred"))) 
;; (def variantIdentifier "http://reg.genome.network/allele/CA012832") 
;; (def altDesignations "") 
;; (def refseq "NC_000014.9")
;; (def start "23424148") 
;; (def stop "23424148") 
;; (def ref "T")
;; (def alt "C")


;; (deftest test-get-interpretation
;;  (testing "Test interpretation id"
;;    (is (= id (form/interp-id i)))
;;    (is (= significance (form/interp-significance t i)))
;;    (is (= eval-date (form/interp-eval-date t i)))
;;    (print :id id "\n":significance significance "\n":eval-date eval-date"\n")))

;; (deftest test-get-condition
;;   (testing "If conditions are retrieved correctly"
;;   (is (= name (form/condition-name t c)))
;;   (is (= idtype (form/condition-idtype t c)))
;;   (is (= idvalue (form/condition-idvals t c)))
;;   (is (= moi (form/condition-moi t c)))
;;   (print :name "" "\n":idtype idtype "\n":idvalue idvalue"\n":moi moi"\n")
;;   (print "---------------------------------------------------------\n")))

;; (deftest test-get-met-evidence
;;   (testing "if evidence met"
;;   (is (= summary (form/evidence-summary t e)))
;;   (is (= rules (form/evidence-rules t e)))
;;   (is (= pmid-citations (form/evidence-pmid-citations t e)))
;;   (print :summary summary "\n":rules rules "\n":pmid-citations pmid-citations"\n")
;;   (print "---------------------------------------------------------\n")))

;; (deftest test-get-variant
;;   (testing "If variants are retrieved correctly"
;;   (is (= variantIdentifier (form/variant-identifier v)))
;;   (is (= altDesignations (form/variant-alt-designations v)))
;;   (is (= refseq (form/variant-refseq t v)))
;;   (is (= start (form/variant-start t v)))
;;   (is (= stop (form/variant-stop t v)))
;;   (is (= ref (form/variant-ref t v)))
;;   (is (= alt (form/variant-alt v)))
;;   (print :variantIdentifier variantIdentifier "\n":altDesignations altDesignations "\n":refseq refseq"\n")
;;   (print "---------------------------------------------------------\n")))
  
;; (run-tests)
