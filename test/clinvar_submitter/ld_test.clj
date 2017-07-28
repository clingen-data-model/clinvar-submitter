(ns clinvar-submitter.ld-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clinvar-submitter.ld :refer :all]))

(def t (generate-symbol-table "data/dmwg2.json" "data/context-new.jsonld"))
(def node  {"id" "_:b216", "type" "AlleleFrequency", "ascertainment" {"id" "_:b217"}, "alleleCount" 0, "alleleNumber" 0, "homozygousAlleleIndividualCount" 0, "population" {"id" "_:b218"}, "allele" "http://reg.genome.network/allele/CA012832", "contribution" {"id" "_:b219"}},)
  
(deftest generate-symbol-table-test
(testing "Test if symbol table is generated from jason input files"  
(is (= t (generate-symbol-table "data/dmwg2.json" "data/context-new.jsonld"))
(println "Display symbol table "))))

(deftest resolve-id-test
 (testing "Test if an identifier is in symbol table"
 (is (= node
 (resolve-id t "_:b216"))
 (println "resolve-id-test result: identifier is in symbol table"))))

(deftest prop-test
(testing "Test if it filter ressults where a specific key (or recursive chain of keys}is equal to value"
(is (= fn? (prop= t "id" "_:b216"))
(println "prop-test result: "))))
           
(deftest ld-get-test
(testing "Test if a map key value is in symbol table"
(is (= nil (ld-get t "contribution" {"id" "_:b219"}))
(println "ld-get-test result: "))))

(deftest ld-test
(testing "Test if a location, and a set of predicates of a,return the set of nodes mapping to a given property " 
(is (= fn?
(ld-> t "_:b592"  "{"id" "_:b592", "type" "Conservation", "allele" "http://reg.genome.network/allele/CA012832", "algorithm" "phastconsp7way", "score" "1", "contribution" {"id" "_:b593"}}"))
(println "ld-test result: "))))

(deftest ld1-test
(testing "Test if a location, and a set of predicates of a,return the set of nodes mapping to a given property " 
(is (= nil
(ld-> t "VariantInterpretation" "type"))
(println "ld1-test result:" ld-> t "VariantInterpretation" "type"))))



