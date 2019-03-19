(ns clinvar-submitter.ld-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clinvar-submitter.form :as form]
            [clinvar-submitter.ld :refer :all])
  (:import  [com.github.jsonldjava.core JsonLdProcessor JsonLdOptions]
            [com.github.jsonldjava.utils JsonUtils]
            [java.util Map List]))

(def interp-path "data/allinput.json")
(def context-path "data/sepio_context.jsonld")
(def t (generate-symbol-table interp-path context-path))
(def node  {"has_evidence_item" "/evaluations/35b77b38-94f6-44f0-8179-85c912e88e8e/", "id" "_:b404", "evidence_has_strength" "SEPIO:0000329", "type" "evidence line"})
(def m (vals t))
(def interps ((prop= t "variant pathogenicity interpretation" "type") m))
(def i (first interps))
(def v (get i "has_evidence_line"))

(deftest generate-symbol-table-test
 (testing "Test if symbol table is generated from jason input files"
  (is (= t (generate-symbol-table interp-path context-path)))
  (println "SYMBOL TABLE")
  (json/pprint (take 1 t))
  (println "END SYMBOL TEST")))

(deftest resolve-id-test
 (testing "Test if an identifier is in symbol table"
  (is (= node (resolve-id t "_:b404")))
  (println "resolve-id-test result: identifier is in symbol table")))

(deftest prop-test
 (testing "Test if it filter ressults where a specific key (or recursive chain of keys is equal to value"
  (is (= interps ((prop= t "variant pathogenicity interpretation" "type") m)))
  (println "prop-test result:")
  (json/pprint(take 1 interps))
  (println "prop-test ended")))

(deftest ld-get-test "Condition met: string?"
 (testing "Test if a single record map key value is in symbol table is the correct type"
  (is (= "variant pathogenicity interpretation"
         (ld-get t i "type")))
  (println "ld-get-test1 result: ")))

(deftest ld-get-test "(Condition met: instance? List loc (interps))"
 (testing "Test if a map key values in symbol table are all of the correct type"
  (is (= '("variant pathogenicity interpretation")
         (distinct (ld-get t interps "type"))))
  (println "ld-get-test2 result: "
           (take 1 (ld-get t interps "type")))))

(deftest ld-get-test3 "(Condition met:(instance? Map v))"
 (testing "Test if a map key value is in symbol table"
  (is (= 49 (count (ld-get t interps "asserted_conclusion"))))
  (println "ld-get-test3 result: Achieved expected result")))

(deftest ld-test
 (testing "Test if a location, and a set of predicates of a,return the set of nodes mapping to a given property "
  (is (= {"id" "LOINC:LA6675-8","label" "Benign"}
         (ld1-> t i "asserted_conclusion")))
  (println "ld-test result: Achieved expected result")))

(deftest ld1-test
 (testing "Test if a location, and a set of predicates of a,return the set of nodes mapping to a given property "
  (is (= {"has_evidence_item" "/evaluations/83d330cd-bbc9-45b9-8e86-99766e75eb78/",
          "id" "_:b1100",
          "evidence_has_strength" "SEPIO:0000325",
          "type" "evidence line"}
         (ld1-> t i "has_evidence_line")))
  (println "ld1-test result: Achieved expected result")))

(run-tests)
