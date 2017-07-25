(ns clinvar-submitter.ld-test
  (:require [clojure.test :refer :all]
            [clinvar-submitter.ld :refer :all]))

(deftest resolve-id-test
  (testing "Test if an identifier is in symbol table"
           (let [t (read-ld "data/test_interp_1.dmwg.json" "data/context-new.jsonld")]
           (is (= {"id" "https://www.ncbi.nlm.nih.gov/pubmed/12501224", "type" "InformationSource"}
                  (resolve-id t "https://www.ncbi.nlm.nih.gov/pubmed/12501224"))
                  (println " Identifier is in symbol table")))))

(deftest ld-get-test
  (testing "Test if an map key value is in symbol table"
  (let [t (read-ld "data/test_interp_1.dmwg.json" "data/context-new.jsonld")]
  (is (= nil
         (ld-get t "id" "https://www.ncbi.nlm.nih.gov/pubmed/12501224"))
         (println " Map is in symbol table")))))