(ns clinvar-submitter.ld-test
  (:require [clojure.test :refer :all]
            [clinvar-submitter.ld :refer :all]))

(deftest read-ld-test
  (testing "Test if an identifier is in symbol table"
           (let [t (read-ld "data/test_interp_1.dmwg.json" "data/context-new.jsonld")]
           (is (= "http://reg.genome.network/allele/CA229854"
           (resolve-id t "http://reg.genome.network/allele/CA229854")
                   (println " Identifier is in symbol table"))))))

(deftest ld-get-test
  (testing "Test if an map key value is in symbol table"
  (let [t (read-ld "data/test_interp_1.dmwg.json" "data/context-new.jsonld")]
  (is (= t
         (ld-get t "cg:A156" "cg:A158")
         (println " Map is in symbol table"))))))