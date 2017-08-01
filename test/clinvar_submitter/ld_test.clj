(ns clinvar-submitter.ld-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clinvar-submitter.form :as form] 
            [clinvar-submitter.ld :refer :all])
  (:import  [com.github.jsonldjava.core JsonLdProcessor JsonLdOptions]
            [com.github.jsonldjava.utils JsonUtils]
            [java.util Map List]))

(def t (generate-symbol-table "data/dmwg2.json" "data/context-new.jsonld"))
(def node  {"id" "_:b404","type" "Conservation","allele" "http://reg.genome.network/allele/CA012832","algorithm" "phastconsp7way","score" "1","contribution" {"id" "_:b405"}})
(def m (vals t))
(def interps ((prop= t "VariantInterpretation" "type") m))
(def i (first interps))
(def v (get i "evidence"))
(def vn (get v "id"))

(deftest generate-symbol-table-test
(testing "Test if symbol table is generated from jason input files"  
(is (= t (generate-symbol-table "data/dmwg2.json" "data/context-new.jsonld")))
(println "SYMBOL TABLE")
(json/pprint (take 5 t))))

(deftest resolve-id-test
(testing "Test if an identifier is in symbol table"
(is (= node (resolve-id t "_:b404")))
(println "resolve-id-test result: identifier is in symbol table")))

(deftest prop-test
(testing "Test if it filter ressults where a specific key (or recursive chain of keys is equal to value"
(is (= interps ((prop= t "VariantInterpretation" "type") m)))
(println "prop-test result:")
(json/pprint(take 5 interps))))
           
(deftest ld-get-test "Condition met: string?"
(testing "Test if a map key value is in symbol table"
(is (= "VariantInterpretation" (ld-get t i "type")))
(println "ld-get-test1 result: ")))

(deftest ld-get-test "(Condition met: instance? List loc (interps))"
(testing "Test if a map key value is in symbol table"
(is (= ["VariantInterpretation"] (ld-get t interps "type")))
(println "ld-get-test2 result: " (ld-get t interps "type"))))

(deftest ld-get-test3 "(Condition met:(instance? Map v))"
(testing "Test if a map key value is in symbol table"
(is (= [{"id" "http://loinc.org/LA6668-3","type" "Coding","code" "LA6668-3", "system" "http://loinc.org/","display" "Pathogenic"}] (ld-get t interps "clinicalSignificance")))
(println "ld-get-test3 result: Achieved expected result")))

(deftest ld-test
(testing "Test if a location, and a set of predicates of a,return the set of nodes mapping to a given property " 
(is (= {"id" "http://loinc.org/LA6668-3","type" "Coding","code" "LA6668-3", "system" "http://loinc.org/","display" "Pathogenic"} (ld-> t i "clinicalSignificance")))
(println "ld-test result: Achieved expected result")))

(deftest ld1-test
(testing "Test if a location, and a set of predicates of a,return the set of nodes mapping to a given property " 
(is (= {"id" "_:b0", "type" "EvidenceLine", "information" {"id" "/evaluations/75000a01-d5ca-4d2e-b17e-1c93f933c9b9/"}, "evidenceStrength" {"id" "_:b147"}} (ld1-> t i "evidence")))
(println "ld1-test result:Achieved expected result")))

(run-tests)

